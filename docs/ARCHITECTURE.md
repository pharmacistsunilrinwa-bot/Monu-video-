# Monu Video AI Architecture

User
 |
 v
Android Chat UI
 |
 v
Monu API
 |
 v
Monu Brain / Orchestrator
 |
 +---- Model Router
 |
 +---- Tool Router
 |
 +---- Video Pipeline
 |       |
 |       +---- FFmpeg
 |       +---- Split
 |       +---- Process
 |       +---- Rejoin
 |
 +---- Memory / Database
 |
 +---- Triple Verification
 |
 v
Final Result

The model is NOT treated as the complete application brain.
Monu's orchestration layer controls routing, tool execution,
state, validation and verification.
