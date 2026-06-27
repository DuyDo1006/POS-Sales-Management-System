package ueh.ueh19;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;

    @FXML
    private void handleLoginButtonAction(ActionEvent event) throws IOException {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Tên đăng nhập và mật khẩu không được để trống.");
            return;
        }

        User user = database.kiemTraDangNhap(username, password);

        if (user != null) {
            App.changeScene("MainDashboard", user);
        } else {
            statusLabel.setText("Sai tên đăng nhập hoặc mật khẩu.");
        }
    }
}