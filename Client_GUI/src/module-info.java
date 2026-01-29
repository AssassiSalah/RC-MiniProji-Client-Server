module Client_GUI {
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.graphics;
	requires javafx.base;
	requires java.logging;
	requires java.base;
	requires java.sql;
	requires java.desktop;

	opens controller to javafx.graphics, javafx.controls,javafx.fxml, javafx.base;
	opens application to javafx.graphics, javafx.controls,javafx.fxml;
}
