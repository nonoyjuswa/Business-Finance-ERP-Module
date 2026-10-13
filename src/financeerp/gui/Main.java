/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package financeerp.gui;
import financeerp.FinanceERP;
import financeerp.model.Product;
import financeerp.gui.ViewDetails;
import financeerp.gui.EditProduct;
import financeerp.controller.ExpenseController;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import financeerp.controller.InventoryController;

/**
 *
 * @author Juswa
 */
public class Main extends javax.swing.JFrame {

    private boolean showingUnpaid = false;

    /**
     * Creates new form Main
     */
    public Main() {
        initComponents();
        loadAPTable();
        loadARTable();
        loadExpenseTable();
        loadGLTable();
        loadProductTable();
        loadTransactionTable();
        jButton13.setText("Toggle Status");
        TableProduct.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateToggleButtonLabel();
        });
    }
    private void loadAPTable() {
        try (Connection conn = FinanceERP.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM AccountsPayable")) {

            DefaultTableModel model = (DefaultTableModel) tblAP.getModel();
            model.setRowCount(0); // clear existing rows

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("InvoiceNumber"),
                    rs.getString("SupplierName"),
                    rs.getDouble("Amount"),
                    rs.getString("DueDate"),
                    rs.getString("Status")
                });
            }
        } catch (Exception e) {
            System.err.println("Load Error: " + e.getMessage());
        }
    }

    private void loadARTable() {
        try (Connection conn = FinanceERP.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM AccountsReceivable")) {

            DefaultTableModel model = (DefaultTableModel) tblAR.getModel();
            model.setRowCount(0);

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("InvoiceNumber"),
                    rs.getString("CustomerName"),
                    rs.getDouble("Amount"),
                    rs.getString("date"),
                    rs.getString("PaymentStatus")
                });
            }
        } catch (Exception e) {
            System.err.println("Load Error (AR): " + e.getMessage());
        }
    }
    
    private void loadExpenseTable() {
        try (Connection conn = FinanceERP.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Expenses")) {

            DefaultTableModel model = (DefaultTableModel) tblExpenses.getModel();
            model.setRowCount(0);

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("Category"),
                    rs.getDouble("Amount"),
                    rs.getString("Date"),
                    rs.getString("Notes")
                });
            }
        } catch (Exception e) {
            System.err.println("Load Error (Expenses): " + e.getMessage());
        }
    }

    private void loadGLTable() {
        try (Connection conn = FinanceERP.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM GeneralLedger")) {

            DefaultTableModel model = (DefaultTableModel) tblGL.getModel();
            model.setRowCount(0);

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("Date"),
                    rs.getString("AccountName"),
                    rs.getDouble("Debit"),
                    rs.getDouble("Credit"),
                    rs.getString("SourceModule")
                });
            }
        } catch (Exception e) {
            System.err.println("Load Error (GL): " + e.getMessage());
        }
    }
    
    public void loadProductTable() {
        InventoryController controller = new InventoryController();
        java.util.List<Product> products = controller.getAllProductsIncludingInactive();

        DefaultTableModel model = (DefaultTableModel) TableProduct.getModel();
        model.setRowCount(0); // clear old rows

        for (Product p : products) {
            model.addRow(new Object[]{
                p.getProductId(),
                p.getProductName(),
                p.getCategory(),
                p.getUnitPrice(),
                p.getCurrentStock(),
                p.getStatus()
            });
        }
        updateInventorySummary();
    }
    
    // Button text follows the selected row: "Deactivate" or "Activate"
    private void updateToggleButtonLabel() {
        int row = TableProduct.getSelectedRow();
        if (row < 0) {
            jButton13.setText("Toggle Status");
            return;
        }
        boolean isActive = "ACTIVE".equalsIgnoreCase(TableProduct.getValueAt(row, 5).toString());
        jButton13.setText(isActive ? "Deactivate" : "Activate");
    }
    
    public void loadTransactionTable() {
        try (Connection conn = FinanceERP.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT t.transaction_id, p.product_name, t.transaction_type, " +
             "t.quantity, t.transaction_date, t.remarks " +
             "FROM StockTransactions t " +
             "JOIN Products p ON t.product_id = p.product_id")) {

            DefaultTableModel model = (DefaultTableModel) TableStock.getModel(); // your transaction JTable
            model.setRowCount(0); // clear old rows

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("transaction_id"),
                    rs.getString("transaction_type"),
                    rs.getString("product_name"),
                    rs.getInt("quantity"),
                    rs.getDate("transaction_date"),
                    rs.getString("remarks")
                });
            }
            updateInventorySummary();
        } catch (Exception e) {
            System.err.println("Load Error (Transactions): " + e.getMessage());
        }
    }
    
    private void addExpense() {
        try {
            String category = txtCategory.getText().trim();
            double amount = Double.parseDouble(txtAmount2.getText().trim());
            String notes = txtNotes.getText().trim();

            new ExpenseController().addExpense(category, amount, LocalDate.now().toString(), notes);

            lblStatus2.setText("Expense added successfully!");
            loadExpenseTable();
            loadGLTable();
        } catch (NumberFormatException e) {
            lblStatus2.setText("Amount must be a valid number.");
        }
    }

    private void showMonthlyExpenseTotal() {
        String sql = "SELECT SUM(Amount) AS Total FROM Expenses "
                   + "WHERE MONTH(Date) = MONTH(CURRENT_DATE) AND YEAR(Date) = YEAR(CURRENT_DATE)";
        try (Connection conn = FinanceERP.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                lblStatus2.setText("Total expenses this month: ₱" + rs.getDouble("Total"));
            }
        } catch (Exception e) {
            System.err.println("Totals Error: " + e.getMessage());
            lblStatus2.setText("Error calculating totals.");
        }
    }
    
    // One place that refreshes everything an inventory change can affect
    private void refreshInventory() {
        loadProductTable();
        loadTransactionTable();
        loadGLTable();
    }

    private void openAddProductDialog() {
        new AddProduct(this, true).setVisible(true); // modal: waits until closed
        refreshInventory();
    }

    private void openStockInOutDialog() {
        new StockInOut(this, true).setVisible(true);
        refreshInventory();
    }

    private void openEditProductDialog() {
        int row = TableProduct.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "❌ Please select a product to edit.");
            return;
        }
        int productId = Integer.parseInt(TableProduct.getValueAt(row, 0).toString());
        Product product = new InventoryController().getProductById(productId);
        if (product == null) {
            JOptionPane.showMessageDialog(this, "❌ Product not found.");
            return;
        }
        new EditProduct(this, true, product).setVisible(true);
        loadProductTable();
    }

    private void toggleSelectedProduct() {
        int row = TableProduct.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "❌ Please select a product first.");
            return;
        }

        int productId = Integer.parseInt(TableProduct.getValueAt(row, 0).toString());
        String name = TableProduct.getValueAt(row, 1).toString();
        int stock = Integer.parseInt(TableProduct.getValueAt(row, 4).toString());
        boolean isActive = "ACTIVE".equalsIgnoreCase(TableProduct.getValueAt(row, 5).toString());

        if (isActive && !confirmDeactivate(name, stock)) return;

        if (new InventoryController().toggleProductStatus(productId) != null) {
            loadProductTable();
            updateToggleButtonLabel();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update product status.");
        }
    }

    private boolean confirmDeactivate(String name, int stock) {
        String message = "Deactivate \"" + name + "\"?\n"
                + "It will be unavailable for Stock In/Out, but its history is kept.";
        if (stock > 0) {
            message += "\n\n⚠ This product still has " + stock + " units in stock.";
        }
        return JOptionPane.showConfirmDialog(this, message, "Confirm Deactivate",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }
    
    private void openViewDetailsDialog() {
        int row = TableProduct.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "❌ Please select a product to view.");
            return;
        }
        int productId = Integer.parseInt(TableProduct.getValueAt(row, 0).toString());
        Product product = new InventoryController().getProductById(productId);
        if (product == null) {
            JOptionPane.showMessageDialog(this, "❌ Product not found.");
            return;
        }
        new ViewDetails(this, true, product).setVisible(true);
    }
    
    public void updateInventorySummary() {
        int[] totals = new InventoryController().getInventoryTotals();
        jLabel27.setText(String.valueOf(totals[0])); // Total items
        jLabel28.setText(String.valueOf(totals[1])); // Total In
        jLabel29.setText(String.valueOf(totals[2])); // Total Out
        jLabel30.setText(String.valueOf(totals[3])); // Current Stock
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane2 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        txtSupplierName = new javax.swing.JTextField();
        txtAmount = new javax.swing.JTextField();
        txtInvoiceNumber = new javax.swing.JTextField();
        txtDueDate = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblAP = new javax.swing.JTable();
        lblStatus = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        jPanel2 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        txtCustomerName = new javax.swing.JTextField();
        txtAmount1 = new javax.swing.JTextField();
        txtInvoiceNumber1 = new javax.swing.JTextField();
        cmbStatus = new javax.swing.JTextField();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        btnViewUnpaid = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JSeparator();
        jLabel10 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblAR = new javax.swing.JTable();
        lblStatus1 = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();
        jPanel3 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        txtCategory = new javax.swing.JTextField();
        txtNotes = new javax.swing.JTextField();
        txtAmount2 = new javax.swing.JTextField();
        jButton6 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jSeparator5 = new javax.swing.JSeparator();
        jLabel14 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tblExpenses = new javax.swing.JTable();
        lblStatus2 = new javax.swing.JLabel();
        jSeparator6 = new javax.swing.JSeparator();
        jPanel4 = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        txtDate = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        txtAccount = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        txtDebit = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        txtCredit = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        txtSource = new javax.swing.JTextField();
        jButton8 = new javax.swing.JButton();
        jButton9 = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        jSeparator7 = new javax.swing.JSeparator();
        jLabel20 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        tblGL = new javax.swing.JTable();
        GLStatus = new javax.swing.JLabel();
        jSeparator8 = new javax.swing.JSeparator();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        TableProduct = new javax.swing.JTable();
        jSeparator9 = new javax.swing.JSeparator();
        jScrollPane6 = new javax.swing.JScrollPane();
        TableStock = new javax.swing.JTable();
        jLabel21 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jButton11 = new javax.swing.JButton();
        jButton12 = new javax.swing.JButton();
        jButton13 = new javax.swing.JButton();
        jButton15 = new javax.swing.JButton();
        jButton16 = new javax.swing.JButton();
        jSeparator10 = new javax.swing.JSeparator();
        jLabel23 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();
        jLabel30 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("Supplier Name: ");

        jLabel2.setText("Amount:");

        jLabel3.setText("Invoice Number:");

        jLabel4.setText("Due Date:");

        txtSupplierName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSupplierNameActionPerformed(evt);
            }
        });

        txtAmount.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtAmountActionPerformed(evt);
            }
        });

        txtDueDate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtDueDateActionPerformed(evt);
            }
        });

        jButton1.setText("Add Invoice");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("Mark as Paid");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setText("View Outstanding");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jLabel5.setText("Outstanding Invoices");

        tblAP.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Invoice Number", "Supplier", "Amount", "Due Date", "Status"
            }
        ));
        jScrollPane1.setViewportView(tblAP);

        lblStatus.setText("Ready...");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator1)
            .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(52, 52, 52)
                                .addComponent(jButton1)
                                .addGap(62, 62, 62)
                                .addComponent(jButton2)
                                .addGap(63, 63, 63)
                                .addComponent(jButton3))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel1)
                                            .addComponent(jLabel2))
                                        .addGap(18, 18, 18)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(txtSupplierName, javax.swing.GroupLayout.DEFAULT_SIZE, 142, Short.MAX_VALUE)
                                            .addComponent(txtAmount))
                                        .addGap(26, 26, 26)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel3)
                                            .addComponent(jLabel4))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(txtInvoiceNumber)
                                            .addComponent(txtDueDate, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addComponent(jLabel5)))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblStatus)))
                        .addGap(0, 134, Short.MAX_VALUE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(txtInvoiceNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(61, 61, 61)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(txtDueDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(txtSupplierName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(61, 61, 61)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(txtAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(28, 28, 28)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2)
                    .addComponent(jButton3))
                .addGap(18, 18, 18)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 31, Short.MAX_VALUE)
                .addComponent(lblStatus)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Accounts Payable", jPanel1);

        jLabel6.setText("Customer Name:");

        jLabel7.setText("Amount:");

        jLabel8.setText("Invoice Number:");

        jLabel9.setText("Payment Status:");

        txtCustomerName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtCustomerNameActionPerformed(evt);
            }
        });

        txtAmount1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtAmount1ActionPerformed(evt);
            }
        });

        jButton4.setText("Add Invoice");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jButton5.setText("Record Payment");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        btnViewUnpaid.setText("View Unpaid");
        btnViewUnpaid.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnViewUnpaidActionPerformed(evt);
            }
        });

        jLabel10.setText("Invoices");

        tblAR.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Invoice Number", "Supplier", "Amount", "Due Date", "Status"
            }
        ));
        jScrollPane2.setViewportView(tblAR);

        lblStatus1.setText("Ready...");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator3, javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(jSeparator4)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(lblStatus1)
                        .addGap(0, 609, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel10)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(83, 83, 83)
                                .addComponent(jButton4)
                                .addGap(30, 30, 30)
                                .addComponent(jButton5)
                                .addGap(39, 39, 39)
                                .addComponent(btnViewUnpaid)))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6)
                            .addComponent(jLabel7))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(txtAmount1, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel9)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cmbStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(txtCustomerName, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 119, Short.MAX_VALUE)
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtInvoiceNumber1, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(41, 41, 41))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jScrollPane2)
                        .addContainerGap())))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel8)
                        .addComponent(txtInvoiceNumber1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel6)
                        .addComponent(txtCustomerName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(53, 53, 53)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(cmbStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtAmount1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7))
                .addGap(35, 35, 35)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton4)
                    .addComponent(jButton5)
                    .addComponent(btnViewUnpaid))
                .addGap(26, 26, 26)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel10)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblStatus1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Accounts Receivable", jPanel2);

        jLabel11.setText("Category:");

        jLabel12.setText("Notes:");

        jLabel13.setText("Amount:");

        txtCategory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtCategoryActionPerformed(evt);
            }
        });

        txtAmount2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtAmount2ActionPerformed(evt);
            }
        });

        jButton6.setText("Add Expense");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        jButton7.setText("View Monthly Totals");
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        jLabel14.setText("Expense Records");

        tblExpenses.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Category", "Amount", "Date", "Notes"
            }
        ));
        jScrollPane3.setViewportView(tblExpenses);

        lblStatus2.setText("Ready...");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator5)
            .addComponent(jSeparator6)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(24, 24, 24)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel11)
                            .addComponent(jLabel12))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(txtCategory, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel13)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(txtAmount2, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(txtNotes)))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGap(135, 135, 135)
                                .addComponent(jButton6)
                                .addGap(77, 77, 77)
                                .addComponent(jButton7))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel14)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane3)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(lblStatus2)
                                .addGap(0, 597, Short.MAX_VALUE)))))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(jLabel13)
                    .addComponent(txtCategory, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtAmount2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(21, 21, 21)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtNotes, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton6)
                    .addComponent(jButton7))
                .addGap(13, 13, 13)
                .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel14)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 219, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator6, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblStatus2)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Expenses", jPanel3);

        jLabel15.setText("Date:");

        txtDate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtDateActionPerformed(evt);
            }
        });

        jLabel16.setText("Accoount:");

        jLabel17.setText("Debit:");

        jLabel18.setText("Credit:");

        txtCredit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtCreditActionPerformed(evt);
            }
        });

        jLabel19.setText("Source Module:");

        jButton8.setText("View All");
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        jButton9.setText("Filter by Module");
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });

        jButton10.setText("Clear Filter");
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });

        jLabel20.setText("Ledger Entries");

        tblGL.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Date", "Account Name", "Debit", "Credit", "Source Module"
            }
        ));
        jScrollPane4.setViewportView(tblGL);

        GLStatus.setText("Displaying all entries.");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator7, javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(113, 113, 113)
                        .addComponent(jButton8)
                        .addGap(18, 18, 18)
                        .addComponent(jButton9)
                        .addGap(18, 18, 18)
                        .addComponent(jButton10))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel15)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtDate, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel16)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtAccount, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel17)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtDebit, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel18)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtCredit, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel19)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtSource, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel20))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(GLStatus)))
                .addContainerGap(74, Short.MAX_VALUE))
            .addComponent(jSeparator8)
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup()
                    .addGap(24, 24, 24)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 559, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(85, Short.MAX_VALUE)))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(txtDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16)
                    .addComponent(txtAccount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel17)
                    .addComponent(txtDebit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18)
                    .addComponent(txtCredit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel19)
                    .addComponent(txtSource, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(41, 41, 41)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton8)
                    .addComponent(jButton9)
                    .addComponent(jButton10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator7, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel20)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 285, Short.MAX_VALUE)
                .addComponent(jSeparator8, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(GLStatus)
                .addContainerGap())
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup()
                    .addGap(157, 157, 157)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 233, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(88, Short.MAX_VALUE)))
        );

        jTabbedPane2.addTab("General Ledger", jPanel4);

        TableProduct.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Product Name", "Title 3", "Price", "Stock", "Status"
            }
        ));
        jScrollPane5.setViewportView(TableProduct);

        TableStock.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Type", "Product Name", "Quantity", "Date", "Remarks"
            }
        ));
        jScrollPane6.setViewportView(TableStock);

        jLabel21.setText("Product List");

        jLabel22.setText("Stock Transactions");

        jButton11.setText("+ Add Product");
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });

        jButton12.setText("Edit Product");
        jButton12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton12ActionPerformed(evt);
            }
        });

        jButton13.setText("X Delete Product");
        jButton13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton13ActionPerformed(evt);
            }
        });

        jButton15.setText("Stock In/Out");
        jButton15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton15ActionPerformed(evt);
            }
        });

        jButton16.setText("  View Details   v");
        jButton16.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton16ActionPerformed(evt);
            }
        });

        jLabel23.setText("Total:");

        jLabel24.setText("Total In:");

        jLabel25.setText("Total Out:");

        jLabel26.setText("Current Stock: ");

        jLabel27.setText("0");

        jLabel28.setText("0");

        jLabel29.setText("0");

        jLabel30.setText("0");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jSeparator10)
                        .addContainerGap())
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane5)
                            .addComponent(jSeparator9)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(jLabel21)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton11)
                        .addGap(18, 18, 18)
                        .addComponent(jButton12)
                        .addGap(18, 18, 18)
                        .addComponent(jButton13)
                        .addGap(44, 44, 44))
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 656, Short.MAX_VALUE)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel22)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel23)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel27)
                        .addGap(57, 57, 57)
                        .addComponent(jLabel24)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel28)
                        .addGap(41, 41, 41)
                        .addComponent(jLabel25)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel29)
                        .addGap(43, 43, 43)
                        .addComponent(jLabel26)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel30)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton16)
                        .addContainerGap())))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton15)
                .addGap(29, 29, 29))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addComponent(jLabel21)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton11)
                    .addComponent(jButton12)
                    .addComponent(jButton13))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator9, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel22)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton15)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 17, Short.MAX_VALUE)
                .addComponent(jSeparator10, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel23)
                    .addComponent(jLabel24)
                    .addComponent(jLabel25)
                    .addComponent(jLabel26)
                    .addComponent(jLabel27)
                    .addComponent(jLabel28)
                    .addComponent(jLabel29)
                    .addComponent(jLabel30)
                    .addComponent(jButton16))
                .addContainerGap())
        );

        jTabbedPane2.addTab("Inventory", jPanel5);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane2, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane2, javax.swing.GroupLayout.Alignment.TRAILING)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtAmountActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtAmountActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtAmountActionPerformed

    private void txtDueDateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtDueDateActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtDueDateActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton3ActionPerformed

    private void txtSupplierNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSupplierNameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtSupplierNameActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        String supplier = txtSupplierName.getText();
        String invoice = txtInvoiceNumber.getText();
        double amount = Double.parseDouble(txtAmount.getText());
        String dueDate = txtDueDate.getText(); // or use a date picker

//        FinanceERP.addAP(supplier, invoice, amount, dueDate);

        lblStatus.setText("Invoice added successfully!");
        loadAPTable(); 
        loadGLTable();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
        int selectedRow = tblAP.getSelectedRow();
        if (selectedRow >= 0) {
            String invoiceNumber = tblAP.getValueAt(selectedRow, 0).toString();
            try (Connection conn = FinanceERP.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("UPDATE AccountsPayable SET Status='Paid' WHERE InvoiceNumber=?")) {
                pstmt.setString(1, invoiceNumber);
                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    lblStatus.setText("Invoice " + invoiceNumber + " marked as Paid!");
                    loadAPTable(); // refresh table
                } else {
                    lblStatus.setText("No invoice updated.");
                }
                loadAPTable();
            } catch (Exception e) {
                System.err.println("Update Error: " + e.getMessage());
            }
        } else {
            lblStatus.setText("Please select an invoice to mark as paid.");
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void txtCustomerNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtCustomerNameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtCustomerNameActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        // TODO add your handling code here:
        String customer = txtCustomerName.getText();
        String invoice = txtInvoiceNumber1.getText();
        double amount = Double.parseDouble(txtAmount1.getText());
        String status = cmbStatus.getText();

//        FinanceERP.addAR(customer, invoice, amount, status);

        lblStatus1.setText("AR Invoice added successfully!");
        loadARTable();
        loadGLTable();
    }//GEN-LAST:event_jButton4ActionPerformed

    private void txtAmount1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtAmount1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtAmount1ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        // TODO add your handling code here:
        int selectedRow = tblAR.getSelectedRow();
        if (selectedRow == -1) {
            lblStatus.setText("Please select an invoice first.");
            return;
        }

        String invoiceNumber = tblAR.getValueAt(selectedRow, 0).toString();

        try (Connection conn = FinanceERP.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE AccountsReceivable SET PaymentStatus='Paid' WHERE InvoiceNumber=?")) {

            pstmt.setString(1, invoiceNumber);
            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                lblStatus.setText("Invoice " + invoiceNumber + " marked as Paid!");
                loadARTable();
            } else {
                lblStatus.setText("No matching invoice found.");
            }

        } catch (Exception e) {
            System.err.println("Update Error (AR): " + e.getMessage());
            lblStatus.setText("Error updating invoice.");
        }
    }//GEN-LAST:event_jButton5ActionPerformed

    private void btnViewUnpaidActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnViewUnpaidActionPerformed
        // TODO add your handling code here:
        if (!showingUnpaid) {
        // Show only unpaid
            try (Connection conn = FinanceERP.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM AccountsReceivable WHERE PaymentStatus='Pending'")) {

                DefaultTableModel model = (DefaultTableModel) tblAR.getModel();
                model.setRowCount(0);

                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getString("InvoiceNumber"),
                        rs.getString("CustomerName"),
                        rs.getDouble("Amount"),
                        rs.getString("PaymentStatus")
                    });
                }
                lblStatus.setText("Showing unpaid invoices.");
                showingUnpaid = true; // toggle ON
            } catch (Exception e) {
                System.err.println("Filter Error (AR): " + e.getMessage());
            }
            btnViewUnpaid.setText("View All");
        } else {
            // Show all
            loadARTable(); // your normal method
            lblStatus.setText("Showing all invoices.");
            btnViewUnpaid.setText("View Unpaid");
            showingUnpaid = false; // toggle OFF
        }
    }//GEN-LAST:event_btnViewUnpaidActionPerformed

    private void txtCategoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtCategoryActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtCategoryActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        // TODO add your handling code here:
       addExpense();
    }//GEN-LAST:event_jButton6ActionPerformed

    private void txtAmount2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtAmount2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtAmount2ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        // TODO add your handling code here:
       showMonthlyExpenseTotal();
    }//GEN-LAST:event_jButton7ActionPerformed

    private void txtDateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtDateActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtDateActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        // TODO add your handling code here:
        loadGLTable();
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        // TODO add your handling code here:
        String date = txtDate.getText().trim();
        String account = txtAccount.getText().trim();
        String debit = txtDebit.getText().trim();
        String credit = txtCredit.getText().trim();
        String source = txtSource.getText().trim();
        
        StringBuilder query = new StringBuilder("SELECT * FROM GeneralLedger WHERE 1=1");

        if (!date.isEmpty()) query.append(" AND Date LIKE '%").append(date).append("%'");
        if (!account.isEmpty()) query.append(" AND AccountName LIKE '%").append(account).append("%'");
        if (!debit.isEmpty()) query.append(" AND Debit LIKE '%").append(debit).append("%'");
        if (!credit.isEmpty()) query.append(" AND Credit LIKE '%").append(credit).append("%'");
        if (!source.isEmpty()) query.append(" AND SourceModule LIKE '%").append(source).append("%'");

        try (Connection conn = FinanceERP.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query.toString())) {

            DefaultTableModel model = (DefaultTableModel) tblGL.getModel();
            model.setRowCount(0);

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("Date"),
                    rs.getString("AccountName"),
                    rs.getDouble("Debit"),
                    rs.getDouble("Credit"),
                    rs.getString("SourceModule"),
                });
            }

            lblStatus.setText("Filtered results displayed.");
        } catch (Exception e) {
            System.err.println("Filter Error (GL): " + e.getMessage());
        }
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        // TODO add your handling code here:
        loadGLTable(); // reload all entries
        // Clear all text fields
        txtDate.setText("");
        txtAccount.setText("");
        txtDebit.setText("");
        txtCredit.setText("");
        txtSource.setText("");
        txtNotes.setText("");

        // Update status
        lblStatus.setText("Displaying all entries.");
    }//GEN-LAST:event_jButton10ActionPerformed

    private void txtCreditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtCreditActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtCreditActionPerformed

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
        // TODO add your handling code here:
        openAddProductDialog();
    }//GEN-LAST:event_jButton11ActionPerformed

    private void jButton16ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton16ActionPerformed
        openViewDetailsDialog();
    }//GEN-LAST:event_jButton16ActionPerformed

    private void jButton15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton15ActionPerformed
        // TODO add your handling code here:
        openStockInOutDialog();
    }//GEN-LAST:event_jButton15ActionPerformed

    private void jButton12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton12ActionPerformed
        // TODO add your handling code here:
        openEditProductDialog();
    }//GEN-LAST:event_jButton12ActionPerformed

    private void jButton13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton13ActionPerformed
        // TODO add your handling code here:
        toggleSelectedProduct();
    }//GEN-LAST:event_jButton13ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Main().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel GLStatus;
    private javax.swing.JTable TableProduct;
    private javax.swing.JTable TableStock;
    private javax.swing.JButton btnViewUnpaid;
    private javax.swing.JTextField cmbStatus;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton15;
    private javax.swing.JButton jButton16;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator10;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JSeparator jSeparator9;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JLabel lblStatus1;
    private javax.swing.JLabel lblStatus2;
    private javax.swing.JTable tblAP;
    private javax.swing.JTable tblAR;
    private javax.swing.JTable tblExpenses;
    private javax.swing.JTable tblGL;
    private javax.swing.JTextField txtAccount;
    private javax.swing.JTextField txtAmount;
    private javax.swing.JTextField txtAmount1;
    private javax.swing.JTextField txtAmount2;
    private javax.swing.JTextField txtCategory;
    private javax.swing.JTextField txtCredit;
    private javax.swing.JTextField txtCustomerName;
    private javax.swing.JTextField txtDate;
    private javax.swing.JTextField txtDebit;
    private javax.swing.JTextField txtDueDate;
    private javax.swing.JTextField txtInvoiceNumber;
    private javax.swing.JTextField txtInvoiceNumber1;
    private javax.swing.JTextField txtNotes;
    private javax.swing.JTextField txtSource;
    private javax.swing.JTextField txtSupplierName;
    // End of variables declaration//GEN-END:variables
}
