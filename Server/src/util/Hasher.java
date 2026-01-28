package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for hashing operations.
 * Provides methods to hash strings, files, and byte arrays using SHA-256.
 */
public class Hasher {

    /**
     * Hashes a given password string using SHA-256.
     *
     * @param password the string to hash
     * @return the hashed string in hexadecimal format
     * @throws RuntimeException if the hashing algorithm is unavailable
     */
    public static String hashPassword(String password) {
        try {
            // Initialize SHA-256 MessageDigest
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Compute the hash
            byte[] hashBytes = digest.digest(password.getBytes());

            // Convert hash bytes to a hexadecimal string
            return bytesToHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error: SHA-256 algorithm not available.");
            throw new RuntimeException("Hashing failed due to missing algorithm: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a SHA-256 hash for a file.
     *
     * @param choosenFile the file to hash
     * @return the SHA-256 hash of the file as a hexadecimal string,
     *         or an empty string if the file does not exist
     */
    public static String hashFile(File choosenFile) {
        // Check if the file exists
        if (!choosenFile.exists()) {
            System.err.println("Error: This file doesn't exist.");
            return "";
        }

        // Delegate to the path-based hashing method
        return hashFile(choosenFile.getAbsolutePath());
    }

    /**
     * Generates a SHA-256 hash for a file given its path.
     *
     * @param filePath the path of the file to hash
     * @return the SHA-256 hash of the file as a hexadecimal string
     * @throws RuntimeException if an error occurs during hashing
     */
    private static String hashFile(String filePath) {
        try {
            // Initialize SHA-256 MessageDigest
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Read the file and update the digest
            try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            // Convert the hash bytes to a hexadecimal string
            return bytesToHex(digest.digest());

        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error: SHA-256 algorithm not available. Ensure your Java environment supports it.");
            throw new RuntimeException("Hashing failed due to missing algorithm: " + e.getMessage(), e);
        } catch (IOException e) {
            System.err.println("Error: Unable to read the file at path: " + filePath);
            System.err.println("Details: " + e.getMessage());
            throw new RuntimeException("Hashing failed due to an I/O error: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a byte array to a hexadecimal string for readability.
     *
     * @param bytes the byte array to convert
     * @return the hexadecimal representation of the byte array
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    /**
     * Computes a hash of a byte array using the specified algorithm.
     *
     * @param data      the byte array to hash
     * @param algorithm the hashing algorithm to use (e.g., "SHA-256")
     * @return the computed hash as a hexadecimal string
     * @throws NoSuchAlgorithmException if the specified algorithm is unavailable
     */
    private static String computeHash(byte[] data, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        byte[] hashBytes = digest.digest(data);
        return bytesToHex(hashBytes);
    }

    /**
     * Computes a SHA-256 hash of a byte array.
     *
     * @param data the byte array to hash
     * @return the computed SHA-256 hash as a hexadecimal string
     * @throws NoSuchAlgorithmException if SHA-256 is unavailable
     */
    public static String computeSHA256(byte[] data) throws NoSuchAlgorithmException {
        return computeHash(data, "SHA-256");
    }
    
 // Method to compute SHA-256 hash for a given byte array, starting from offset and reading length bytes
    public static String computeSHA256(byte[] buffer, int offset, int length) {
        try {
            // Create a MessageDigest instance for SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Update the digest with the specified portion of the byte array
            digest.update(buffer, offset, length);

            // Get the hash as a byte array
            byte[] hashBytes = digest.digest();

            // Convert the byte array to a hexadecimal string
            return bytesToHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            // Handle error if SHA-256 algorithm is not available (should never happen)
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
