package application;
	
import javafx.application.Application;
import com.google.gson.Gson;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;


public class Main extends Application {

	 @Override
	 public void start(Stage primaryStage) {
		 try {
			  Parent root = FXMLLoader.load(getClass().getResource("/Main.fxml"));

			  Scene scene = new Scene(root,1080,720,Color.rgb(18, 18, 18, 1));
			  Stage stage = new Stage();
			  Image icon = new Image("nexlaunchlogo.png");
			  scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			  
			  stage.getIcons().add(icon);
			  stage.setTitle("NexLaunch");
			  
			  
			  
			  stage.setScene(scene);
			  stage.show();
		 } catch(Exception e) {
			 e.printStackTrace();
		 } 
	 }
	 


	 public class TestGson {
	     public static void main(String[] args) {
	         Gson gson = new Gson();
	         String json = gson.toJson("Hello JavaFX + Gson");
	         System.out.println(json);
	     }
	 }
	 
	 public static void main(String[] args) {
		  launch(args);
		 }

	}
