# Gateway

Gateway is a one-to-one fallback server rewrite focused on stability, compatibility, and lightweight performance.

## Start

```bash
java -jar Gateway.jar --nogui
```

Place your schematic in the same directory as the server jar and configure `server.properties`.

## Compatibility

- Native protocol support: `1.21.11`
- Multi-version support (`1.17.1 -> 1.21.11`): set `protocol-translation-mode=builtin`
- Translation mode is configurable in `server.properties`:
  - `builtin` (recommended)
  - `off` (strict native-only)
## Security

Gateway includes configurable inbound hardening controls in `server.properties`:
- `max-packet-size-bytes`
- `max-inbound-packets-per-second`
- `max-chat-message-length`
- `max-command-length`
- `max-plugin-message-payload-bytes`
- `max-command-args`

