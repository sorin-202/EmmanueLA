"""Exercise the real CI shell entry point and UTP output after APK uninstall."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).with_name("run-ui-regressions.sh")


class UiRegressionsTest(unittest.TestCase):
    def run_fixture(self, gradle_status, captures_present):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "gradlew").write_text(
                '#!/bin/sh\nprintf "%s\\n" "$@" > gradle-arguments\n'
                'if [ "$FIXTURE_CAPTURES_PRESENT" = yes ]; then\n'
                '  output=app/build/outputs/connected_android_test_additional_output/debugAndroidTest/connected/emulator/ux-screenshots\n'
                '  mkdir -p "$output"\n'
                '  printf "captured display" > "$output/settings-overview.png"\n'
                'fi\n'
                'exit "$FIXTURE_GRADLE_STATUS"\n'
            )
            (root / "gradlew").chmod(0o755)
            env = dict(os.environ, PATH=str(root) + os.pathsep + os.environ["PATH"],
                       FIXTURE_GRADLE_STATUS=str(gradle_status),
                       FIXTURE_CAPTURES_PRESENT="yes" if captures_present else "no")
            result = subprocess.run(["bash", str(SCRIPT)], cwd=root, env=env,
                                    capture_output=True, text=True, check=False)
            self.assertEqual((root / "gradle-arguments").read_text().splitlines(),
                             [":app:connectedDebugAndroidTest", "--stacktrace", "--max-workers=2"])
            self.assertTrue((root / "app/build/reports/androidTests").is_dir())
            archived = list((root / "app/build/reports/androidTests").glob("**/ux-screenshots/*.png"))
            self.assertEqual(len(archived), 1 if captures_present else 0)
            if captures_present:
                self.assertEqual(archived[0].read_text(), "captured display")
            return result.returncode

    def test_passing_suite_and_capture_pass(self):
        self.assertEqual(self.run_fixture(0, True), 0)

    def test_missing_captures_fail_a_passing_suite(self):
        self.assertEqual(self.run_fixture(0, False), 1)

    def test_failed_suite_keeps_its_status_and_still_collects_evidence(self):
        self.assertEqual(self.run_fixture(7, True), 7)
        self.assertEqual(self.run_fixture(7, False), 7)


if __name__ == "__main__":
    unittest.main()
