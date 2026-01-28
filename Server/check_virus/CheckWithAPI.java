package check_virus;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

import java.io.File;
import java.net.HttpURLConnection;

public class CheckWithAPI {
	// this is my API_KEY Replace with your actual API key
    private static final String API_KEY = "d8a5989672eccfc593947bd43427237bffc669216da12e349dfd86b76d6ce5e1";//"e969ad249616db21a38e88c6fb57105a8399b94b5561c71b69682d3bb03401be"; 

    protected static boolean isSafe(File file) {	
    	
    	if (!CheckInternet.isOnline()) {
    	    System.out.println("No internet connection. Please check your network and try again.");
    	    return false;
    	}
    	
    	try {
            // Step 1: Upload the file
            String fileId = uploadFileToVirusTotal(file);
            if (fileId == null) {
                return false; // File upload failed
            }

            // Step 2: Poll the analysis status
            JSONObject stats = pollAnalysisStatus(fileId);
            if (stats == null) {
                return false; // Analysis did not complete
            }

            // Step 3: Process the analysis results
            return processAnalysisResults(stats);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String uploadFileToVirusTotal(File file) throws Exception {
        HttpResponse<JsonNode> response = Unirest.post("https://www.virustotal.com/api/v3/files")
                .header("x-apikey", API_KEY)
                .field("file", file)
                .asJson();
        
        /*
        .asJsonAsync(response -> {
      		// Handle the response in a callback
      		System.out.println(response.getBody());
  			});
        */

        if (response.getStatus() != HttpURLConnection.HTTP_OK) {
            System.out.println("Failed to upload file. Status code: " + response.getStatus());
            return null;
        }

        System.out.println("File successfully uploaded.");
        JSONObject responseBody = response.getBody().getObject();
        return responseBody.getJSONObject("data").getString("id");
    }
    
    private static JSONObject fetchAnalysisReport(String fileId) throws Exception {
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

    private static JSONObject pollAnalysisStatus(String fileId) throws Exception {
        for (int i = 0; i < 10; i++) { // Retry up to 10 times with a 6-second interval
            Thread.sleep(6000);

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

    private static boolean processAnalysisResults(JSONObject stats) {
        int maliciousCount = stats.getInt("malicious");
        System.out.println("Analysis completed. Malicious count: " + maliciousCount);
        return maliciousCount == 0; // File is safe if no malicious detections
    }

}
