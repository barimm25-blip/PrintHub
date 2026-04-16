# PrintHub

An Android application for barcode scanning and label printing, supporting 3 types of printers: **Zebra**, **TSC**, and **Network printers**.

## Features

- 📷 **Barcode Scanning** — Scan barcodes using the device camera
- 🖨️ **Multi-Printer Support** — Print labels to Zebra, TSC, and Network printers
- 🏷️ **Label Printing** — Generate and print labels directly from the app

## Supported Printers

| Printer Type | Connection |
|---|---|
| Zebra | WiFi / Network |
| TSC | WiFi / Network |
| Network Printer | TCP/IP |

## Requirements

- Android 7.0 (API 24) or higher
- WiFi connection for printer communication
- Camera permission for barcode scanning

## Getting Started

1. Clone the repository
```bash
   git clone https://github.com/barimm25-blip/PrintHub.git
```
2. Open the project in **Android Studio**
3. Configure printer IP addresses in `MainActivity.kt`
4. Build and run on your device

## Configuration

Before running the app, update the printer IP addresses in `MainActivity.kt`:

```kotlin
private const val PRINT_SERVER_IP = "xxx.xxx.xxx.xxx"
private const val ZEBRA_IP = "xxx.xxx.xxx.xxx"
```

## Tech Stack

- **Language:** Kotlin
- **Platform:** Android
- **Build System:** Gradle (Kotlin DSL)
- **Min SDK:** 24
- **Target SDK:** 36

## License

This project is for internal use only.