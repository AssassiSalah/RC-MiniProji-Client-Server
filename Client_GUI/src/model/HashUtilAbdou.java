package model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtilAbdou {

    // Hash an entire byte array with the specified algorithm
    public static String computeHash(byte[] data, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        byte[] hashBytes = digest.digest(data);
        return bytesToHex(hashBytes);
    }

    // Hash a specific portion of the byte array (from offset to length) with the specified algorithm
    public static String computeHash(byte[] data, int offset, int length, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        digest.update(data, offset, length);  // Update the digest with a specific part of the byte array
        byte[] hashBytes = digest.digest();  // Finalize the hash
        return bytesToHex(hashBytes);
    }

    // Hash a file with the specified algorithm
    public static String computeHash(File file, String algorithm) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);  // Update the digest with the file content
            }
        }
        byte[] hashBytes = digest.digest();  // Finalize the hash
        return bytesToHex(hashBytes);
    }

    // Hash a packet buffer with the specified algorithm
    public static String computeHashFromBuffer(byte[] packetBuffer, String algorithm) throws NoSuchAlgorithmException {
        return computeHash(packetBuffer, algorithm);
    }

    // Convert byte array to hex string for readability
    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    // Example usage for common hash algorithms
    public static String computeMD5(byte[] data) throws NoSuchAlgorithmException {
        return computeHash(data, "MD5");
    }

    public static String computeSHA1(byte[] data) throws NoSuchAlgorithmException {
        return computeHash(data, "SHA-1");
    }

    public static String computeSHA256(byte[] data) throws NoSuchAlgorithmException {
        return computeHash(data, "SHA-256");
    }

    public static String computeSHA384(byte[] data) throws NoSuchAlgorithmException {
        return computeHash(data, "SHA-384");
    }

    public static String computeSHA512(byte[] data) throws NoSuchAlgorithmException {
        return computeHash(data, "SHA-512");
    }
}
