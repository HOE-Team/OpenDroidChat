<h1 align="center">OpenDroidChat</h1>

<h3 align="center">一款为 Android 6+ 设计的开源、轻量级 LLM API 聊天客户端</h3>

<p align="center"><s>哇还有API额度浪费装置</s></p>

<div align="center">
  
[![Stars](https://img.shields.io/github/stars/HOE-Team/OpenDroidChat?style=for-the-badge&logo=data:image/svg%2bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZlcnNpb249IjEiIHdpZHRoPSIxNiIgaGVpZ2h0PSIxNiI+PHBhdGggZD0iTTggLjI1YS43NS43NSAwIDAgMSAuNjczLjQxOGwxLjg4MiAzLjgxNSA0LjIxLjYxMmEuNzUuNzUgMCAwIDEgLjQxNiAxLjI3OWwtMy4wNDYgMi45Ny43MTkgNC4xOTJhLjc1MS43NTEgMCAwIDEtMS4wODguNzkxTDggMTIuMzQ3bC0zLjc2NiAxLjk4YS43NS43NSAwIDAgMS0xLjA4OC0uNzlsLjcyLTQuMTk0TC44MTggNi4zNzRhLjc1Ljc1IDAgMCAxIC40MTYtMS4yOGw0LjIxLS42MTFMNy4zMjcuNjY4QS43NS43NSAwIDAgMSA4IC4yNVoiIGZpbGw9IiNlYWM1NGYiLz48L3N2Zz4=&logoSize=auto&label=Stars&labelColor=444444&color=eac54f)](https://github.com/HOE-Team/OpenDroidChat)
[![LICENSE](https://img.shields.io/github/license/HOE-Team/OpenDroidChat?style=for-the-badge)](https://github.com/HOE-Team/OpenDroidChat/blob/main/LICENSE)
![GitHub Release](https://img.shields.io/github/v/release/HOE-Team/OpenDroidChat?label=Release&logo=github&style=for-the-badge)

**简体中文 | [English](docs/README_EN.md)**
</div>

> [!NOTE]
> 由于HOE Team开发人员错误的Git操作，导致了安全问题的发生，现已重新创建仓库以消除安全问题，对各贡献者及开发者带来的不便还请谅解，HOE Team将在今后的开发中避免此类安全问题的再次发生。  
> 因为仓库的重新创建，先前的Releases、Star、Commit和Issue记录都已不存在，对各位用户和支持者带来的困扰我们深感歉意。  
> 注意：本项目已执行密钥轮换，您可能需要卸载后重新安装应用

> [!IMPORTANT]
> 由于安全原因(参见：[加密 | App quality | Android Developers[↗]](https://developer.android.google.cn/privacy-and-security/cryptography?hl=zh-cn#kotlin))，Alpha-0.6fix(6)以后的版本已**不支持**Android 5.x(API 21-22)的Android版本，你需要使用Android 6.0(API 23)及以上的 Android 运行此程序。

> [!WARNING]
> 如果您使用Android 5.x(API 21-22)，您只能使用Alpha-0.5(5)及以下的版本。  
> Alpha-0.5(5) 及更早版本缺乏对 API 密钥等敏感信息的加密保护。继续在 Android 5.x(API 21-22) 设备上使用这些旧版本，可能导致您的凭据泄露。开发者对此类数据泄露概不负责。

## 🔨 技术栈

* [**Kotlin**[↗]](https://kotlinlang.org/) — 主要编程语言
* [**Jetpack Compose**[↗]](https://developer.android.com/jetpack/compose) — 声明式 UI 框架
* [**Material Design 3 (M3)**[↗]](https://m3.material.io/) — 原生 GUI 设计实现
* [**Gradle**[↗]](https://gradle.org/) — 构建工具
* [**Markwon**[↗]](https://github.com/noties/Markwon) — 用于渲染 LLM 消息中的 Markdown
* [**OkHttp**[↗]](https://square.ac.cn/okhttp/) — 高效、功能强大的 Java/Android HTTP 客户端。

## ✨ 特性

* 支持 Android 6.0(API等级23) 及以上版本
* 系统原生、美观的 Compose + M3 界面
* **支持 Markdown 消息渲染**（代码块、标题、链接等）
* 轻量级，易于安装和使用
* 可接入多种 LLM API
* 使用加密，保护用户LLM API Key

> [!NOTE]
>
> ### 预置的 LLM 提供商
>
> * [x] OpenAI / Azure OpenAI
> * [x] Google Gemini
> * [x] DeepSeek
> * [x] Dashscope (阿里云百炼)
> * [x] Claude 

## 📥 安装

1. 从 [Releases[↗]](https://github.com/HOE-Team/OpenDroidChat/releases) 下载APK 文件
2. 使用软件安装器安装

## 🛠️ 从源代码构建

确保已安装 **Android Studio** 或 **Gradle**。

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

**如果要执行Release构建，请参阅 [Release构建指南[↗]](/docs/PERFORM_A_RELEASE_BUILD.md)**


## 📁 项目结构

```
project/
├─ app/                  # 源码
├─ docs/                 # 文档
├─ gradle/               # Gradle 配置
├─ build.gradle.kts      # 项目构建配置
├─ settings.gradle.kts   # Gradle 设置
├─ gradlew               # Gradle Wrapper (Linux / macOS)
├─ gradlew.bat           # Gradle Wrapper (Windows)
├─ LICENSE               # 许可证
├─ CODE_OF_CONDUCT.md    # 贡献者准则
└─ README.md             # 项目说明
```

## 🚀 使用方法

1. 打开应用
2. 添加一个实例
3. 选择你的大语言模型 API 提供商
4. 输入要使用的模型名称
5. 输入你的 LLM API Key
6. 输入你的LLM AppID（可选）
7. 配置一个人格（可选）
8. 开始聊天！

## 🤝 贡献

我们欢迎各种形式的贡献！请参阅 [CODE_OF_CONDUCT.md[↗]](CODE_OF_CONDUCT.md) 了解贡献者行为准则。 

欢迎提交：
- 🐛 Issue - 报告错误或提出新功能建议  
- 🔧 Pull Request - 直接贡献代码改进

## 💬 群组
欢迎加入OpenDroidChat Bug反馈/交流群，你可以在这里交流和反馈问题。  
你可以通过搜索群号（887447099）的方式或者点击[这里](https://qun.qq.com/universal-share/share?ac=1&authKey=DMse5JOJpU4DQbDSXq81177Otr2MtUkJYowAZqhb1AZXkk%2BqV5ZxK7iWaYHbzk3k&busi_data=eyJncm91cENvZGUiOiI4ODc0NDcwOTkiLCJ0b2tlbiI6IjRORVV5VnZpeXRzYjlPOHBBYWViZzJFSkRQeXdLakJFcG9CVU9zbnBYOXJMbGk5RDNHZ3JQbE9iZWU5V1l4MjkiLCJ1aW4iOiI4NTc3MTMxNTAifQ%3D%3D&data=DppwomuajQ62pIIxMwmGWzYlXV0Evn50pE4LwSxUviqpF8mva5LDqOnX3b2WY3Z0cklTpnahpMvCok-VcL6dfA&svctype=4&tempid=h5_group_info)加入群聊。  

如有任何疑问，请发送邮件到 hoe_software_team@outlook.com

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
应用图标由[ChatGPT[↗]](https://chatgpt.com)生成
