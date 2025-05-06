package com.system.inventory.controller;

import com.system.inventory.database.SQLConnection;
import com.system.inventory.model.LoggedInStaff;
import com.system.inventory.model.Staff;
import com.system.inventory.model.Stocks;
import com.system.inventory.model.Supplier;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class EditStocksController implements Initializable {

    private LoggedInStaff loggedInStaff;
    private DashboardController dashboardController;
    @Override
    public void initialize (URL url, ResourceBundle resourceBundle){

    }

    @FXML
    private TextField brandField;

    @FXML
    private TextField categoryField;

    @FXML
    private TextField idField;

    @FXML
    private Label labelButton;

    @FXML
    private VBox newPrice;

    @FXML
    private TextField newPriceField;

    @FXML
    private TextField priceField;

    @FXML
    private TextField qtyField;

    @FXML
    private TextField seriesField;



    @FXML
    private TextField sizesField;

    @FXML
    private VBox stockIn;

    @FXML
    private TextField stockInField;

    @FXML
    private VBox stockOut;

    @FXML
    private TextField stockOutField;

    @FXML
    private TextField supplierField;


    @FXML
    void cancelButton() {
        close();
    }

    @FXML
    void newPriceButton() {
        newPrice.setVisible(true);
        stockOut.setVisible(false);
        stockIn.setVisible(false);
    }

    @FXML
    void stockInButton() {
        newPrice.setVisible(false);
        stockOut.setVisible(false);
        stockIn.setVisible(true);
    }

    @FXML
    void stockOutButton() {
        newPrice.setVisible(false);
        stockOut.setVisible(true);
        stockIn.setVisible(false);
    }

    @FXML
    void updateButton() {
        String price = newPriceField.getText();
        String out = stockOutField.getText();
        String in = stockInField.getText();
        String id = idField.getText();

        if (newPrice.isVisible()) {
            double newPriceVal;
            try {
                newPriceVal = Double.parseDouble(price);
            } catch (NumberFormatException e) {
                showAlert("Price must be a valid number!", Alert.AlertType.ERROR);
                return;
            }

            try (Connection conn = SQLConnection.getConnection()) {
                double currentPrice = -1;
                String priceQuery = "SELECT price FROM stocks WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(priceQuery)) {
                    stmt.setInt(1, Integer.parseInt(id));
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        currentPrice = rs.getDouble("price");
                    }
                }

                if (currentPrice == newPriceVal) {
                    showAlert("New price is the same as the current price.", Alert.AlertType.WARNING);
                    return;
                }

                Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                confirmation.setTitle("Confirm Price Update");
                confirmation.setHeaderText(null);
                confirmation.setContentText("Are you sure you want to update the price of this stock?\nID: " + id);
                confirmation.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        try {
                            String updateQuery = "UPDATE stocks SET price = ? WHERE id = ?";
                            PreparedStatement ps = conn.prepareStatement(updateQuery);
                            ps.setDouble(1, newPriceVal);
                            ps.setInt(2, Integer.parseInt(id));
                            ps.executeUpdate();

                            String transQuery = """
                        INSERT INTO transactions (id, type, name, supplier_name, brand, series, qty, price, transaction_date)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)
                    """;
                            try (PreparedStatement trans = conn.prepareStatement(transQuery)) {
                                trans.setInt(1, transactionID());
                                trans.setString(2, "New Price");
                                trans.setString(3, loggedInStaff.getStaffName());
                                trans.setString(4, "");
                                trans.setString(5, brandField.getText());
                                trans.setString(6, seriesField.getText());
                                trans.setInt(7, 0);
                                trans.setDouble(8, newPriceVal);
                                trans.executeUpdate();
                            }

                            showAlert("Price updated successfully.", Alert.AlertType.INFORMATION);
                        } catch (SQLException e) {
                            showAlert("Failed to update price: " + e.getMessage(), Alert.AlertType.ERROR);
                        }
                    }
                });

            } catch (SQLException e) {
                showAlert("Failed to check current price: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else if (stockIn.isVisible()) {
            int stockInVal;
            try {
                stockInVal = Integer.parseInt(in);
            } catch (NumberFormatException e) {
                showAlert("Stock In must be a valid number!", Alert.AlertType.ERROR);
                return;
            }

            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirm Stock In");
            confirmation.setHeaderText(null);
            confirmation.setContentText("Are you sure you want to add quantity?\nID: " + id);
            confirmation.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        int oldQty = Integer.parseInt(qtyField.getText());
                        int newQty = oldQty + stockInVal;

                        String newStatus = getStockStatus(newQty);

                        try (Connection conn = SQLConnection.getConnection()) {
                            String updateQuery = "UPDATE stocks SET qty = ?, status = ? WHERE id = ?";
                            PreparedStatement ps = conn.prepareStatement(updateQuery);
                            ps.setInt(1, newQty);
                            ps.setString(2, newStatus);
                            ps.setInt(3, Integer.parseInt(id));
                            ps.executeUpdate();

                            String transQuery = """
                                                INSERT INTO transactions (id, type, name, supplier_name, brand, series, qty, price, transaction_date)
                                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)
                                                """;
                            try(PreparedStatement trans = conn.prepareStatement(transQuery)){
                                trans.setInt(1, transactionID());
                                trans.setString(2, "Stock In");
                                trans.setString(3, loggedInStaff.getStaffName());
                                trans.setString(4, "");
                                trans.setString(5, brandField.getText());
                                trans.setString(6, seriesField.getText());
                                trans.setInt(7, stockInVal);
                                trans.setDouble(8, 0.0);
                                trans.executeUpdate();
                            }

                            if (dashboardController != null){
                                dashboardController.updateLabels();
                            }

                            if (dashboardController != null) {
                                dashboardController.populateBrandStockChart();
                                dashboardController.populateMonthlyStockChart();
                            }
                            showAlert("Stock quantity increased.", Alert.AlertType.INFORMATION);
                        }
                    } catch (Exception e) {
                        showAlert("Failed to increase stock: " + e.getMessage(), Alert.AlertType.ERROR);
                    }
                }
            });

        } else if (stockOut.isVisible()) {
            int stockOutVal;
            try {
                stockOutVal = Integer.parseInt(out);
            } catch (NumberFormatException e) {
                showAlert("Stock Out must be a valid number!", Alert.AlertType.ERROR);
                return;
            }

            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirm Stock Out");
            confirmation.setHeaderText(null);
            confirmation.setContentText("Are you sure you want to deduct quantity?\nID: " + id);
            confirmation.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        int oldQty = Integer.parseInt(qtyField.getText());
                        int newQty = oldQty - stockOutVal;

                        if (newQty < 0) {
                            showAlert("Stock cannot be negative!", Alert.AlertType.ERROR);
                            return;
                        }

                        String newStatus = getStockStatus(newQty);

                        try (Connection conn = SQLConnection.getConnection()) {
                            String updateQuery = "UPDATE stocks SET qty = ?, status = ? WHERE id = ?";
                            PreparedStatement ps = conn.prepareStatement(updateQuery);
                            ps.setInt(1, newQty);
                            ps.setString(2, newStatus);
                            ps.setInt(3, Integer.parseInt(id));
                            ps.executeUpdate();

                            String transQuery = """
                                                INSERT INTO transactions (id, type, name, supplier_name, brand, series, qty, price, transaction_date)
                                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)
                                                """;
                            try(PreparedStatement trans = conn.prepareStatement(transQuery)){
                                trans.setInt(1, transactionID());
                                trans.setString(2, "Stock out");
                                trans.setString(3, loggedInStaff.getStaffName());
                                trans.setString(4, "");
                                trans.setString(5, brandField.getText());
                                trans.setString(6, seriesField.getText());
                                trans.setInt(7, stockOutVal);
                                trans.setDouble(8, 0.0);
                                trans.executeUpdate();
                            }

                            if (dashboardController != null){
                                dashboardController.updateLabels();
                            }

                            if (dashboardController != null) {
                                dashboardController.populateBrandStockChart();
                                dashboardController.populateMonthlyStockChart();
                            }
                            showAlert("Stock quantity deducted.", Alert.AlertType.INFORMATION);
                        }
                    } catch (Exception e) {
                        showAlert("Failed to reduce stock: " + e.getMessage(), Alert.AlertType.ERROR);
                    }
                }
            });
        }
    }

    private String getStockStatus(int qty) {
        if (qty == 0) {
            return "Out of Stocks";
        } else if (qty >= 1 && qty <= 30) {
            return "Low Level";
        } else if (qty >= 31 && qty <= 80) {
            return "Medium Level";
        } else {
            return "High Level";
        }
    }


    private void close(){
        Stage stage = (Stage) brandField.getScene().getWindow();
        stage.close();
    }


    private int transactionID() {
        int transactionId;
        boolean exists;

        String query = "SELECT 1 FROM transactions WHERE id = ?";

        try (Connection conn = SQLConnection.getConnection()) {
            do {
                int randomNum = (int) (Math.random() * 1_000_000);
                String formattedNumber = String.format("%06d", randomNum);
                transactionId = Integer.parseInt("05" + formattedNumber);

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, transactionId);
                    ResultSet rs = stmt.executeQuery();
                    exists = rs.next();
                }

            } while (exists);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error generating transaction ID: " + e.getMessage(), Alert.AlertType.ERROR);
            return -1;
        }

        return transactionId;
    }

    Stage stage;
    private Runnable onCloseCallback;

    public void setStockData(Stocks stock) {
        idField.setText(String.valueOf(stock.getId()));
        brandField.setText(stock.getBrand());
        seriesField.setText(stock.getSeries());
        supplierField.setText(stock.getSupplier());
        categoryField.setText(stock.getCom());
        qtyField.setText(String.valueOf(stock.getQty()));
        priceField.setText(String.valueOf(stock.getPrice()));
        sizesField.setText(String.valueOf(stock.getSize()));
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setOnCloseRequest(e -> {
            if (onCloseCallback != null) onCloseCallback.run();
        });
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    private static void showAlert(String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle("Notification");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setLoggedInStaff(LoggedInStaff staff) {

        this.loggedInStaff = staff;
    }

    public void updateBarChart(DashboardController dashboardController){
        this.dashboardController = dashboardController;
    }

    public void updateLabels(DashboardController dashboardController){
        this.dashboardController = dashboardController;
    }
}
