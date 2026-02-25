/*
OpenDroidChat Keystore Encryption Util
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// 密钥别名
private const val KEY_ALIAS = "llm_api_key_master_key"
// GCM 标签长度
private const val GCM_TAG_LENGTH = 128

/**
 * 封装加密后的数据：密文和初始化向量 (IV)
 */
data class CiphertextWrapper(val ciphertext: ByteArray, val iv: ByteArray)

/**
 * 使用原始 Android Keystore API 的加解密工具类
 */
class KeyStoreEncryptionUtil {

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    // 获取或生成 SecretKey
    private val secretKey: SecretKey
        get() {
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                generateNewKey()
            }
            // 从 Keystore 加载密钥
            return keyStore.getKey(KEY_ALIAS, null) as SecretKey
        }

    // 在 Keystore 中生成新的 AES 密钥
    private fun generateNewKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    // ---------------------- 加密 ----------------------
    fun encrypt(plainText: String): CiphertextWrapper {
        val cipher = Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv

        return CiphertextWrapper(ciphertext, iv)
    }

    // ---------------------- 解密 ----------------------
    fun decrypt(wrapper: CiphertextWrapper): String {
        val cipher = Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, wrapper.iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decryptedBytes = cipher.doFinal(wrapper.ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}

// ------------------- 扩展函数 (用于 DataStore 存储) -------------------

// 将字节数组转换为 Base64 字符串以便安全存储到 JSON/DataStore 中
fun ByteArray.toBase64String(): String =
    android.util.Base64.encodeToString(this, android.util.Base64.NO_WRAP)

fun String.fromBase64String(): ByteArray =
    android.util.Base64.decode(this, android.util.Base64.NO_WRAP)

/**
 * 将 CiphertextWrapper 序列化为单个字符串 (格式: 密文:IV)
 * 解决 SettingsRepository 中的 toStorageString 错误
 */
fun CiphertextWrapper.toStorageString(): String {
    return "${this.ciphertext.toBase64String()}:${this.iv.toBase64String()}"
}

/**
 * 从存储字符串反序列化为 CiphertextWrapper
 * 解决 SettingsRepository 中的 toCiphertextWrapper 错误
 */
fun String.toCiphertextWrapper(): CiphertextWrapper {
    val parts = this.split(":")
    require(parts.size == 2) { "Invalid storage string format" }
    return CiphertextWrapper(parts[0].fromBase64String(), parts[1].fromBase64String())
}