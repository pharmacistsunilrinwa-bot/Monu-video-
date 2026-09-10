# Monu Video AI

Owner: Sunil Rinwa
Assistant: Monu

## Primary interface

The Android application is chat-first.

Greeting:

"What can I help with, Sunil?"

## Core architecture

Monu Brain
- intent analysis
- model selection
- tool selection
- execution planning
- result validation
- verification

## Video

Maximum intended input duration:
1 hour

Pipeline:

upload
 -> probe
 -> split
 -> process
 -> join
 -> triple verification
 -> final output

## Storage

Persistent SQLite database is used for project state.

Important:
Permanent data should never be automatically deleted by cleanup jobs.

## Status

The Android client should poll the server every 60 seconds
for long-running video jobs.

## API key security

Real API keys must remain on the server.
Never place GEMINI_API_KEY inside the Android APK.

## Development

Run:

./scripts/verify_project.sh

Then:

cd server
./scripts/start.sh
