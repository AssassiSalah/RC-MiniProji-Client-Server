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
    private long lastBatchTime = 0; // To store the timestamp of the last packet batch
    private double averageTimeBetweenUpdates = 0; // To store the average time between updates

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
        speedTransferLabel.setText("0 M/sec");
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

    long packetSize = 40960; // Packet size in bytes
    long currentTime = System.currentTimeMillis(); // Get the current time

    // Check if this is the first update or not
    if (lastBatchTime != 0) {
        // Calculate the time difference between the last and current update
        long timeElapsed = currentTime - lastBatchTime;

        // Update average time between updates
        averageTimeBetweenUpdates = (averageTimeBetweenUpdates == 0) 
            ? timeElapsed 
            : (averageTimeBetweenUpdates + timeElapsed) / 2.0;

        // Calculate current speed using the average time
        double timeInSeconds = averageTimeBetweenUpdates / 1000.0; // Convert ms to seconds
        double currentSpeed = timeInSeconds > 0 ? ((packetSize / timeInSeconds)/1024)/10 : 0; // KB/sec

        // Display average speed and time
        System.out.printf("Average Time Between Updates: %.2f ms%n", averageTimeBetweenUpdates);
        System.out.printf("Current Speed: %.2f MB/sec%n", currentSpeed);

        // Update UI elements for speed and remaining time
        speedTransferLabel.setText(String.format("%.1f KB/sec", currentSpeed));
        double remainingTime = averageTimeBetweenUpdates > 0 
        	    ? (((totalSize - currentSize) ) * (currentSpeed))/(1000*1024) 
        	    : Double.POSITIVE_INFINITY;
        remainTimeLabel.setText(String.format("%.1f sec", remainingTime));
    }

    // Update lastBatchTime to current time for the next update
    lastBatchTime = currentTime;

    // Update additional UI elements
    currentSizeLabel.setText(String.format("%.1f KB", (currentSize)));
    totalSizeLabel.setText(String.format("%.1f KB", totalSize));
    currentTimeLabel.setText(String.format("%.1f sec", (currentTime - startTime) / 1000.0));

    // If transfer is complete, stop progress
    if (currentSize >= totalSize) {
        circularProgressBar.setProgress(1);
        remainTimeLabel.setText("0 sec");
    }
}

}
