#!/usr/bin/env python3
"""Prepare an official Firefox fixture using only observed Android UI controls.

This is setup, not an enforcement test. It does not enable EmmanueLA Accessibility,
modify Firefox preferences, access accounts, or alter the Android HOME role.
"""
import argparse
import json
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from urllib.parse import urlsplit
from pathlib import Path

PACKAGE = "org.mozilla.firefox"
URL = "https://example.com/focus-fixture"
FIELD_IDS = {
    "ADDRESSBAR_URL_BOX", f"{PACKAGE}:id/ADDRESSBAR_URL_BOX",
    f"{PACKAGE}:id/mozac_browser_toolbar_url_view",
    f"{PACKAGE}:id/mozac_browser_toolbar_edit_url_view", f"{PACKAGE}:id/url_bar",
}
# Decline optional account, promotion and notification prompts before advancing.
LABELS = (
    "Dismiss", "No Thanks", "No thanks", "Not now", "Skip", "Agree and start browsing",
    "Continue browsing", "Done", "Continue",
)
PAGE_CLASSES = {"android.webkit.WebView", "org.mozilla.geckoview.GeckoView", "org.mozilla.gecko.GeckoView"}
NULL_UI_ROOT = "ERROR: null root node returned by UiTestAutomationBridge."


def ui_snapshot_available(stdout, stderr):
    """ADB can exit successfully while UIAutomator reports a transient null root."""
    errors = [line.strip() for output in (stdout, stderr) for line in output.splitlines()
              if "ERROR:" in line]
    if errors == [NULL_UI_ROOT]:
        return False
    if errors:
        raise RuntimeError(f"UIAutomator could not obtain current UI: {'; '.join(errors)}")
    return True


def parse_ui_hierarchy(xml):
    """Reject a failed `adb exec-out cat` even when ADB returns status zero."""
    try:
        root = ET.fromstring(xml)
    except ET.ParseError as error:
        raise RuntimeError(f"UIAutomator did not produce a valid current UI hierarchy: {error}") from error
    if root.tag != "hierarchy":
        raise RuntimeError(f"UIAutomator returned an unexpected root: {root.tag!r}")
    return root


def bounds(node):
    values = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
    if not values:
        return None
    x1, y1, x2, y2 = map(int, values.groups())
    return ((x1+x2)//2, (y1+y2)//2) if x2 > x1 and y2 > y1 else None


def quickstep_anr_wait_control(root, already_waited):
    """Allow one observed Quickstep startup ANR; reject other or repeated ANRs."""
    system_nodes = [node for node in root.iter("node") if node.attrib.get("package") == "android"]
    if not any(node.attrib.get("resource-id") in {"android:id/aerr_wait", "android:id/aerr_close"}
               for node in system_nodes):
        return None
    titles = [node.attrib.get("text") for node in system_nodes
              if node.attrib.get("resource-id") == "android:id/alertTitle"]
    if titles != ["Quickstep isn't responding"]:
        raise RuntimeError(f"Unexpected Android failure dialog during Firefox setup: {titles!r}")
    if already_waited:
        raise RuntimeError("Quickstep ANR repeated after the single recorded Wait recovery")
    controls = [node for node in system_nodes if node.attrib.get("resource-id") == "android:id/aerr_wait"
                and node.attrib.get("text") == "Wait" and node.attrib.get("clickable") == "true"
                and node.attrib.get("enabled") == "true" and bounds(node)]
    if len(controls) != 1:
        raise RuntimeError("Observed Quickstep ANR has no unique enabled Android Wait control")
    return controls[0]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--serial", required=True)
    parser.add_argument("--artifacts", default="build/firefox-preflight")
    parser.add_argument("--max-seconds", type=int, default=180)
    parser.add_argument("--max-pages", type=int, default=16)
    parser.add_argument("--expected-version", default="157.0.1")
    args = parser.parse_args()
    artifacts = Path(args.artifacts)
    artifacts.mkdir(parents=True, exist_ok=True)
    transcript = []
    record = {"package": PACKAGE, "serial": args.serial, "url": URL, "verified": False, "steps": [],
              "quickstep_anr_recovery": {"wait_attempts": 0, "outcome": "not_needed"}}
    prefix = [args.adb, "-s", args.serial]

    def command(*parts, binary=False, check=True, timeout=30):
        result = subprocess.run(prefix + list(parts), stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
        transcript.append({"command": list(parts), "returncode": result.returncode,
                           "stdout": "<binary>" if binary else result.stdout.decode(errors="replace"),
                           "stderr": result.stderr.decode(errors="replace")})
        if check and result.returncode:
            raise RuntimeError(f"ADB command failed: {parts!r}: {result.stderr.decode(errors='replace')}")
        return result.stdout if binary else result.stdout.decode(errors="replace").strip()

    def screenshot(name):
        try:
            (artifacts / f"{name}.png").write_bytes(command("exec-out", "screencap", "-p", binary=True, timeout=20))
        except Exception as error:
            record["screenshot_error"] = str(error)

    def snapshot(index):
        remote = "/sdcard/emmanuela-firefox-preflight.xml"
        command("shell", "rm", "-f", remote)
        output = command("shell", "uiautomator", "dump", remote)
        stderr = transcript[-1]["stderr"]
        if not ui_snapshot_available(output, stderr):
            record.setdefault("unavailable_ui_snapshots", []).append({"index": index, "reason": NULL_UI_ROOT})
            return None
        xml = command("exec-out", "cat", remote)
        (artifacts / f"page-{index:02d}.xml").write_text(xml)
        return parse_ui_hierarchy(xml)

    def chrome_nodes(root):
        def walk(node):
            if node.attrib.get("class") in PAGE_CLASSES:
                return
            yield node
            for child in node:
                yield from walk(child)
        return list(walk(root))

    try:
        if command("shell", "getprop", "sys.boot_completed") != "1":
            raise RuntimeError("Android has not completed boot")
        package_info = command("shell", "dumpsys", "package", PACKAGE)
        version = re.search(r"versionName=([^\s]+)", package_info)
        if not version:
            raise RuntimeError("Official Firefox is not installed")
        record["version"] = version.group(1)
        if record["version"] != args.expected_version:
            raise RuntimeError(f"Unexpected Firefox version: {record['version']!r}, expected {args.expected_version!r}")
        command("shell", "cmd", "role", "add-role-holder", "--user", "0", "android.app.role.BROWSER", PACKAGE)
        # Android 10 supports add-role-holder but not get-role-holders. Verify the
        # observable result through the package resolver, which is available on
        # all supported fixture APIs and also detects an unresolved chooser.
        resolved = command("shell", "cmd", "package", "resolve-activity", "--brief", "--user", "0",
                           "-a", "android.intent.action.VIEW", "-c", "android.intent.category.BROWSABLE", "-d", URL)
        if not any(line.strip().startswith(PACKAGE + "/") for line in resolved.splitlines()):
            raise RuntimeError(f"Firefox did not resolve the default HTTPS intent: {resolved!r}")
        record["browser_role_resolver"] = resolved
        command("shell", "am", "start", "-W", "-a", "android.intent.action.MAIN", "-c", "android.intent.category.LAUNCHER",
                "-n", f"{PACKAGE}/org.mozilla.firefox.App")
        deadline = time.monotonic() + args.max_seconds
        opened_url = False
        quickstep_waited = False
        steps = 0
        index = 0
        while time.monotonic() < deadline and steps < args.max_pages:
            root = snapshot(index)
            index += 1
            if root is None:
                # Keep the original preflight deadline. No address, control or
                # success may be inferred from an unavailable hierarchy.
                time.sleep(1)
                continue
            wait_control = quickstep_anr_wait_control(root, quickstep_waited)
            if wait_control is not None:
                screenshot("observed-quickstep-anr")
                x, y = bounds(wait_control)
                record["quickstep_anr_recovery"] = {"wait_attempts": 1, "outcome": "wait_requested"}
                command("shell", "input", "tap", str(x), str(y))
                record["quickstep_anr_recovery"]["outcome"] = "wait_tapped"
                quickstep_waited = True
                record["steps"].append({"action": "single observed Quickstep ANR Wait recovery",
                                        "title": "Quickstep isn't responding", "bounds": wait_control.attrib["bounds"]})
                print("::warning::Firefox fixture encountered Quickstep ANR; tapped its observed Wait control once. Recovery is recorded in result.json.", flush=True)
                steps += 1
                time.sleep(1)
                continue
            nodes = chrome_nodes(root)
            parents = {child: parent for parent in root.iter() for child in parent}
            chosen = None
            label = None
            for label in LABELS:
                for node in nodes:
                    if node.attrib.get("package") != PACKAGE:
                        continue
                    if label not in (node.attrib.get("text"), node.attrib.get("content-desc")):
                        continue
                    control = node
                    while control is not None and control.attrib.get("clickable") != "true":
                        control = parents.get(control)
                    if control is not None and control.attrib.get("enabled", "true") == "true" and bounds(control):
                        chosen = control
                        break
                if chosen is not None:
                    break
            if chosen is not None:
                x, y = bounds(chosen)
                command("shell", "input", "tap", str(x), str(y))
                record["steps"].append({"label": label, "bounds": chosen.attrib["bounds"]})
                steps += 1
                time.sleep(1)
                continue
            fields = [node for node in nodes if node.attrib.get("package") == PACKAGE and node.attrib.get("resource-id") in FIELD_IDS]
            for field in fields:
                value = " ".join((field.attrib.get("text", ""), field.attrib.get("content-desc", "")))
                editing = field.attrib.get("focused") == "true" and field.attrib.get("class") == "android.widget.EditText"
                address_match = False
                for token in re.split(r"[\s,;]+", value.strip()):
                    try:
                        parsed = urlsplit(token if "://" in token else "https://" + token)
                        address_match |= parsed.scheme.lower() in ("http", "https") and parsed.hostname is not None and parsed.hostname.rstrip(".").lower() == "example.com"
                    except ValueError:
                        pass
                if opened_url and address_match and not editing and bounds(field):
                    record["verified"] = True
                    if quickstep_waited:
                        record["quickstep_anr_recovery"]["outcome"] = "fixture_verified_after_wait"
                    record["address_control"] = {key: field.attrib.get(key) for key in ("resource-id", "text", "content-desc", "class", "bounds")}
                    screenshot("success")
                    return 0
            packages = {node.attrib.get("package") for node in nodes}
            if not opened_url and PACKAGE in packages:
                command("shell", "am", "start", "-W", "-a", "android.intent.action.VIEW", "-d", URL, "-p", PACKAGE)
                opened_url = True
                record["steps"].append({"action": "explicit ACTION_VIEW preflight"})
                time.sleep(1)
                continue
            if packages & {"com.android.permissioncontroller", "com.google.android.permissioncontroller"}:
                activity = command("shell", "dumpsys", "activity", "activities")
                if "RequestRoleActivity" in activity:
                    command("shell", "input", "keyevent", "KEYCODE_BACK")
                    record["steps"].append({"action": "dismiss exact default-browser role request"})
                    steps += 1
                    continue
            record["last_packages"] = sorted(package for package in packages if package)
            record["last_visible_chrome_labels"] = [node.attrib.get("text") or node.attrib.get("content-desc") for node in nodes
                                                    if node.attrib.get("package") == PACKAGE and (node.attrib.get("text") or node.attrib.get("content-desc"))]
            time.sleep(1)
        raise RuntimeError("Bounded preflight did not reach an unedited example.com address control")
    except Exception as error:
        record["error"] = str(error)
        if record["quickstep_anr_recovery"]["wait_attempts"]:
            record["quickstep_anr_recovery"]["outcome"] = "fixture_failed_after_wait"
        screenshot("failure")
        try:
            (artifacts / "failure-activity.txt").write_text(command("shell", "dumpsys", "activity", "activities", timeout=20))
            (artifacts / "failure-logcat.txt").write_text(command("shell", "logcat", "-d", "-t", "200", timeout=20))
        except Exception as capture_error:
            record["capture_error"] = str(capture_error)
        return 1
    finally:
        (artifacts / "result.json").write_text(json.dumps(record, indent=2))
        (artifacts / "commands.json").write_text(json.dumps(transcript, indent=2))
        print(json.dumps(record, indent=2), flush=True)


if __name__ == "__main__":
    sys.exit(main())

