package application;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.util.Duration;

public class Controller {

	@FXML
	private TextArea textAreaDisplay;

	private String[] words = { "Welcome", " to", " WIVE", " FLOW", ",", " the", " best", " application", " in",
			" terms", " of", " speed", " of", " dealing", " with", " files.", " If", " I", " have", " interested",
			" you", ",", " please", " click", " on", " Get", " Start." };

	private int wordIndex = 0;
	private boolean isWriting = true;

	@FXML
	public void initialize() {
		textAreaDisplay.setEditable(false);
		textAreaDisplay.setWrapText(true);
			startWritingEffect();
	}

	private void startWritingEffect() {
		Timeline timeline = new Timeline(new KeyFrame(Duration.millis(300), e -> {
			if (isWriting) {
					textAreaDisplay.setText(String.join(" ", java.util.Arrays.copyOfRange(words, 0, wordIndex + 1)));
				wordIndex++;

				if (wordIndex >= words.length) {
						isWriting = false;
					wordIndex = words.length - 1; 
					}
			} else {
					textAreaDisplay.setText(String.join(" ", java.util.Arrays.copyOfRange(words, 0, wordIndex)));
				wordIndex--;

				if (wordIndex <= 0) {
						isWriting = true;
					wordIndex = 0;
				}
			}
		}));
		timeline.setCycleCount(Timeline.INDEFINITE);
		timeline.play();
	}
}
