# Release Build Guide (English)

This document guides you through building a signed Release version APK for distribution and publishing.

> [!NOTE]
> 对于简体中文版本，请参阅 [Release版本构建指南[↗]](PERFORM_A_RELEASE_BUILD.md)

> [!NOTE]
> **For Contributors**: Usually, you only need to build a Debug version for testing and don't need to follow this guide.  
> Only project maintainers and developers who need to publish official releases should follow the steps below.

> [!TIP]
> **Signing Key** is used to prove the authenticity of the application, ensure code integrity, and guarantee secure application updates.

---

## 📋 Prerequisites

- Basic build completed (refer to the Debug build steps in the main README)
- JDK 11 or higher installed (includes the `keytool` utility)
- Keystore information ready for signing

---

## 🔑 Generate a Signing Keystore

If you don't have a keystore yet, generate one using the following command:

```bash
# Linux / macOS
keytool -genkey -v -keystore release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000

# Windows (Command Prompt)
keytool -genkey -v -keystore release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000
```

The system will prompt you to enter the following information:
- Keystore password
- Key alias password (can be the same as the keystore password)
- Identifying information such as name, organizational unit, organization, city, province, country code, etc.

> [!IMPORTANT]
> - **Make sure to store your passwords securely!** If lost, you won't be able to update published applications
> - It is recommended to save the generated `release.keystore` file in a secure location
> - **Never** commit the keystore file to the code repository

---

## ⚙️ Configure Signing Information

### Method 1: Using keystore.properties for Local Builds (Recommended)

1. **Create a `keystore.properties` file in the project root directory**:

```properties
# keystore.properties
storeFile=../release.keystore
storePassword=your_keystore_password
keyAlias=release
keyPassword=your_key_alias_password
```

2. **Ensure the file is ignored by `.gitignore`** (already configured):

```gitignore
# Confirm .gitignore contains the following
keystore.properties
*.keystore
/KeystoreArchive
```

### Method 2: CI/CD Builds (e.g., GitHub Actions)

In a CI environment, you need to dynamically create the `keystore.properties` file before the build starts.

**Step 1: Configure Secrets in GitHub Repository**

Go to Settings → Secrets and variables → Actions, and add the following Secrets:
- `KEYSTORE_BASE64`: Base64 encoded content of the keystore file
- `STORE_PASSWORD`: Keystore password
- `KEY_ALIAS`: Key alias
- `KEY_PASSWORD`: Key alias password

**Step 2: Convert the Keystore to Base64**

```bash
# Linux / macOS
base64 -w 0 release.keystore

# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore"))
```

Copy the output string to the `KEYSTORE_BASE64` Secret.

**Step 3: Create the Configuration File in the Workflow**

```yaml
- name: Setup Release Signing
  run: |
    # Decode the keystore file
    echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 --decode > release.keystore
    
    # Create keystore.properties file
    echo "storeFile=../release.keystore" > keystore.properties
    echo "storePassword=${{ secrets.STORE_PASSWORD }}" >> keystore.properties
    echo "keyAlias=${{ secrets.KEY_ALIAS }}" >> keystore.properties
    echo "keyPassword=${{ secrets.KEY_PASSWORD }}" >> keystore.properties

- name: Build Release APK
  run: ./gradlew assembleRelease
```

---

## 🏗️ Build the Release Version

### Local Build

```bash
# Linux / macOS
./gradlew assembleRelease

# Windows
gradlew.bat assembleRelease
```

### Build Process Explanation

The project build script automatically handles the signing configuration:

| Build Type | Behavior |
|------------|----------|
| **Debug Build** (`assembleDebug`) | Skips Release signing configuration, uses default Debug signature |
| **Release Build** (`assembleRelease`) | Checks for existence of `keystore.properties`<br>✅ If exists: Uses configured signing information<br>⚠️ If not exists: Prints warning, uses Debug signature (for testing only) |

> [!WARNING]
> If `keystore.properties` does not exist, the Release build will **automatically fall back to using the Debug signature**.  
> Such an APK **cannot be used for official release** because:
> - Debug signing keys are public and insecure
> - Users cannot update published applications using an APK signed with a Debug key

---

## 📂 Build Outputs

After a successful build, the Release APK is located at:

```
app/build/outputs/apk/release/
```

Generated files:
- `app-release.apk` - Signed Release APK
- `output-metadata.json` - Build metadata

---

## 🔍 Verify the Signature

Use the following command to verify the APK signature:

```bash
# Linux / macOS
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# Windows
jarsigner -verify -verbose -certs app\build\outputs\apk\release\app-release.apk
```

Successful verification will show `jar verified`.

---

## 🚨 Frequently Asked Questions

### Q1: Build fails with "keystore.properties not found"
**Cause**: The signing configuration file was not found  
**Solution**:  
- Local build: Create a `keystore.properties` file in the project root directory
- CI build: Add a step in the workflow to create the configuration file

### Q2: Build succeeded, but the generated APK is not signed
**Cause**: The Release build could not find the signing configuration and automatically fell back to using the Debug signature  
**Solution**: Check that `keystore.properties` exists and its content is correct

### Q3: I see the "Using debug signing key for release" warning in the build log
**Meaning**: This is an **important warning** indicating that your Release APK is using the Debug signature
- If just for testing: Can be ignored
- For official release: **Must** configure the correct signing information

### Q4: How to use different signing configurations for different environments?
The project uses **intelligent task name detection**, no manual switching required:

- **Debug Build** (e.g., `assembleDebug`): Completely skips Release signing configuration
- **Release Build** (e.g., `assembleRelease`): Automatically attempts to load `keystore.properties`

This design ensures:
- Local development: No signing configuration needed
- Local release: Just create `keystore.properties`
- CI/CD release: Generate `keystore.properties` file before the build

### Q5: What if my signing information is leaked?
1. **Immediately revoke** the leaked certificate (if possible)
2. **Generate a new keystore** and update all related configurations
3. **Publish a new version** using the new signature
4. **Notify users** that the old version will not receive updates

---

## 📖 Related Documentation

- [Sign your app | Android Studio | Android Developers[↗]](https://developer.android.com/studio/publish/app-signing)
- [Using secrets in GitHub Actions - GitHub Docs[↗]](https://docs.github.com/en/actions/security-guides/using-secrets-in-github-actions)
- [keytool Command Documentation[↗]](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/keytool.html)

---

## ✅ Pre-release Checklist

- [ ] Keystore file securely backed up (offline storage)
- [ ] Keystore passwords securely stored (password manager)
- [ ] `keystore.properties` is ignored by `.gitignore`
- [ ] Running `./gradlew assembleRelease` shows no warnings
- [ ] Signature verification successful using `jarsigner -verify`
- [ ] Testing passed on multiple Android versions
- [ ] For CI builds, confirm Secrets are configured correctly

---

> [!TIP]
> **Best Practices**:
> - Configure a dedicated signing key for official releases, do not mix with test keys
> - It is recommended to set the keystore validity to over 25 years (10000 days ≈ 27 years)

> [!NOTE]
> This document was translated by AI and may contain translation errors. Please verify important information.