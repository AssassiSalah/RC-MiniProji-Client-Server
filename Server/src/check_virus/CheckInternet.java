package check_virus;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class CheckInternet {

    // Check if the user is online by pinging Google
    public static boolean isOnline() { // isInternetAvailable
        try {
            // Create a URI and convert it to a URL
            URI uri = new URI("https://www.google.com");
            URL url = uri.toURL();

            // Open a connection and check the response
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD"); // Minimal request to check connectivity
            connection.setConnectTimeout(5000); // 5 seconds timeout for connection
            connection.setReadTimeout(5000);    // 5 seconds timeout for reading

            int responseCode = connection.getResponseCode();

            // Return true if the response code is 200 (HTTP OK)
            return responseCode == HttpURLConnection.HTTP_OK;

        } catch (Exception e) {
            // Handle any exception indicating no internet connection
            return false;
        }
    }
}
