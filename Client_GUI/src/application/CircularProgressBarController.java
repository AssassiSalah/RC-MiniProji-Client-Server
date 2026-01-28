package application;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

public class CircularProgressBarController {

    @FXML private ProgressIndicator circularProgressBar;
    @FXML private Label percentageLabel;
    @FXML private Label currentSizeLabel, totalSizeLabel, remainTimeLabel, currentTimeLabel, speedTransferLabel;
    double progress=0;
    private double totalSize = 0; // Total file size in KB
    private long startTime=0; // Start time in milliseconds
    // private long lastUpdateTime; // Last time progress was updated
    private double lastSize; // File size at the last update

    public void initialize(double totalSize_) {
        System.out.println("Start: ");
        totalSize = totalSize_/1024;
        startTime = System.currentTimeMillis();
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
            if (currentSize > totalSize) {
                currentSize = totalSize;
            }
        }

        // Calculate progress
        progress = currentSize / totalSize;
        circularProgressBar.setProgress(progress);
        percentageLabel.setText(String.format("%.0f%%", progress * 100));
        long deffrent =40960;
        long currentTime = System.currentTimeMillis();
        // Calculate transfer speed (currentSize / timeElapsed)
        double currentSpeed = (deffrent) / ((totalSize) / 10000.0); // KB/sec
        System.out.println("Current speed: " + currentSpeed + " KB/sec");

        // Calculate remaining time using current speed
        double remainingTime = currentSpeed > 0 ? (totalSize / 10000) / currentSpeed : Double.POSITIVE_INFINITY;
        System.out.println("Remaining time: " + remainingTime + " seconds");

        // Update UI elements
        currentSizeLabel.setText(String.format("%.1f KB", currentSize));
        totalSizeLabel.setText(String.format("%.1f KB", totalSize));
        remainTimeLabel.setText(String.format("%.1f sec", remainingTime));
        currentTimeLabel.setText(String.format("%.1f sec", (currentTime - startTime) / 1000.0));
        speedTransferLabel.setText(String.format("%.1f KB/sec", currentSpeed));

        lastSize = currentSize; // Update lastSize for next speed calculation

        // If transfer is complete, stop progress
        if (currentSize >= totalSize) {
            circularProgressBar.setProgress(1);
            remainTimeLabel.setText("0 sec");
        }
    }


}
