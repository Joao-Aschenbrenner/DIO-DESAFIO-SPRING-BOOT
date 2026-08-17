# v0.3.5 Logic and Error Audit

## Bugs/risks found and addressed

1. **Frontend/backend category mismatch** — fixed previously and protected by contract test.
2. **Tool list filter implicitly required** — `listarDespesas` now marks category as optional, so an unfiltered query does not force the model to fabricate a category.
3. **REST-only validation gap** — invariants now also live in `Transaction`, covering calls that arrive through Tool Calling.
4. **Blank AI responses** — command and voice flows fail with a controlled application error instead of returning misleading success.
5. **Blank transcription** — rejected before invoking the chat model.
6. **Raw provider payload exposure** — NVIDIA response bodies are no longer propagated to end-user error messages; full causes stay in logs.
7. **Blind temporary upload destination** — NVIDIA asset upload URL must be HTTPS and may not target local addresses.
8. **Desktop API exposed to LAN** — server is now bound to `127.0.0.1`.
9. **H2 console unnecessarily enabled** — disabled by default.
10. **Generic 500s** — validation, invalid JSON/parameters, missing parts, 404, 405, 415, persistence, provider and optional TTS failures have explicit mappings.
11. **Optional TTS could become a hard dependency** — TTS defaults to `none`; the controller uses `ObjectProvider<TextToSpeechModel>` and the UI falls back to local speech.
12. **Launcher startup timeout could leave a backend alive** — startup failures now terminate the spawned process before resetting the UI.
13. **Unrelated launcher Codex installer action** — removed from the delivery launcher to keep runtime scope focused and avoid executing remote installer code.
14. **Technical/raw UI obscured the challenge flow** — redesigned around the DIO voice flow with readable results and collapsible diagnostics.
15. **Old responsive grid could force narrow-screen overflow** — replaced with `minmax(0, 1fr)` plus tablet/mobile breakpoints.

## Remaining external dependencies

- Real NVIDIA inference still depends on an active NVIDIA API credential, model availability and network access.
- Real cloud MP3 TTS is optional and depends on an OpenAI TTS credential when enabled.
- Local browser speech availability depends on the installed browser/OS speech engine.

These external failures are handled without corrupting persisted transaction data or exposing stack traces to the UI.
