/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package financeerp.dao;
import financeerp.FinanceERP;
import java.sql.*;
import java.util.*;

/**
 *
 * @author Juswa
 */
public class ExpenseDAO {
    public void insertExpense(String category, double amount, String date, String notes) throws SQLException {
        try (Connection conn = FinanceERP.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO expenses (Category, Amount, Date, Notes) VALUES (?, ?, ?, ?)")) {
            pstmt.setString(1, category);
            pstmt.setDouble(2, amount);
            pstmt.setString(3, date);
            pstmt.setString(4, notes);
            pstmt.executeUpdate();
            
            GeneralLedgerDAO glDao = new GeneralLedgerDAO();
            glDao.postToGL(date, category, amount, "Cash", amount, "Expense", notes);
        }
    }

    public List<String[]> getAllExpenses() throws SQLException {
        List<String[]> expenses = new ArrayList<>();
        try (Connection conn = FinanceERP.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM expenses")) {
            while (rs.next()) {
                expenses.add(new String[]{
                    rs.getString("Date"),
                    rs.getString("Category"),
                    String.valueOf(rs.getDouble("Amount")),
                    rs.getString("Notes")
                });
            }
        }
        return expenses;
    }
}
