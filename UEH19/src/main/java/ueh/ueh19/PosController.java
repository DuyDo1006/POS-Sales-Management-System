package ueh.ueh19;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

// --- THÊM 2 IMPORT NÀY ---
import java.text.NumberFormat;
import javafx.scene.control.TableCell;

public class PosController implements Initializable {

    // --- Biến toàn cục ---
    private User currentUser;
    private ObservableList<Product> availableProductsList;
    private ObservableList<OrderItem> currentOrderItemsList;

    // --- Giao diện bên trái: Danh sách sản phẩm ---
    @FXML private TextField productSearchField; // Ô tìm kiếm mới
    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, String> colProductSku;
    @FXML private TableColumn<Product, String> colProductName;
    @FXML private TableColumn<Product, Double> colProductPrice;
    @FXML private TableColumn<Product, Integer> colProductStock;
    @FXML private TextField quantityField;
    @FXML private Button addToOrderButton;

    // --- Giao diện bên phải: Đơn hàng hiện tại ---
    @FXML private TableView<OrderItem> orderItemsTable;
    @FXML private TableColumn<OrderItem, String> colOrderItemName;
    @FXML private TableColumn<OrderItem, Double> colOrderItemPrice;
    @FXML private TableColumn<OrderItem, Integer> colOrderItemQuantity;
    @FXML private TableColumn<OrderItem, Double> colOrderItemSubtotal;
    @FXML private Button removeItemButton;
    @FXML private Label totalAmountLabel;
    @FXML private Button completeSaleButton;

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // 1. Khởi tạo danh sách
        currentOrderItemsList = FXCollections.observableArrayList();
        orderItemsTable.setItems(currentOrderItemsList);

        // 2. Cấu hình các cột (Hàm này đã được thêm code định dạng)
        setupTableColumns();

        // 3. Tải danh sách sản phẩm từ CSDL
        loadAvailableProducts();
        
        // 4. Mặc định số lượng là 1
        quantityField.setText("1");

        // 5. THÊM LISTENER CHO Ô TÌM KIẾM
        productSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterProductList(newValue);
        });
    }

    private void setupTableColumns() {
        // Tạo một bộ định dạng số
        NumberFormat formatter = NumberFormat.getNumberInstance();
        
        // Bảng sản phẩm (bên trái)
        colProductSku.setCellValueFactory(new PropertyValueFactory<>("sku"));
        colProductName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colProductPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colProductStock.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));

        // --- CODE THÊM: Định dạng cột giá sản phẩm (bên trái) ---
        colProductPrice.setCellFactory(column -> {
            return new TableCell<Product, Double>() {
                @Override
                protected void updateItem(Double item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                    } else {
                        setText(formatter.format(item));
                    }
                }
            };
        });

        // Bảng đơn hàng (bên phải)
        colOrderItemName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colOrderItemPrice.setCellValueFactory(new PropertyValueFactory<>("pricePerUnit"));
        colOrderItemQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colOrderItemSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        // --- CODE THÊM: Định dạng cột đơn giá (bên phải) ---
        colOrderItemPrice.setCellFactory(column -> {
            return new TableCell<OrderItem, Double>() {
                @Override
                protected void updateItem(Double item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                    } else {
                        setText(formatter.format(item));
                    }
                }
            };
        });

        // --- CODE THÊM: Định dạng cột thành tiền (bên phải) ---
        colOrderItemSubtotal.setCellFactory(column -> {
            return new TableCell<OrderItem, Double>() {
                @Override
                protected void updateItem(Double item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                    } else {
                        setText(formatter.format(item));
                    }
                }
            };
        });
    }

    private void loadAvailableProducts() {
        availableProductsList = FXCollections.observableArrayList(database.getAllProducts());
        productsTable.setItems(availableProductsList);
    }

    private void filterProductList(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            productsTable.setItems(availableProductsList);
        } else {
            ObservableList<Product> filteredList = FXCollections.observableArrayList();
            String lowerCaseKeyword = keyword.toLowerCase();

            for (Product product : availableProductsList) {
                // Tìm kiếm theo cả Tên và SKU
                if (product.getName().toLowerCase().contains(lowerCaseKeyword) ||
                    product.getSku().toLowerCase().contains(lowerCaseKeyword)) {
                    filteredList.add(product);
                }
            }
            productsTable.setItems(filteredList);
        }
    }

    @FXML
    private void handleAddToOrder() {
        // ... (Code của hàm này giữ nguyên, không thay đổi)
        Product selectedProduct = productsTable.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            showAlert("Lỗi", "Vui lòng chọn một sản phẩm để thêm vào đơn hàng.");
            return;
        }
        int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText());
            if (quantity <= 0) {
                showAlert("Lỗi", "Số lượng phải là một số nguyên dương.");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Số lượng không hợp lệ.");
            return;
        }
        if (quantity > selectedProduct.getStockQuantity()) {
            showAlert("Hết hàng", "Số lượng yêu cầu vượt quá số lượng tồn kho (" + selectedProduct.getStockQuantity() + ").");
            return;
        }
        for (OrderItem item : currentOrderItemsList) {
            if (item.getProductId() == selectedProduct.getId()) {
                int newQuantity = item.getQuantity() + quantity;
                if (newQuantity > selectedProduct.getStockQuantity()) {
                     showAlert("Hết hàng", "Tổng số lượng yêu cầu vượt quá số lượng tồn kho.");
                     return;
                }
                item.setQuantity(newQuantity);
                orderItemsTable.refresh();
                updateTotalAmount();
                return;
            }
        }
        OrderItem newItem = new OrderItem(selectedProduct.getId(), selectedProduct.getName(), quantity, selectedProduct.getPrice());
        currentOrderItemsList.add(newItem);
        updateTotalAmount();
    }

    @FXML
    private void handleRemoveItem() {
        // ... (Code của hàm này giữ nguyên, không thay đổi)
        OrderItem selectedItem = orderItemsTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert("Lỗi", "Vui lòng chọn một mặt hàng để xóa khỏi đơn hàng.");
            return;
        }
        currentOrderItemsList.remove(selectedItem);
        updateTotalAmount();
    }

    @FXML
    private void handleCompleteSale() {
        if (currentOrderItemsList.isEmpty()) {
            showAlert("Lỗi", "Đơn hàng trống, không thể thanh toán.");
            return;
        }
        boolean success = database.createOrder(currentUser.getId(), currentOrderItemsList);
        if (success) {
            showAlert("Thành công", "Thanh toán và tạo đơn hàng thành công!");
            Stage stage = (Stage) completeSaleButton.getScene().getWindow();
            stage.close();
        } else {
            showAlert("Thất bại", "Đã xảy ra lỗi trong quá trình tạo đơn hàng. Vui lòng thử lại.");
        }
    }

    private void updateTotalAmount() {
        // ... (Code của hàm này giữ nguyên, không thay đổi)
        double total = 0;
        for (OrderItem item : currentOrderItemsList) {
            total += item.getSubtotal();
        }
        // Hàm này đã định dạng tổng tiền VNĐ rất tốt, nên tôi giữ nguyên
        totalAmountLabel.setText(String.format("Tổng tiền: %,.0f VNĐ", total));
    }
    
    private void showAlert(String title, String message) {
        // ... (Code của hàm này giữ nguyên, không thay đổi)
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}