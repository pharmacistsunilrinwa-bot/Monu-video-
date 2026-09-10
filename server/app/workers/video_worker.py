from pathlib import Path

from app.video.pipeline import VideoPipeline
from app.video.transformer import VideoTransformer
from app.verification.triple_check import TripleVerification


class VideoWorker:
    def __init__(self, project_root):
        self.root = Path(project_root)
        self.pipeline = VideoPipeline(self.root)
        self.transformer = VideoTransformer(self.root)
        self.verifier = TripleVerification()

    def process(
        self,
        source,
        output,
        chunk_seconds=300,
        cartoon=False,
        replacement_audio=None,
        change_audio=False,
        pitch=1.0,
        volume=1.0,
    ):
        source = Path(source)
        output = Path(output)

        duration = self.pipeline.probe(source)

        if duration > 3600:
            raise ValueError(
                "Video exceeds the maximum supported duration of 1 hour."
            )

        chunks = self.pipeline.split(source, chunk_seconds)

        processed_chunks = []

        for index, chunk in enumerate(chunks):
            current = chunk

            if cartoon:
                cartoon_output = (
                    self.pipeline.processed
                    / f"{chunk.stem}_cartoon.mp4"
                )
                current = self.transformer.cartoon(
                    current,
                    cartoon_output,
                )

            if replacement_audio is not None:
                audio_output = (
                    self.pipeline.processed
                    / f"{chunk.stem}_audio.mp4"
                )
                current = self.transformer.replace_audio(
                    current,
                    replacement_audio,
                    audio_output,
                )

            elif change_audio:
                changed_output = (
                    self.pipeline.processed
                    / f"{chunk.stem}_voice.mp4"
                )
                current = self.transformer.change_audio(
                    current,
                    changed_output,
                    pitch=pitch,
                    volume=volume,
                )

            processed_chunks.append(current)

        final = self.pipeline.join(
            processed_chunks,
            output,
        )

        verification = self.verifier.verify(
            source,
            final,
        )

        if not verification["passed"]:
            raise RuntimeError(
                "Triple verification failed."
            )

        return {
            "input": str(source),
            "output": str(final),
            "chunks": len(chunks),
            "duration": duration,
            "cartoon": cartoon,
            "replacement_audio": (
                str(replacement_audio)
                if replacement_audio is not None
                else None
            ),
            "change_audio": change_audio,
            "pitch": pitch,
            "volume": volume,
            "verification": verification,
        }
