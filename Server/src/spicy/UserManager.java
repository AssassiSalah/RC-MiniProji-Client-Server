package spicy;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class UserManager {
    private List<User> users;

    public UserManager() {
        users = new ArrayList<>();
    }

    public void addUser(User user) {
        users.add(user);
    }

    public void removeUser(User user) {
        users.remove(user);
    }

    public User findUserByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    public void modifyUser(String username, User newUserDetails) {
        User user = findUserByUsername(username);
        if (user != null) {
            user.setUsername(newUserDetails.getUsername());
            user.setHashPassword(newUserDetails.getHashPassword());
            user.setPublicKey(newUserDetails.getPublicKey());
            user.setTotalFilesSent(newUserDetails.getTotalFilesSent());
            user.setTotalFilesReceived(newUserDetails.getTotalFilesReceived());
            user.setNumberOfVirusSends(newUserDetails.getNumberOfVirusSends());
            user.setLastTimeConnected(newUserDetails.getLastTimeConnected());
            user.setLastIpUsed(newUserDetails.getLastIpUsed());
            user.setNormalDisconnect(newUserDetails.isNormalDisconnect());
            user.setJoinTime(newUserDetails.getJoinTime());
            user.setListFiles_categorizeFilesByVisibility(newUserDetails.getListFiles());
            user.setHistoryId(newUserDetails.getHistoryId());
            user.setCommands(newUserDetails.getCommands());  // Updated to handle list of commands
            user.setVisibility(newUserDetails.getVisibility());
            user.setTimeExecution(newUserDetails.getTimeExecution());
            user.setExecutedCorrectly(newUserDetails.isExecutedCorrectly());
        }
    }

    public List<User> getUsers() {
        return users;
    }

    public void initializeRandomUsers(int count) {
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            List<String> fileList = new ArrayList<>();
            // Alternate between "private" and "public"
            fileList.add((i % 2 == 0 ? "private_" : "public_") + "File" + i);
            fileList.add((i % 2 != 0 ? "private_" : "public_") + "File" + (i + 1));

            // Generate random commands and their execution states
            List<CommandExecutionState> commands = new ArrayList<>();
            for (int j = 0; j < 2; j++) {
                boolean executedCorrectly = random.nextBoolean();
                commands.add(new CommandExecutionState("command" + (i * 2 + j), executedCorrectly));
            }

            User user = new User(
                    "user" + i,
                    "password" + i,
                    "publicKey" + i,
                    random.nextInt(100),
                    random.nextInt(100),
                    random.nextInt(10),
                    "2024-12-03T12:45:00",  // Sample date string
                    "192.168.1." + random.nextInt(255),
                    random.nextBoolean(),
                    "2024-12-03T12:45:00",  // Sample JoinTime as String
                    fileList,
                    i,
                    commands,  // Using a list of CommandExecutionState here
                    random.nextBoolean() ? "public" : "private",
                    "2024-12-03T12:45:00",  // Sample TimeExecution as String
                    random.nextBoolean()
            );
            users.add(user);
        }
    }

    public void displayUsers() {
        System.out.println("Username | HashPassword | PublicKey | TotalFilesSent | TotalFilesReceived | NumberOfVirusSends | LastTimeConnected | LastIpUsed | NormalDisconnect | JoinTime | ListFiles | HistoryId | Commands | Visibility | TimeExecution | ExecutedCorrectly");
        for (User user : users) {
            String files = String.join(", ", user.getListFiles());
            // Displaying command states as well
            String commands = "";
            for (CommandExecutionState command : user.getCommands()) {
                commands += command.getCommand() + " (Executed: " + (command.isExecutedCorrectly() ? "Yes" : "No") + "), ";
            }
            commands = commands.isEmpty() ? "No commands" : commands.substring(0, commands.length() - 2);  // Removing trailing comma

            System.out.println(user.getUsername() + " | " +
                               user.getHashPassword() + " | " +
                               user.getPublicKey() + " | " +
                               user.getTotalFilesSent() + " | " +
                               user.getTotalFilesReceived() + " | " +
                               user.getNumberOfVirusSends() + " | " +
                               user.getLastTimeConnected() + " | " +
                               user.getLastIpUsed() + " | " +
                               user.isNormalDisconnect() + " | " +
                               user.getJoinTimeString() + " | " +  // Formatted date
                               files + " | " +
                               user.getHistoryId() + " | " +
                               commands + " | " +
                               user.getVisibility() + " | " +
                               user.getTimeExecutionString() + " | " +  // Formatted date
                               user.isExecutedCorrectly());
        }
    }

    private List<String> parseFileList(String fileListStr) {
        List<String> fileList = new ArrayList<>();
        if (fileListStr != null && !fileListStr.isEmpty()) {
            String[] files = fileListStr.split(",");
            for (String file : files) {
                fileList.add(file.trim());
            }
        }
        return fileList;
    }

    private List<String> parseCommandList(String commandListStr) {
        List<String> commandList = new ArrayList<>();
        if (commandListStr != null && !commandListStr.isEmpty()) {
            String[] commands = commandListStr.split(",");
            for (String command : commands) {
                commandList.add(command.trim());
            }
        }
        return commandList;
    }
}
