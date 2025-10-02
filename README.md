# OpenDroidChat

一款适用于 **Android 5+** 的开源、操作简单的 LLM API 聊天软件
An open-source and easy-to-use LLM API chat software for Android 5+

## 技术栈 / Tech Stack

* [**Kotlin**](https://kotlinlang.org/) — 主要编程语言
* [**Jetpack Compose**](https://developer.android.com/jetpack/compose) — 声明式 UI 框架
* [**Material Design 3 (M3)**](https://m3.material.io/) — 原生 GUI 设计实现
* [**Gradle**](https://gradle.org/) — 构建工具
* [**Compose Markdown**](https://github.com/jeziellago/compose-markdown) — 用于渲染 LLM 消息中的 Markdown

## 特性 / Features

* 支持 Android 5.0(API等级21) 及以上版本
* 系统原生、美观的 Compose + M3 界面
* **支持 Markdown 消息渲染**（代码块、标题、链接等）
* 轻量级，易于安装和使用
* 可接入多种 LLM API

> [!NOTE]
>
> ### 目前预置的 LLM 提供商
>
> * [x] OpenAI / Azure OpenAI
> * [x] Google Gemini
> * [x] DeepSeek
> * [x] Dashscope (阿里云百炼)
> ### 将来会加入的预置 LLM 提供商
> * [ ] Claude

## 安装 / Installation

1. 下载最新的 [Releases](https://github.com/HOE-Team/OpenDroidChat/releases) APK 文件
2. 使用软件安装器安装

## 从源代码构建 / Build from Source

确保已安装 **Android Studio** 或 **Gradle**。

> [!IMPORTANT]
>
> * 你可能需要使用 VPN 或代理来下载 Gradle 使用的外部资源。
> * 你可能需要一台性能足够的计算机来完成编译。

```bash
# 克隆仓库
git clone https://github.com/HOE-Team/OpenDroidChat.git
cd OpenDroidChat

# 使用 Gradle Wrapper 构建
./gradlew build    # Linux / macOS
gradlew.bat build  # Windows
```

构建成功后，APK 位于 `app/build/outputs/apk/`。

## 文件结构 / Project Structure

```
project/
├─ app/                  # Kotlin 源码（Jetpack Compose + M3）
├─ docs/                 # 文档
├─ gradle/               # Gradle 配置
├─ build.gradle.kts      # 项目构建配置
├─ settings.gradle.kts   # Gradle 设置
├─ gradlew               # Gradle Wrapper (Linux / macOS)
├─ gradlew.bat           # Gradle Wrapper (Windows)
├─ LICENSE               # 许可证
└─ README.md             # 项目说明
```

## 使用方法 / Usage

1. 打开应用
2. 添加一个实例
3. 选择你的大语言模型 API 提供商
4. 输入要使用的模型名称
5. 输入你的 LLM API Key
6. 配置一个人格（可选）
7. 开始聊天！

## 贡献 / Contributing

欢迎提交 Issue 或 Pull Request 来帮助改进项目。
Contributions are welcome! Please open an issue or pull request.

## 许可证 / License

本项目基于 [MIT License](LICENSE) 开源。
Licensed under the MIT License.

> [!NOTE]
> 这份许可证意味着：
>
> 1. **你可以随意使用这个项目代码**，无论是在个人项目还是商业项目中。
> 2. **你可以修改并重新发布**这个代码。
> 3. **你甚至可以用它来开发商业软件并销售**，只要你在你的产品中包含原始的 MIT 许可证文本和版权声明。
> 4. **作者不提供任何保证**，如果使用该软件导致任何问题，你需要自己承担风险。

版权所有 © 2025 HOE Team