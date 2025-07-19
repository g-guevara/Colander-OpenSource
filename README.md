# Colander

**Instagram Content Blocker for Android**

Colander is an open-source Android application that intelligently blocks distracting content on Instagram while preserving essential functionality. It uses accessibility services and system overlays to selectively block the Instagram feed, reels, and search content, helping users maintain focus and reduce mindless scrolling.

## ✨ Features

- **Smart Content Blocking**: Blocks Instagram feed, reels, and search content with configurable overlays
- **Context-Aware Detection**: Automatically detects which Instagram section you're in (Feed, Search, Reels, Direct Messages)
- **Keyboard-Friendly**: Allows normal typing when the keyboard is active in search
- **Navigation Respect**: Temporarily disables blocking when back button is detected
- **Direct Messages Safe**: Never blocks direct messages functionality
- **Customizable Margins**: Configure overlay positions for different screen sizes and preferences
- **Persistent Service**: Continues working across app restarts and device reboots
- **Minimal UI**: Clean, focused interface inspired by terminal aesthetics

## 🚀 How It Works

Colander uses Android's Accessibility Service to detect specific Instagram UI elements:
- `feed_tab` - Home/Feed section
- `search_tab` - Search/Explore section  
- `clips_tab` - Reels button
- Active text fields and keyboard state

When these elements are detected, the app displays strategic white overlays to block distracting content while keeping navigation and essential features accessible.

## 🛠️ Installation


### From Source
1. Clone this repository:
   ```bash
   git clone https://github.com/yourusername/colander.git
   cd colander
   ```

2. Open in Android Studio
3. Build and install:
   ```bash
   ./gradlew assembleRelease
   adb install app/build/outputs/apk/release/app-release.apk
   ```

### Required Permissions
The app will guide you through granting these permissions:

1. **Overlay Permission** - Display blocking overlays over Instagram
2. **Usage Stats Permission** - Detect when Instagram is running  
3. **Accessibility Service** - Analyze Instagram's UI elements

## 📖 Usage

1. **Launch Colander** and tap "Start"
2. **Grant permissions** when prompted (overlay, usage stats, accessibility)
3. **Open Instagram** - blocking will activate automatically
4. **Customize settings** using the "Settings>" button to adjust overlay margins
5. **Normal usage** - Direct messages and typing in search work normally


## 🏗️ Architecture

### Core Components

- **AppDetectionService** - Main foreground service that monitors Instagram
- **InstagramAccessibilityService** - Detects Instagram UI elements
- **OverlayManager** - Manages system overlay windows
- **ElementStateManager** - Tracks Instagram state changes
- **InstagramElementDetector** - Finds specific UI elements
- **InstagramContextAnalyzer** - Determines current Instagram context

### Service Flow

```
InstagramAccessibilityService → ElementStateManager → AppDetectionService → OverlayManager
         ↓                           ↓                      ↓                    ↓
   Detects elements          Tracks state changes     Decides actions      Shows/hides overlays
```

## 🛡️ Privacy & Security

- **Local Operation**: All processing happens on your device
- **No Data Collection**: No personal information is collected or transmitted
- **Instagram Only**: Accessibility service only monitors Instagram
- **Open Source**: Full source code available for review

## 🔧 Development

### Building
```bash
./gradlew build
```

### Testing
```bash
./gradlew test
./gradlew connectedAndroidTest
```

### Code Structure
```
app/src/main/java/com/example/colander/
├── MainActivity.kt                 # Main UI and permission handling
├── AppDetectionService.kt         # Core service
├── InstagramAccessibilityService.kt # Instagram element detection
├── OverlayManager.kt             # Overlay management
├── ElementStateManager.kt        # State tracking
├── InstagramElementDetector.kt   # UI element detection
└── InstagramContextAnalyzer.kt   # Context analysis
```

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Development Guidelines
- Follow Kotlin coding conventions
- Add tests for new functionality
- Update documentation for API changes
- Test on multiple Android versions


## 📄 License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Inspired by the need for mindful social media usage
- Built with Android's Accessibility Services framework

**Disclaimer**: This app is not affiliated with Instagram or Meta. It's designed to help users manage their Instagram usage mindfully and works by analyzing publicly available UI elements.
