import subprocess
from pathlib import Path


class TripleVerification:

    def verify(self, source, output):
        results = []

        results.append(self._check_exists(output))
        results.append(self._check_readable(output))
        results.append(self._check_duration(source, output))

        return {
            "passed": all(results),
            "checks": results
        }

    def _check_exists(self, output):
        return Path(output).exists() and Path(output).stat().st_size > 0

    def _check_readable(self, output):
        try:
            subprocess.run(
                [
                    "ffprobe",
                    "-v", "error",
                    str(output)
                ],
                check=True,
                capture_output=True
            )
            return True
        except Exception:
            return False

    def _check_duration(self, source, output):
        def duration(path):
            result = subprocess.run(
                [
                    "ffprobe",
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    str(path)
                ],
                capture_output=True,
                text=True,
                check=True
            )
            return float(result.stdout.strip())

        try:
            a = duration(source)
            b = duration(output)

            if a == 0:
                return False

            difference = abs(a - b) / a
            return difference < 0.02
        except Exception:
            return False
