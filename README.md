# 🌸 ReportSystem

> Advanced report system for Minecraft servers with Discord integration and admin moderation tools

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21-brightgreen.svg)](https://www.spigotmc.org/)
[![Paper](https://img.shields.io/badge/Paper-Required-blue.svg)](https://papermc.io/)
[![Discord](https://img.shields.io/badge/Discord-Integration-7289da.svg)](https://discord.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## ✨ Features

### 📝 Report System
- **Interactive Report Creation** - Step-by-step form in Minecraft chat
- **Beautiful GUI** - View your reports with pagination and status tracking
- **Multi-language Support** - English and Russian localizations included
- **Spring Color Palette** - Pleasant gradients for better UX
- **Automatic Validation** - Checks player existence and prevents self-reports

### 🎮 Discord Integration
- **Real-time Notifications** - Reports sent instantly to Discord channel
- **Interactive Buttons** - Approve, Reject, Check, and Comment directly from Discord
- **Punishment System** - Modal form for ban/mute/kick/warn with duration
- **Player Check Feature** - Freeze players and call them to voice channel
- **Automatic Commands** - Execute punishments automatically

### ⚙️ Admin Tools
- **Report Management** - Review, comment, and update report status
- **Player Freeze** - Freeze violators during verification
- **Voice Channel Integration** - Call players for checks via Discord
- **Statistics** - Track approved, rejected, and pending reports
- **Hot Reload** - Update configuration without server restart

## 📦 Installation

### Requirements
- Minecraft Server 1.21+
- Paper/Spigot
- Java 17+
- Discord Bot (optional, but recommended)

### Setup Steps

1. **Download** the latest release from [Releases](https://github.com/yourusername/ReportSystem/releases)

2. **Install** the plugin:
   ```bash
   cp ReportSystem-1.0.0.jar /path/to/server/plugins/
   ```

3. **Start** your server to generate configuration files

4. **Configure Discord Bot**:
   - Create a bot at [Discord Developer Portal](https://discord.com/developers/applications)
   - Enable the following intents:
     - `MESSAGE CONTENT`
     - `GUILD MESSAGES`
     - `GUILD VOICE STATES`
   - Copy the bot token

5. **Edit** `plugins/ReportSystem/config.yml`:
   ```yaml
   language: "en-EN"  # or "ru-RU"
   
   discord:
     use-bot: true
     bot-token: "YOUR_BOT_TOKEN_HERE"
     channel-id: "YOUR_CHANNEL_ID_HERE"
   
   reports:
     max-active-reports: 5
     form-timeout: 300
     history-limit: 18
   ```

6. **Restart** the server

## 🎨 Commands

### Player Commands
| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/report` | `/rep` | Create a new report | `reportsystem.report` |
| `/reports` | - | View your reports | `reportsystem.reports` |
| `/report help` | - | Show help | `reportsystem.report` |

### Admin Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/report reload` | Reload configuration | `reportsystem.admin.reload` |
| `/unfreeze <player>` | Unfreeze a player | `reportsystem.admin` |

## 🔐 Permissions

```yaml
# Full access
reportsystem.*

# Player permissions
reportsystem.report         # Create reports
reportsystem.reports        # View own reports

# Admin permissions
reportsystem.admin          # Access to all admin commands
reportsystem.admin.reload   # Reload configuration
reportsystem.admin.stats    # View statistics
```

## 🎯 Usage Examples

### Creating a Report

1. Type `/report` in chat
2. Enter violator's nickname
3. Enter reason for report
4. Add additional details or type 'skip'
5. Confirm by clicking `[YES]` button

### Discord Moderation

#### Approve with Punishment
1. Click `Approve` button
2. Fill the modal form:
   - **Punishment Type**: `ban`, `mute`, `kick`, or `warn`
   - **Duration**: `30m`, `7d`, `1y`, or `permanent`
   - **Reason**: Additional details
3. Player is automatically punished

#### Call for Check
1. Join a voice channel in Discord
2. Click `Check` button
3. Player is frozen in-game
4. Player receives notification with voice channel name
5. Conduct verification
6. Use `/unfreeze <player>` to unfreeze

## 🌍 Localization

### Supported Languages
- 🇬🇧 English (`en-EN`)
- 🇷🇺 Russian (`ru-RU`)

### Custom Languages

1. Copy `locales/en-EN.yml` to `locales/your-lang.yml`
2. Translate all strings
3. Set `language: "your-lang"` in `config.yml`

### Gradient Colors

The plugin uses beautiful spring-themed gradients:
```yaml
# Cherry Blossom → Peach
prefix: "<gradient:#FFB7C5:#FFDAB9>[Report]</gradient>"

# Mint → Fresh Green
report-success: "<gradient:#98FB98:#7FFF00>Report submitted!</gradient>"
```

## 🔧 API Usage

### Maven
```xml
<repository>
    <id>jitpack</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.yourusername</groupId>
    <artifactId>ReportSystem</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Example Code
```java
// Get the plugin instance
ReportSystem plugin = ReportSystem.getInstance();

// Create a report programmatically
Report report = new Report(
    "REP-" + System.currentTimeMillis(),
    player.getUniqueId(),
    player.getName(),
    "Griefer123",
    UUID.fromString("..."),
    "Griefing",
    "Destroyed spawn area"
);

// Save the report
plugin.getDataManager().saveReport(report);

// Send to Discord
plugin.getDiscordBot().sendReport(report);
```

## 📊 Statistics

View server-wide report statistics:
```
=== Report Statistics ===
Total reports: 156
Approved: 89
Rejected: 45
Pending: 22
```

## 🛠️ Configuration

### config.yml
```yaml
language: "en-EN"

discord:
  use-bot: true
  bot-token: "YOUR_TOKEN"
  channel-id: "YOUR_CHANNEL_ID"

reports:
  max-active-reports: 5    # Maximum pending reports per player
  form-timeout: 300         # Form timeout in seconds (0 = no timeout)
  history-limit: 18         # Reports per page in GUI
```

### Localization Files
- `locales/en-EN.yml` - English translations
- `locales/ru-RU.yml` - Russian translations

## 🎨 Spring Color Palette

The plugin features a beautiful spring-themed color scheme:

- 🌸 **Cherry Blossom Pink** - Headers and prefixes
- 🍑 **Peach** - Warnings and timeouts
- 💜 **Lavender** - Questions and info
- 🌿 **Mint Green** - Success messages
- ✅ **Fresh Green** - Approvals

## 🐛 Troubleshooting

### Discord Bot Not Working
1. Check bot token in `config.yml`
2. Verify bot has required intents enabled
3. Ensure channel ID is correct
4. Check bot has permissions in the channel

### Player Can't Create Reports
1. Check permission: `reportsystem.report`
2. Verify they haven't reached max reports limit
3. Check if report system is available (Discord connected)

### Voice Check Not Working
1. Ensure `GUILD_VOICE_STATES` intent is enabled
2. Admin must be in a voice channel
3. Player must be online
4. Check bot permissions

## 📝 Changelog

### Version 1.0.0
- Initial release
- Report creation system
- Discord integration with buttons
- Punishment modal system
- Player freeze and check feature
- Multi-language support
- Spring color palette
- GUI with pagination

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 💖 Credits

- **Author**: Lolka12321
- **Discord Integration**: JDA Library
- **Color System**: Adventure API
- **Inspired by**: Community feedback and suggestions

## 📞 Support

- **Discord**: [Join our server](https://discord.gg/yourserver)
- **Issues**: [GitHub Issues](https://github.com/yourusername/ReportSystem/issues)
- **Wiki**: [Documentation](https://github.com/yourusername/ReportSystem/wiki)

## ⭐ Show Your Support

Give a ⭐️ if this project helped you!

---

Made with 💚 for the Minecraft community
