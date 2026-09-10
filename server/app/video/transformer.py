import subprocess
from pathlib import Path


class VideoTransformer:
    """FFmpeg-based video transformation engine."""

    def __init__(self, root):
        self.root = Path(root)
        self.processed = self.root / "data" / "processed"
        self.processed.mkdir(parents=True, exist_ok=True)

    def cartoon(self, source, output):
        """
        Create a cartoon-style video using FFmpeg filters.
        Keeps the original audio.
        """
        source = Path(source)
        output = Path(output)

        output.parent.mkdir(parents=True, exist_ok=True)

        filter_graph = (
            "scale=iw:ih:flags=lanczos,"
            "eq=saturation=1.35:contrast=1.15,"
            "unsharp=5:5:1.2:5:5:0.0,"
            "edgedetect=low=0.08:high=0.25,"
            "negate,"
            "negate"
        )

        subprocess.run(
            [
                "ffmpeg",
                "-y",
                "-i", str(source),
                "-vf", filter_graph,
                "-map", "0:v:0",
                "-map", "0:a?",
                "-c:v", "libx264",
                "-preset", "medium",
                "-crf", "20",
                "-c:a", "aac",
                "-b:a", "192k",
                "-movflags", "+faststart",
                str(output),
            ],
            check=True,
        )

        return output

    def replace_audio(self, video, audio, output):
        """
        Replace the video's original audio with a supplied audio file.
        Video is copied without re-encoding.
        """
        video = Path(video)
        audio = Path(audio)
        output = Path(output)

        output.parent.mkdir(parents=True, exist_ok=True)

        subprocess.run(
            [
                "ffmpeg",
                "-y",
                "-i", str(video),
                "-i", str(audio),
                "-map", "0:v:0",
                "-map", "1:a:0",
                "-c:v", "copy",
                "-c:a", "aac",
                "-b:a", "192k",
                "-shortest",
                "-movflags", "+faststart",
                str(output),
            ],
            check=True,
        )

        return output

    def change_audio(self, video, output, pitch=1.0, volume=1.0):
        """
        Change the video's audio pitch/volume while retaining video.
        pitch=1.0 means unchanged.
        """
        video = Path(video)
        output = Path(output)

        if pitch <= 0:
            raise ValueError("pitch must be greater than zero")

        if volume <= 0:
            raise ValueError("volume must be greater than zero")

        # atempo supports 0.5-2.0 per filter instance.
        tempo = pitch
        filters = []

        if tempo < 0.5:
            while tempo < 0.5:
                filters.append("atempo=0.5")
                tempo /= 0.5
        elif tempo > 2.0:
            while tempo > 2.0:
                filters.append("atempo=2.0")
                tempo /= 2.0

        filters.append(f"atempo={tempo}")
        filters.append(f"volume={volume}")

        subprocess.run(
            [
                "ffmpeg",
                "-y",
                "-i", str(video),
                "-map", "0:v:0",
                "-map", "0:a:0?",
                "-c:v", "copy",
                "-af", ",".join(filters),
                "-c:a", "aac",
                "-b:a", "192k",
                "-movflags", "+faststart",
                str(output),
            ],
            check=True,
        )

        return output
