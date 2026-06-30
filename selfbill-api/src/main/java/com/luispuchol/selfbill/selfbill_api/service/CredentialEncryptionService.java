package com.luispuchol.selfbill.selfbill_api.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

@Service
public class CredentialEncryptionService {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;

    private final SecretKey secretKey;

    public CredentialEncryptionService() {
        this.secretKey = loadOrCreateKey();
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv).put(cipherText);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt credential", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] data = Base64.getDecoder().decode(encoded);
            ByteBuffer buffer = ByteBuffer.wrap(data);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt credential", e);
        }
    }

    /**
     * Stored outside the SQLite database (under the user's home directory) so that
     * copying selfbill.db alone is not enough to recover stored credentials.
     */
    private SecretKey loadOrCreateKey() {
        try {
            Path keyPath = Path.of(System.getProperty("user.home"), ".selfbill", "secret.key");
            if (Files.exists(keyPath)) {
                byte[] keyBytes = Base64.getDecoder().decode(Files.readString(keyPath).trim());
                return new SecretKeySpec(keyBytes, "AES");
            }

            Files.createDirectories(keyPath.getParent());
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            SecretKey newKey = keyGenerator.generateKey();
            Files.writeString(keyPath, Base64.getEncoder().encodeToString(newKey.getEncoded()));
            return newKey;
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Failed to initialize credential encryption key", e);
        }
    }
}
