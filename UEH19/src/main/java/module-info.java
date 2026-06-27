
module ueh.ueh19 {
    // Các thư viện đã có
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires io;
    requires kernel;
    requires layout;
    requires org.slf4j;
    
    requires java.desktop;    // Cho phép sử dụng các lớp của AWT/Swing (BufferedImage, ImageIO)
    requires javafx.swing;    // Cho phép sử dụng lớp cầu nối SwingFXUtils

    opens ueh.ueh19 to javafx.fxml;
    exports ueh.ueh19;
}
    
