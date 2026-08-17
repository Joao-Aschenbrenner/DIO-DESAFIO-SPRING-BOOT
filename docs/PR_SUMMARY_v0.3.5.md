# PR Summary — v0.3.5 Delivery Candidate

## Objective
Prepare the DIO Spring Boot + Spring AI project for final Windows acceptance testing.

## Main changes
- Rebuilt the web UI around the challenge's voice flow and responsive/mobile use.
- Converted raw JSON-oriented screens into readable transaction/results components.
- Fixed/strengthened Tool Calling contracts and shared domain validation.
- Added optional Spring AI `TextToSpeechModel` MP3 endpoint with local Web Speech fallback.
- Expanded structured error handling and correlation IDs.
- Hardened NVIDIA temporary audio upload handling.
- Restricted the desktop server to localhost and disabled H2 Console by default.
- Simplified and hardened the Windows launcher.
- Updated CI/release workflow for v0.3.5 and kept the custom icon + bundled Java 21.
- Added regression, domain, TTS, UI, configuration and error-contract tests.

## Release gate
Merge only after CI is green. The main-branch Windows workflow will then build and publish `v0.3.5-windows` as a prerelease for manual installation testing.
