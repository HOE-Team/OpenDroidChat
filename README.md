<h1 align="center">OpenDroidChat</h1>

<div align="center">
    <img width="150" src="images/odc-logo.png" alt="项目Logo">
</div>

<h3 align="center">一款为 Android 6+ 设计的开源、轻量级 LLM API 聊天客户端</h3>

<p align="center"><s>哇还有API额度浪费装置</s></p>

<div align="center">
  
[![Stars](https://img.shields.io/github/stars/HOE-Team/OpenDroidChat?style=for-the-badge&logo=data:image/svg%2bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZlcnNpb249IjEiIHdpZHRoPSIxNiIgaGVpZ2h0PSIxNiI+PHBhdGggZD0iTTggLjI1YS43NS43NSAwIDAgMSAuNjczLjQxOGwxLjg4MiAzLjgxNSA0LjIxLjYxMmEuNzUuNzUgMCAwIDEgLjQxNiAxLjI3OWwtMy4wNDYgMi45Ny43MTkgNC4xOTJhLjc1MS43NTEgMCAwIDEtMS4wODguNzkxTDggMTIuMzQ3bC0zLjc2NiAxLjk4YS43NS43NSAwIDAgMS0xLjA4OC0uNzlsLjcyLTQuMTk0TC44MTggNi4zNzRhLjc1Ljc1IDAgMCAxIC40MTYtMS4yOGw0LjIxLS42MTFMNy4zMjcuNjY4QS43NS43NSAwIDAgMSA4IC4yNVoiIGZpbGw9IiNlYWM1NGYiLz48L3N2Zz4=&logoSize=auto&label=Stars&labelColor=444444&color=eac54f)](https://github.com/HOE-Team/OpenDroidChat)
[![LICENSE](https://img.shields.io/github/license/HOE-Team/OpenDroidChat?style=for-the-badge)](https://github.com/HOE-Team/OpenDroidChat/blob/main/LICENSE)
![GitHub Release](https://img.shields.io/github/v/release/HOE-Team/OpenDroidChat?label=Release&logo=github&style=for-the-badge)

**简体中文 | [English](docs/README_EN.md) | [繁體中文](docs/README_ZHTW.md)**
</div>

> [!IMPORTANT]
> OpenDroidChat正处于危机！访问并了解 [KeepAndroidOpen[↗]](https://keepandroidopen.org)，反对 Google 强制验证所有 Android 开发者的政策 —— 这破坏了 Android 的开放根基

> [!NOTE]
> 程序翻译尚不完善，如果您有能力进行 OpenDroidChat 项目的本土化，欢迎向我们发送 [Pull Request](https://github.com/HOE-Team/OpenDroidChat/pulls)。

## 🔨 技术栈

* [**Kotlin**[↗]](https://kotlinlang.org/) — 主要编程语言
* [**Jetpack Compose**[↗]](https://developer.android.com/jetpack/compose) — 声明式 UI 框架
* [**Material Design 3 (M3)**[↗]](https://m3.material.io/) — 原生 GUI 设计实现
* [**Gradle**[↗]](https://gradle.org/) — 构建工具
* [**Ktor**[↗]](https://ktor.io/) — HTTP 客户端，负责与 LLM API / MCP 服务器通信
* [**kotlinx.serialization**[↗]](https://github.com/Kotlin/kotlinx.serialization) — 请求与响应数据的 JSON 序列化
* [**AndroidX DataStore**[↗]](https://developer.android.com/topic/libraries/architecture/datastore) — 配置、偏好与启动行为存储
* [**Markwon**[↗]](https://github.com/noties/Markwon) — 渲染 LLM 消息中的 Markdown

## ✨ 特性

### 💬 对话与模型

* 支持 Android 6.0（API 等级 23）及以上版本
* 原生、美观的 Material 3 Expressive 界面，支持深色模式与横屏 NavigationRail
* 内置 OpenAI / Azure OpenAI、Google Gemini、DeepSeek、Dashscope（阿里云百炼）、Claude，并支持自定义 API
* 支持流式（打字机）与非流式输出
* 思考模式：展示模型推理过程，可按提供商配置思考深度
* **Markdown 消息渲染**（代码块、标题、链接等）
* 消息编辑、重新生成、一键复制
* 可将文本 / 代码文件作为上下文发送给模型

### 🧰 MCP 工具调用

* 支持添加多个 [MCP（Model Context Protocol）](https://modelcontextprotocol.io/) 服务器
* 可按模型单独开关「允许调用 MCP 服务器」
* 对话中展示工具调用记录：服务器、工具名称、耗时与错误信息

### 🗂️ 历史对话

* 对话会自动保存在本机，无需手动保存
* 顶栏时钟按钮打开侧边抽屉，随时查看、继续或删除历史对话
* 继续历史对话时会完整恢复消息上下文，可直接接着聊
* 支持删除单条会话、清空全部记录，最多保留最近 100 条会话
* 「启动时恢复上次对话」可在设置中开启；默认每次启动都从新对话开始

### 🔒 隐私与安全

* API Key 使用 Android Keystore 加密后保存
* 所有配置与对话仅存储在本机应用私有目录，不含云同步
* 仅直接连接用户配置的 API 提供商 / MCP 服务器，不经过开发者服务器
* 不包含统计、广告或遥测 SDK
* 首次启动需阅读并同意法律与隐私政策，不同意可直接退出应用

### ⚙️ 设置

* 深色模式
* 界面语言：跟随系统 / 简体中文 / 繁體中文 / English
* 自动检查更新（通过 GitHub Releases）
* 启动时恢复上次对话（默认关闭）

> [!NOTE]
>
> ### 预置的 LLM 提供商
>
> * [x] OpenAI / Azure OpenAI
> * [x] Google Gemini
> * [x] DeepSeek
> * [x] Dashscope (阿里云百炼)
> * [x] Claude
> * [x] 自定义 API

## 🔐 隐私与数据

OpenDroidChat 不需要账号，也不会收集任何用户数据：

* **本地存储**：模型配置、API Key（Keystore 加密）、对话历史与偏好设置全部保存在本机应用私有目录中。
* **按需传输**：只有在你发送消息、获取 MCP 工具列表或调用工具时，客户端才会直接连接你配置的 API / MCP 服务，并且仅发送完成该请求所必需的内容（对话、系统提示、你选择的文件、工具参数）。
* **无遥测**：本程序不包含统计、广告或遥测 SDK，也不会在后台采集使用数据。
* **可删除**：你可以随时删除单条对话或清空全部历史；卸载应用会一并移除本机保存的全部数据。
* **更新检查**：开启自动检查更新时，仅访问 GitHub Releases 获取版本信息，请求中不包含任何对话内容或 API Key。

完整条款可在应用内「设置 → 关于 → 法律与隐私政策」中查看。

## 📥 安装

1. 从 [Releases[↗]](https://github.com/HOE-Team/OpenDroidChat/releases) 下载APK 文件
2. (建议) 校验安装包完整性
3. 使用软件安装器安装

## 🛠️ 从源代码构建

确保已安装 **Android Studio** 或 **Gradle**，以及 **JDK 17 或更高版本**（推荐 JDK 21）。

> [!IMPORTANT]
>
> * 你可能需要使用 VPN 或代理来下载 Gradle 使用的外部资源。
> * 你可能需要一台性能足够的计算机来完成编译。

### 🔧 基本构建（Debug 版本）

```bash
# 克隆仓库
git clone https://github.com/HOE-Team/OpenDroidChat.git
cd OpenDroidChat

# 使用 Gradle Wrapper 构建 Debug 版本
./gradlew assembleDebug    # Linux / macOS
gradlew.bat assembleDebug   # Windows
```

构建成功后，APK 位于 `app/build/outputs/apk/`。

**如果要执行Release构建，请参阅 [Release构建指南[↗]](docs/PERFORM_A_RELEASE_BUILD.md)**

### 🧪 运行测试

```bash
# JVM 单元测试
./gradlew :app:testDebugUnitTest

# 设备端仪器测试（需要已连接的设备或模拟器）
./gradlew :app:connectedDebugAndroidTest
```

## 📁 项目结构

```
project/
├─ app/
│  └─ src/main/java/com/hoeteam/opendroidchat/
│     ├─ data/          # 模型配置、对话历史、加密与本地存储
│     ├─ network/       # LLM API 与 MCP 客户端
│     ├─ ui/            # Compose 界面（screens / components / theme）
│     ├─ viewmodel/     # 聊天、主题等状态管理
│     └─ MainActivity.kt
├─ docs/                # 文档与多语言 README
├─ gradle/              # Gradle 配置与 Version Catalog
├─ build.gradle.kts     # 项目构建配置
├─ settings.gradle.kts  # Gradle 设置
├─ gradlew              # Gradle Wrapper (Linux / macOS)
├─ gradlew.bat          # Gradle Wrapper (Windows)
├─ LICENSE              # 许可证
├─ CODE_OF_CONDUCT.md   # 贡献者准则
└─ README.md            # 项目说明
```

## 🚀 使用方法

### 首次启动

1. 阅读并同意「法律与隐私政策」（需要先打开条款页面阅读，之后才能点击“同意并继续”）
2. 如不同意，可选择直接退出应用

### 配置模型

1. 进入「模型」页，点击右下角 **+** 添加实例
2. 选择 API 提供商，填写模型名称与 API Key
3. 可选：填写 Base URL / AppID、系统提示词，开启流式、思考或 MCP 工具调用
4. 保存后回到聊天页即可开始对话

### 聊天

* 输入消息后点击发送；也可以点击输入框左侧 **+** 添加文本 / 代码文件作为上下文
* 消息下方提供编辑、重新生成与复制按钮
* 开启思考模式后，可展开查看模型的推理过程

### 历史对话

1. 点击聊天页顶栏的时钟按钮打开历史对话抽屉
2. 点击任意会话即可继续聊天，消息上下文会自动恢复
3. 点击会话右侧的删除按钮可删除单条记录，底部「清空全部」可清除所有历史
4. 如希望下次启动时自动回到上次对话，可在「设置 → 通用设置」中开启「启动时恢复上次对话」

### 使用 MCP 工具

1. 进入「MCP」页，添加 MCP 服务器（名称、URL，按需填写 API Key）
2. 在模型配置中开启「允许调用 MCP 服务器」
3. 对话时模型会自动请求工具，聊天界面会展示每次调用的服务器、工具与耗时

## 🤝 贡献

我们欢迎各种形式的贡献！请参阅 [CODE_OF_CONDUCT.md[↗]](CODE_OF_CONDUCT.md) 了解贡献者行为准则。 

欢迎提交：
- 🐛 Issue - 报告错误或提出新功能建议  
- 🔧 Pull Request - 直接贡献代码改进

## 💬 群组
欢迎加入OpenDroidChat Bug反馈/交流群，你可以在这里交流和反馈问题。  
你可以通过搜索群号（887447099）的方式或者点击[这里[↗]](https://qun.qq.com/universal-share/share?ac=1&authKey=DMse5JOJpU4DQbDSXq81177Otr2MtUkJYowAZqhb1AZXkk%2BqV5ZxK7iWaYHbzk3k&busi_data=eyJncm91cENvZGUiOiI4ODc0NDcwOTkiLCJ0b2tlbiI6IjRORVV5VnZpeXRzYjlPOHBBYWViZzJFSkRQeXdLakJFcG9CVU9zbnBYOXJMbGk5RDNHZ3JQbE9iZWU5V1l4MjkiLCJ1aW4iOiI4NTc3MTMxNTAifQ%3D%3D&data=DppwomuajQ62pIIxMwmGWzYlXV0Evn50pE4LwSxUviqpF8mva5LDqOnX3b2WY3Z0cklTpnahpMvCok-VcL6dfA&svctype=4&tempid=h5_group_info)加入群聊。  

如有任何疑问，请发送邮件到 hoe_software_team@outlook.com

## 🔧 开发人员内容
1. 如要构建 Release APK，请参阅 [Release 版本构建指南 (简体中文)](https://github.com/HOE-Team/OpenDroidChat/blob/main/docs/PERFORM_A_RELEASE_BUILD.md)，如果已经熟悉了 Release 构建流程，可忽略此条提示。

2. OpenDroidChat 使用的 JKS 密钥文件指纹：  
请通过以下SHA-256指纹验证应用的真实性（您可通过keytool -list -v -keystore或APK分析工具比对）：
    - **旧证书指纹（已停用）**：
        06:F6:A5:21:94:DE:BD:85:71:9E:4F:AD:FB:0F:62:44:BA:07:D7:B3:B3:F0:76:16:E3:EA:AC:A4:1F:9F:10:2D
    - **新证书指纹（当前生效）**：
        4B:F3:B1:FF:B1:6B:1C:AB:CA:6C:C4:5B:DA:B0:84:4E:48:A9:EA:BC:0A:07:BC:36:CD:71:D0:8E:5A:E7:93:FB

## 📄 许可证

本项目基于 [MIT License[↗]](LICENSE) 开源。

> [!NOTE]
> 这份许可证意味着：
>
> 1. **你可以随意使用这个项目代码**，无论是在个人项目还是商业项目中。
> 2. **你可以修改并重新发布**这个代码。
> 3. **你甚至可以用它来开发商业软件并销售**，只要你在你的产品中包含原始的 MIT 许可证文本和版权声明。
> 4. **作者不提供任何保证**，如果使用该软件导致任何问题，你需要自己承担风险。

版权所有 © 2025-2026 HOE Team，保留所有权利。  

> [!WARNING]
> ### 著作权声明
> OpenDroidChat Geometry 系列徽标为 HOE Team 所有，受法律保护。未经明确书面授权，不得用于商业用途或进行修改后使用。
> 
> 旧式应用图标（OpenDroidChat Bugdroid-Say）由[ChatGPT[↗]](https://chatgpt.com)生成，使用CC BY-NC-SA协议开放

> [!NOTE]
> 由于HOE Team开发人员错误的Git操作，导致了安全问题的发生，现已重新创建仓库以消除安全问题，对各贡献者及开发者带来的不便还请谅解，HOE Team将在今后的开发中避免此类安全问题的再次发生。  
> 因为仓库的重新创建，先前的Releases、Star、Commit和Issue记录都已不存在，对各位用户和支持者带来的困扰我们深感歉意。  
> 注意：本项目已执行密钥轮换，您可能需要卸载后重新安装应用
