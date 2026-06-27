package ueh.ueh19;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import ueh.ueh19.database.UserForManagement; // Import lớp nội bộ

public class UserManagementController implements Initializable {

    // --- Giao diện bên trái: Nhập liệu ---
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField fullNameField;
    @FXML private ChoiceBox<String> roleChoiceBox;
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;

    // --- Giao diện bên phải: Bảng và Tìm kiếm ---
    @FXML private TextField searchField;
    @FXML private TableView<UserForManagement> userTable;
    @FXML private TableColumn<UserForManagement, Integer> idColumn;
    @FXML private TableColumn<UserForManagement, String> usernameColumn;
    @FXML private TableColumn<UserForManagement, String> fullNameColumn;
    @FXML private TableColumn<UserForManagement, String> roleColumn;
    @FXML private TableColumn<UserForManagement, Integer> orderCountColumn;

    private ObservableList<UserForManagement> userList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // 1. Cấu hình các cột cho TableView
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        orderCountColumn.setCellValueFactory(new PropertyValueFactory<>("orderCount"));

        // 2. Cấu hình ChoiceBox cho vai trò
        roleChoiceBox.setItems(FXCollections.observableArrayList("admin", "user"));
        roleChoiceBox.setValue("user"); // Mặc định là user

        // 3. Tải dữ liệu ban đầu
        loadUsers();

        // 4. Listener để hiển thị thông tin khi chọn một dòng
        userTable.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    fillUserDetails(newValue);
                }
            }
        );

        // 5. Listener cho ô tìm kiếm
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterUserList(newValue);
        });
    }

    private void loadUsers() {
        userList = FXCollections.observableArrayList(database.getAllUsers());
        userTable.setItems(userList);
    }

    private void fillUserDetails(UserForManagement user) {
        usernameField.setText(user.getUsername());
        fullNameField.setText(user.getFullName());
        roleChoiceBox.setValue(user.getRole());
        passwordField.setDisable(true); // Vô hiệu hóa ô mật khẩu khi ở chế độ sửa
        passwordField.clear();
    }

    @FXML
    private void handleAddButton() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String fullName = fullNameField.getText();
        String role = roleChoiceBox.getValue();

        if (username.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
            showAlert("Vui lòng nhập đầy đủ thông tin.");
            return;
        }
        if (database.kiemTraUsernameTonTai(username)) {
            showAlert("Tên đăng nhập đã tồn tại. Vui lòng chọn tên khác.");
            return; // Dừng lại, không thêm
        }
        // Kiểm tra xem Họ và tên có chứa số không
        if (coChuaSo(fullName)) {
            showAlert("Họ và tên không được chứa ký tự số.");
            return;
        }
        database.addUser(username, password, fullName, role);
        handleRefreshButton();
    }

    @FXML
    private void handleUpdateButton() {
        UserForManagement selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert("Vui lòng chọn một người dùng để sửa.");
            return;
        }

        String username = usernameField.getText();
        String fullName = fullNameField.getText();
        String role = roleChoiceBox.getValue();

        if (username.isEmpty() || fullName.isEmpty()) {
            showAlert("Tên đăng nhập và Họ tên không được để trống.");
            return;
        }
    // Chỉ kiểm tra nếu tên đăng nhập đã bị thay đổi
        if (!username.equals(selectedUser.getUsername())) {
            // Tên đã đổi, kiểm tra xem tên mới này có trùng không
            if (database.kiemTraUsernameTonTai(username)) {
                showAlert("Tên đăng nhập đã tồn tại. Vui lòng chọn tên khác.");
                return; // Dừng lại, không cập nhật
            }
        }
        // Kiểm tra xem Họ và tên có chứa số không
        if (coChuaSo(fullName)) {
            showAlert("Họ và tên không được chứa ký tự số.");
            return;
        }
        database.updateUser(selectedUser.getId(), username, fullName, role);
        handleRefreshButton();
    }

    @FXML
    private void handleDeleteButton() {
        UserForManagement selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert("Vui lòng chọn một người dùng để xóa.");
            return;
        }
        
        // Thêm một cảnh báo để không tự xóa chính mình
        if (selectedUser.getUsername().equals("admin")) {
            showAlert("Không thể xóa tài khoản admin gốc.");
            return;
        }

        database.deleteUser(selectedUser.getId());
        handleRefreshButton();
    }

    @FXML
    private void handleRefreshButton() {
        loadUsers();
        clearFields();
        searchField.clear();
    }

    private void filterUserList(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            userTable.setItems(userList);
        } else {
            ObservableList<UserForManagement> filteredList = FXCollections.observableArrayList();
            String lowerCaseKeyword = keyword.toLowerCase();

            for (UserForManagement user : userList) {
                if (user.getUsername().toLowerCase().contains(lowerCaseKeyword) ||
                    user.getFullName().toLowerCase().contains(lowerCaseKeyword)) {
                    filteredList.add(user);
                }
            }
            userTable.setItems(filteredList);
        }
    }

    private void clearFields() {
        usernameField.clear();
        passwordField.clear();
        fullNameField.clear();
        roleChoiceBox.setValue("user");
        userTable.getSelectionModel().clearSelection();
        passwordField.setDisable(false); // Bật lại ô mật khẩu
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Kiểm tra xem một chuỗi có chứa bất kỳ ký tự số nào không.
     * @param s Chuỗi cần kiểm tra.
     * @return true nếu chuỗi chứa số, ngược lại trả về false.
     */
    private boolean coChuaSo(String s) {
        if (s == null) {
            return false;
        }
        // Regex ".*\d.*" có nghĩa là:
        // ".*" : Bất kỳ ký tự nào, 0 hoặc nhiều lần
        // "\d" : Một ký tự số
        // ".*" : Bất kỳ ký tự nào, 0 hoặc nhiều lần
        // -> Tìm xem có ký tự số nào ở bất kỳ đâu trong chuỗi không.
        return s.matches(".*\\d.*");
    }
}
