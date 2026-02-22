# Release版本构建指南(简体中文)

本文档指导您如何构建签名的 Release 版本 APK，用于分发和发布。

> [!NOTE]
> For the English version of this document, please refer to [Perform a Release Build[↗]](PERFORM_A_RELEASE_BUILD_EN.md)

> [!NOTE]
> **对于贡献者**：通常您只需要构建 Debug 版本进行测试，无需关注本指南。  
> 只有项目维护者和需要发布正式版的开发者才需要执行以下步骤。

> [!TIP]
> **签名密钥**用于证明应用的真实性、保证代码不被篡改，并确保应用更新的安全。

---

## 📋 前置要求

- 已完成基本构建（参考主 README 中的 Debug 构建步骤）
- 已安装 JDK 11 或更高版本（包含 `keytool` 工具）
- 准备用于签名的密钥库信息

---

## 🔑 生成签名密钥库

如果您还没有密钥库，请使用以下命令生成：

```bash
# Linux / macOS
keytool -genkey -v -keystore release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000

# Windows (命令提示符)
keytool -genkey -v -keystore release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000
```

系统会提示您输入以下信息：
- 密钥库密码
- 密钥别名密码（可以和密钥库密码相同）
- 姓名、组织单位、组织名称、城市、省份、国家代码等身份信息

> [!IMPORTANT]
> - **请务必妥善保管密码！** 如果丢失，将无法更新已发布的应用
> - 建议将生成的 `release.keystore` 文件保存在安全的位置
> - **永远不要**将密钥库文件提交到代码仓库

---

## ⚙️ 配置签名信息

### 方法一：本地构建使用 keystore.properties（推荐）

1. **在项目根目录创建 `keystore.properties` 文件**：

```properties
# keystore.properties
storeFile=../release.keystore
storePassword=你的密钥库密码
keyAlias=release
keyPassword=你的密钥别名密码
```

2. **确保该文件已被 `.gitignore` 忽略**（已配置）：

```gitignore
# 确认 .gitignore 中包含以下内容
keystore.properties
*.keystore
/KeystoreArchive
```

### 方法二：CI/CD 构建（如 GitHub Actions）

在 CI 环境中，您需要在构建开始前动态创建 `keystore.properties` 文件。

**第一步：在 GitHub 仓库配置 Secrets**

进入 Settings → Secrets and variables → Actions，添加以下 Secrets：
- `KEYSTORE_BASE64`：Base64 编码的密钥库文件内容
- `STORE_PASSWORD`：密钥库密码
- `KEY_ALIAS`：密钥别名
- `KEY_PASSWORD`：密钥别名密码

**第二步：将密钥库转换为 Base64**

```bash
# Linux / macOS
base64 -w 0 release.keystore

# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore"))
```

将输出的字符串复制到 `KEYSTORE_BASE64` Secret 中。

**第三步：在工作流中创建配置文件**

```yaml
- name: Setup Release Signing
  run: |
    # 解码密钥库文件
    echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 --decode > release.keystore
    
    # 创建 keystore.properties 文件
    echo "storeFile=../release.keystore" > keystore.properties
    echo "storePassword=${{ secrets.STORE_PASSWORD }}" >> keystore.properties
    echo "keyAlias=${{ secrets.KEY_ALIAS }}" >> keystore.properties
    echo "keyPassword=${{ secrets.KEY_PASSWORD }}" >> keystore.properties

- name: Build Release APK
  run: ./gradlew assembleRelease
```

---

## 🏗️ 构建 Release 版本

### 本地构建

```bash
# Linux / macOS
./gradlew assembleRelease

# Windows
gradlew.bat assembleRelease
```

### 构建过程说明

项目构建脚本会自动处理签名配置：

| 构建类型 | 行为 |
|---------|------|
| **Debug 构建** (`assembleDebug`) | 跳过 Release 签名配置，使用默认 Debug 签名 |
| **Release 构建** (`assembleRelease`) | 检查是否存在 `keystore.properties`<br>✅ 存在：使用配置的签名信息<br>⚠️ 不存在：打印警告，使用 Debug 签名（仅用于测试） |

> [!WARNING]
> 如果 `keystore.properties` 不存在，Release 构建会**自动降级使用 Debug 签名**。  
> 这种 APK **不能用于正式发布**，因为：
> - Debug 签名密钥是公开的，不安全
> - 用户无法通过 Debug 签名的 APK 更新已发布的应用

---

## 📂 构建产物

构建成功后，Release APK 位于：

```
app/build/outputs/apk/release/
```

生成的文件：
- `app-release.apk` - 已签名的 Release APK
- `output-metadata.json` - 构建元数据

---

## 🔍 验证签名

使用以下命令验证 APK 签名：

```bash
# Linux / macOS
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# Windows
jarsigner -verify -verbose -certs app\build\outputs\apk\release\app-release.apk
```

成功验证会显示 `jar verified`。

---

## 🚨 常见问题

### Q1: 构建失败，提示 "keystore.properties not found"
**原因**：没有找到签名配置文件  
**解决**：  
- 本地构建：在项目根目录创建 `keystore.properties` 文件
- CI 构建：在工作流中添加创建配置文件的步骤

### Q2: 构建成功了，但生成的 APK 没有签名
**原因**：Release 构建找不到签名配置，自动降级使用 Debug 签名  
**解决**：检查 `keystore.properties` 是否存在且内容正确

### Q3: 构建日志中看到 "Using debug signing key for release" 警告
**含义**：这是一个**重要警告**，表示您的 Release APK 使用的是 Debug 签名
- 如果只是测试：可以忽略
- 如果要正式发布：**必须**配置正确的签名信息

### Q4: 如何在不同环境使用不同的签名配置？
项目使用**任务名称智能判断**，无需手动切换：

- **Debug 构建**（如 `assembleDebug`）：完全跳过 Release 签名配置
- **Release 构建**（如 `assembleRelease`）：自动尝试加载 `keystore.properties`

这种设计使得：
- 本地开发：无需配置签名
- 本地发布：创建 `keystore.properties` 即可
- CI/CD 发布：在构建前生成 `keystore.properties` 文件

### Q5: 签名信息泄露了怎么办？
1. **立即撤销**泄露的证书（如果可能）
2. **生成新的密钥库**并更新所有相关配置
3. **发布新版本**使用新签名
4. **通知用户**旧版本将无法更新

---

## 📖 相关文档

- [为应用签名 | Android Studio | Android Developers[↗]](https://developer.android.google.cn/studio/publish/app-signing?hl=zh-cn)
- [在 GitHub Actions 中使用机密 - GitHub文档[↗]](https://docs.github.com/zh/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets)
- [keytool 命令文档(英文)[↗]](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/keytool.html)

---

## ✅ 发布前检查清单

- [ ] 密钥库文件已安全备份（离线存储）
- [ ] 密钥库密码已妥善保管（密码管理器）
- [ ] `keystore.properties` 已被 `.gitignore` 忽略
- [ ] 执行 `./gradlew assembleRelease` 无警告信息
- [ ] 使用 `jarsigner -verify` 验证签名成功
- [ ] 在多个 Android 版本上测试通过
- [ ] 如果是 CI 构建，确认 Secrets 配置正确

---

> [!TIP]
> **最佳实践提示**：
> - 为正式发布配置独立的签名密钥，不要与测试密钥混用
> - 密钥库有效期建议设置为 25 年以上（10000 天约 27 年）