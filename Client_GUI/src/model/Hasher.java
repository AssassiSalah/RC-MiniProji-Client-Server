package model;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.File;

public class Hasher {
	
	/**
     * Hashes a given string using SHA-256 and returns the hash as a hexadecimal string.
     *
     * @param input the string to hash
     * @return the hashed string in hexadecimal format
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
            System.out.println("Error: SHA-256 algorithm not available.");
            throw new RuntimeException("Hashing failed due to missing algorithm: " + e.getMessage(), e);
        }
    }
	
	/**
	 * Generates a SHA-256 hash for the given file.
	 *
	 * @param choosenFile the file to hash
	 * @return the SHA-256 hash of the file as a hexadecimal string, 
	 *         or an empty string if the file does not exist
	 */
	public static String hashFile(File choosenFile) {
	    // Check if the file exists
	    if (!choosenFile.exists()) {
	        System.out.println("This File Doesn't Exist");
	        return "";
	    }

	    // Delegate to the path-based hashing method
	    return hashFile(choosenFile.getAbsolutePath());
	}
	
	/**
	 * Generates a SHA-256 hash for the given file.
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
	        System.out.println("Error: SHA-256 algorithm not available. Ensure your Java environment supports it.");
	        throw new RuntimeException("Hashing failed due to missing algorithm: " + e.getMessage(), e);
	    } catch (IOException e) {
	        System.out.println("Error: Unable to read the file at path: " + filePath);
	        System.out.println("Details: " + e.getMessage());
	        throw new RuntimeException("Hashing failed due to an I/O error: " + e.getMessage(), e);
	    }
	}

	/**
	 * Converts a byte array to a hexadecimal string.
	 *
	 * @param bytes the byte array to convert
	 * @return the hexadecimal representation of the byte array
	 */
	private static String bytesToHex(byte[] bytes) {
	    StringBuilder hexString = new StringBuilder();
	    for (byte b : bytes) {
	        hexString.append(String.format("%02x", b));
	    }
	    return hexString.toString();
	}

}
