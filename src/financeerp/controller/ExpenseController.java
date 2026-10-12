/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package financeerp.controller;
import financeerp.dao.ExpenseDAO; 
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Juswa
 */
public class ExpenseController {
    private ExpenseDAO dao;

    public ExpenseController() {
        dao = new ExpenseDAO();
    }

    public void addExpense(String category, double amount, String date, String notes) {
        try {
            dao.insertExpense(category, amount, date, notes);
            System.out.println("Expense added successfully!");
        } catch (Exception e) {
            System.err.println("Error adding expense: " + e.getMessage());
        }
    }

    public List<String[]> viewAllExpenses() {
        try {
            return dao.getAllExpenses();
        } catch (Exception e) {
            System.err.println("Error retrieving expenses: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
