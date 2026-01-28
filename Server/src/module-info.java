module server {
	requires javafx.controls;
	requires javafx.fxml;
	requires unirest.java;
	
	opens application to javafx.graphics, javafx.fxml;
}
