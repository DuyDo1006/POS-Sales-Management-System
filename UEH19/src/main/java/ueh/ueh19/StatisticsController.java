package ueh.ueh19;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;
import ueh.ueh19.database.EmployeeSaleStat;
import ueh.ueh19.database.ProductSaleStat;

public class StatisticsController implements Initializable {

    @FXML private BarChart<String, Number> barChart;
    @FXML private Button exportPdfButton;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        List<ProductSaleStat> topProducts = database.getTopSellingProducts();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Số lượng đã bán");
        for (ProductSaleStat stat : topProducts) {
            series.getData().add(new XYChart.Data<>(stat.getProductName(), stat.getTotalQuantity()));
        }
        barChart.getData().add(series);
    }

    @FXML
    private void handleExportPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu Báo cáo Thống kê");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try {
                // --- BẮT ĐẦU TẠO FILE PDF ---
                PdfWriter writer = new PdfWriter(file.getAbsolutePath());
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);

                // --- THÊM TIÊU ĐỀ CHÍNH ---
                document.add(new Paragraph("Bao cao Thong ke Kinh doanh")
                        .setBold().setFontSize(20).setTextAlignment(TextAlignment.CENTER));

                // --- PHẦN 1: BIỂU ĐỒ SẢN PHẨM BÁN CHẠY ---
                document.add(new Paragraph("Bieu do Top 5 San pham Ban chay nhat")
                        .setBold().setFontSize(14).setMarginTop(20));
                
                // Chụp ảnh biểu đồ
                WritableImage image = barChart.snapshot(new SnapshotParameters(), null);
                File tempImageFile = File.createTempFile("chart_snapshot", ".png");
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
                ImageIO.write(bufferedImage, "png", tempImageFile);
                
                // Thêm ảnh vào PDF
                Image chartImage = new Image(ImageDataFactory.create(tempImageFile.toURI().toURL()));
                chartImage.setWidth(UnitValue.createPercentValue(100));
                document.add(chartImage);
                
                // --- PHẦN 2: BẢNG THỐNG KÊ THEO NHÂN VIÊN ---
                document.add(new Paragraph("Thong ke doanh thu theo Nhan vien")
                        .setBold().setFontSize(14).setMarginTop(20));

                // Lấy dữ liệu từ CSDL
                List<EmployeeSaleStat> employeeSales = database.getEmployeeSalesStats();
                
                // Tạo bảng với 3 cột
                Table table = new Table(UnitValue.createPercentArray(new float[]{4, 2, 3}));
                table.setWidth(UnitValue.createPercentValue(100));

                // Thêm Header cho bảng (đã được style)
                addStyledHeaderCell(table, "Ten Nhan vien");
                addStyledHeaderCell(table, "So don da ban");
                addStyledHeaderCell(table, "Tong doanh thu");
                
                // Thêm dữ liệu vào bảng
                for (EmployeeSaleStat stat : employeeSales) {
                    table.addCell(stat.getFullName());
                    table.addCell(String.valueOf(stat.getOrderCount())).setTextAlignment(TextAlignment.CENTER);
                    table.addCell(String.format("%,.0f VND", stat.getTotalRevenue())).setTextAlignment(TextAlignment.RIGHT);
                }
                document.add(table);

                // Thêm dòng nêu bật nhân viên xuất sắc nhất
                if (!employeeSales.isEmpty()) {
                    EmployeeSaleStat topEmployee = employeeSales.get(0); // Do đã sắp xếp theo doanh thu
                    document.add(new Paragraph("=> Nhan vien xuat sac nhat: " + topEmployee.getFullName())
                            .setBold().setFontColor(ColorConstants.BLUE).setMarginTop(10));
                }

                // --- KẾT THÚC VÀ ĐÓNG FILE ---
                document.close();
                tempImageFile.delete(); 
                showAlert("Thành công", "Xuất báo cáo PDF thành công!");

            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Lỗi", "Đã xảy ra lỗi khi tạo file PDF.");
            }
        }
    }
    
    // Phương thức trợ giúp để tạo Header Cell có style (Tương đương với class CSS)
    private void addStyledHeaderCell(Table table, String content) {
        Cell cell = new Cell().add(new Paragraph(content));
        cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        cell.setBold();
        cell.setTextAlignment(TextAlignment.CENTER);
        table.addHeaderCell(cell);
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}