# CSV Encryption Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Encrypt CSV files with hybrid RSA + AES-256-GCM before uploading to Telegram, so only the researcher with the private key can decrypt them.

**Architecture:** New `CsvEncryptor` class sits between `CsvGenerator` and `TelegramUploader` in the upload pipeline. Uses standard `javax.crypto` / `java.security` APIs. RSA-2048 public key loaded from `BuildConfig`. A Python `decrypt.py` script provides the researcher-side decryption workflow.

**Tech Stack:** Kotlin, Java crypto APIs (AES-256-GCM, RSA-OAEP), Python `cryptography` library for decryption script.

**Spec:** `docs/superpowers/specs/2026-03-18-csv-encryption-design.md`

---

## File Structure

| Action | File | Responsibility |
|--------|------|---------------|
| Create | `android/app/src/main/java/com/datainterview/app/upload/CsvEncryptor.kt` | Encrypt a CSV file using hybrid RSA+AES-GCM |
| Create | `android/app/src/test/java/com/datainterview/app/upload/CsvEncryptorTest.kt` | Round-trip encrypt/decrypt tests |
| Create | `decrypt.py` (project root) | CLI tool for researcher to decrypt `.enc` files |
| Modify | `android/gradle.properties` | Add `ENCRYPTION_PUBLIC_KEY` |
| Modify | `android/app/build.gradle.kts` | Expose `ENCRYPTION_PUBLIC_KEY` via `BuildConfig` |
| Modify | `android/app/src/main/java/com/datainterview/app/upload/TelegramUploader.kt` | Change content type to `application/octet-stream` |
| Modify | `android/app/src/main/java/com/datainterview/app/activation/ActivationManager.kt` | Insert encryption step, handle failures |

---

### Task 1: Build Config — Expose encryption public key

**Files:**
- Modify: `android/gradle.properties`
- Modify: `android/app/build.gradle.kts:20-22`

- [ ] **Step 1: Generate a test RSA keypair for development**

Run on your machine:

```bash
openssl genrsa -out /tmp/test_private_key.pem 2048
openssl rsa -in /tmp/test_private_key.pem -pubout -outform DER | base64
```

Copy the Base64 output (single line, no line breaks).

- [ ] **Step 2: Add ENCRYPTION_PUBLIC_KEY to gradle.properties**

In `android/gradle.properties`, add after the Telegram config:

```properties
# Encryption Config (researcher's RSA-2048 public key, Base64-encoded DER)
# NOTE: Unlike Telegram credentials, this public key is NOT a secret.
# It can only encrypt, not decrypt. Safe to commit to version control.
ENCRYPTION_PUBLIC_KEY=<paste Base64 DER from step 1>
```

- [ ] **Step 3: Expose via BuildConfig in build.gradle.kts**

In `android/app/build.gradle.kts`, inside `defaultConfig {}`, after the existing `TELEGRAM_CHAT_ID` line, add:

```kotlin
buildConfigField("String", "ENCRYPTION_PUBLIC_KEY", "\"${project.findProperty("ENCRYPTION_PUBLIC_KEY") ?: ""}\"")
```

- [ ] **Step 4: Verify the project builds**

Run:

```bash
cd android && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL. `BuildConfig.ENCRYPTION_PUBLIC_KEY` is now available.

- [ ] **Step 5: Commit**

```bash
git add android/gradle.properties android/app/build.gradle.kts
git commit -m "feat: add ENCRYPTION_PUBLIC_KEY to build config"
```

---

### Task 2: CsvEncryptor — Write failing tests

**Files:**
- Create: `android/app/src/test/java/com/datainterview/app/upload/CsvEncryptorTest.kt`

This test file generates its own RSA keypair at test time and verifies the full encrypt/decrypt round-trip. It uses standard Java crypto APIs to decrypt (simulating what the Python script does).

- [ ] **Step 1: Write the test file**

Create `android/app/src/test/java/com/datainterview/app/upload/CsvEncryptorTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```bash
cd android && ./gradlew test --tests "com.datainterview.app.upload.CsvEncryptorTest" --info
```

Expected: FAIL — `CsvEncryptor` class does not exist yet.

- [ ] **Step 3: Commit the failing tests**

```bash
git add android/app/src/test/java/com/datainterview/app/upload/CsvEncryptorTest.kt
git commit -m "test: add CsvEncryptor tests (red phase)"
```

---

### Task 3: CsvEncryptor — Implementation

**Files:**
- Create: `android/app/src/main/java/com/datainterview/app/upload/CsvEncryptor.kt`

- [ ] **Step 1: Write the CsvEncryptor class**

Create `android/app/src/main/java/com/datainterview/app/upload/CsvEncryptor.kt`:

```kotlin
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
```

Uses `java.util.Base64` (available on Android API 26+ and in JVM unit tests). Android API 21-25 devices are <1% market share — acceptable trade-off for clean, testable code with no `android.util` dependency.

- [ ] **Step 2: Run the tests to verify they pass**

Run:

```bash
cd android && ./gradlew test --tests "com.datainterview.app.upload.CsvEncryptorTest" --info
```

Expected: All 4 tests PASS.

- [ ] **Step 3: Run the full test suite to verify no regressions**

Run:

```bash
cd android && ./gradlew test --info
```

Expected: All tests PASS (CsvGeneratorTest + CsvEncryptorTest).

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/java/com/datainterview/app/upload/CsvEncryptor.kt
git commit -m "feat: implement CsvEncryptor with hybrid RSA+AES-GCM"
```

---

### Task 4: TelegramUploader — Change content type

**Files:**
- Modify: `android/app/src/main/java/com/datainterview/app/upload/TelegramUploader.kt:32-36`

- [ ] **Step 1: Update the content type to be based on file extension**

In `TelegramUploader.kt`, change line 35 from:

```kotlin
file.asRequestBody("text/csv".toMediaType())
```

to:

```kotlin
val contentType = if (file.extension == "enc") "application/octet-stream" else "text/csv"
file.asRequestBody(contentType.toMediaType())
```

- [ ] **Step 2: Verify project builds**

Run:

```bash
cd android && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/java/com/datainterview/app/upload/TelegramUploader.kt
git commit -m "feat: change Telegram upload content type to octet-stream for encrypted files"
```

---

### Task 5: ActivationManager — Insert encryption step

**Files:**
- Modify: `android/app/src/main/java/com/datainterview/app/activation/ActivationManager.kt:49-77`

- [ ] **Step 1: Add CsvEncryptor import**

Add at the top of `ActivationManager.kt`, after the existing imports:

```kotlin
import com.datainterview.app.upload.CsvEncryptor
```

- [ ] **Step 2: Insert encryption between CSV generation and upload**

In `ActivationManager.kt`, replace the section from CSV generation through upload (lines 49–77) with:

```kotlin
        // Generate CSV
        val events = db.eventDao().getByActivation(activation.id)
        val csvFile = CsvGenerator(context).generate(events, activation.id)

        // Encrypt CSV (deletes plaintext on success)
        val publicKey = BuildConfig.ENCRYPTION_PUBLIC_KEY
        val fileToUpload = if (publicKey.isNotEmpty()) {
            try {
                CsvEncryptor(publicKey).encrypt(csvFile)
            } catch (e: Exception) {
                android.util.Log.e("ActivationManager", "Encryption failed", e)
                // Do NOT upload plaintext — delete it and mark as failed
                csvFile.delete()
                db.activationDao().update(
                    activation.copy(
                        endTime = System.currentTimeMillis(),
                        status = Activation.STATUS_COMPLETED,
                        csvFilePath = null,
                        eventCount = db.eventDao().countByActivation(activation.id),
                        uploadStatus = "encryption_failed"
                    )
                )
                return
            }
        } else {
            csvFile
        }

        // Update activation record
        val eventCount = db.eventDao().countByActivation(activation.id)
        db.activationDao().update(
            activation.copy(
                endTime = System.currentTimeMillis(),
                status = Activation.STATUS_COMPLETED,
                csvFilePath = fileToUpload.absolutePath,
                eventCount = eventCount,
                uploadStatus = "pending"
            )
        )

        // Auto-upload via Telegram (credentials baked in at build time)
        val token = BuildConfig.TELEGRAM_BOT_TOKEN
        val chatId = BuildConfig.TELEGRAM_CHAT_ID
        if (token.isNotEmpty() && chatId.isNotEmpty()) {
            val success = withContext(Dispatchers.IO) {
                TelegramUploader(token, chatId).upload(fileToUpload)
            }
            db.activationDao().update(
                db.activationDao().getById(activation.id)!!.copy(
                    uploadStatus = if (success) "success" else "failed"
                )
            )
        }
```

- [ ] **Step 3: Verify project builds**

Run:

```bash
cd android && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run all tests**

Run:

```bash
cd android && ./gradlew test --info
```

Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/com/datainterview/app/activation/ActivationManager.kt
git commit -m "feat: encrypt CSV before Telegram upload, block plaintext on failure"
```

---

### Task 6: Decryption script

**Files:**
- Create: `decrypt.py` (project root)

- [ ] **Step 1: Write the decryption script**

Create `decrypt.py` at the project root:

```python
#!/usr/bin/env python3
"""Decrypt .enc files produced by the Data Interview app.

Usage:
    python decrypt.py --key private_key.pem --input file.enc

Requires: pip install cryptography
"""
import argparse
import struct
from pathlib import Path

from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.ciphers.aead import AESGCM


def decrypt(private_key_path: str, enc_path: str) -> None:
    # Load private key
    with open(private_key_path, "rb") as f:
        private_key = serialization.load_pem_private_key(f.read(), password=None)

    # Read .enc file
    data = Path(enc_path).read_bytes()
    offset = 0

    # [4 bytes] encrypted AES key length
    (enc_key_len,) = struct.unpack(">I", data[offset : offset + 4])
    offset += 4

    # [enc_key_len bytes] encrypted AES key
    enc_key = data[offset : offset + enc_key_len]
    offset += enc_key_len

    # [12 bytes] IV
    iv = data[offset : offset + 12]
    offset += 12

    # [remaining] ciphertext + GCM tag
    ciphertext = data[offset:]

    # Decrypt AES key with RSA-OAEP
    aes_key = private_key.decrypt(
        enc_key,
        padding.OAEP(
            mgf=padding.MGF1(algorithm=hashes.SHA1()),
            algorithm=hashes.SHA256(),
            label=None,
        ),
    )

    # Decrypt CSV with AES-GCM
    aesgcm = AESGCM(aes_key)
    plaintext = aesgcm.decrypt(iv, ciphertext, None)

    # Write output
    out_path = Path(enc_path).with_suffix(".csv")
    out_path.write_bytes(plaintext)
    print(f"Decrypted: {out_path}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Decrypt Data Interview .enc files")
    parser.add_argument("--key", required=True, help="Path to RSA private key (PEM)")
    parser.add_argument("--input", required=True, help="Path to .enc file")
    args = parser.parse_args()
    decrypt(args.key, args.input)
```

**Important note on MGF1 hash:** Java's `RSA/ECB/OAEPWithSHA-256AndMGF1Padding` uses SHA-256 for OAEP but **SHA-1 for MGF1** by default. The Python script must match: `MGF1(algorithm=hashes.SHA1())` with `algorithm=hashes.SHA256()`.

- [ ] **Step 2: Test the script against a test-encrypted file**

Generate a test keypair, encrypt a test file with the app's logic, then decrypt:

```bash
# Generate test key
openssl genrsa -out /tmp/test_key.pem 2048
openssl rsa -in /tmp/test_key.pem -pubout -outform DER | base64 > /tmp/test_pubkey.b64

# Create a small test CSV
echo "type_interaction;heure;nom_app_widget" > /tmp/test.csv
echo "deverrouillage;14:30;" >> /tmp/test.csv

# Run the Android unit test that produces an .enc file, or use the app
# For now, verify the script at least parses arguments correctly:
python3 decrypt.py --key /tmp/test_key.pem --input /tmp/nonexistent.enc 2>&1 || true
```

Expected: FileNotFoundError (confirming the script loads and parses args). Full round-trip testing happens in Task 7.

- [ ] **Step 3: Commit**

```bash
git add decrypt.py
git commit -m "feat: add decrypt.py script for researcher-side .enc decryption"
```

---

### Task 7: End-to-end verification

- [ ] **Step 1: Run the full test suite**

```bash
cd android && ./gradlew test --info
```

Expected: All tests PASS.

- [ ] **Step 2: Build the APK**

```bash
cd android && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Manual round-trip test**

Write a quick JUnit test or Kotlin script that:
1. Creates a CSV
2. Encrypts it with `CsvEncryptor`
3. Runs `decrypt.py` on the `.enc` output
4. Compares the decrypted CSV to the original

This is already covered by `CsvEncryptorTest` for the Java side. For the Python script, do a manual test once the app produces its first `.enc` file on a real device/emulator.

- [ ] **Step 4: Verify .gitignore excludes private keys**

Check that `*.pem` is in `.gitignore` (or add it) to prevent accidental commit of private keys:

```bash
grep -q "\*.pem" .gitignore || echo "*.pem" >> .gitignore
git add .gitignore
git commit -m "chore: add *.pem to gitignore to prevent private key commits"
```

- [ ] **Step 5: Final commit (if any remaining changes)**

```bash
git status
# If clean, nothing to do. Otherwise:
git add -A && git commit -m "feat: CSV encryption complete — hybrid RSA+AES-GCM"
```
