# Budget AI v0.3.5 — Delivery Candidate

This candidate focuses on making the DIO Spring AI project demonstrable, robust and installable.

## Highlights

- Complete responsive redesign centered on the voice-command challenge flow.
- Human-readable results and transaction list instead of raw JSON.
- Manual values entered in BRL while the API/domain continue using integer cents.
- Exact backend categories loaded by the UI with an embedded safe fallback.
- Tool schema fix: category filtering is optional when listing all transactions.
- Domain validation shared by REST and AI tool calls.
- Broader structured error handling with correlation IDs.
- NVIDIA upload hardening and sanitized provider errors.
- Application bound to localhost; H2 Console disabled by default.
- Optional Spring AI `TextToSpeechModel` endpoint producing MP3 when a TTS key is supplied.
- Web Speech API remains the automatic local fallback.
- Simplified Windows launcher: NVIDIA key required, TTS key optional, no unrelated Codex installer action.
- Java 21 remains bundled and the custom Budget AI icon remains wired into `jpackage`.

## Validation gates

The candidate must pass the repository CI and Windows packaging workflow before the installer is considered ready for manual acceptance testing.
