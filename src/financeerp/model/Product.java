/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package financeerp.model;

/**
 *
 * @author Juswa
 */
public class Product {
    private int productId;
    private String productName;
    private String category;
    private double unitPrice;
    private int currentStock;
    private String status;

    public Product(int productId, String productName, String category, double unitPrice, int currentStock, String status) {
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.unitPrice = unitPrice;
        this.currentStock = currentStock;
        this.status = status;
    }

    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getCategory() { return category; }
    public double getUnitPrice() { return unitPrice; }
    public int getCurrentStock() { return currentStock; }
    public String getStatus() { return status; }

    public void setProductId(int productId) { this.productId = productId; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setCategory(String category) { this.category = category; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return productId + " - " + productName + " (" + category + ") | ₱" + unitPrice + " | Stock: " + currentStock;
    }
}
