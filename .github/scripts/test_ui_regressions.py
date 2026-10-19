"""Exercise the real CI shell entry point with controlled Gradle/ADB outcomes."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).with_name("run-ui-regressions.sh")


class UiRegressionsTest(unittest.TestCase):
    def run_fixture(self, gradle_status, pull_status):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "gradlew").write_text(
                '#!/bin/sh\nprintf "%s\\n" "$@" > gradle-arguments\nexit "$FIXTURE_GRADLE_STATUS"\n'
            )
            (root / "adb").write_text(
                '#!/bin/sh\nprintf "%s\\n" "$@" > adb-arguments\nexit "$FIXTURE_PULL_STATUS"\n'
            )
            for name in ("gradlew", "adb"):
                (root / name).chmod(0o755)
            env = dict(os.environ, PATH=str(root) + os.pathsep + os.environ["PATH"],
                       FIXTURE_GRADLE_STATUS=str(gradle_status), FIXTURE_PULL_STATUS=str(pull_status))
            result = subprocess.run(["bash", str(SCRIPT)], cwd=root, env=env,
                                    capture_output=True, text=True, check=False)
            self.assertEqual((root / "gradle-arguments").read_text().splitlines(),
                             [":app:connectedDebugAndroidTest", "--stacktrace", "--max-workers=2"])
            self.assertEqual((root / "adb-arguments").read_text().splitlines(),
                             ["pull", "/sdcard/Android/data/com.emmanuela.launcher/files/ux-screenshots",
                              "app/build/reports/androidTests/"])
            self.assertTrue((root / "app/build/reports/androidTests").is_dir())
            return result.returncode

    def test_passing_suite_and_capture_pass(self):
        self.assertEqual(self.run_fixture(0, 0), 0)

    def test_missing_captures_fail_a_passing_suite(self):
        self.assertEqual(self.run_fixture(0, 1), 1)

    def test_failed_suite_keeps_its_status_and_still_collects_evidence(self):
        self.assertEqual(self.run_fixture(7, 0), 7)
        self.assertEqual(self.run_fixture(7, 1), 7)


if __name__ == "__main__":
    unittest.main()
