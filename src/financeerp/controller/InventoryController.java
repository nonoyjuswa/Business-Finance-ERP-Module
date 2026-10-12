/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package financeerp.controller;
import financeerp.dao.InventoryDAO;
import financeerp.model.Product;
import financeerp.model.StockTransaction;
import java.time.LocalDate;
import java.util.List;

/**
 *
 * @author Juswa
 */
public class InventoryController {
    private InventoryDAO inventoryDAO;

    public InventoryController() {
        this.inventoryDAO = new InventoryDAO();
    }

    // ✅ Add product with validation
    public boolean addProduct(String name, String category, double price, int initialStock) {
        if (name == null || name.trim().isEmpty()) {
            System.out.println("❌ Product name cannot be empty.");
            return false;
        }
        if (price < 0) {
            System.out.println("❌ Price cannot be negative.");
            return false;
        }
        if (initialStock < 0) {
            System.out.println("❌ Initial stock cannot be negative.");
            return false;
        }

        Product product = new Product(0, name, category, price, initialStock, "ACTIVE");
        return inventoryDAO.addProduct(product);
    }

    // ✅ Get all products
    public List<Product> getProducts() {
        return inventoryDAO.getAllProducts();
    }

    // ✅ Stock IN
    public boolean stockIn(int productId, int quantity, String remarks) {
        if (quantity <= 0) {
            System.out.println("❌ Quantity must be greater than zero.");
            return false;
        }

        StockTransaction tx = new StockTransaction(productId, "IN", quantity, LocalDate.now(), remarks);
        boolean success = inventoryDAO.addTransaction(tx);
        if (success) {
            inventoryDAO.updateStock(productId, quantity, "IN");
        }
        return success;
    }

    // ✅ Stock OUT
    public boolean stockOut(int productId, int quantity, String remarks) {
        if (quantity <= 0) {
            System.out.println("❌ Quantity must be greater than zero.");
            return false;
        }

        // Check current stock first
        Product product = inventoryDAO.getAllProducts()
                .stream()
                .filter(p -> p.getProductId() == productId)
                .findFirst()
                .orElse(null);

        if (product == null) {
            System.out.println("❌ Product not found.");
            return false;
        }

        if (product.getCurrentStock() < quantity) {
            System.out.println("❌ Insufficient stock. Available: " + product.getCurrentStock());
            return false;
        }

        StockTransaction tx = new StockTransaction(productId, "OUT", quantity, LocalDate.now(), remarks);
        boolean success = inventoryDAO.addTransaction(tx);
        if (success) {
            inventoryDAO.updateStock(productId, quantity, "OUT");
        }
        return success;
    }
    
    public Product getProductById(int productId) {
        return inventoryDAO.getProductById(productId);
    }

    public List<StockTransaction> getTransactionsByProduct(int productId) {
        return inventoryDAO.getTransactionsByProduct(productId);
    }
}
