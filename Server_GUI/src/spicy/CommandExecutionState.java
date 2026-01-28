package spicy;

public class CommandExecutionState {
    private String command;
    private boolean executedCorrectly;

    // Constructor
    public CommandExecutionState(String command, boolean executedCorrectly) {
        this.command = command;
        this.executedCorrectly = executedCorrectly;
    }

    // Getter and Setter for command and executedCorrectly
    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean isExecutedCorrectly() {
        return executedCorrectly;
    }

    public void setExecutedCorrectly(boolean executedCorrectly) {
        this.executedCorrectly = executedCorrectly;
    }
}
