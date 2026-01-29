module Client_GUI {
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.graphics;
	requires java.logging;
	requires java.base;
	requires java.desktop;
	requires javafx.base;

	opens controller to javafx.graphics, javafx.fxml, javafx.base;
	opens application to javafx.graphics, javafx.fxml;
}
