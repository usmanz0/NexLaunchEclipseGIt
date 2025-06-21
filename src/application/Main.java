package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class Main extends Application {

    private Controller controller; 
    
    @Override
    public void start(Stage primaryStage) { 

        System.out.println("Main: Application starting in normal GUI mode.");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Main.fxml"));

            Parent root = loader.load();

            controller = loader.getController();


            Scene scene = new Scene(root, 1080, 720, Color.rgb(18, 18, 18, 1));
            scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

            Image icon = new Image("nexlaunchlogo.png");
            primaryStage.getIcons().add(icon);
            primaryStage.setTitle("NexLaunch");

            primaryStage.setScene(scene);
            primaryStage.show();

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
        launch(args);
    }
}