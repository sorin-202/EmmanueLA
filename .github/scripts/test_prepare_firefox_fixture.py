"""Host checks for narrowly scoped Android fixture recovery; no device required."""
import importlib.util
import contextlib
import io
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET

SPEC = importlib.util.spec_from_file_location("firefox_fixture", Path(__file__).with_name("prepare-firefox-fixture.py"))
FIXTURE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(FIXTURE)


def dialog(title="Quickstep isn't responding", package="android", enabled="true"):
    root = ET.Element("hierarchy")
    ET.SubElement(root, "node", {"package": package, "resource-id": "android:id/alertTitle", "text": title})
    ET.SubElement(root, "node", {"package": package, "resource-id": "android:id/aerr_wait", "text": "Wait",
                                "clickable": "true", "enabled": enabled, "bounds": "[70,996][1010,1122]"})
    return root


class QuickstepRecoveryTest(unittest.TestCase):
    def test_observed_quickstep_dialog_selects_android_wait(self):
        control = FIXTURE.quickstep_anr_wait_control(dialog(), already_waited=False)
        self.assertEqual("android:id/aerr_wait", control.attrib["resource-id"])
        self.assertEqual((540, 1059), FIXTURE.bounds(control))

    def test_other_android_anrs_are_failures(self):
        for title in ("Firefox isn't responding", "System UI isn't responding"):
            with self.subTest(title=title), self.assertRaisesRegex(RuntimeError, "Unexpected Android failure"):
                FIXTURE.quickstep_anr_wait_control(dialog(title=title), already_waited=False)

    def test_repeated_quickstep_anr_is_a_failure(self):
        with self.assertRaisesRegex(RuntimeError, "repeated"):
            FIXTURE.quickstep_anr_wait_control(dialog(), already_waited=True)

    def test_browser_content_cannot_impersonate_android_recovery(self):
        self.assertIsNone(FIXTURE.quickstep_anr_wait_control(dialog(package="org.mozilla.firefox"), already_waited=False))

    def test_disabled_wait_is_not_tapped(self):
        with self.assertRaisesRegex(RuntimeError, "no unique enabled"):
            FIXTURE.quickstep_anr_wait_control(dialog(enabled="false"), already_waited=False)


class CurrentUiSnapshotTest(unittest.TestCase):
    def run_preflight(self, *, null_root=True, missing_dump=False, clock=None):
        calls = []
        dumps = 0
        hierarchy = (
            f'<hierarchy><node package="{FIXTURE.PACKAGE}" resource-id="ADDRESSBAR_URL_BOX" '
            f'text="{FIXTURE.URL}" class="android.widget.TextView" focused="false" '
            'bounds="[0,0][100,100]" /></hierarchy>'
        )

        def adb(command, **kwargs):
            nonlocal dumps
            parts = command[3:]
            calls.append(parts)
            stdout, stderr = "", ""
            if parts[:2] == ["shell", "getprop"]:
                stdout = "1\n"
            elif parts[:3] == ["shell", "dumpsys", "package"]:
                stdout = "versionName=157.0.1\n"
            elif parts[:4] == ["shell", "cmd", "package", "resolve-activity"]:
                stdout = FIXTURE.PACKAGE + "/org.mozilla.fenix.IntentReceiverActivity\n"
            elif parts[:3] == ["shell", "uiautomator", "dump"]:
                dumps += 1
                if dumps > 4:
                    raise AssertionError("Preflight did not use the available hierarchy")
                if null_root and dumps == 1:
                    stderr = FIXTURE.NULL_UI_ROOT + "\n"
                else:
                    stdout = "UI hierchary dumped to: " + parts[-1] + "\n"
            elif parts[:2] == ["exec-out", "cat"]:
                stdout = "cat: " + parts[-1] + ": No such file or directory\n" if missing_dump else hierarchy
            elif parts[:2] == ["exec-out", "screencap"]:
                stdout = "fixture screenshot"
            return subprocess.CompletedProcess(command, 0, stdout.encode(), stderr.encode())

        with tempfile.TemporaryDirectory() as directory:
            argv = ["fixture", "--serial", "fixture-device", "--artifacts", directory]
            clock_patch = patch.object(FIXTURE.time, "monotonic", side_effect=clock) if clock else patch.object(FIXTURE.time, "monotonic", return_value=0)
            with patch.object(FIXTURE.sys, "argv", argv), patch.object(FIXTURE.subprocess, "run", side_effect=adb), \
                    patch.object(FIXTURE.time, "sleep"), clock_patch, contextlib.redirect_stdout(io.StringIO()):
                status = FIXTURE.main()
            result = json.loads((Path(directory) / "result.json").read_text())
            pages = sorted(path.name for path in Path(directory).glob("page-*.xml"))
        return status, result, calls, pages

    def test_success_status_with_null_root_on_stderr_waits_for_real_current_ui(self):
        status, result, calls, pages = self.run_preflight()
        self.assertEqual(0, status)
        self.assertTrue(result["verified"])
        self.assertEqual([{"index": 0, "reason": FIXTURE.NULL_UI_ROOT}], result["unavailable_ui_snapshots"])
        self.assertEqual(2, sum(parts[:2] == ["exec-out", "cat"] for parts in calls))
        self.assertEqual(["page-01.xml", "page-02.xml"], pages)
        self.assertEqual(FIXTURE.URL, result["address_control"]["text"])

    def test_null_root_cannot_verify_fixture_or_extend_existing_deadline(self):
        status, result, calls, pages = self.run_preflight(clock=[0, 0, 181])
        self.assertEqual(1, status)
        self.assertFalse(result["verified"])
        self.assertIn("Bounded preflight did not reach", result["error"])
        self.assertFalse(any(parts[:2] == ["exec-out", "cat"] for parts in calls))
        self.assertEqual([], pages)

    def test_missing_dump_with_successful_adb_status_is_a_failure(self):
        status, result, _, _ = self.run_preflight(null_root=False, missing_dump=True)
        self.assertEqual(1, status)
        self.assertFalse(result["verified"])
        self.assertIn("did not produce a valid current UI hierarchy", result["error"])

    def test_unrelated_uiautomator_errors_are_not_recovered(self):
        with self.assertRaisesRegex(RuntimeError, "could not obtain current UI"):
            FIXTURE.ui_snapshot_available("", "ERROR: Unable to connect to automation bridge")
        with self.assertRaisesRegex(RuntimeError, "could not obtain current UI"):
            FIXTURE.ui_snapshot_available(FIXTURE.NULL_UI_ROOT, "ERROR: unexpected second failure")

    def test_xml_without_android_hierarchy_is_a_failure(self):
        with self.assertRaisesRegex(RuntimeError, "unexpected root"):
            FIXTURE.parse_ui_hierarchy("<error />")


if __name__ == "__main__":
    unittest.main()
