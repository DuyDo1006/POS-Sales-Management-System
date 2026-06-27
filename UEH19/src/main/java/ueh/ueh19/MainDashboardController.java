package ueh.ueh19;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class MainDashboardController {

    // --- CÁC THÀNH PHẦN GIAO DIỆN  ---
    @FXML private Label welcomeLine1; 
    @FXML private Label welcomeLine2; 
    
    @FXML private Button productManagementButton;
    @FXML private Button userManagementButton;
    @FXML private Button posButton;
    @FXML private Button statisticsButton;
    
    @FXML private Button logoutButton; 

    private User currentUser;

    /**
     * Phương thức này nhận dữ liệu người dùng từ màn hình đăng nhập
     * và thực hiện phân quyền trên giao diện.
     */
    public void initializeData(User user) {
        this.currentUser = user;

        welcomeLine1.setText("Chào mừng " + currentUser.getFullName() + "!");
        welcomeLine2.setText("Vai trò của bạn là: " + currentUser.getRole());

        // --- LOGIC PHÂN QUYỀN ---
        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            productManagementButton.setVisible(true);
            userManagementButton.setVisible(true);
            statisticsButton.setVisible(true);
            posButton.setVisible(false);
        } else {
            productManagementButton.setVisible(false);
            userManagementButton.setVisible(false);
            statisticsButton.setVisible(false);
            posButton.setVisible(true);
        }
    }

    // =========================================================
    // PHƯƠNG THỨC MỚI ĐỂ XỬ LÝ SỰ KIỆN ĐĂNG XUẤT (GIỮ NGUYÊN)
    // =========================================================
    /**
     * Chuyển về màn hình đăng nhập.
     */
    @FXML
    private void handleLogout() {
        try {
            // Gọi phương thức tĩnh để chuyển Scene về màn hình đăng nhập
            App.changeSceneToLogin();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Mở cửa sổ Quản lý Sản phẩm.
     */
    @FXML
    private void openProductManagement() {
        openNewWindow("ProductManagement.fxml", "Quản lý Sản phẩm");
    }

    /**
     * Mở cửa sổ Quản lý Người dùng.
     */
    @FXML
    private void openUserManagement() {
        openNewWindow("UserManagement.fxml", "Quản lý Người dùng");
    }

    /**
     * Mở cửa sổ Bán hàng (POS).
     */
    @FXML
    private void openPosScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Pos.fxml"));
            Parent root = loader.load();

            // Truyền thông tin người dùng hiện tại sang màn hình POS
            PosController controller = loader.getController();
            controller.setCurrentUser(this.currentUser);

            Stage stage = new Stage();
            stage.setTitle("Bán hàng (POS)");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Mở cửa sổ Thống kê & Báo cáo.
     */
    @FXML
    private void openStatisticsScreen() {
        openNewWindow("Statistics.fxml", "Thống kê và Báo cáo");
    }

    /**
     * Phương thức trợ giúp để mở một cửa sổ mới (tái sử dụng code).
     * @param fxmlFile Tên file FXML cần mở.
     * @param title Tiêu đề của cửa sổ mới.
     */
    private void openNewWindow(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}