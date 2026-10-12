/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package financeerp.dao;
import financeerp.FinanceERP;
import java.sql.*;

/**
 *
 * @author Juswa
 */
public class GeneralLedgerDAO {
    public void postToGL(String date, String debitAccount, double debitAmount,
                         String creditAccount, double creditAmount,
                         String source, String notes) throws SQLException {
        try (Connection conn = FinanceERP.getConnection()) {
            // Debit entry
            PreparedStatement pstmtDebit = conn.prepareStatement(
                "INSERT INTO GeneralLedger (Date, AccountName, Debit, Credit, SourceModule, Notes) VALUES (?, ?, ?, ?, ?, ?)");
            pstmtDebit.setString(1, date);
            pstmtDebit.setString(2, debitAccount);
            pstmtDebit.setDouble(3, debitAmount);
            pstmtDebit.setDouble(4, 0);
            pstmtDebit.setString(5, source);
            pstmtDebit.setString(6, notes);
            pstmtDebit.executeUpdate();

            // Credit entry
            PreparedStatement pstmtCredit = conn.prepareStatement(
                "INSERT INTO GeneralLedger (Date, AccountName, Debit, Credit, SourceModule, Notes) VALUES (?, ?, ?, ?, ?, ?)");
            pstmtCredit.setString(1, date);
            pstmtCredit.setString(2, creditAccount);
            pstmtCredit.setDouble(3, 0);
            pstmtCredit.setDouble(4, creditAmount);
            pstmtCredit.setString(5, source);
            pstmtCredit.setString(6, notes);
            pstmtCredit.executeUpdate();
        }
    }
}
