package ueh.ueh19;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class database {
    private static final String URL = "jdbc:h2:./database/pos_db";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void initializeDatabase() {
        String[] sqlCommands = {
            "CREATE TABLE IF NOT EXISTS CATEGORIES (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255) NOT NULL UNIQUE, description TEXT);",
            "CREATE TABLE IF NOT EXISTS USERS (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(100) NOT NULL UNIQUE, password_hash VARCHAR(255) NOT NULL, full_name VARCHAR(255) NOT NULL, role VARCHAR(50) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);",
            "CREATE TABLE IF NOT EXISTS CUSTOMERS (id INT AUTO_INCREMENT PRIMARY KEY, full_name VARCHAR(255) NOT NULL, phone_number VARCHAR(20) UNIQUE, address TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);",
            "CREATE TABLE IF NOT EXISTS PRODUCTS (id INT AUTO_INCREMENT PRIMARY KEY, sku VARCHAR(100) NOT NULL UNIQUE, name VARCHAR(255) NOT NULL, description TEXT, price DECIMAL(10, 2) NOT NULL, stock_quantity INT DEFAULT 0, image_path VARCHAR(255), category_id INT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (category_id) REFERENCES CATEGORIES(id));",
            "CREATE TABLE IF NOT EXISTS ORDERS (id INT AUTO_INCREMENT PRIMARY KEY, customer_id INT, user_id INT NOT NULL, total_amount DECIMAL(12, 2) NOT NULL, status VARCHAR(50) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (customer_id) REFERENCES CUSTOMERS(id), FOREIGN KEY (user_id) REFERENCES USERS(id));",
            "CREATE TABLE IF NOT EXISTS ORDER_ITEMS (order_item_id INT AUTO_INCREMENT PRIMARY KEY, order_id INT NOT NULL, product_id INT NOT NULL, quantity INT NOT NULL, price_per_unit DECIMAL(10, 2) NOT NULL, FOREIGN KEY (order_id) REFERENCES ORDERS(id), FOREIGN KEY (product_id) REFERENCES PRODUCTS(id));"
        };
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : sqlCommands) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // --- HÀM XỬ LÝ ĐĂNG NHẬP ---
    public static User kiemTraDangNhap(String username, String password) {
        taoTaiKhoanAdminMacDinh();
        String sql = "SELECT * FROM USERS WHERE username = ? AND password_hash = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("username"), rs.getString("full_name"), rs.getString("role"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void taoTaiKhoanAdminMacDinh() {
        String sqlCheck = "SELECT COUNT(*) FROM USERS WHERE username = 'admin'";
        String sqlInsert = "INSERT INTO USERS(username, password_hash, full_name, role) VALUES('admin', 'admin', 'Administrator', 'admin')";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlCheck)) {
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.executeUpdate(sqlInsert);
            }
        } catch (SQLException e) {
            // Ignore
        }
    }

    // --- CÁC HÀM CRUD CHO SẢN PHẨM ---
    public static List<Product> getAllProducts() {
        List<Product> productList = new ArrayList<>();
        String sql = "SELECT * FROM PRODUCTS";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                productList.add(new Product(rs.getInt("id"), rs.getString("sku"), rs.getString("name"), rs.getDouble("price"), rs.getInt("stock_quantity")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return productList;
    }

    public static void addProduct(String sku, String name, double price, int quantity) {
        String sql = "INSERT INTO PRODUCTS(sku, name, price, stock_quantity) VALUES(?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sku);
            pstmt.setString(2, name);
            pstmt.setDouble(3, price);
            pstmt.setInt(4, quantity);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateProduct(int id, String sku, String name, double price, int quantity) {
        String sql = "UPDATE PRODUCTS SET sku = ?, name = ?, price = ?, stock_quantity = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sku);
            pstmt.setString(2, name);
            pstmt.setDouble(3, price);
            pstmt.setInt(4, quantity);
            pstmt.setInt(5, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteProduct(int id) {
        String sql = "DELETE FROM PRODUCTS WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- HÀM XỬ LÝ ĐƠN HÀNG ---
    public static boolean createOrder(int userId, List<OrderItem> items) {
        String sqlCreateOrder = "INSERT INTO ORDERS (user_id, total_amount, status) VALUES (?, ?, ?)";
        String sqlCreateOrderItem = "INSERT INTO ORDER_ITEMS (order_id, product_id, quantity, price_per_unit) VALUES (?, ?, ?, ?)";
        String sqlUpdateStock = "UPDATE PRODUCTS SET stock_quantity = stock_quantity - ? WHERE id = ?";
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // (1) TẮT chế độ "Tự động Lưu"
            // (2) Tính tổng tiền
            double totalAmount = 0;
            for (OrderItem item : items) {
                totalAmount += item.getSubtotal();
            }
            // (3) Tạo Hóa đơn (ORDERS)
            PreparedStatement pstmtOrder = conn.prepareStatement(sqlCreateOrder, Statement.RETURN_GENERATED_KEYS);
            pstmtOrder.setInt(1, userId);
            pstmtOrder.setDouble(2, totalAmount);
            pstmtOrder.setString(3, "completed");
            pstmtOrder.executeUpdate();
            // (4) Lấy ID của Hóa đơn vừa tạo
            ResultSet generatedKeys = pstmtOrder.getGeneratedKeys();
            int orderId;
            if (generatedKeys.next()) {
                orderId = generatedKeys.getInt(1);
            } else {
                throw new SQLException("Tạo đơn hàng thất bại, không lấy được ID.");
            }
            // (5) Chuẩn bị thêm Chi tiết Hóa đơn (ORDER_ITEMS) và Trừ kho (PRODUCTS)
            PreparedStatement pstmtItem = conn.prepareStatement(sqlCreateOrderItem);
            PreparedStatement pstmtStock = conn.prepareStatement(sqlUpdateStock);
            for (OrderItem item : items) {
                pstmtItem.setInt(1, orderId);
                pstmtItem.setInt(2, item.getProductId());
                pstmtItem.setInt(3, item.getQuantity());
                pstmtItem.setDouble(4, item.getPricePerUnit());
                pstmtItem.addBatch();
                pstmtStock.setInt(1, item.getQuantity());
                pstmtStock.setInt(2, item.getProductId());
                pstmtStock.addBatch();
            }
            pstmtItem.executeBatch();
            pstmtStock.executeBatch();
            // (7) Nếu mọi thứ OK -> LƯU VĨNH VIỄN
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            // (8) Nếu có BẤT KỲ lỗi nào (vd: hết hàng, mất điện) -> HỦY BỎ TẤT CẢ
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            // (9) Luôn luôn bật lại chế độ "Tự động Lưu" và "Đóng cửa kho"
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // --- LỚP NỘI BỘ VÀ CÁC HÀM CHO BÁO CÁO/THỐNG KÊ ---
    public static class OrderReport {
        public final int id;
        public final int userId;
        public final double totalAmount;
        public final String createdAt;
        public OrderReport(int id, int userId, double totalAmount, String createdAt) {
            this.id = id; this.userId = userId; this.totalAmount = totalAmount; this.createdAt = createdAt;
        }
    }

    public static List<OrderReport> getAllOrdersForReport() {
        List<OrderReport> orderList = new ArrayList<>();
        String sql = "SELECT id, user_id, total_amount, FORMATDATETIME(created_at, 'yyyy-MM-dd HH:mm:ss') as created_at_str FROM ORDERS ORDER BY created_at DESC";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                orderList.add(new OrderReport(rs.getInt("id"), rs.getInt("user_id"), rs.getDouble("total_amount"), rs.getString("created_at_str")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orderList;
    }
    
    public static class ProductSaleStat {
        private final String productName;
        private final int totalQuantity;

    public ProductSaleStat(String productName, int totalQuantity) {
        this.productName = productName;
        this.totalQuantity = totalQuantity;
    }
    public String getProductName() { return productName; }
    public int getTotalQuantity() { return totalQuantity; }
}
    
    public static List<ProductSaleStat> getTopSellingProducts() {
        List<ProductSaleStat> stats = new ArrayList<>();
        // Câu lệnh SQL này join 2 bảng, nhóm theo sản phẩm, đếm tổng số lượng bán,
        // sắp xếp giảm dần và chỉ lấy 5 kết quả đầu tiên.
        String sql = "SELECT p.name, SUM(oi.quantity) as total_sold " +
                     "FROM ORDER_ITEMS oi " +
                     "JOIN PRODUCTS p ON oi.product_id = p.id " +
                     "GROUP BY p.name " +
                     "ORDER BY total_sold DESC " +
                     "LIMIT 5";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                stats.add(new ProductSaleStat(
                    rs.getString("name"),
                    rs.getInt("total_sold")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }
    
    // --- LỚP NỘI BỘ VÀ HÀM CHO BÁO CÁO THEO NHÂN VIÊN ---
    public static class EmployeeSaleStat {
        private final String fullName;
        private final int orderCount;
        private final double totalRevenue;

        public EmployeeSaleStat(String fullName, int orderCount, double totalRevenue) {
            this.fullName = fullName;
            this.orderCount = orderCount;
            this.totalRevenue = totalRevenue;
        }
        public String getFullName() { return fullName; }
        public int getOrderCount() { return orderCount; }
        public double getTotalRevenue() { return totalRevenue; }
    }

    public static List<EmployeeSaleStat> getEmployeeSalesStats() {
        List<EmployeeSaleStat> stats = new ArrayList<>();
        // Câu lệnh này join USERS và ORDERS, nhóm theo user,
        // đếm số đơn hàng và tính tổng doanh thu, sắp xếp theo doanh thu giảm dần.
        String sql = "SELECT u.full_name, COUNT(o.id) as order_count, SUM(o.total_amount) as total_revenue " +
                     "FROM USERS u " +
                     "LEFT JOIN ORDERS o ON u.id = o.user_id " +
                     "WHERE o.id IS NOT NULL " + // Chỉ lấy user có bán hàng
                     "GROUP BY u.full_name " +
                     "ORDER BY total_revenue DESC";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                stats.add(new EmployeeSaleStat(
                    rs.getString("full_name"),
                    rs.getInt("order_count"),
                    rs.getDouble("total_revenue")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }
    
    // --- LỚP NỘI BỘ VÀ CÁC HÀM CRUD CHO QUẢN LÝ NGƯỜI DÙNG ---
    public static class UserForManagement {
        private int id;
        private String username;
        private String fullName;
        private String role;
        private int orderCount;
        public UserForManagement(int id, String username, String fullName, String role, int orderCount) {
            this.id = id; this.username = username; this.fullName = fullName; this.role = role; this.orderCount = orderCount;
        }
        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getFullName() { return fullName; }
        public String getRole() { return role; }
        public int getOrderCount() { return orderCount; }
    }

    public static List<UserForManagement> getAllUsers() {
        List<UserForManagement> userList = new ArrayList<>();
        String sql = "SELECT u.id, u.username, u.full_name, u.role, COUNT(o.id) as order_count " +
                     "FROM USERS u LEFT JOIN ORDERS o ON u.id = o.user_id " +
                     "GROUP BY u.id, u.username, u.full_name, u.role";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                userList.add(new UserForManagement(rs.getInt("id"), rs.getString("username"), rs.getString("full_name"), rs.getString("role"), rs.getInt("order_count")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userList;
    }

    public static void addUser(String username, String password, String fullName, String role) {
        String sql = "INSERT INTO USERS(username, password_hash, full_name, role) VALUES(?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, fullName);
            pstmt.setString(4, role);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateUser(int id, String username, String fullName, String role) {
        String sql = "UPDATE USERS SET username = ?, full_name = ?, role = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, fullName);
            pstmt.setString(3, role);
            pstmt.setInt(4, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteUser(int id) {
        String sql = "DELETE FROM USERS WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Không thể xóa người dùng này. Có thể do người dùng đã tạo đơn hàng.");
            e.printStackTrace();
        }
    }
    // === THÊM HÀM NÀY VÀO FILE database.java ===

/**
 * Kiểm tra xem một SKU đã tồn tại trong CSDL hay chưa.
 * @param sku Mã SKU cần kiểm tra.
 * @return true nếu SKU đã tồn tại, false nếu chưa.
 */
    public static boolean kiemTraSkuTonTai(String sku) {
        String sql = "SELECT COUNT(*) FROM PRODUCTS WHERE sku = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, sku);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Nếu rs.getInt(1) > 0, nghĩa là đã tìm thấy (tồn tại)
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Nếu có lỗi hoặc không tìm thấy, trả về false
        return false;
    }
    /**
 * Kiểm tra xem một username đã tồn tại trong CSDL hay chưa.
 * @param username Tên đăng nhập cần kiểm tra.
 * @return true nếu username đã tồn tại, false nếu chưa.
 */
    public static boolean kiemTraUsernameTonTai(String username) {
        String sql = "SELECT COUNT(*) FROM USERS WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Nếu rs.getInt(1) > 0, nghĩa là đã tìm thấy (tồn tại)
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Nếu có lỗi hoặc không tìm thấy, trả về false
        return false;
    }
}

        