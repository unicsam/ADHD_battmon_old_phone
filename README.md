# ADHD Battery Monitor

A simple Android app that helps people with ADHD who forget to charge their phones or miss system notifications.

## The Problem

People with ADHD often:
- Forget to charge their phones
- Miss system notifications telling them battery is low
- Don't notice the battery warning in the notification drawer

## How It Helps

This app solves that by:

1. **Persistent Notification** - Shows battery level with a progress bar in the notification area (not just in drawer)
2. **Loud Popup Alerts** - Displays prominent warning dialogs at battery levels: 20%, 15%, 10%, 8%, 6%, 4%, and 2%
3. **Visual Warnings** - Different background colors (red/yellow) make it impossible to miss
4. **Charging Detection** - Automatically dismisses warnings when you plug in your charger

## Features

- Background service monitors battery continuously
- Visual popup alerts at critical battery levels
- Notification shows battery percentage with progress bar
- Warns at: 20% → 15% → 10% → 8% → 6% → 4% → 2%
- Closes popup when charging starts
- Works while phone is locked
- Lightweight (only ~5MB, minimal RAM usage)

## Screenshots

The app displays:
- A notification with battery percentage and progress bar
- Full-screen warning dialogs with colored backgrounds

## Installation

1. Download the APK from Releases
2. Enable "Install from unknown sources"
3. Open the APK to install
4. Grant required permissions (notification, overlay)

## Permissions Required

- `POST_NOTIFICATIONS` - To show battery notifications
- `SYSTEM_ALERT_WINDOW` - To display popup warnings over other apps
- `FOREGROUND_SERVICE` - To run in background

## Tech Stack

- Kotlin
- Android SDK
- Gradle

## License

MIT License
