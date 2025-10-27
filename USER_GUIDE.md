# Umazing Helper - User Guide

Quick guide for using the Umazing Helper application.

## Table of Contents

1. [First Time Setup](#first-time-setup)
2. [Long-Press Feature](#long-press-feature) ⭐ **Important**
3. [Character Filter Tips](#character-filter-tips)
4. [Troubleshooting](#troubleshooting)

---

## First Time Setup

### Required Permissions

The app needs two permissions to work:

1. **Screen Capture**: Tap "Request Screen Capture Permission" → Tap "Start now"
2. **Overlay Permission**: The app will redirect you to Settings → Enable "Allow display over other apps"

After granting permissions, tap "Start Overlay" to show the floating scan button.

---

## Long-Press Feature

**⭐ This is the key feature that's not obvious without documentation!**

### Quick Guide

**To open character selection overlay:**

1. **Hold the green "U" scan button for 0.5 seconds** (don't just tap!)
2. Character selection overlay appears
3. Tap a character to select
4. Overlay closes and syncs with main app

### Visual Guide

```
Hold button for 0.5 seconds
   ┌─────┐
   │  U  │  ← Hold this (not tap!)
   └─────┘
         ↓
   ┌──────────────────┐
   │ 🌐 All (No Filter)│  ← Overlay appears
   │ 👤 Special Week   │
   │ 👤 Silence Suzuka │
   │ ...scroll...      │
   └──────────────────┘
```

### Important Notes

- ⏱️ **Must hold for 0.5 seconds** - quick tap won't work
- 🚫 **Dragging cancels** - if you move while holding, it drags instead
- 🔄 **Auto-sync** - selection syncs between overlay and main app
- 📱 **Shows 6 characters** - scroll to see more

---

## Character Filter Tips

### Basic Usage

- **Search**: Type character name to filter (e.g., "Special" → "Special Week")
- **Clear**: Tap X button or select "All Characters"
- **Read-only mode**: When character is selected, field is read-only (tap to clear and search again)

### Behaviors You Should Know

1. **Dropdown closes when clicking outside** - this is intentional
2. **Selection persists** - your choice is saved when you close the app
3. **Syncs with overlay** - changing in main app updates the overlay and vice versa
4. **No cursor when selected** - field becomes read-only to prevent accidental edits

---

## Troubleshooting

### "Failed to load characters" on first long-press

**Solution**: Wait 1-2 seconds and try again. Data loads asynchronously on first launch.

### Dropdown doesn't appear

**Solution**: Make sure you're tapping directly on the text field. If persists, restart the app.

### Scan button not appearing

**Solution**:

1. Check "Start Overlay" button shows "Stop Overlay"
2. Verify overlay permission is enabled in Settings
3. Restart the overlay service

### Long-press not working

**Common mistakes**:

- Tapping too quickly (must hold 0.5 seconds)
- Moving finger while holding (triggers drag instead)
- Button not fully loaded yet (wait a moment after starting overlay)

### Character selection not syncing

**Solution**: Restart the overlay service and reopen the main app.

### Permission Issues (Xiaomi, Huawei, etc.)

Some manufacturers have additional restrictions:

- Look for "Display popup windows" or similar settings
- Check "Auto-start" permissions
- Disable battery optimization for the app

---

## Quick Reference

### Scan Button Actions

- **Tap**: (Reserved for future quick scan)
- **Hold 0.5s**: Open character selection overlay ⭐
- **Drag**: Move button position

### Character Filter

- **Tap field**: Opens dropdown
- **Type**: Real-time search
- **Tap X**: Clear selection
- **Click outside**: Close dropdown

---

## Getting Help

For issues not covered here:

- Check [README.md](README.md) for full documentation
- Open an issue on GitHub with your device info and error details
- Review [ACKNOWLEDGEMENTS.md](ACKNOWLEDGEMENTS.md) for licenses and credits

---

**Enjoy using Umazing Helper!** 🏇✨
