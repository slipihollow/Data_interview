package com.datainterview.app.upload

import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

class CsvEncryptor(publicKeyBase64: String) {

    private val publicKey = KeyFactory.getInstance("RSA")
        .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)))

    fun encrypt(csvFile: File): File {
        val csvBytes = csvFile.readBytes()

        // Generate random AES-256 key
        val aesKeyGen = KeyGenerator.getInstance("AES")
        aesKeyGen.init(256, SecureRandom())
        val aesKey = aesKeyGen.generateKey()

        // Generate random 12-byte IV
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)

        // Encrypt CSV with AES-256-GCM
        val gcmCipher = Cipher.getInstance("AES/GCM/NoPadding")
        gcmCipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(128, iv))
        val ciphertext = gcmCipher.doFinal(csvBytes)

        // Encrypt AES key with RSA-OAEP
        val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encryptedKey = rsaCipher.doFinal(aesKey.encoded)

        // Write .enc file: [key length][encrypted key][IV][ciphertext]
        val encFile = File(csvFile.parent, csvFile.nameWithoutExtension + ".enc")
        DataOutputStream(FileOutputStream(encFile)).use { out ->
            out.writeInt(encryptedKey.size)
            out.write(encryptedKey)
            out.write(iv)
            out.write(ciphertext)
        }

        // Delete plaintext CSV
        csvFile.delete()

        return encFile
    }
}
