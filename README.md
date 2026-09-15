# Translyrical

A modern Android application designed to fetch, display, and translate song lyrics with real-time synchronization. Built with Kotlin and declarative Jetpack Compose UI following Clean Architecture principles.

---

## Screenshots & Demo

| Now Playing | Library | Search |
|:---:|:---:|:---:|
| ![Screen 1](screenshots/now_playing.png) | ![Screen 2](screenshots/library.png) | ![Screen 3](screenshots/search.png) |

> *Tip: Place a short screen recording GIF here if available.*

---

## Features

* **Synchronized Lyrics:** Smooth, auto-scrolling lyric tracking synced with current audio playback.
* **Instant Translation:** Line-by-line or full-song translation across multiple target languages.
* **Modern Material 3 UI:** Fluid animations, Custom Runtime Shader using AGSL, dynamic color theming, and edge-to-edge support. 

---

## Architecture & Tech Stack

The project adheres to **MVVM + Clean Architecture** guidelines to ensure separation of concerns, testability, and scalability.
* **Language:** [Kotlin](https://kotlinlang.org/)
* **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
* **Asynchronous Flow:** Coroutines & StateFlow / SharedFlow
* **Networking & Parsing:** Retrofit, OkHttp, Kotlinx Serialization / Gson
* **Dependency Injection:** Koin
* **Architecture Components:** ViewModel, Navigation Compose, Lifecycle

---

## 🚀 Getting Started

### Prerequisites
* Android Studio Hedgehog | 2023.1.1 or newer
* JDK 17+
* Android SDK 34+
