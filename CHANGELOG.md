# Changelog

## 1.1.0
- Retargeted to Paper 1.21.x / Java 21 (Gradle, plugin.yml api-version 1.21).
- Prefix is now regular "UC |" with red gradient; built as its own component so small caps can never touch it.
- **Fixed:** YAML 1.1 booleans - keys `on/off/yes/no` and value `YES/NO` were parsed as true/false, breaking
  `toggle.on/off`, `gui.state.*`, the reset-confirmation GUI buttons and hint texts. Renamed (enabled/disabled, confirm/cancel).
- **Fixed:** chat off did not reliably hide messages when other plugins add viewers or use legacy recipients; now filtered
  at HIGH + MONITOR and on `AsyncPlayerChatEvent`; also independent of `messages.chat`.
- **Fixed:** chat-format AUTO detection ran before other plugins were enabled; now runs after startup and on reload.
- Private messages: refused when receiver has PM off, chat off (configurable) or ignores the sender; bypass permission kept;
  PM text is never hidden by plugin-messages/messages.
- Usage/list/admin replies are no longer hidden by the plugin-messages toggle (only confirmations are).
- Disabled death type: behaviour configurable (`death.disabled-type-shows-vanilla`).
- Vanished players: no join/quit/death/achievement broadcast; tab completion hides them.
- GUI: per-setting permission check, no double sound when switching language.
- Help lines now carry the prefix. Removed unused key `general.usage`.
- New: `/chat ignore`, death location hint, first-join message, offline lookup for `/chat setting <player>`.
- Config/messages/GUI files get `file-version`; outdated files are backed up and replaced.
