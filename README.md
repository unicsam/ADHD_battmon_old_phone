# ADHD Battery Monitor

A simple Android app that helps people with ADHD who forget to charge their phones or miss system notifications.

## The Problem

People with ADHD often:
- Forget to charge their phones
- Miss system notifications telling them battery is low
- Don't notice the battery warning in the notification drawer

## How It Helps

This app solves that by:
1. **Persistent Notification** - Shows battery level with a progress bar
2. **Loud Popup Alerts** - Full-screen warning dialogs at critical battery levels
3. **Smart Beep Alerts** - When screen is locked and popup can't show:
   - Battery 11-19%: Beeps every 5 minutes, device sleeps between beeps
   - Battery 6-10%: Beeps every 10 minutes, device sleeps between beeps
   - Battery ≤5%: Stops all alerts to preserve remaining battery
4. **Charging Detection** - Automatically dismisses warnings when charger is plugged in

## Alert Flow

| Battery Level | Screen Unlocked | Screen Locked |
|---------------|-----------------|---------------|
| 20%-15% | Show popup | Beep |
| 15%-10% | Show popup | Beep |
| 10%-6% | Show popup | Beep every 10 min |
| 5%-0% | Show popup | No alerts (sleep) |

## Features

- Background service monitors battery continuously
- Visual popup alerts at: 20%, 15%, 10%, 8%, 6%, 4%, 2%
- Smart beeping that doesn't drain battery (device sleeps between beeps)
- Notification shows battery percentage with progress bar
- Closes popup when charging starts
- Works on Android 5.0+ (API 21+)
- Lightweight (~5MB)

## Installation

1. Download the APK from Releases
2. Enable "Install from unknown sources"
3. Open the APK to install
4. Grant required permissions

## Permissions Required

- `POST_NOTIFICATIONS` - To show battery notifications
- `SYSTEM_ALERT_WINDOW` - To display popup warnings over other apps
- `FOREGROUND_SERVICE` - To run in background
- `SCHEDULE_EXACT_ALARM` - For smart beep scheduling
- `VIBRATE` - For haptic feedback

## Tech Stack

- Kotlin
- Android SDK
- Gradle
