/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package financeerp.model;
import financeerp.FinanceERP;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime; 
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Juswa
 */
public class StockTransaction {
    private int transactionId;
    private int productId;
    private String transactionType; // IN or OUT
    private int quantity;
    private LocalDateTime transactionDate;
    private String remarks;
    private Object tblTransactions;

    public StockTransaction(int productId, String transactionType, int quantity, LocalDateTime transactionDate, String remarks) {
        this.productId = productId;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.transactionDate = transactionDate;
        this.remarks = remarks;
    }
    
    public int getTransactionId() { return transactionId; }
    public int getProductId() { return productId; }
    public String getTransactionType() { return transactionType; }
    public int getQuantity() { return quantity; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public String getRemarks() { return remarks; }

    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }
    public void setProductId(int productId) { this.productId = productId; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    @Override
    public String toString() {
        return "TX#" + transactionId + " | ProductID: " + productId + " | " + transactionType +
               " " + quantity + " | Date: " + transactionDate + " | " + remarks;
    }
}
