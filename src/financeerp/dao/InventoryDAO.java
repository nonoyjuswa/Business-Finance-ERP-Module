/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package financeerp.dao;
import financeerp.FinanceERP;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import financeerp.model.Product;
import financeerp.model.StockTransaction;

/**
 *
 * @author Juswa
 */
public class InventoryDAO {

    // ✅ Add a new product
    public boolean addProduct(Product product) {
        String sql = "INSERT INTO Products (product_name, category, unit_price, current_stock, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = FinanceERP.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getProductName());
            ps.setString(2, product.getCategory());
            ps.setDouble(3, product.getUnitPrice());
            ps.setInt(4, product.getCurrentStock());
            ps.setString(5, product.getStatus());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ✅ Get all products
    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM Products WHERE status='ACTIVE'";
        try (Connection conn = FinanceERP.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Product p = new Product(
                    rs.getInt("product_id"),
                    rs.getString("product_name"),
                    rs.getString("category"),
                    rs.getDouble("unit_price"),
                    rs.getInt("current_stock"),
                    rs.getString("status")
                );
                products.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    // ✅ Add stock transaction (IN/OUT)
    public boolean addTransaction(StockTransaction tx) {
        String sql = "INSERT INTO StockTransactions (product_id, transaction_type, quantity, transaction_date, remarks) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = FinanceERP.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tx.getProductId());
            ps.setString(2, tx.getTransactionType());
            ps.setInt(3, tx.getQuantity());
            ps.setDate(4, Date.valueOf(tx.getTransactionDate()));
            ps.setString(5, tx.getRemarks());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ✅ Update product stock after transaction
    public boolean updateStock(int productId, int quantity, String type) {
        String sql = (type.equals("IN"))
            ? "UPDATE Products SET current_stock = current_stock + ? WHERE product_id=?"
            : "UPDATE Products SET current_stock = current_stock - ? WHERE product_id=? AND current_stock >= ?";
        try (Connection conn = FinanceERP.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, productId);
            if (type.equals("OUT")) ps.setInt(3, quantity); // prevent negative stock
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
