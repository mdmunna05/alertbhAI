# AlertbhAI - Your Bhai in Emergency

AlertbhAI is an offline-first Android emergency support app.
It detects possible accidents using phone sensors, asks for user safety confirmation, then automatically sends SOS support actions when needed.

## Key Highlights

- Accident detection with accelerometer in a foreground service
- 10-second emergency confirmation popup (`Are you safe?`)
- Auto SOS flow if no response
- Sends SMS with Google Maps location link to saved emergency contact
- Calls emergency contact automatically
- Fallback calls to `108` and `100` if contact call fails
- Helper mode screen for nearby people to assist quickly
- Works without backend (offline-friendly)

## Current Tech Stack

- Java (Android SDK)
- XML layouts and drawables
- Google Play Services Location (`FusedLocationProviderClient`)
- Runtime permissions + Foreground Service

## Project Structure

```text
app/
  src/main/
	java/com/example/alertbhai/
	  IntroActivity.java
	  MainActivity.java
	  EmergencyActivity.java
	  AccidentService.java
	  EmergencyActions.java
	res/
	  layout/
		activity_intro.xml
		activity_main.xml
		activity_emergency.xml
	  drawable/
	  anim/
	AndroidManifest.xml
  build.gradle.kts

build.gradle.kts
settings.gradle.kts
gradle/
  wrapper/
gradlew
gradlew.bat
```

## Core Flow

1. User opens app (`IntroActivity` -> `MainActivity`)
2. User grants permissions and saves emergency contact
3. User starts monitoring
4. `AccidentService` watches accelerometer spikes in background
5. On suspected accident, `EmergencyActivity` opens with 10s timer
6. If user does not cancel:
   - send SOS SMS with location link
   - call saved contact
   - fallback to 108 and 100 when needed
7. Helper mode buttons allow instant assistance actions

## Permissions Used

- `SEND_SMS`
- `CALL_PHONE`
- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `FOREGROUND_SERVICE`
- `POST_NOTIFICATIONS`

## Build and Run

### Prerequisites

- Android Studio (with embedded JBR/JDK)
- Android SDK + emulator/device
- ADB available in PATH

### Commands (Windows PowerShell)

```powershell
Set-Location "D:\alertbhAI-showcase"
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:installDebug
adb shell am start -n com.example.alertbhai/.IntroActivity
```

## Important Notes

- Call UI may auto-format phone numbers (example: `8919489814` -> `(891) 948-9814`). This is expected Android formatting.
- Real telephony behavior (SMS/call) should be validated on a physical SIM-enabled device.
- This repository is intentionally cleaned to keep only important showcase files.

## Planned Next Improvements

- Refactor to Kotlin + MVVM package structure (`ui`, `viewmodel`, `repository`, helpers)
- Improve false-positive reduction with additional motion heuristics
- Add localized strings and full accessibility polish
- Add test coverage for emergency flow and permission edge cases
