module Server {
	requires javafx.graphics;
	requires javafx.fxml;
	requires javafx.controls;
	requires java.sql;
	requires unirest.java;

	opens application to javafx.graphics,javafx.controls,javafx.fxml;
	opens spicy to javafx.graphics,javafx.controls,javafx.fxml;
}
