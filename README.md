# ⚡ Smart Energy Usage Monitoring System

A real-time energy usage monitoring system using **ESP32**, **PZEM-004T v3.0**, and **Firebase Realtime Database**. This Arduino-based project tracks key electrical parameters and stores daily/monthly summaries in the cloud. A 20x4 I2C LCD displays live readings while the system handles real-time sync, WiFi reconnection, and user-controlled backlight.

---

## 🚀 Features

- 📡 Real-time monitoring of:
  - Voltage
  - Current
  - Power
  - Frequency
  - Energy
- 🕓 NTP-based timestamping for accurate logs
- 📟 20x4 I2C LCD with real-time data and status display
- ☁️ Firebase Realtime Database integration
- 🧠 Daily and Monthly summary storage
- 🔦 LCD backlight control via push button with auto-timeout
- 🔄 Automatic WiFi reconnect & PZEM reset handling
- 🧮 Tracks max power of the day and stores energy usage trends

---

## 🧰 Hardware & Libraries

| Component              | Description                            |
|------------------------|----------------------------------------|
| ESP32 Dev Board        | Microcontroller                        |
| PZEM-004T v3.0         | Voltage/Current/Power/Energy sensor    |
| LCD 20x4 with I2C      | Data display                           |
| Push Button            | For backlight toggle                   |

**Arduino Libraries Used:**

- `WiFi.h`  
- `WiFiUdp.h`, `NTPClient.h`  
- `FirebaseESP32.h`  
- `PZEM004Tv30.h`  
- `LiquidCrystal_I2C.h`  
- `TimeLib.h`  
- `ArduinoJson.h`  

---

## 🔌 Firebase Realtime Structure

```plaintext
/energy_data/YYYY/MM/DD/
    - timestamp
    - voltage
    - current
    - power
    - frequency
    - daily_energy
    - current_month_energy
    - previous_month_energy

/daily_summary/YYYY/MM/DD/
    - total_energy
    - max_power
    - timestamp

/monthly_data/YYYY/MM/
    - total_energy
    - last_updated
