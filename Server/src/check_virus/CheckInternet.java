package check_virus;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * Utility class to check internet connectivity.
 * It verifies if the user is online by attempting to reach a well-known website (Google).
 */
public class CheckInternet {

    /**
     * Checks if the internet connection is available by sending a minimal request (HEAD)
     * to a reliable website (Google).
     *
     * @return {@code true} if an internet connection is available, {@code false} otherwise
     */
    public static boolean isOnline() {
        try {
            // Create a URI and convert it to a URL
            URI uri = new URI("https://www.google.com");
            URL url = uri.toURL();

            // Open a connection and configure it for minimal interaction
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD"); // Minimal request to check connectivity
            connection.setConnectTimeout(5000); // 5 seconds timeout for connection
            connection.setReadTimeout(5000);    // 5 seconds timeout for reading

            // Get the HTTP response code
            int responseCode = connection.getResponseCode();

            // Return true if the response code indicates success (HTTP OK)
            return responseCode == HttpURLConnection.HTTP_OK;

        } catch (Exception e) {
            // Handle any exception indicating no internet connection
            return false;
        }
    }
}
