# Bluetooth RFCOMM Lab

A standalone proof-of-concept for testing Android Bluetooth Classic RFCOMM/SPP against a Windows PC. This project is separate from PhoneKeys.

## What this first test does

- Starts an Android RFCOMM server using the standard Serial Port Profile UUID.
- Makes the phone temporarily discoverable after user approval.
- Accepts one Windows connection at a time.
- Reads newline-terminated UTF-8 messages and replies with `PHONEKEYS_ACK:<message>`.

This is only a transport test. It does **not** stream video, and it does not prove that Windows can display arbitrary RFCOMM data without receiver software.

## Build without Android Studio

Push this repository to GitHub. The workflow in `.github/workflows/build.yml` builds a debug APK. Download it from the completed Actions run under **Artifacts**.

## Android test

1. Install `app-debug.apk` on your Android phone.
2. Turn Bluetooth on.
3. Open RFCOMM Lab and tap **Start RFCOMM server**.
4. Grant Bluetooth permissions and approve the temporary discoverability prompt.
5. Keep the app open while testing.

The app uses UUID `00001101-0000-1000-8000-00805F9B34FB` (Bluetooth SPP).

## Windows test

1. Pair Windows with the phone through Windows Bluetooth settings.
2. Open **Control Panel → Hardware and Sound → Devices and Printers**.
3. Open the phone's Bluetooth properties and inspect the **Services** tab / COM port options. Windows UI differs by version and adapter.
4. If Windows exposes an outgoing Bluetooth serial COM port for the phone's SPP service, note its COM number (for example `COM7`).
5. Run the included PowerShell script from a PowerShell window:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\windows\Test-RfcommComPort.ps1 -Port COM7
```

Replace `COM7` with the actual outgoing port.

If Windows does not expose an outgoing COM port, do not assume the Android server is broken. Windows Bluetooth SPP/COM-port availability depends on Windows version, adapter, pairing state and service discovery. Report the exact Windows screen/error.

## Protocol

Send one UTF-8 line ending in newline, such as:

`PHONEKEYS_RFCOMM_TEST_001`

The Android app responds:

`PHONEKEYS_ACK:PHONEKEYS_RFCOMM_TEST_001`

## Scope and safety

- Bluetooth only; no Wi-Fi, internet transport or USB data.
- No changes to the PhoneKeys project.
- No background persistence: keep the test app open.
- This prototype has not been validated on physical devices yet.
