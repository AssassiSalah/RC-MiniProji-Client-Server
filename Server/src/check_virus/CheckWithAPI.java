package check_virus;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import server.AppConst;

import java.io.File;
import java.net.HttpURLConnection;

/**
 * This class handles file safety checks by interacting with the VirusTotal API.
 * It provides methods to upload files, poll analysis status, and process results.
 */
public class CheckWithAPI {

    /**
     * API key for VirusTotal. Retrieved from AppConst configuration.
     */
    private static final String API_KEY = AppConst.API_KEY;

    /**
     * Checks if a given file is safe by uploading it to VirusTotal, analyzing its status, 
     * and processing the results.
     *
     * @param file the file to be checked
     * @return {@code true} if the file is deemed safe, {@code false} otherwise
     */
    protected static boolean isSafe(File file) {
        if (!CheckInternet.isOnline()) {
            System.out.println("No internet connection. Please check your network and try again.");
            return false;
        }

        // Step 1: Upload the file
        String fileId = uploadFileToVirusTotal(file);
        if (fileId == null) {
            return false; // File upload failed
        }

        // Step 2: Poll the analysis status
        JSONObject stats = pollAnalysisStatus(fileId);
        if (stats == null) {
            // Save the fileId
            return true; // Analysis did not complete
        }

        // Step 3: Process the analysis results
        return processAnalysisResults(stats);
    }

    /**
     * Uploads a file to VirusTotal for analysis.
     *
     * @param file the file to upload
     * @return the file ID assigned by VirusTotal, or {@code null} if the upload fails
     */
    private static String uploadFileToVirusTotal(File file) {
        HttpResponse<JsonNode> response = Unirest.post("https://www.virustotal.com/api/v3/files")
                .header("x-apikey", API_KEY)
                .field("file", file)
                .asJson();

        if (response.getStatus() != HttpURLConnection.HTTP_OK) {
            System.out.println("Failed to upload file. Status code: " + response.getStatus());
            return null;
        }

        System.out.println("File successfully uploaded.");
        JSONObject responseBody = response.getBody().getObject();
        return responseBody.getJSONObject("data").getString("id");
    }

    /**
     * Retrieves the analysis report for a given file ID.
     *
     * @param fileId the file ID to fetch the report for
     * @return the analysis report as a {@link JSONObject}, or {@code null} if retrieval fails
     */
    private static JSONObject fetchAnalysisReport(String fileId) {
        HttpResponse<JsonNode> reportResponse = Unirest.get("https://www.virustotal.com/api/v3/analyses/" + fileId)
                .header("x-apikey", API_KEY)
                .asJson();

        System.out.println(reportResponse.getStatus());
        if (reportResponse.getStatus() != HttpURLConnection.HTTP_OK) {
            System.out.println("Failed to retrieve analysis report. Status code: " + reportResponse.getStatus());
            return null;
        }

        return reportResponse.getBody().getObject();
    }

    /**
     * Polls the analysis status for a given file ID until it is complete or the retry limit is reached.
     *
     * @param fileId the file ID to check
     * @return the analysis statistics as a {@link JSONObject}, or {@code null} if polling fails
     */
    private static JSONObject pollAnalysisStatus(String fileId) {
        for (int i = 0; i < 10; i++) { // Retry up to 10 times with a 6-second interval
            try {
                Thread.sleep(6000);
            } catch (InterruptedException e) {
                System.err.println("Sleep interrupted");
                return null;
            }

            JSONObject reportObject = fetchAnalysisReport(fileId);
            if (reportObject == null) {
                continue; // Retry if the report fetch failed
            }

            JSONObject attributes = reportObject.getJSONObject("data").getJSONObject("attributes");
            String status = attributes.getString("status");

            System.out.println("Scan status: " + status);
            if ("completed".equals(status)) {
                return attributes.getJSONObject("stats");
            }
        }

        System.out.println("Analysis did not complete within the expected time.");
        return null;
    }

    /**
     * Processes the analysis results and determines if the file is safe.
     *
     * @param stats the analysis statistics
     * @return {@code true} if the file has no malicious detections, {@code false} otherwise
     */
    private static boolean processAnalysisResults(JSONObject stats) {
        int maliciousCount = stats.getInt("malicious");
        System.out.println("Analysis completed. Malicious count: " + maliciousCount);
        return maliciousCount == 0; // File is safe if no malicious detections
    }

    /**
     * Checks the safety of a file using its previously generated file ID.
     *
     * @param fileId the file ID to check
     * @return {@code true} if the file is safe, {@code false} otherwise
     */
    public static boolean chechWithID(String fileId) {
        // Step 2: Poll the analysis status
        JSONObject stats = pollAnalysisStatus(fileId);
        if (stats == null) {
            return false; // Analysis did not complete
        }

        // Step 3: Process the analysis results
        return processAnalysisResults(stats);
    }
}
