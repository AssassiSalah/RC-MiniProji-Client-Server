package application;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

public class CircularProgressBarController {

    @FXML private ProgressIndicator circularProgressBar;
    @FXML private Label percentageLabel;
    @FXML private Label currentSizeLabel, totalSizeLabel, remainTimeLabel, currentTimeLabel, speedTransferLabel;

    private double totalSize = 1000; // Total file size in KB
    private double speed = 10; // Transfer speed in KB/sec
    private long startTime; // Start time in milliseconds
    // private long lastUpdateTime; // Last time progress was updated
    private double lastSize; // File size at the last update

    public void initialize() {
        // Initialize progress bar and labels
        percentageLabel.setText("0%");
        currentSizeLabel.setText("0 KB");
        totalSizeLabel.setText("0 KB");
        remainTimeLabel.setText("N/A");
        currentTimeLabel.setText("0 sec");
        speedTransferLabel.setText("0 KB/sec");

        // Record start time
        //startTime = System.currentTimeMillis();
        //lastUpdateTime = startTime;
        //lastSize = currentSize;
    }
    
    void start(double totalSize_) {
        System.out.println("Start: ");
        totalSize = totalSize_;
        startTime = System.currentTimeMillis();
        speed = 4096;
        lastSize = 0;
        circularProgressBar.setProgress(0.0);

        // Initialize progress bar and labels
        percentageLabel.setText("0%");
        currentSizeLabel.setText("0 KB");
        totalSizeLabel.setText(String.format("%.1f KB", totalSize));
        remainTimeLabel.setText("N/A");
        currentTimeLabel.setText("0 sec");
        speedTransferLabel.setText("0 KB/sec");
    }

    public void update(double currentSize, double timeBetweenPacket) {
        if (currentSize < totalSize) {
            currentSize += speed;
            if (currentSize > totalSize) {
                currentSize = totalSize;
            }
        }

        double progress = currentSize / totalSize;
        circularProgressBar.setProgress(progress);
        percentageLabel.setText(String.format("%.0f%%", progress * 100));

        long currentTime = System.currentTimeMillis();
        double elapsedTime = (currentTime - startTime) / 1000.0;
        System.out.println("Elapsed time: " + elapsedTime + " seconds");

        double remainingTime = speed > 0 ? (totalSize - currentSize) / speed : Double.POSITIVE_INFINITY;
        System.out.println("Remaining time: " + remainingTime + " seconds");

        double currentSpeed = (currentSize - lastSize) / timeBetweenPacket;
        System.out.println("Current speed: " + currentSpeed + " KB/sec");

        currentSizeLabel.setText(String.format("%.1f KB", currentSize));
        totalSizeLabel.setText(String.format("%.1f KB", totalSize));
        remainTimeLabel.setText(String.format("%.1f sec", remainingTime));
        currentTimeLabel.setText(String.format("%.1f sec", elapsedTime));
        speedTransferLabel.setText(String.format("%.1f KB/sec", currentSpeed));

        lastSize = currentSize;

        if (currentSize >= totalSize) {
            circularProgressBar.setProgress(1);
            remainTimeLabel.setText("0 sec");
        }
    }

}
