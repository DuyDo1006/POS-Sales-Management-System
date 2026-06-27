package ueh.ueh19;

public class OrderItem {
    private int productId;
    private String productName;
    private int quantity;
    private double pricePerUnit;
    private double subtotal;

    public OrderItem(int productId, String productName, int quantity, double pricePerUnit) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.subtotal = quantity * pricePerUnit;
    }

    // Getters
    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getPricePerUnit() { return pricePerUnit; }
    public double getSubtotal() { return subtotal; }
    
    // Setter cho quantity để có thể cập nhật
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.subtotal = this.quantity * this.pricePerUnit;
    }
}