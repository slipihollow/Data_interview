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
            mgf=padding.MGF1(algorithm=hashes.SHA256()),
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
