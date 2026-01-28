package server;

/**
 * A utility class to store constants used throughout the application.
 * This includes API keys and commonly used file paths.
 */
public class AppConst {

    /**
     * API key for accessing external services like VirusTotal.
     * Replace "API_Code" with your actual API key.
     */
    public static String API_KEY_SALAH = "API_Code";

    /**
     * The base path for the project's storage directory.
     * Defaults to a folder named "RC_miniproj" in the user's home directory.
     */
    public static final String PATH_PROJECT = System.getProperty("user.home") + "/RC_miniproj";

    /**
     * The directory for server-side storage within the project.
     * Defaults to a folder named "Server_storage" inside the project directory.
     */
    public static final String PATH_SERVER = PATH_PROJECT + "/Server_storage";
}
