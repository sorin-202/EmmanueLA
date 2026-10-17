"""Host checks for narrowly scoped Android fixture recovery; no device required."""
import importlib.util
from pathlib import Path
import unittest
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


if __name__ == "__main__":
    unittest.main()
