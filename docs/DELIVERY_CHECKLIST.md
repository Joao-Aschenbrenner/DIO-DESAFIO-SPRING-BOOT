# Budget AI v0.3.5 — Delivery Checklist

## DIO challenge flow
- [x] Spring Boot application
- [x] Spring AI ChatClient
- [x] Tool Calling wired to real Java use cases
- [x] Audio upload
- [x] Audio transcription
- [x] Create/query persisted transactions
- [x] Final text response
- [x] Optional Spring AI TextToSpeechModel returning MP3
- [x] Local speech fallback when cloud TTS is not configured

## Robustness
- [x] Domain invariants apply to REST and Tool Calling
- [x] Exact category contract shared with UI
- [x] Optional list category is marked optional in tool schema
- [x] Blank model/transcription responses handled
- [x] Provider errors sanitized and correlated
- [x] Persistence, media type, method, missing input and validation errors mapped
- [x] Audio size/format validation
- [x] Temporary upload URL must be HTTPS and non-local
- [x] H2 Console disabled by default
- [x] Server bound to 127.0.0.1

## UI/UX
- [x] Voice flow is the primary action
- [x] Five challenge stages are explained visually
- [x] Human-readable transaction cards instead of raw JSON
- [x] Manual amount entered in BRL
- [x] Technical diagnostics are collapsible
- [x] Human-first error messages with technical details on demand
- [x] Responsive breakpoints for desktop/tablet/mobile
- [x] No fixed 300px minimum grid card that can force horizontal overflow

## Windows release
- [x] Java 21 bundled
- [x] Custom Budget AI .ico supplied to jpackage
- [x] Launcher does not persist API keys
- [x] NVIDIA key required; TTS key optional
- [x] Startup diagnostics and log access
- [x] CI/tests run before installer packaging

Final release remains a prerelease until the installer is validated manually on Windows.
