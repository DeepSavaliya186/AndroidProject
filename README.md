# DriveGuard: Ultrasonic Overparking Alert System

**DriveGuard** is a mobile application paired with an embedded CC3200 system that detects the distance between your vehicle and nearby obstacles. It provides real-time audio, vibration, and visual alerts to assist in safe parking. The system is ideal for personal vehicles, garages, and smart parking setups.

---

## Features

### Android Application

- Real-time ultrasonic distance reading over Wi-Fi
- Visual distance indicator with colored proximity zones
- Audio alert when too close (DANGER zone)
- Warning beep in the YELLOW zone
- Vibration alert support
- Light/Dark mode toggle

### Embedded Firmware (CC3200)

- Ultrasonic sensor interface via TRIG/ECHO
- Wi-Fi Access Point setup (`DistanceMeter`)
- JSON API (`/sensor`) serving distance data
- Buzzer and LED alert when object is within 20 cm
- Serial debug output via UART

---

## Requirements

### Hardware

- TI CC3200 LaunchPad (or compatible)
- Grove Ultrasonic Sensor (or compatible)
- Buzzer
- LED

### Software

- Energia IDE (e.g., energia-1.8.7E21)
- Android Studio (Meerkat or later)
- Android device (API 24+ recommended)

---

## Android App Installation

1. **Clone the repository**
   ```bash
   git clone git@github.com:DeepSavaliya186/AndroidProject.git
   ```

2. **Open in Android Studio**
   - Open the `DriveGuard` project directory
   - Wait for Gradle to sync
   - Build and run the app on your Android device

3. **Permissions**
   - The app does not require internet; it connects locally via Wi-Fi

---

## ⚙️ CC3200 Firmware Setup

1. **Install Energia IDE**  
   [Download Energia](https://energia.nu/)

2. **Connect CC3200 and select board**
   - Board: `CC3200-LAUNCHXL` under `Tools > Board`
   - Select the correct COM port

3. **Upload firmware**
   - Open `firmware.ino` (provided in `/firmware` folder)
   - Flash the sketch to the board

4. **Hardware Wiring**

| Component  | CC3200 Pin |
|------------|-------------|
| Ultrasonic | Pin 24      |
| Buzzer     | Pin 39      |
| LED        | Pin 3       |

5. **Boot in Access Point Mode**
   - Board creates AP with SSID: `DistanceMeter`
   - Android app connects and reads from `http://192.168.1.1/sensor`

---

## File Structure

```bash
DriveGuard/
├── app/                      # Android Jetpack Compose UI
│   ├── MainActivity.kt       # Live distance UI and alerts
│   ├── StepsActivity.kt      # Connection instructions screen
│   └── res/
│       ├── drawable/         # Logos, icons, car top view
│       └── raw/              # alert_sound.wav, warning_beep.wav
│
├── firmware/
│   └── firmware.ino          # Energia CC3200 code
│                             # Wi-Fi AP + Ultrasonic + JSON Server
│
└── README.md                 # Project overview and instructions
```

---

## Communication Protocol

- HTTP GET request to:  
  `http://192.168.1.1/sensor`

- Expected JSON response:
```json
{ "distance": 25 }
```

---

## Alert Zones

- **DANGER (0–19 cm)**: Red zone, buzzer ON, vibration ON  
- **WARNING (20–29 cm)**: Yellow zone, warning beep  
- **SAFE (30+ cm)**: Green zone, no alerts

---

## Developer & Credits

- **Developer**: Deep Savaliya  
- **Supervisor**: Prof. Dr. Götz Winterfeldt  
- **Institution**: Hochschule Augsburg – University of Applied Sciences  

---

## Troubleshooting

- **App shows "No Connection"**  
  Ensure your phone is connected to the `DistanceMeter` Wi-Fi hotspot before opening the app.

- **Firmware fails to compile**  
  Confirm you are using the Energia IDE and the correct board is selected.

- **Distance stays at -1**  
  Check the ultrasonic wiring and ensure the sensor is functioning properly.

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
