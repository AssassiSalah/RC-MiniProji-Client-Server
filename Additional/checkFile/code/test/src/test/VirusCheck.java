package test;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;

import java.io.File;

public class VirusCheck {
	// this is my API_KEY Replace with your actual API key
    private static final String API_KEY = "//*MOUTAZ API*//"; 

    public  void testFile(File file) {
     // Replace with the actual path to your file
        if (isFileSafe(file)) {
            System.out.println("The file is safe.");
        } else {
            System.out.println("The file might be infected.");
        }
    }

    public static boolean isFileSafe(File file) {
        try {
            // Upload the file to VirusTotal
            HttpResponse<JsonNode> response = Unirest.post("https://www.virustotal.com/api/v3/files")
                .header("x-apikey", API_KEY)
                .field("file", file)
                .asJson();

           
            if (response.getStatus() != 200) {
                System.out.println("Failed to upload file. Status code: " + response.getStatus());
                return false;
            }

            
            kong.unirest.json.JSONObject responseBody = response.getBody().getObject();
            String fileId = responseBody.getJSONObject("data").getString("id");

            
            Thread.sleep(10000); 

            HttpResponse<JsonNode> reportResponse = Unirest.get("https://www.virustotal.com/api/v3/analyses/" + fileId)
                .header("x-apikey", API_KEY)
                .asJson();

            kong.unirest.json.JSONObject reportObject = reportResponse.getBody().getObject();
            kong.unirest.json.JSONObject stats = reportObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats");

            
            int maliciousCount = stats.getInt("malicious");

            
            return maliciousCount == 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
