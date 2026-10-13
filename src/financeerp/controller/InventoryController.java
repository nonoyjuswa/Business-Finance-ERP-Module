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
import financeerp.dao.GeneralLedgerDAO;
import java.sql.SQLException;

/**
 *
 * @author Juswa
 */
public class InventoryController {
    private InventoryDAO inventoryDAO;
    private static final String ACC_INVENTORY = "Inventory";
    private static final String ACC_CASH = "Cash";
    private static final String ACC_EQUITY = "Owner's Equity";
    private static final String ACC_COGS = "Cost of Goods Sold";

    // Posts a balanced debit/credit pair to the General Ledger
    private void postInventoryGL(String debitAcc, String creditAcc, double amount, String notes) {
        if (amount <= 0) return;
        try {
            new GeneralLedgerDAO().postToGL(
                LocalDate.now().toString(),
                debitAcc, amount,
                creditAcc, amount,
                "Inventory", notes);
        } catch (SQLException e) {
            System.err.println("⚠ GL posting failed: " + e.getMessage());
        }
    }

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
        boolean success = inventoryDAO.addProduct(product);

        if (success && initialStock > 0) {
            postInventoryGL(ACC_INVENTORY, ACC_EQUITY, price * initialStock,
                "Initial stock: " + name + " x" + initialStock);
        }
        return success;
    }

    // ✅ Get all products
    public List<Product> getProducts() {
        return inventoryDAO.getAllProducts();
    }
    
    // All products, including inactive (for the Inventory table)
    public List<Product> getAllProductsIncludingInactive() {
        return inventoryDAO.getAllProductsIncludingInactive();
    }

    // Flip ACTIVE <-> INACTIVE. Returns the new status, or null on failure.
    public String toggleProductStatus(int productId) {
        Product existing = inventoryDAO.getProductById(productId);
        if (existing == null) {
            System.out.println("❌ Product not found.");
            return null;
        }
        String newStatus = "ACTIVE".equalsIgnoreCase(existing.getStatus()) ? "INACTIVE" : "ACTIVE";
        return inventoryDAO.setProductStatus(productId, newStatus) ? newStatus : null;
    }

    // ✅ Stock IN
    public boolean stockIn(int productId, int quantity, String remarks) {
        if (quantity <= 0) {
            System.out.println("❌ Quantity must be greater than zero.");
            return false;
        }

        Product product = inventoryDAO.getProductById(productId);
        if (product == null) {
            System.out.println("❌ Product not found.");
            return false;
        }

        StockTransaction tx = new StockTransaction(productId, "IN", quantity, LocalDate.now(), remarks);
        boolean success = inventoryDAO.addTransaction(tx);
        if (success && inventoryDAO.updateStock(productId, quantity, "IN")) {
            postInventoryGL(ACC_INVENTORY, ACC_CASH, quantity * product.getUnitPrice(),
                "Stock IN: " + product.getProductName() + " x" + quantity
                + (remarks.isEmpty() ? "" : " (" + remarks + ")"));
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
        if (success && inventoryDAO.updateStock(productId, quantity, "OUT")) {
            postInventoryGL(ACC_COGS, ACC_INVENTORY, quantity * product.getUnitPrice(),
                "Stock OUT: " + product.getProductName() + " x" + quantity
                + (remarks.isEmpty() ? "" : " (" + remarks + ")"));
        }
        return success;
    }
    
    public Product getProductById(int productId) {
        return inventoryDAO.getProductById(productId);
    }
    
    public boolean updateProduct(int productId, String name, String category, double price, String status) {
        if (name == null || name.trim().isEmpty()) {
            System.out.println("❌ Product name cannot be empty.");
            return false;
        }
        if (price < 0) {
            System.out.println("❌ Price cannot be negative.");
            return false;
        }
        Product existing = inventoryDAO.getProductById(productId);
        if (existing == null) {
            System.out.println("❌ Product not found.");
            return false;
        }
        existing.setProductName(name.trim());
        existing.setCategory(category);
        existing.setUnitPrice(price);
        existing.setStatus(status);
        return inventoryDAO.updateProduct(existing);
    }

    public List<StockTransaction> getTransactionsForProduct(int productId) {
        return inventoryDAO.getTransactionsByProduct(productId);
    }
    
    public int[] getInventoryTotals() {
        return inventoryDAO.getInventoryTotals();
    }
}
