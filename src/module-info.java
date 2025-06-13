module NexLaunch {
	requires javafx.controls;
	requires javafx.graphics;
	requires javafx.fxml;
	requires java.desktop;
	requires javafx.base;
	requires com.google.gson;
	
	opens application to javafx.graphics, javafx.fxml;
}
