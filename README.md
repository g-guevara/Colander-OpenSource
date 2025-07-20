# Colander

**Feed Blocker for Android**

Colander is an open-source Android application that intelligently blocks distracting content on Instagram while preserving essential functionality. It uses accessibility services and system overlays to selectively block the Instagram feed, reels, and search content, helping users maintain focus and reduce mindless scrolling.

## 🚀 How It Works

Colander uses Android's Accessibility Service to detect specific Instagram UI elements:
- `feed_tab` - Home/Feed section
- `search_tab` - Search/Explore section  
- `clips_tab` - Reels button
- Active text fields and keyboard state

When these elements are detected, the app displays strategic white overlays to block distracting content while keeping navigation and essential features accessible.


## ✨ Contributing

Contributions are welcome! 
Here are the things that you can colaborate:

- **Context-Aware Detection Consistency**: Automatically detects which Instagram section you're in (Feed, Search, Reels, Direct Messages), but when you stay still for a while without touching the screen, the overlay disapears.
- **Devices**: currently is only working on a Xiaomi redmi note 10 pro, it has never been tested on any other device
- **Reels after DM**: When a friend send you a reel, you can see it, but also you can keep scrolling tru the recomendations of reels to send back to your friend. there's a "pill" to return to the chat, that's the only indicator to know if you´re on that type of screen
- **Close reels Screen**: The app blocks permantly the reels button of the navtab. but not the reels screen itself.
- **Better UI**: if you want to contribuite to the ui of the app or the logo, is very welcome. 
- **Other Apps** : we want to expand this solution to other platafroms like youtube, tiktok, facebook, x and linkedin.
  
And here's how you can help:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

Legal contribuition is also very welcome, we want to distribuite this app on playstore at some point

## 🛠️ Installation


### From Source
1. Clone this repository:
   ```bash
   git clone https://github.com/yourusername/colander.git
   cd colander
   ```
2. Open in Android Studio
3. Build:
   -  (!) the simulator doesnt work with this app, you must try the apk on a real device
   -  In the menu optoins, select build-> generate signed APK.
   -  Choose your own password
     

### Required Permissions
The app will guide you through granting these permissions:

1. **Overlay Permission** - Display blocking overlays over Instagram
2. **Usage Stats Permission** - Detect when Instagram is running  
3. **Accessibility Service** - Analyze Instagram's UI elements

### Identify div's

In order to identify elements of the screen of the app, you can use the app Developer Assistant. 

## 📖 Usage

1. **Launch Colander** and tap "Start"
2. **Grant permissions** when prompted (overlay, usage stats, accessibility)
3. **Open Instagram** - blocking will activate automatically
4. **Customize settings** using the "Settings>" button to adjust overlay margins. 
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

## 📄 License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Inspired by the need for mindful social media usage
- Built with Android's Accessibility Services framework

**Disclaimer**: This app is not affiliated with Instagram or Meta. It's designed to help users manage their Instagram usage mindfully and works by analyzing publicly available UI elements.
