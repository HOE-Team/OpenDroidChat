<h1 align="center">OpenDroidChat</h1>

<h3 align="center">An open-source, lightweight LLM API chat client designed for Android 6+</h3>

<p align="center"><s>Wow, there's also an API quota waste device</s></p>

<div align="center">
  
[![Stars](https://img.shields.io/github/stars/HOE-Team/OpenDroidChat?style=for-the-badge&logo=data:image/svg%2bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZlcnNpb249IjEiIHdpZHRoPSIxNiIgaGVpZ2h0PSIxNiI+PHBhdGggZD0iTTggLjI1YS43NS43NSAwIDAgMSAuNjczLjQxOGwxLjg4MiAzLjgxNSA0LjIxLjYxMmEuNzUuNzUgMCAwIDEgLjQxNiAxLjI3OWwtMy4wNDYgMi45Ny43MTkgNC4xOTJhLjc1MS43NTEgMCAwIDEtMS4wODguNzkxTDggMTIuMzQ3bC0zLjc2NiAxLjk4YS43NS43NSAwIDAgMS0xLjA4OC0uNzlsLjcyLTQuMTk0TC44MTggNi4zNzRhLjc1Ljc1IDAgMCAxIC40MTYtMS4yOGw0LjIxLS42MTFMNy4zMjcuNjY4QS43NS43NSAwIDAgMSA4IC4yNVoiIGZpbGw9IiNlYWM1NGYiLz48L3N2Zz4=&logoSize=auto&label=Stars&labelColor=444444&color=eac54f)](https://github.com/HOE-Team/OpenDroidChat)
[![LICENSE](https://img.shields.io/github/license/HOE-Team/OpenDroidChat?style=for-the-badge)](https://github.com/HOE-Team/OpenDroidChat/blob/main/LICENSE)
![GitHub Release](https://img.shields.io/github/v/release/HOE-Team/OpenDroidChat?label=Release&logo=github&style=for-the-badge)

**[简体中文](../README.md) | English**
</div>

> [!IMPORTANT]
> For security reasons (see: [Cryptography | App quality | Android Developers[↗]](https://developer.android.google.cn/privacy-and-security/cryptography?hl=en)), versions after Alpha-0.6fix(6) **no longer support** Android 5.x (API 21-22). You need to use Android 6.0 (API 23) or higher to run this application.

> [!WARNING]
> If you are using Android 5.x (API 21-22), you can only use Alpha-0.5(5) or earlier versions.  
> Alpha-0.5(5) and earlier versions lack encryption protection for sensitive information such as API keys. Continuing to use these older versions on Android 5.x (API 21-22) devices may result in credential leaks. The developer is not responsible for any such data breaches.

## 🔨 Tech Stack

* [**Kotlin**[↗]](https://kotlinlang.org/) — Primary programming language
* [**Jetpack Compose**[↗]](https://developer.android.com/jetpack/compose) — Declarative UI framework
* [**Material Design 3 (M3)**[↗]](https://m3.material.io/) — Native GUI design implementation
* [**Gradle**[↗]](https://gradle.org/) — Build tool
* [**Compose Markdown**[↗]](https://github.com/jeziellago/compose-markdown) — For rendering Markdown in LLM messages
* [**OkHttp**[↗]](https://square.ac.cn/okhttp/) — Efficient, powerful Java/Android HTTP client

## ✨ Features

* Supports Android 6.0 (API level 23) and above
* Native, beautiful Compose + M3 interface
* **Support for Markdown message rendering** (code blocks, headings, links, etc.)
* Lightweight, easy to install and use
* Can be integrated with multiple LLM APIs
* Uses encryption to protect user LLM API Keys

> [!NOTE]
>
> ### Pre-configured LLM Providers
>
> * [x] OpenAI / Azure OpenAI
> * [x] Google Gemini
> * [x] DeepSeek
> * [x] Dashscope (Aliyun Bailian)
> * [x] Claude 

## 📥 Installation

1. Download the APK file from [Releases[↗]](https://github.com/HOE-Team/OpenDroidChat/releases)
2. Install using your app installer

## 🛠️ Build from Source

Make sure you have **Android Studio** or **Gradle** installed.

> [!IMPORTANT]
>
> * You may need to use a VPN or proxy to download external resources used by Gradle.
> * You may need a computer with sufficient performance to complete the compilation.

```bash
# Clone the repository
git clone https://github.com/HOE-Team/OpenDroidChat.git
cd OpenDroidChat

# Build using Gradle Wrapper
./gradlew build    # Linux / macOS
gradlew.bat build  # Windows
```

After a successful build, the APK will be located at `app/build/outputs/apk/`.

## 📁 Project Structure

```
project/
├─ app/                  # Source code
├─ docs/                 # Documentation
├─ gradle/               # Gradle configuration
├─ build.gradle.kts      # Project build configuration
├─ settings.gradle.kts   # Gradle settings
├─ gradlew               # Gradle Wrapper (Linux / macOS)
├─ gradlew.bat           # Gradle Wrapper (Windows)
├─ LICENSE               # License
├─ CODE_OF_CONDUCT.md    # Contributor Code of Conduct
└─ README.md             # Project documentation
```

## 🚀 Usage

1. Open the app
2. Add an instance
3. Select your LLM API provider
4. Enter the model name you want to use
5. Enter your LLM API Key
6. Enter your LLM AppID (optional)
7. Configure a persona (optional)
8. Start chatting!

## 🤝 Contributing

We welcome contributions in all forms! Please see [CODE_OF_CONDUCT.md[↗]](CODE_OF_CONDUCT.md) to understand the contributor code of conduct.

You are welcome to submit:
- 🐛 Issues - Report bugs or suggest new features  
- 🔧 Pull Requests - Contribute code improvements directly

## 💬 Community Group
You are welcome to join the OpenDroidChat bug feedback/chat group, where you can discuss and report issues.  
You can join by searching the group number (887447099) or clicking [here](https://qun.qq.com/universal-share/share?ac=1&authKey=DMse5JOJpU4DQbDSXq81177Otr2MtUkJYowAZqhb1AZXkk%2BqV5ZxK7iWaYHbzk3k&busi_data=eyJncm91cENvZGUiOiI4ODc0NDcwOTkiLCJ0b2tlbiI6IjRORVV5VnZpeXRzYjlPOHBBYWViZzJFSkRQeXdLakJFcG9CVU9zbnBYOXJMbGk5RDNHZ3JQbE9iZWU5V1l4MjkiLCJ1aW4iOiI4NTc3MTMxNTAifQ%3D%3D&data=DppwomuajQ62pIIxMwmGWzYlXV0Evn50pE4LwSxUviqpF8mva5LDqOnX3b2WY3Z0cklTpnahpMvCok-VcL6dfA&svctype=4&tempid=h5_group_info) to join the group.

For any questions, please send an email to hoe_software_team@outlook.com

## 📄 License

This project is open-sourced under the [MIT License[↗]](LICENSE).

> [!NOTE]
> This license means:
>
> 1. **You can use this project code freely**, whether in personal or commercial projects.
> 2. **You can modify and redistribute** this code.
> 3. **You can even use it to develop and sell commercial software**, as long as you include the original MIT license text and copyright notice in your product.
> 4. **The author provides no warranty**. If any issues arise from using this software, you assume the risk yourself.

Copyright © 2025-2026 HOE Team. All rights reserved.  
Application icon generated by [ChatGPT[↗]](https://chatgpt.com)
