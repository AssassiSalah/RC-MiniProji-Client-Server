package draft;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.FileInputStream;
import java.io.IOException;

public class FTPUploadExample {
    public static void main(String[] args) {
        FTPClient ftpClient = new FTPClient();
        String server = "ftp.dlptest.com";//"ftp.example.com";
        int port = 21;
        String user = "dlpuser";//"username";
        String pass = "rNrKYTX9g7z3RgJRmxWuGHbeu";//"password";
        
        try {
            ftpClient.connect(server, port);
            boolean login = ftpClient.login(user, pass);
            if (login) {
                System.out.println("Connected to the FTP server.");
                
                ftpClient.enterLocalPassiveMode();
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

                // Upload a file
                String localFilePath = "C:/Users/Salah Eddine/Desktop/test.txt";
                String remoteFilePath = "server/test.txt";

	            try (FileInputStream fis = new FileInputStream(localFilePath)) {
	                boolean done = ftpClient.storeFile(remoteFilePath, fis);
	                if (done) {
	                    System.out.println("File uploaded successfully.");
	                } else {
	                    System.out.println("Failed to upload file.");
	                }
	            }
            } else {
            	System.out.println("Failed to connect to the FTP server.");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}

