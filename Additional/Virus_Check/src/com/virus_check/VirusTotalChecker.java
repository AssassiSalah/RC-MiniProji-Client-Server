package com.virus_check;

import java.io.*;
import java.net.*;

public class VirusTotalChecker {
    private static final String API_KEY = System.getenv("d771c282fb323ad4a933bf849968f505dabd950e9f8af3497f8617e52ee01d07"); // Retrieve API key from environment

    public static void checkFile(String filePath) throws IOException {
        if (API_KEY == null || API_KEY.isEmpty()) {
            System.out.println("API key is missing!");
            return;
        }

        String apiUrl = "https://www.virustotal.com/api/v3/files/";
        URL url = new URL(apiUrl + filePath);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("x-apikey", API_KEY);

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // Print the result (You can customize this part to display a nicer output)
        System.out.println("VirusTotal Scan Result: " + response.toString());
    }
}
