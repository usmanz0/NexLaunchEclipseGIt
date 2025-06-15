package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader; // Import FXMLLoader
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class Main extends Application {

    private Controller controller; // Declare the controller field
    
    @Override
    public void start(Stage primaryStage) { // Use primaryStage for the main window
        // Removed: All runInBackground checks and conditional logic

        System.out.println("Main: Application starting in normal GUI mode.");
        try {
            // 1. Create an FXMLLoader instance
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Main.fxml"));

            // 2. Load the FXML using the loader instance
            Parent root = loader.load();

            // 3. Get the controller instance from the loader
            controller = loader.getController();

            // Removed: controller.setBackgroundMode(false); // No longer needed

            // Setup the Scene
            Scene scene = new Scene(root, 1080, 720, Color.rgb(18, 18, 18, 1));
            scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

            // Setup the Stage (primaryStage is already provided by JavaFX)
            Image icon = new Image("nexlaunchlogo.png"); // Make sure nexlaunchlogo.png is in src/main/resources/
            primaryStage.getIcons().add(icon);
            primaryStage.setTitle("NexLaunch");

            // Set the scene on the primaryStage
            primaryStage.setScene(scene);
            primaryStage.show();

            // Set the close request handler on the primaryStage
            primaryStage.setOnCloseRequest(event -> {
                if (controller != null) {
                    controller.saveLaunchers();
                    System.out.println("Main: Application closing, launchers saved.");
                }
            });

        } catch(Exception e) {
            System.err.println("Main: Error during normal application startup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Pass command line arguments to the JavaFX application lifecycle
        launch(args);
    }
}