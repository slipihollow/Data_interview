package com.datainterview.app.upload

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.DataInputStream
import java.io.FileInputStream
import java.security.KeyPairGenerator
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CsvEncryptorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun generateTestKeyPair(): Pair<String, java.security.PrivateKey> {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        val pair = keyGen.generateKeyPair()
        val publicKeyBase64 = Base64.getEncoder().encodeToString(pair.public.encoded)
        return publicKeyBase64 to pair.private
    }

    /**
     * Decrypt helper that mirrors the .enc file format:
     * [4 bytes: key length][encrypted AES key][12 bytes: IV][ciphertext+tag]
     */
    private fun decryptFile(encFile: java.io.File, privateKey: java.security.PrivateKey): ByteArray {
        val data = encFile.readBytes()
        val stream = DataInputStream(data.inputStream())

        // Read encrypted AES key
        val encKeyLen = stream.readInt()
        val encKey = ByteArray(encKeyLen)
        stream.readFully(encKey)

        // Read IV
        val iv = ByteArray(12)
        stream.readFully(iv)

        // Read ciphertext (remainder)
        val ciphertext = stream.readBytes()

        // Decrypt AES key with RSA private key
        val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey)
        val aesKeyBytes = rsaCipher.doFinal(encKey)

        // Decrypt CSV with AES-GCM
        val aesKey = SecretKeySpec(aesKeyBytes, "AES")
        val gcmCipher = Cipher.getInstance("AES/GCM/NoPadding")
        gcmCipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(128, iv))
        return gcmCipher.doFinal(ciphertext)
    }

    @Test
    fun `encrypt produces file that decrypts to original content`() {
        val (publicKeyBase64, privateKey) = generateTestKeyPair()

        // Create a test CSV
        val csvFile = tempFolder.newFile("test.csv")
        val csvContent = "type_interaction;heure;nom_app_widget\ndeverrouillage;14:30;\n"
        csvFile.writeText(csvContent)

        // Encrypt
        val encryptor = CsvEncryptor(publicKeyBase64)
        val encFile = encryptor.encrypt(csvFile)

        // Verify .enc extension
        assertTrue(encFile.name.endsWith(".enc"))
        assertTrue(encFile.exists())

        // Decrypt and verify content matches
        val decrypted = decryptFile(encFile, privateKey)
        assertEquals(csvContent, String(decrypted))
    }

    @Test
    fun `encrypt deletes the original csv file`() {
        val (publicKeyBase64, _) = generateTestKeyPair()

        val csvFile = tempFolder.newFile("test.csv")
        csvFile.writeText("header\nrow\n")

        val encryptor = CsvEncryptor(publicKeyBase64)
        encryptor.encrypt(csvFile)

        assertFalse("Original CSV should be deleted", csvFile.exists())
    }

    @Test
    fun `encrypted file has correct structure`() {
        val (publicKeyBase64, _) = generateTestKeyPair()

        val csvFile = tempFolder.newFile("test.csv")
        csvFile.writeText("data")

        val encryptor = CsvEncryptor(publicKeyBase64)
        val encFile = encryptor.encrypt(csvFile)

        val stream = DataInputStream(FileInputStream(encFile))
        val encKeyLen = stream.readInt()
        // RSA-2048 produces 256-byte encrypted output
        assertEquals(256, encKeyLen)

        val encKey = ByteArray(encKeyLen)
        stream.readFully(encKey)

        val iv = ByteArray(12)
        stream.readFully(iv)

        // Remaining bytes = ciphertext + 16-byte GCM tag
        val remaining = stream.readBytes()
        // "data" = 4 bytes plaintext + 16 bytes GCM auth tag = 20 bytes
        assertEquals(4 + 16, remaining.size)

        stream.close()
    }

    @Test(expected = Exception::class)
    fun `encrypt with invalid public key throws`() {
        val csvFile = tempFolder.newFile("test.csv")
        csvFile.writeText("data")

        val encryptor = CsvEncryptor("not-a-valid-key")
        encryptor.encrypt(csvFile)
    }
}
