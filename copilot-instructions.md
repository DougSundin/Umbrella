# Copilot Instructions for WhetherOrNot Weather App

## General Guidelines
- **Always provide both Java and Kotlin code examples** for any feature, fix, or implementation.
- **Include a detailed explanation** of what changes are being proposed, including reasoning, best practices, and any trade-offs.
- Ensure the UI and user experience are **consistent between Java and Kotlin implementations**.
- Follow **Android best practices** for architecture, UI, and code style.
- Use **Room database** for local data storage in both Java and Kotlin versions.
- The app should have a **clean, readable, and modern interface**.

## Application Requirements
- The app is a weather reporting application with **two tabs**:
  - One tab uses **Kotlin** as the coding base.
  - One tab uses **Java** as the coding base.
- Both tabs must provide similar functionality and a consistent UI.
- The app must **fetch weather data** from:
  - `https://api.openweathermap.org/data/3.0/onecall?lat={lat}&lon={lon}&exclude={part}&appid={API key}`
  - Use API key: `150cc45f78958ce4fb9d708c85bfdc1c`
  - Use device location if available; otherwise, prompt the user to:
    - Enter a US zip code
    - Enter a Canadian postal code
    - Search for any city in the world
- All weather data should be **stored locally using Room**.

## Implementation Notes
- Use **MVVM architecture** for both Java and Kotlin implementations.
- Use **Jetpack Compose** for Kotlin UI and **XML layouts** for Java UI, but ensure visual consistency.
- Use **dependency injection** where appropriate (e.g., Hilt or Dagger).
- Handle **permissions** and **error states** gracefully.
- Provide **unit tests** and **UI tests** for both versions.
- Document all code and provide comments explaining key logic.

## Response Format
- For every request, provide:
    1. **Kotlin implementation**
    2. **Java implementation**
    3. **Detailed explanation** of the changes, reasoning, and best practices

## API Configuration
- **Units**: Use "imperial" units for all API calls (Fahrenheit, mph, inches of mercury)
- **Excluded Data**: Exclude "minutely,alerts" from API responses
- **Test Location**: Duluth, MN (46.8384°N, 92.1800°W)

## Logging Configuration
- **Kotlin Weather Logs**: Use tag "KotlinWeather" for debugging API responses
- **Java Weather Logs**: Use tag "JavaWeather" for debugging API responses
- All JSON responses are logged to Logcat for debugging purposes

## Sample Weather API Response Structure
The OpenWeatherMap One Call API 3.0 returns a comprehensive weather object with the following structure:

```json
{
  "lat": 46.8384,
  "lon": -92.18,
  "timezone": "America/Chicago",
  "timezone_offset": -21600,
  "current": {
    "dt": 1722988800,
    "sunrise": 1722942123,
    "sunset": 1722996789,
    "temp": 72.5,
    "feels_like": 73.2,
    "pressure": 1013,
    "humidity": 65,
    "dew_point": 61.2,
    "uvi": 5.8,
    "clouds": 20,
    "visibility": 10000,
    "wind_speed": 8.5,
    "wind_deg": 210,
    "wind_gust": 12.3,
    "weather": [
      {
        "id": 801,
        "main": "Clouds",
        "description": "few clouds",
        "icon": "02d"
      }
    ]
  },
  "hourly": [
    {
      "dt": 1722988800,
      "temp": 72.5,
      "feels_like": 73.2,
      "pressure": 1013,
      "humidity": 65,
      "dew_point": 61.2,
      "uvi": 5.8,
      "clouds": 20,
      "visibility": 10000,
      "wind_speed": 8.5,
      "wind_deg": 210,
      "wind_gust": 12.3,
      "pop": 0.1,
      "weather": [
        {
          "id": 801,
          "main": "Clouds",
          "description": "few clouds",
          "icon": "02d"
        }
      ]
    }
    // ... more hourly data (48 hours total)
  ],
  "daily": [
    {
      "dt": 1722967200,
      "sunrise": 1722942123,
      "sunset": 1722996789,
      "moonrise": 1722975456,
      "moonset": 1722936789,
      "moon_phase": 0.25,
      "summary": "Partly cloudy with light winds",
      "temp": {
        "day": 75.8,
        "min": 62.1,
        "max": 78.3,
        "night": 65.2,
        "eve": 72.4,
        "morn": 64.5
      },
      "feels_like": {
        "day": 76.5,
        "night": 66.1,
        "eve": 73.2,
        "morn": 65.3
      },
      "pressure": 1013,
      "humidity": 62,
      "dew_point": 61.8,
      "wind_speed": 9.2,
      "wind_deg": 215,
      "wind_gust": 15.1,
      "weather": [
        {
          "id": 801,
          "main": "Clouds",
          "description": "few clouds",
          "icon": "02d"
        }
      ],
      "clouds": 25,
      "pop": 0.15,
      "uvi": 6.2
    }
    // ... more daily data (8 days total)
  ]
}
```

### Key Data Points:
- **Temperature**: Imperial units (Fahrenheit)
- **Wind Speed**: Miles per hour (mph)
- **Pressure**: Inches of mercury (inHg)
- **Visibility**: Miles
- **Weather Icons**: Use icon codes for weather condition display
- **Timezone**: Includes timezone and offset information
- **Forecasts**: 48-hour hourly and 8-day daily forecasts

## Response Format
- For every request, provide:
  1. **Kotlin implementation**
  2. **Java implementation**
  3. **Detailed explanation** of the changes, reasoning, and best practices

---
## Weather Icon API
- **Base URL**: `https://openweathermap.org/img/wn/{icon}@2x.png`
- **Icon Resolution**: Available in multiple sizes (@2x provides 100x100 pixel icons)
- **Icon Property**: Found in `weather[].icon` field of API responses
- **Usage**: Extract icon code from weather objects and construct full image URL

### Icon Code Examples:
- `01d` - Clear sky (day)
- `01n` - Clear sky (night)
- `02d` - Few clouds (day)
- `02n` - Few clouds (night)
- `03d/03n` - Scattered clouds
- `04d/04n` - Broken clouds
- `09d/09n` - Shower rain
- `10d` - Rain (day)
- `10n` - Rain (night)
- `11d/11n` - Thunderstorm
- `13d/13n` - Snow
- `50d/50n` - Mist

### Implementation Notes:
- Day/night variants use 'd' and 'n' suffixes
- Icons are available for current weather, hourly forecasts, and daily forecasts
- Use AsyncImage (Coil) for loading remote images in Compose
- Consider caching icons for better performance

---

**Always follow these instructions when responding to requests for this project.**
