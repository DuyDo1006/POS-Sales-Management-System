package ueh.ueh19;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import java.text.NumberFormat;
import javafx.scene.control.TableCell;

public class ProductManagementController implements Initializable {

    @FXML private TextField skuField;
    @FXML private TextField nameField;
    @FXML private TextField priceField;
    @FXML private TextField quantityField;
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Integer> idColumn;
    @FXML private TableColumn<Product, String> skuColumn;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, Double> priceColumn;
    @FXML private TableColumn<Product, Integer> quantityColumn;
    @FXML private TextField searchField;

    private ObservableList<Product> productList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Cấu hình các cột của TableView
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        skuColumn.setCellValueFactory(new PropertyValueFactory<>("sku"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));

        // --- BẮT ĐẦU CODE THÊM ĐỂ ĐỊNH DẠNG SỐ ---
        
        // Tạo một bộ định dạng số
        NumberFormat formatter = NumberFormat.getNumberInstance();
        
        priceColumn.setCellFactory(column -> {
            return new TableCell<Product, Double>() {
                @Override
                protected void updateItem(Double item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                    } else {
                        // Sử dụng bộ định dạng để hiển thị "15,000"
                        setText(formatter.format(item));
                    }
                }
            };
        });
        

        // Tải dữ liệu ban đầu
        loadProducts();

        // Listener để khi click vào một dòng, thông tin sẽ hiện lên các ô TextField
        productTable.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    fillProductDetails(newValue);
                }
            }
        );

        // --- LOGIC TÌM KIẾM MỚI ---
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            // Chuyển đổi từ khóa tìm kiếm thành chữ thường để tìm kiếm không phân biệt hoa thường
            String keyword = newValue.toLowerCase();          
            // Lọc danh sách sản phẩm
            ObservableList<Product> filteredList = FXCollections.observableArrayList();
            for (Product product : productList) {
                if (product.getName().toLowerCase().contains(keyword)) {
                    filteredList.add(product);
                }
            }
            // Cập nhật lại bảng với danh sách đã được lọc
            productTable.setItems(filteredList);
        });
    }

    private void loadProducts() {
        productList = FXCollections.observableArrayList(database.getAllProducts());
        productTable.setItems(productList);
    }
    
    private void fillProductDetails(Product product) {
        skuField.setText(product.getSku());
        nameField.setText(product.getName());
        priceField.setText(String.format("%,.0f", product.getPrice()));
        quantityField.setText(String.valueOf(product.getStockQuantity()));
    }

    @FXML
    private void handleAddButton() {
        try {
            String sku = skuField.getText();
            String name = nameField.getText();         
            String priceText = priceField.getText();
            String quantityText = quantityField.getText();           
            String cleanPriceText = priceText.replace(",", "");
            String cleanQuantityText = quantityText.replace(",", ""); 

            if (sku.isEmpty() || name.isEmpty()) {
                showAlert("SKU và Tên sản phẩm không được để trống.");
                return;
            }

            // Giờ thì parse (chuyển đổi) từ chuỗi SẠCH
            double price = Double.parseDouble(cleanPriceText);
            int quantity = Integer.parseInt(cleanQuantityText);

            // Kiểm tra số lượng âm
            if (quantity < 0) {
                showAlert("Số lượng phải là một số nguyên không âm.");
                return;
            }
            // Kiểm tra điều kiện giá          
            if (price < 1000) {
                showAlert("Giá sản phẩm phải lớn hơn hoặc bằng 1,000.");
                return; // Dừng lại, không thêm sản phẩm
            }
            // KIỂM TRA SKU
            if (database.kiemTraSkuTonTai(sku)) {
                showAlert("Mã SKU đã tồn tại. Vui lòng nhập mã SKU khác.");
                return;
            }
            // Nếu mọi thứ hợp lệ, thêm sản phẩm
            database.addProduct(sku, name, price, quantity);
            handleRefreshButton();

        } catch (NumberFormatException e) {
            // Thông báo lỗi nếu người dùng nhập "abc" hoặc sai định dạng số
            showAlert("Giá và Số lượng phải là số hợp lệ.");
        }
    }

    @FXML
    private void handleUpdateButton() {
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            showAlert("Vui lòng chọn một sản phẩm để sửa.");
            return;
        }

        try {
            String sku = skuField.getText();
            String name = nameField.getText();
            // 1. Lấy text gốc từ ô nhập liệu (ví dụ: "30,000" hoặc "1,000")
            String priceText = priceField.getText();
            String quantityText = quantityField.getText();

            String cleanPriceText = priceText.replace(",", "");
            String cleanQuantityText = quantityText.replace(",", "");
          //  parse (chuyển đổi) từ chuỗi SẠCH
            double price = Double.parseDouble(cleanPriceText);
            int quantity = Integer.parseInt(cleanQuantityText);
            //  kiểm tra logic
            if (sku.isEmpty() || name.isEmpty()) {
                showAlert("SKU và Tên sản phẩm không được để trống.");
                return;
            }
            // Kiểm tra số lượng âm
            if (quantity < 0) {
                showAlert("Số lượng phải là một số nguyên không âm.");
                return;
            }
            if (price < 1000) {
                showAlert("Giá sản phẩm phải lớn hơn hoặc bằng 1,000.");
                return; 
            }
           
            // Lấy SKU cũ (từ sản phẩm đã chọn)
            String oldSku = selectedProduct.getSku();

            // Chỉ kiểm tra nếu SKU đã bị thay đổi
            if (!sku.equals(oldSku)) { 
                // SKU đã đổi, bây giờ mới kiểm tra xem SKU mới này có trùng không
                if (database.kiemTraSkuTonTai(sku)) { 
                    showAlert("Mã SKU đã tồn tại. Vui lòng chọn mã khác.");
                    return; 
                }
            }
            // Cập nhật database
            database.updateProduct(selectedProduct.getId(), sku, name, price, quantity);
            handleRefreshButton();

        } catch (NumberFormatException e) {
            // Dù người dùng nhập "abc" hay "30,00,0" (sai), 
            showAlert("Giá và Số lượng phải là số hợp lệ.");
        }
    }

    @FXML
    private void handleDeleteButton() {
     
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            showAlert("Vui lòng chọn một sản phẩm để xóa.");
            return;
        }
        database.deleteProduct(selectedProduct.getId());
        handleRefreshButton();
    }

    @FXML
    private void handleRefreshButton() {
        loadProducts();
        clearFields();
        searchField.clear(); 
    }

    private void clearFields() {
        skuField.clear();
        nameField.clear();
        priceField.clear();
        quantityField.clear();
        productTable.getSelectionModel().clearSelection();
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}