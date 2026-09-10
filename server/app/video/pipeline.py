import subprocess
from pathlib import Path


class VideoPipeline:

    def __init__(self, root):
        self.root = Path(root)
        self.chunks = self.root / "data" / "chunks"
        self.processed = self.root / "data" / "processed"
        self.final = self.root / "data" / "final"

        for directory in [
            self.chunks,
            self.processed,
            self.final
        ]:
            directory.mkdir(parents=True, exist_ok=True)

    def probe(self, source):
        result = subprocess.run(
            [
                "ffprobe",
                "-v", "error",
                "-show_entries",
                "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                str(source)
            ],
            capture_output=True,
            text=True,
            check=True
        )

        return float(result.stdout.strip())

    def split(self, source, chunk_seconds=300):
        source = Path(source)

        pattern = self.chunks / f"{source.stem}_%04d.mp4"

        subprocess.run(
            [
                "ffmpeg",
                "-y",
                "-i", str(source),
                "-map", "0",
                "-c", "copy",
                "-f", "segment",
                "-segment_time", str(chunk_seconds),
                "-reset_timestamps", "1",
                str(pattern)
            ],
            check=True
        )

        return sorted(self.chunks.glob(f"{source.stem}_*.mp4"))

    def join(self, chunks, output):
        list_file = self.final / "concat.txt"

        with list_file.open("w") as f:
            for chunk in chunks:
                f.write(f"file '{Path(chunk).resolve()}'\n")

        subprocess.run(
            [
                "ffmpeg",
                "-y",
                "-f", "concat",
                "-safe", "0",
                "-i", str(list_file),
                "-c", "copy",
                str(output)
            ],
            check=True
        )

        return Path(output)
