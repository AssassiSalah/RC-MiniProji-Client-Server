module Server_interface {
	requires javafx.controls;
	requires javafx.fxml;
	requires org.json;
	requires javafx.graphics;
	requires javafx.base;
    exports spicy;  // If you want other modules to use spicy

	opens application to javafx.graphics, javafx.fxml;
}
