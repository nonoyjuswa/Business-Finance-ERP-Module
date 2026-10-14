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
import java.util.regex.Pattern;

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
    public static final int NAME_MIN_LENGTH = 3;
    public static final int NAME_MAX_LENGTH = 50;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}][\\p{L}\\p{N} \\-_.,()/&']*$");

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
    
    public String normalizeProductName(String name) {
        return name == null ? "" : name.trim().replaceAll("\\s+", " ");
    }
    
    public String validateProductName(String rawName) {
        String name = normalizeProductName(rawName);

        if (name.isEmpty()) {
            return "Product name cannot be empty.";
        }
        if (name.length() < NAME_MIN_LENGTH || name.length() > NAME_MAX_LENGTH) {
            return "Product name must be " + NAME_MIN_LENGTH + "-" + NAME_MAX_LENGTH + " characters long.";
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            return "Product name must start with a letter or number and may only contain "
                 + "letters, numbers, spaces and - _ . , ( ) / & '";
        }
        if (!name.matches(".*\\p{L}.*")) {
            return "Product name must contain at least one letter.";
        }
        if (inventoryDAO.productNameExists(name)) {
            return "A product named \"" + name + "\" already exists.";
        }
        return null;
    }

    // ✅ Add product with validation
    public boolean addProduct(String name, String category, double price, int initialStock) {
        String nameError = validateProductName(name);
        if (nameError != null) {
            System.out.println("❌ " + nameError);
            return false;
        }
        name = normalizeProductName(name);

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
