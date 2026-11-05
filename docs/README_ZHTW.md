<h1 align="center">OpenDroidChat</h1>

<div align="center">
    <img width="150" src="../images/odc-logo.png" alt="專案Logo">
</div>

<h3 align="center">一款為 Android 6+ 設計的開源、輕量級 LLM API 聊天客戶端</h3>

<p align="center"><s>哇還有API額度浪費裝置</s></p>

<div align="center">
  
[![Stars](https://img.shields.io/github/stars/HOE-Team/OpenDroidChat?style=for-the-badge&logo=data:image/svg%2bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZlcnNpb249IjEiIHdpZHRoPSIxNiIgaGVpZ2h0PSIxNiI+PHBhdGggZD0iTTggLjI1YS43NS43NSAwIDAgMSAuNjczLjQxOGwxLjg4MiAzLjgxNSA0LjIxLjYxMmEuNzUuNzUgMCAwIDEgLjQxNiAxLjI3OWwtMy4wNDYgMi45Ny43MTkgNC4xOTJhLjc1MS43NTEgMCAwIDEtMS4wODguNzkxTDggMTIuMzQ3bC0zLjc2NiAxLjk4YS43NS43NSAwIDAgMS0xLjA4OC0uNzlsLjcyLTQuMTk0TC44MTggNi4zNzRhLjc1Ljc1IDAgMCAxIC40MTYtMS4yOGw0LjIxLS42MTFMNy4zMjcuNjY4QS43NS43NSAwIDAgMSA4IC4yNVoiIGZpbGw9IiNlYWM1NGYiLz48L3N2Zz4=&logoSize=auto&label=Stars&labelColor=444444&color=eac54f)](https://github.com/HOE-Team/OpenDroidChat)
[![LICENSE](https://img.shields.io/github/license/HOE-Team/OpenDroidChat?style=for-the-badge)](https://github.com/HOE-Team/OpenDroidChat/blob/main/LICENSE)
![GitHub Release](https://img.shields.io/github/v/release/HOE-Team/OpenDroidChat?label=Release&logo=github&style=for-the-badge)

**[简体中文](../README.md) | [English](README_EN.md) | 繁體中文**
</div>

> [!IMPORTANT]
> OpenDroidChat正處於危機！訪問並了解 [KeepAndroidOpen[↗]](https://keepandroidopen.org)，反對 Google 強制驗證所有 Android 開發者的政策 —— 這破壞了 Android 的開放根基

> [!NOTE]
> 程式翻譯尚不完善，如果您有能力進行 OpenDroidChat 專案的本土化，歡迎向我們發送 [Pull Request](https://github.com/HOE-Team/OpenDroidChat/pulls)。

## 🔨 技術棧

* [**Kotlin**[↗]](https://kotlinlang.org/) — 主要程式語言
* [**Jetpack Compose**[↗]](https://developer.android.com/jetpack/compose) — 宣告式 UI 框架
* [**Material Design 3 (M3)**[↗]](https://m3.material.io/) — 原生 GUI 設計實現
* [**Gradle**[↗]](https://gradle.org/) — 建構工具
* [**Ktor**[↗]](https://ktor.io/) — HTTP 用戶端，負責與 LLM API / MCP 伺服器通訊
* [**kotlinx.serialization**[↗]](https://github.com/Kotlin/kotlinx.serialization) — 請求與回應資料的 JSON 序列化
* [**AndroidX DataStore**[↗]](https://developer.android.com/topic/libraries/architecture/datastore) — 設定、偏好與啟動行為儲存
* [**Markwon**[↗]](https://github.com/noties/Markwon) — 用於渲染 LLM 訊息中的 Markdown

## ✨ 特性

### 💬 對話與模型

* 支援 Android 6.0（API 等級 23）及以上版本
* 原生、美觀的 Material 3 Expressive 介面，支援深色模式與橫向 NavigationRail
* 內建 OpenAI / Azure OpenAI、Google Gemini、DeepSeek、Dashscope（阿里雲百煉）、Claude，並支援自訂 API
* 支援串流（打字機）與非串流輸出
* 思考模式：展示模型推理過程，可依提供商設定思考深度
* **Markdown 訊息渲染**（程式碼區塊、標題、連結等）
* 訊息編輯、重新生成、一鍵複製
* 可將文字 / 程式碼檔案作為上下文傳送給模型

### 🧰 MCP 工具呼叫

* 支援新增多個 [MCP（Model Context Protocol）](https://modelcontextprotocol.io/) 伺服器
* 可依模型個別開關「允許呼叫 MCP 伺服器」
* 對話中展示工具呼叫記錄：伺服器、工具名稱、耗時與錯誤資訊

### 🗂️ 歷史對話

* 對話會自動保存在本機，無需手動儲存
* 頂欄時鐘按鈕開啟側邊抽屜，隨時檢視、繼續或刪除歷史對話
* 繼續歷史對話時會完整還原訊息上下文，可直接接著聊
* 支援刪除單一對話、清空全部記錄，最多保留最近 100 個對話
* 「啟動時恢復上次對話」可在設定中開啟；預設每次啟動都從新對話開始

### 🔒 隱私與安全

* API Key 使用 Android Keystore 加密後儲存
* 所有設定與對話僅儲存在本機應用程式私有目錄，不含雲端同步
* 僅直接連線使用者設定的 API 提供商 / MCP 伺服器，不經過開發者伺服器
* 不包含統計、廣告或遙測 SDK
* 首次啟動需閱讀並同意法律與隱私政策，不同意可直接退出應用程式

### ⚙️ 設定

* 深色模式
* 介面語言：跟隨系統 / 简体中文 / 繁體中文 / English
* 自動檢查更新（透過 GitHub Releases）
* 啟動時恢復上次對話（預設關閉）

> [!NOTE]
>
> ### 預置的 LLM 提供商
>
> * [x] OpenAI / Azure OpenAI
> * [x] Google Gemini
> * [x] DeepSeek
> * [x] Dashscope (阿里雲百煉)
> * [x] Claude
> * [x] 自訂 API

## 🔐 隱私與資料

OpenDroidChat 不需要帳號，也不會蒐集任何使用者資料：

* **本機儲存**：模型設定、API Key（Keystore 加密）、對話歷史與偏好設定全部保存在本機應用程式私有目錄中。
* **按需傳輸**：只有在你傳送訊息、取得 MCP 工具清單或呼叫工具時，用戶端才會直接連線你設定的 API / MCP 服務，並且僅傳送完成該請求所必需的內容（對話、系統提示、你選擇的檔案、工具參數）。
* **無遙測**：本程式不包含統計、廣告或遙測 SDK，也不會在背景蒐集使用資料。
* **可刪除**：你可以隨時刪除單一對話或清空全部歷史；解除安裝應用程式會一併移除本機儲存的所有資料。
* **更新檢查**：開啟自動檢查更新時，僅存取 GitHub Releases 取得版本資訊，請求中不包含任何對話內容或 API Key。

完整條款可在應用程式內「設定 → 關於 → 法律與隱私政策」中查看。

## 📥 安裝

1. 從 [Releases[↗]](https://github.com/HOE-Team/OpenDroidChat/releases) 下載APK 檔案
2. (建議) 校驗安裝包完整性
3. 使用軟體安裝器安裝

## 🛠️ 從原始碼建構

確保已安裝 **Android Studio** 或 **Gradle**，以及 **JDK 17 或更高版本**（建議 JDK 21）。

> [!IMPORTANT]
>
> * 你可能需要使用 VPN 或代理來下載 Gradle 使用的外部資源。
> * 你可能需要一台效能足夠的電腦來完成編譯。

### 🔧 基本建構（Debug 版本）

```bash
# 克隆倉庫
git clone https://github.com/HOE-Team/OpenDroidChat.git
cd OpenDroidChat

# 使用 Gradle Wrapper 建構 Debug 版本
./gradlew assembleDebug    # Linux / macOS
gradlew.bat assembleDebug   # Windows
```

建構成功後，APK 位於 `app/build/outputs/apk/`。

**如果要執行Release建構，請參閱 [Release建構指南[↗]](PERFORM_A_RELEASE_BUILD.md)**

### 🧪 執行測試

```bash
# JVM 單元測試
./gradlew :app:testDebugUnitTest

# 裝置端儀器測試（需要已連線的裝置或模擬器）
./gradlew :app:connectedDebugAndroidTest
```

## 📁 專案結構

```
project/
├─ app/
│  └─ src/main/java/com/hoeteam/opendroidchat/
│     ├─ data/          # 模型設定、對話歷史、加密與本機儲存
│     ├─ network/       # LLM API 與 MCP 用戶端
│     ├─ ui/            # Compose 介面（screens / components / theme）
│     ├─ viewmodel/     # 聊天、主題等狀態管理
│     └─ MainActivity.kt
├─ docs/                # 文件與多語言 README
├─ gradle/              # Gradle 設定與 Version Catalog
├─ build.gradle.kts     # 專案建構設定
├─ settings.gradle.kts  # Gradle 設定
├─ gradlew              # Gradle Wrapper (Linux / macOS)
├─ gradlew.bat          # Gradle Wrapper (Windows)
├─ LICENSE              # 許可證
├─ CODE_OF_CONDUCT.md   # 貢獻者準則
└─ README.md            # 專案說明
```

## 🚀 使用方法

### 首次啟動

1. 閱讀並同意「法律與隱私政策」（需要先開啟條款頁面閱讀，之後才能點擊「同意並繼續」）
2. 如不同意，可選擇直接退出應用程式

### 設定模型

1. 進入「模型」頁，點擊右下角 **+** 新增執行個體
2. 選擇 API 提供商，填寫模型名稱與 API Key
3. 選填：Base URL / AppID、系統提示詞，並可開啟串流、思考或 MCP 工具呼叫
4. 儲存後回到聊天頁即可開始對話

### 聊天

* 輸入訊息後點擊傳送；也可以點擊輸入框左側 **+** 新增文字 / 程式碼檔案作為上下文
* 訊息下方提供編輯、重新生成與複製按鈕
* 開啟思考模式後，可展開查看模型的推理過程

### 歷史對話

1. 點擊聊天頁頂欄的時鐘按鈕開啟歷史對話抽屜
2. 點擊任一對話即可繼續聊天，訊息上下文會自動還原
3. 點擊對話右側的刪除按鈕可刪除單筆記錄，底部「清空全部」可清除所有歷史
4. 如希望下次啟動時自動回到上次對話，可在「設定 → 一般設定」中開啟「啟動時恢復上次對話」

### 使用 MCP 工具

1. 進入「MCP」頁，新增 MCP 伺服器（名稱、URL，視需要填寫 API Key）
2. 在模型設定中開啟「允許呼叫 MCP 伺服器」
3. 對話時模型會自動請求工具，聊天介面會展示每次呼叫的伺服器、工具與耗時

## 🤝 貢獻

我們歡迎各種形式的貢獻！請參閱 [CODE_OF_CONDUCT.md[↗]](../CODE_OF_CONDUCT.md) 了解貢獻者行為準則。 

歡迎提交：
- 🐛 Issue - 回報錯誤或提出新功能建議  
- 🔧 Pull Request - 直接貢獻程式碼改進

## 💬 群組
歡迎加入OpenDroidChat Bug回饋/交流群，你可以在這裡交流和回饋問題。  
你可以透過搜尋群號（887447099）的方式或者點選[這裡[↗]](https://qun.qq.com/universal-share/share?ac=1&authKey=DMse5JOJpU4DQbDSXq81177Otr2MtUkJYowAZqhb1AZXkk%2BqV5ZxK7iWaYHbzk3k&busi_data=eyJncm91cENvZGUiOiI4ODc0NDcwOTkiLCJ0b2tlbiI6IjRORVV5VnZpeXRzYjlPOHBBYWViZzJFSkRQeXdLakJFcG9CVU9zbnBYOXJMbGk5RDNHZ3JQbE9iZWU5V1l4MjkiLCJ1aW4iOiI4NTc3MTMxNTAifQ%3D%3D&data=DppwomuajQ62pIIxMwmGWzYlXV0Evn50pE4LwSxUviqpF8mva5LDqOnX3b2WY3Z0cklTpnahpMvCok-VcL6dfA&svctype=4&tempid=h5_group_info)加入群聊。  

如有任何疑問，請發送郵件到 hoe_software_team@outlook.com

## 🔧 開發人員內容
1. 如要建構 Release APK，請參閱 [Release 版本建構指南 (簡體中文)](https://github.com/HOE-Team/OpenDroidChat/blob/main/docs/PERFORM_A_RELEASE_BUILD.md)，如果已經熟悉了 Release 建構流程，可忽略此條提示。

2. OpenDroidChat 使用的 JKS 金鑰檔案指紋：  
請透過以下SHA-256指紋驗證應用的真實性（您可透過keytool -list -v -keystore或APK分析工具比對）：
    - **舊憑證指紋（已停用）**：
        06:F6:A5:21:94:DE:BD:85:71:9E:4F:AD:FB:0F:62:44:BA:07:D7:B3:B3:F0:76:16:E3:EA:AC:A4:1F:9F:10:2D
    - **新憑證指紋（目前生效）**：
        4B:F3:B1:FF:B1:6B:1C:AB:CA:6C:C4:5B:DA:B0:84:4E:48:A9:EA:BC:0A:07:BC:36:CD:71:D0:8E:5A:E7:93:FB

## 📄 許可證

本專案基於 [MIT License[↗]](../LICENSE) 開源。

> [!NOTE]
> 這份許可證意味著：
>
> 1. **你可以隨意使用這個專案程式碼**，無論是在個人專案還是商業專案中。
> 2. **你可以修改並重新發布**這個程式碼。
> 3. **你甚至可以用它來開發商業軟體並銷售**，只要你在你的產品中包含原始的 MIT 許可證文字和版權聲明。
> 4. **作者不提供任何保證**，如果使用該軟體導致任何問題，你需要自己承擔風險。

版權所有 © 2025-2026 HOE Team，保留所有權利。  

> [!WARNING]
> ### 著作權聲明
> OpenDroidChat Geometry 系列徽標為 HOE Team 所有，受法律保護。未經明確書面授權，不得用於商業用途或進行修改後使用。
> 
> 舊式應用圖示（OpenDroidChat Bugdroid-Say）由[ChatGPT[↗]](https://chatgpt.com)生成，使用CC BY-NC-SA協議開放

> [!NOTE]
> 由於HOE Team開發人員錯誤的Git操作，導致了安全問題的發生，現已重新建立倉庫以消除安全問題，對各貢獻者及開發者帶來的不便還請諒解，HOE Team將在今後的開發中避免此類安全問題的再次發生。  
> 因為倉庫的重新建立，先前的Releases、Star、Commit和Issue記錄都已不存在，對各位使用者和支援者帶來的困擾我們深感歉意。  
> 注意：本專案已執行金鑰輪換，您可能需要卸載後重新安裝應用
