package server;

import java.io.PrintWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class SQLiteDatabaseHandler {
    private static final String DB_URL = "jdbc:sqlite:user_info.db";
    private static final String USER_TABLE = "user_info";

    public SQLiteDatabaseHandler() {
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS " + USER_TABLE + " (" +
                    "username TEXT PRIMARY KEY, " +
                    "password_hash TEXT NOT NULL, " +
                    "registration_date TEXT NOT NULL, " +
                    "sentfiles_number INTEGER DEFAULT 0, " +
                    "receivedfiles_number INTEGER DEFAULT 0, " +
                    "sentviruses_number INTEGER DEFAULT 0, " +
                    "last_connection_date TEXT, " +
                    "last_used_ip TEXT, " +
                    "normal_disconnect BOOLEAN DEFAULT 1" +
                    ")";
            stmt.execute(createTableSQL);
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }
    
    public void createHistoryTable(String username) {
        String tableName = "history_" + username;
        String createTableSQL = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id_history INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "command TEXT NOT NULL, " +
                "file_name TEXT NOT NULL, " +
                "visibility BOOLEAN, " +
                "execution_time TEXT NOT NULL, " +
                "executed_correctly BOOLEAN NOT NULL)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
        } catch (SQLException e) {
            System.err.println("Error creating history table for user " + username + ": " + e.getMessage());
        }
    }
    
    public void createSharedFilesTable() {
        String sql = "CREATE TABLE IF NOT EXISTS shared_files (" +
                     "file_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                     "file_name TEXT NOT NULL, " +
                     "owner TEXT NOT NULL, " +
                     "FOREIGN KEY (owner) REFERENCES user_info(username)" +
                     ");";
        try (Connection conn = DriverManager.getConnection(DB_URL);
        	 Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("shared_files table created or already exists.");
        } catch (SQLException e) {
            System.err.println("Error creating shared_files table: " + e.getMessage());
        }
    }

    
    public void logCommand(String username, String command, String fileName, Boolean visibility, boolean executedCorrectly) {
        String tableName = "history_" + username;
        String insertSQL = "INSERT INTO " + tableName +
                "(command, file_name, visibility, execution_time, executed_correctly) VALUES (?, ?, ?, ?, ?)";
        String executionTime = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new java.util.Date());
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, command);
            pstmt.setString(2, fileName);
            if (visibility == null) {
                pstmt.setNull(3, Types.BOOLEAN);
            } else {
                pstmt.setBoolean(3, visibility);
            }
            pstmt.setString(4, executionTime);
            pstmt.setBoolean(5, executedCorrectly);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error logging command for user " + username + ": " + e.getMessage());
        }
    }
    
    
    public void addSharedFile(String fileName, String owner) {
        String sql = "INSERT INTO shared_files (file_name, owner) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
        	 PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileName);
            pstmt.setString(2, owner);
            pstmt.executeUpdate();
            System.out.println("File added to shared_files table: " + fileName);
        } catch (SQLException e) {
            System.err.println("Error adding shared file: " + e.getMessage());
        }
    }
    
    public void listSharedFiles(PrintWriter writer) {
        String sql = "SELECT file_name, owner FROM shared_files";
        try (Connection conn = DriverManager.getConnection(DB_URL); 
        	 Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            writer.println("Shared Files:");
            while (rs.next()) {
                String fileName = rs.getString("file_name");
                String owner = rs.getString("owner");
                writer.println(fileName + " (Owner: " + owner + ")");
            }
            writer.println("END");
        } catch (SQLException e) {
            writer.println("Error retrieving shared files: " + e.getMessage());
        }
    }



    public boolean registerUser(String username, String passwordHash) {
        String insertSQL = "INSERT INTO " + USER_TABLE + " (username, password_hash, registration_date) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.setString(3, LocalDateTime.now().toString());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error registering user: " + e.getMessage());
            return false;
        }
    }

    public boolean authenticateUser(String username, String passwordHash) {
        String querySQL = "SELECT * FROM " + USER_TABLE + " WHERE username = ? AND password_hash = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(querySQL)) {
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error authenticating user: " + e.getMessage());
            return false;
        }
    }

    public Map<String, Object> getUserDetails(String username) {
        String querySQL = "SELECT * FROM " + USER_TABLE + " WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(querySQL)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Map<String, Object> userDetails = new HashMap<>();
                userDetails.put("username", rs.getString("username"));
                userDetails.put("registration_date", rs.getString("registration_date"));
                userDetails.put("sentfiles_number", rs.getInt("sentfiles_number"));
                userDetails.put("receivedfiles_number", rs.getInt("receivedfiles_number"));
                userDetails.put("sentviruses_number", rs.getInt("sentviruses_number"));
                userDetails.put("last_connection_date", rs.getString("last_connection_date"));
                userDetails.put("last_used_ip", rs.getString("last_used_ip"));
                userDetails.put("normal_disconnect", rs.getBoolean("normal_disconnect"));
                return userDetails;
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving user details: " + e.getMessage());
        }
        return null;
    }

    public void updateUserConnection(String username, String ip, boolean normalDisconnect) {
        String updateSQL = "UPDATE " + USER_TABLE + " SET last_connection_date = ?, last_used_ip = ?, normal_disconnect = ? WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            pstmt.setString(1, LocalDateTime.now().toString());
            pstmt.setString(2, ip);
            pstmt.setBoolean(3, normalDisconnect);
            pstmt.setString(4, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating user connection: " + e.getMessage());
        }
    }
    
    public void updateFileTransferStats(String username, boolean isSent, int fileCount) {
        String column = isSent ? "sentfiles_number" : "receivedfiles_number";
        String updateSQL = "UPDATE " + USER_TABLE + " SET " + column + " = " + column + " + ? WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            pstmt.setInt(1, fileCount);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating file transfer stats: " + e.getMessage());
        }
    }

    public void updateSentVirusesCount(String username, int virusCount) {
        String updateSQL = "UPDATE " + USER_TABLE + " SET sentviruses_number = sentviruses_number + ? WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            pstmt.setInt(1, virusCount);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating virus count: " + e.getMessage());
        }
    }

}
