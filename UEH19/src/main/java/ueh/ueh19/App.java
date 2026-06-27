package ueh.ueh19;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("login"), 640, 480);
        stage.setScene(scene);
        stage.setTitle("Phần mềm Quản lý Bán hàng");
        stage.show();
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void changeScene(String fxml, User user) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        Parent root = fxmlLoader.load();

        MainDashboardController controller = fxmlLoader.getController();
        controller.initializeData(user);

        scene.setRoot(root);
    }
    
    public static void changeSceneToLogin() throws IOException {
        scene.setRoot(loadFXML("login"));
    }

    public static void main(String[] args) {
        database.initializeDatabase();
        launch();
    }
}


