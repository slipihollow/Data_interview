# CSV Encryption Design — Hybrid RSA + AES-256-GCM

**Date:** 2026-03-18
**Status:** Approved
**Scope:** Encrypt CSV files before Telegram upload so only trusted key holders can decrypt

---

## Problem

CSV files containing participant behavioral data (app usage, unlock timestamps, interaction types) are uploaded as plain text to a Telegram channel. Anyone with access to the chat can read the data. For a research app handling sensitive behavioral data, this is insufficient.

## Solution

Hybrid encryption: each CSV is encrypted with a random AES-256-GCM key, and that AES key is encrypted with the researcher's RSA-2048 public key. Only the holder of the corresponding private key can decrypt.

## Encryption Architecture

### File Format (`.enc`)

```
[4 bytes: encrypted AES key length (big-endian int)]
[encrypted AES key (RSA-OAEP + SHA-256, 256 bytes for RSA-2048)]
[12 bytes: AES-GCM IV/nonce]
[remaining bytes: AES-256-GCM ciphertext + 16-byte auth tag]
```

### Encryption Flow

```
CSV file
  -> generate random 256-bit AES key
  -> encrypt CSV bytes with AES-256-GCM (random 12-byte IV)
  -> encrypt AES key with researcher's RSA public key (OAEP + SHA-256)
  -> write .enc file (key length + encrypted key + IV + ciphertext)
  -> upload .enc to Telegram instead of .csv
```

### Key Management

- RSA-2048 public key stored in `gradle.properties` as Base64-encoded DER (X.509 SubjectPublicKeyInfo format)
- Accessed at runtime via `BuildConfig.ENCRYPTION_PUBLIC_KEY`
- Private key stays on the researcher's machine, never in the app
- Multi-recipient support: future enhancement (v1 supports a single public key only)

### Cryptographic Primitives

All from standard Java/Android crypto APIs (`javax.crypto`, `java.security`). No external dependencies.

| Operation | Algorithm | API |
|---|---|---|
| CSV encryption | AES-256-GCM | `javax.crypto.Cipher` |
| AES key generation | SecureRandom 256-bit | `javax.crypto.KeyGenerator` |
| AES key wrapping | RSA/ECB/OAEPWithSHA-256AndMGF1Padding | `javax.crypto.Cipher` |
| Public key loading | X.509 DER from Base64 | `java.security.KeyFactory` + `X509EncodedKeySpec` |
| IV generation | 12-byte SecureRandom | `java.security.SecureRandom` |

**Constraint:** Only standard, public, trusted crypto libraries. Never custom or modified crypto.

**Compatibility note:** AES-GCM is available on Android API 21+ but some early devices had buggy implementations. Test on a low-API emulator during QA. No proguard rules are needed — all crypto APIs are Android framework classes.

## Code Changes

### New Files

1. **`CsvEncryptor.kt`** (`com.datainterview.app.upload`)
   - Constructor takes Base64 public key string (same pattern as `TelegramUploader` taking credentials)
   - Single public method: `fun encrypt(csvFile: File): File`
   - Returns encrypted `File` (`.enc` extension, written alongside the CSV)
   - Handles AES key generation, RSA wrapping, file assembly
   - Deletes the plaintext CSV after successful encryption

2. **`CsvEncryptorTest.kt`**
   - Verifies encrypt/decrypt round-trip with a test keypair
   - Ensures file format is correct (key length prefix, IV, ciphertext)

3. **`decrypt.py`** (project root)
   - CLI script: `python decrypt.py --key private_key.pem --input file.enc`
   - Outputs decrypted `.csv` file
   - Uses Python `cryptography` library (standard, well-audited)

### Modified Files

1. **`ActivationManager.kt`** — after CSV generation, call `CsvEncryptor.encrypt()`, pass `.enc` file to `TelegramUploader`. If encryption fails, do NOT upload the plaintext — log the error and set `uploadStatus = "encryption_failed"`. The `csvFilePath` field in the activation record will now point to the `.enc` file.
2. **`TelegramUploader.kt`** — change content type from `text/csv` to `application/octet-stream`
3. **`gradle.properties`** — add `ENCRYPTION_PUBLIC_KEY=<base64 RSA public key>`
4. **`build.gradle.kts`** — expose `ENCRYPTION_PUBLIC_KEY` via `BuildConfig`

### Untouched

`CsvGenerator`, database, UI, service, all other files.

## Decryption Workflow

### One-Time Setup (Researcher's Machine)

```bash
openssl genrsa -out private_key.pem 2048
openssl rsa -in private_key.pem -pubout -outform DER | base64  # paste into gradle.properties
```

The value in `gradle.properties` must be Base64-encoded DER (not PEM). The command above outputs DER directly and Base64-encodes it, avoiding double-encoding.

### Per-File Decryption

```bash
python decrypt.py --key private_key.pem --input data_interview_1_20260318.enc
# -> outputs: data_interview_1_20260318.csv
```

### Adding Team Members (Future Enhancement)

Multi-recipient encryption is out of scope for v1. When needed, the file format and `CsvEncryptor` will be extended to support multiple public keys (encrypt the AES key once per recipient).

## Testing

- `CsvEncryptorTest`: round-trip encrypt/decrypt with test keypair
- `CsvGeneratorTest`: unchanged, still validates CSV content
- Manual: verify `.enc` file uploads to Telegram and decrypts correctly
