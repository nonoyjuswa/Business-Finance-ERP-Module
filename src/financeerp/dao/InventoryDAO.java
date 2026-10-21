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
    // ✅ Add a new product and record its initial stock as a Stock IN transaction
    public boolean addProduct(Product product) {
        String insertProduct = "INSERT INTO Products (product_name, category, unit_price, current_stock, status, location_id, created_by) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String insertTx = "INSERT INTO StockTransactions (product_id, transaction_type, quantity, transaction_date, remarks, party_name, encoded_by) VALUES (?, 'IN', ?, ?, ?, ?, ?)";

        try (Connection conn = FinanceERP.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int productId;
                try (PreparedStatement ps = conn.prepareStatement(insertProduct, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, product.getProductName());
                    ps.setString(2, product.getCategory());
                    ps.setDouble(3, product.getUnitPrice());
                    ps.setInt(4, product.getCurrentStock());
                    ps.setString(5, product.getStatus());
                    ps.setInt(6, product.getLocationId());
                    ps.setString(7, product.getCreatedBy());
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            conn.rollback();
                            return false;
                        }
                        productId = keys.getInt(1);
                    }
                }

                if (product.getCurrentStock() > 0) {
                    try (PreparedStatement ps = conn.prepareStatement(insertTx)) {
                        ps.setInt(1, productId);
                        ps.setInt(2, product.getCurrentStock());
                        ps.setTimestamp(3, Timestamp.valueOf(java.time.LocalDateTime.now()));
                        ps.setString(4, "Initial stock");
                        ps.setString(5, "Initial stock");
                        ps.setString(6, product.getCreatedBy());
                        ps.executeUpdate();
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    // ✅ Get all products
    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, COALESCE(l.location_name, 'Unassigned') AS location_name " +
                     "FROM Products p LEFT JOIN Locations l ON p.location_id = l.location_id " +
                     "WHERE p.status='ACTIVE'";
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
                p.setLocationId(rs.getInt("location_id"));
                p.setLocationName(rs.getString("location_name"));
                p.setCreatedBy(rs.getString("created_by"));
                products.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }
    
    public java.util.LinkedHashMap<Integer, String> getAllLocations() {
        java.util.LinkedHashMap<Integer, String> map = new java.util.LinkedHashMap<>();
        try (Connection conn = FinanceERP.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Locations ORDER BY location_name")) {
            while (rs.next()) {
                map.put(rs.getInt("location_id"), rs.getString("location_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return  map;
    }
    
    public boolean recordStockMovement(StockTransaction tx) {
        String insertSql = "INSERT INTO StockTransactions (product_id, transaction_type, quantity, transaction_date, remarks, party_name, encoded_by) VALUES (?, ?, ?, ?, ?, ?, ?)";
        boolean isIn = tx.getTransactionType().equals("IN");
        String updateSql = isIn
            ? "UPDATE Products SET current_stock = current_stock + ? WHERE product_id = ?"
            : "UPDATE Products SET current_stock = current_stock - ? WHERE product_id = ? AND current_stock >= ?";

        try (Connection conn = FinanceERP.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ins = conn.prepareStatement(insertSql);
                 PreparedStatement upd = conn.prepareStatement(updateSql)) {

                ins.setInt(1, tx.getProductId());
                ins.setString(2, tx.getTransactionType());
                ins.setInt(3, tx.getQuantity());
                ins.setTimestamp(4, Timestamp.valueOf(tx.getTransactionDate()));
                ins.setString(5, tx.getRemarks());
                ins.setString(6, tx.getPartyName());
                ins.setString(7, tx.getEncodedBy());
                ins.executeUpdate();

                upd.setInt(1, tx.getQuantity());
                upd.setInt(2, tx.getProductId());
                if (!isIn) upd.setInt(3, tx.getQuantity());

                if (upd.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    

    // ✅ Get a single product by ID (no status filter, so inactive ones still open)
    public Product getProductById(int productId) {
        String sql = "SELECT p.*, COALESCE(l.location_name, 'Unassigned') AS location_name " +
                     "FROM Products p LEFT JOIN Locations l ON p.location_id = l.location_id " +
                     "WHERE p.product_id=?";
        try (Connection conn = FinanceERP.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Product p = new Product(
                        rs.getInt("product_id"),
                        rs.getString("product_name"),
                        rs.getString("category"),
                        rs.getDouble("unit_price"),
                        rs.getInt("current_stock"),
                        rs.getString("status")
                    );
                    p.setLocationId(rs.getInt("location_id"));
                    p.setLocationName(rs.getString("location_name"));
                    p.setCreatedBy(rs.getString("created_by"));
                    return p;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public boolean updateProduct(Product product) {
        String sql = "UPDATE Products SET product_name=?, category=?, unit_price=?, status=?, location_id=? WHERE product_id=?";
        try (Connection conn = FinanceERP.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getProductName());
            ps.setString(2, product.getCategory());
            ps.setDouble(3, product.getUnitPrice());
            ps.setString(4, product.getStatus());
            if (product.getLocationId() > 0) {
                ps.setInt(5, product.getLocationId());
            } else {
                ps.setNull(5, Types.INTEGER);   // no location: store NULL, not 0
            }
            ps.setInt(6, product.getProductId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ✅ Get all transactions for one product (newest first)
    // ✅ Get all stock transactions for one product (newest first)
    public List<StockTransaction> getTransactionsByProduct(int productId) {
        List<StockTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM StockTransactions WHERE product_id=? "
                   + "ORDER BY transaction_date DESC, transaction_id DESC";
        try (Connection conn = FinanceERP.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockTransaction tx = new StockTransaction(
                        rs.getInt("product_id"),
                        rs.getString("transaction_type"),
                        rs.getInt("quantity"),
                        rs.getTimestamp("transaction_date").toLocalDateTime(),
                        rs.getString("remarks")
                    );
                    tx.setTransactionId(rs.getInt("transaction_id"));
                    tx.setPartyName(rs.getString("party_name"));
                    tx.setEncodedBy(rs.getString("encoded_by"));
                    list.add(tx);
                    list.add(tx);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    // ✅ Summary totals for the Inventory tab
// Returns {totalItems, totalIn, totalOut, currentStock}
    public int[] getInventoryTotals() {
        String sql =
            "SELECT " +
            " (SELECT COUNT(*) FROM Products WHERE status='ACTIVE') AS total_items, " +
            " (SELECT COALESCE(SUM(current_stock),0) FROM Products WHERE status='ACTIVE') AS current_stock, " +
            " (SELECT COALESCE(SUM(t.quantity),0) FROM StockTransactions t " +
            "    JOIN Products p ON t.product_id = p.product_id " +
            "    WHERE t.transaction_type='IN' AND p.status='ACTIVE') AS total_in, " +
            " (SELECT COALESCE(SUM(t.quantity),0) FROM StockTransactions t " +
            "    JOIN Products p ON t.product_id = p.product_id " +
            "    WHERE t.transaction_type='OUT' AND p.status='ACTIVE') AS total_out";
        try (Connection conn = FinanceERP.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return new int[]{
                    rs.getInt("total_items"),
                    rs.getInt("total_in"),
                    rs.getInt("total_out"),
                    rs.getInt("current_stock")
                };
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new int[]{0, 0, 0, 0};
    }
    
    // ✅ Soft delete: mark product as INACTIVE (keeps transaction history intact)
    // ✅ Get ALL products (active + inactive), used only for the Inventory table in Main
    public List<Product> getAllProductsIncludingInactive() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, COALESCE(l.location_name, 'Unassigned') AS location_name " +
                     "FROM Products p LEFT JOIN Locations l ON p.location_id = l.location_id " +
                     "ORDER BY p.product_id";
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
                p.setLocationId(rs.getInt("location_id"));
                p.setLocationName(rs.getString("location_name"));
                p.setCreatedBy(rs.getString("created_by"));
                products.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    // ✅ Set a product's status (ACTIVE / INACTIVE)
    public boolean setProductStatus(int productId, String status) {
        String sql = "UPDATE Products SET status=? WHERE product_id=?";
        try (Connection conn = FinanceERP.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // ✅ Check if a product name already exists (case-insensitive)
    public boolean productNameExists(String name) {
        String sql = "SELECT COUNT(*) FROM Products WHERE LOWER(product_name) = LOWER(?)";
        try (Connection conn = FinanceERP.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return true; // fail safe: block the insert if the check can't run
        }
    }
}
