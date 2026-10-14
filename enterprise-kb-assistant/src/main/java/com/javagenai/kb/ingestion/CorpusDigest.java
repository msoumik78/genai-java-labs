package com.javagenai.kb.ingestion;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 of the raw bytes. Same file, same digest, regardless of which parser ran. */
public final class CorpusDigest {

    private CorpusDigest() {
    }

    public static String sha256(byte[] content) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(content == null ? new byte[0] : content);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required", e);
        }
    }
}
