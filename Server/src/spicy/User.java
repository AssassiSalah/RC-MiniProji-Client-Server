package spicy;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class User {
    private String username;
    private String hashPassword;
    private String publicKey;
    private int totalFilesSent;
    private int totalFilesReceived;
    private int numberOfVirusSends;
    private String lastTimeConnected;
    private String lastIpUsed;
    private boolean normalDisconnect;
    private LocalDateTime joinTime;  // Change to LocalDateTime
    private List<String> listFiles;
    private int historyId;
    private List<CommandExecutionState> commands;  // Change to store CommandExecutionState objects
    private String visibility;
    private LocalDateTime timeExecution;  // Change to LocalDateTime
    private boolean executedCorrectly;
    private List<FileWithVisibility> filesWithVisibility;  // New list to store files with visibility

    public void setFilesWithVisibility(List<FileWithVisibility> filesWithVisibility) {
		this.filesWithVisibility = filesWithVisibility;
	}
 // Constructor with LocalDateTime for joinTime and timeExecution
    public User(String username, String hashPassword, String publicKey, int totalFilesSent, int totalFilesReceived,
                int numberOfVirusSends, String lastTimeConnected, String lastIpUsed, boolean normalDisconnect, 
                String joinTimeStr, List<String> listFiles, int historyId, List<CommandExecutionState> commands, 
                String visibility, String timeExecutionStr, boolean executedCorrectly) {
        
        this.username = username;
        this.hashPassword = hashPassword;
        this.publicKey = publicKey;
        this.totalFilesSent = totalFilesSent;
        this.totalFilesReceived = totalFilesReceived;
        this.numberOfVirusSends = numberOfVirusSends;
        this.lastTimeConnected = lastTimeConnected;
        this.lastIpUsed = lastIpUsed;
        this.normalDisconnect = normalDisconnect;
        this.joinTime = convertStringToLocalDateTime(joinTimeStr);
        this.listFiles = listFiles;
        this.historyId = historyId;
        this.commands = commands;
        this.visibility = visibility;
        this.timeExecution = convertStringToLocalDateTime(timeExecutionStr);
        this.executedCorrectly = executedCorrectly;
        this.setListFiles_categorizeFilesByVisibility(this.getListFiles());
    }

    // Method to categorize files by visibility
    private List<FileWithVisibility> categorizeFilesByVisibility(List<String> listFiles) {
        List<FileWithVisibility> categorizedFiles = new ArrayList<>();
        for (String file : listFiles) {
            String visibility = file.startsWith("private") ? "private" : "public";
            categorizedFiles.add(new FileWithVisibility(file, visibility));
        }
        return categorizedFiles;
    }


    public List<FileWithVisibility> getFilesWithVisibility() {
        return filesWithVisibility != null ? filesWithVisibility : List.of();
    }

    // Helper class to store file name and visibility
    public static class FileWithVisibility {
        private String fileName;
        private String visibility;

        public FileWithVisibility(String fileName, String visibility) {
            this.fileName = fileName;
            this.visibility = visibility;
        }

        public String getFileName() {
            return fileName;
        }

        public String getVisibility() {
            return visibility;
        }
    }
    // Utility method to convert String to LocalDateTime
    private LocalDateTime convertStringToLocalDateTime(String dateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return LocalDateTime.parse(dateStr, formatter);
    }

    // Getter and Setter methods
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    public String getJoinTimeString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return joinTime.format(formatter);
    }

    public String getTimeExecutionString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return timeExecution.format(formatter);
    }

    public String getHashPassword() {
        return hashPassword;
    }

    public void setHashPassword(String hashPassword) {
        this.hashPassword = hashPassword;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public int getTotalFilesSent() {
        return totalFilesSent;
    }

    public void setTotalFilesSent(int totalFilesSent) {
        this.totalFilesSent = totalFilesSent;
    }

    public int getTotalFilesReceived() {
        return totalFilesReceived;
    }

    public void setTotalFilesReceived(int totalFilesReceived) {
        this.totalFilesReceived = totalFilesReceived;
    }

    public int getNumberOfVirusSends() {
        return numberOfVirusSends;
    }

    public void setNumberOfVirusSends(int numberOfVirusSends) {
        this.numberOfVirusSends = numberOfVirusSends;
    }

    public String getLastTimeConnected() {
        return lastTimeConnected;
    }

    public void setLastTimeConnected(String lastTimeConnected) {
        this.lastTimeConnected = lastTimeConnected;
    }

    public String getLastIpUsed() {
        return lastIpUsed;
    }

    public void setLastIpUsed(String lastIpUsed) {
        this.lastIpUsed = lastIpUsed;
    }

    public boolean isNormalDisconnect() {
        return normalDisconnect;
    }

    public void setNormalDisconnect(boolean normalDisconnect) {
        this.normalDisconnect = normalDisconnect;
    }

    public LocalDateTime getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(LocalDateTime joinTime) {
        this.joinTime = joinTime;
    }

    public List<String> getListFiles() {
        return listFiles;
    }

    public void setListFiles_categorizeFilesByVisibility(List<String> listFiles) {
        this.listFiles = listFiles;
        this.filesWithVisibility = categorizeFilesByVisibility(listFiles); // Automatically categorize files
    }

    public int getHistoryId() {
        return historyId;
    }

    public void setHistoryId(int historyId) {
        this.historyId = historyId;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public LocalDateTime getTimeExecution() {
        return timeExecution;
    }

    public void setTimeExecution(LocalDateTime timeExecution) {
        this.timeExecution = timeExecution;
    }

    public boolean isExecutedCorrectly() {
        return executedCorrectly;
    }

    public void setExecutedCorrectly(boolean executedCorrectly) {
        this.executedCorrectly = executedCorrectly;
    }
	public List<CommandExecutionState> getCommands() {
		return commands;
	}
	public void setCommands(List<CommandExecutionState> commands) {
		this.commands = commands;
	}
	public void setListFiles(List<String> listFiles) {
		this.listFiles = listFiles;
	}
    
}
