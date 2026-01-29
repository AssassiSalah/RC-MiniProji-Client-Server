package controller;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import application.Load_Interfaces;
import application.Main;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import protocol.Communication;

public class Log_In {

	@FXML
	private TextField usernameField;

	@FXML
	private TextField passwordField;

	
	public Communication communication_Manager;

	@FXML
	public void initialize() {
		communication_Manager = Main.communication_Manager;
	}
	
	private boolean handle_Username_Password(String command) {
		
		if(command.equals("REGISTER")) { // REGISTER
			String username = usernameField.getText();
			String password = passwordField.getText();

			communication_Manager.write(command);
	        communication_Manager.write(username);
	        communication_Manager.write(password);
			
			return communication_Manager.read().contains("Successful");
		}
	    
		try { // LOG_IN
	        String username = usernameField.getText();
	        String password = passwordField.getText();

	        // Send username and password to the server
	        communication_Manager.write(command);
	        communication_Manager.write(username);
	        communication_Manager.write(password);

	        // Read authentication response from the server
	        String response = communication_Manager.read();
	        if (!response.contains("Successful")) {
	            return false;
	        }

	        // Step 1: Receive Server's RSA Public Key
	        // The server sends its RSA public key encoded in Base64 format.
	        String encodedPublicKey = communication_Manager.read();
	        byte[] publicKeyBytes = Base64.getDecoder().decode(encodedPublicKey);

	        // Convert the Base64-decoded bytes into an RSA PublicKey object using KeyFactory.
	        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	        PublicKey serverPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

	        // Step 2: Generate AES Key and Initialization Vector (IV)
	        // Generate a 256-bit AES symmetric key using a KeyGenerator.
	        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
	        keyGen.init(256); // AES-256 for enhanced security
	        SecretKey aesKey = keyGen.generateKey();

	        // Generate a 16-byte IV for AES encryption using a SecureRandom.
	        byte[] iv = new byte[16];
	        SecureRandom secureRandom = new SecureRandom();
	        secureRandom.nextBytes(iv); // Populate the IV with random bytes
	        IvParameterSpec ivSpec = new IvParameterSpec(iv);

	        // Step 3: Encrypt AES Key and IV Using Server's RSA Public Key
	        // Use the server's RSA public key to encrypt the AES key and IV.
	        Cipher rsaCipher = Cipher.getInstance("RSA");
	        rsaCipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);

	        // Encrypt the AES key and IV, then encode the results in Base64 to make them suitable for transmission.
	        String encryptedAESKey = Base64.getEncoder().encodeToString(rsaCipher.doFinal(aesKey.getEncoded()));
	        String encryptedIV = Base64.getEncoder().encodeToString(rsaCipher.doFinal(iv));

	        // Step 4: Send Encrypted AES Key and IV to the Server
	        communication_Manager.write(encryptedAESKey); // Transmit encrypted AES key
	        communication_Manager.write(encryptedIV); // Transmit encrypted IV

	        // Step 5: Save the AES Key and IV Locally for the Session
	        // These will be used for symmetric encryption during the session.
	        communication_Manager.fileManager.setSessionAESKey(aesKey);
	        communication_Manager.fileManager.setSessionIV(ivSpec);

	        System.out.println("Authentication and key exchange completed successfully.");
	        return true;

	    } catch (Exception e) {
	        e.printStackTrace();
	        return false;
	    }
	}



	@FXML
	private void log_In() {
		if(!AppConst.communication_Manager.isConnect())
			AppConst.communication_Manager.connect();
		
		if (handle_Username_Password("LOG_IN")) {
			Load_Interfaces.informationAlert("Log In Successful", "Now You Are Authonticated");
			Load_Interfaces.displayMainApplication();
		} else {
			Load_Interfaces.informationAlert("Log In Failed", "Usernane or the Password Is Inncorect");
		}
	}

	@FXML
	private void register() {
		if(!AppConst.communication_Manager.isConnect())
			AppConst.communication_Manager.connect();
		
		if (handle_Username_Password("REGISTER")) {
			Load_Interfaces.informationAlert("Register Successful", "Now You Can Log In");
		} else {
			Load_Interfaces.informationAlert("Register Failed", "Usernane Already Exist");
		}
	}
}
