package com.system.inventory.controller;

import com.system.inventory.database.SQLConnection;
import com.system.inventory.model.LoggedInStaff;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class newStocksController implements Initializable {
    private LoggedInStaff loggedInStaff;
    private DashboardController dashboardController;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadSuppliers();
        loadBrands();
        loadSeries();

        forChoiceBox.setItems(FXCollections.observableArrayList(
                "Men",
                "Women",
                "Unisex",
                "Child"
        ));
    }

    private void loadSuppliers() {
        ObservableList<String> suppliers = FXCollections.observableArrayList();

        String sql = "SELECT name FROM supplier";

        try (Connection conn = SQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                suppliers.add(rs.getString("name"));
            }

            supplierChoiceBox.setItems(suppliers);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadBrands() {
        ObservableList<String> brands = FXCollections.observableArrayList();

        String sql = "SELECT name FROM brands";

        try (Connection conn = SQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                brands.add(rs.getString("name"));
            }

            brandChoiceBox.setItems(brands);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadSeries() {
        ObservableList<String> series = FXCollections.observableArrayList();

        String sql = "SELECT name FROM series";

        try (Connection conn = SQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                series.add(rs.getString("name"));
            }

            seriesChoiceBox.setItems(series);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private ChoiceBox<String> brandChoiceBox;

    @FXML
    private ChoiceBox<String> forChoiceBox;

    @FXML
    private TextField priceField;

    @FXML
    private TextField qtyField;

    @FXML
    private ChoiceBox<String> seriesChoiceBox;

    @FXML
    private ChoiceBox<String> supplierChoiceBox;

    @FXML
    private TextField sizeField;

    @FXML
    void addButton() {

        int id = stockID();

        String brand = brandChoiceBox.getValue();
        String series = seriesChoiceBox.getValue();
        String supplier = supplierChoiceBox.getValue();
        String com = forChoiceBox.getValue();

        String quantity = qtyField.getText();
        String price = priceField.getText();
        String size = sizeField.getText();

        if (brand == null) {
            showAlert("Please select a brand!", Alert.AlertType.ERROR);
            return;
        } else if (series == null) {
            showAlert("Please select a series!", Alert.AlertType.ERROR);
            return;
        } else if (supplier == null) {
            showAlert("Please select a supplier!", Alert.AlertType.ERROR);
            return;
        } else if (com == null) {
            showAlert("Please select a category!", Alert.AlertType.ERROR);
            return;
        } else if (quantity.isEmpty()) {
            showAlert("Quantity field is empty", Alert.AlertType.ERROR);
            return;
        } else if (price.isEmpty()) {
            showAlert("Price field is empty", Alert.AlertType.ERROR);
            return;
        } else if (size.isEmpty()) {
            showAlert("Size field is empty", Alert.AlertType.ERROR);
            return;
        }

        int sizeVal, qtyVal;
        double priceVal;

        try {
            sizeVal = Integer.parseInt(size);
        } catch (NumberFormatException e) {
            showAlert("Size must be a valid number!", Alert.AlertType.ERROR);
            return;
        }

        try {
            qtyVal = Integer.parseInt(quantity);
        } catch (NumberFormatException e) {
            showAlert("Quantity must be a valid number!", Alert.AlertType.ERROR);
            return;
        }

        try {
            priceVal = Double.parseDouble(price);
        } catch (NumberFormatException e) {
            showAlert("Price must be a valid number!", Alert.AlertType.ERROR);
            return;
        }

        // Determine stock status
        String status;
        if (qtyVal >= 1 && qtyVal < 30) {
            status = "Low Level";
        } else if (qtyVal >= 30 && qtyVal < 80) {
            status = "Medium Level";
        } else if (qtyVal >= 80) {
            status = "High Level";
        } else {
            showAlert("Quantity must not be zero!", Alert.AlertType.ERROR);
            return;
        }

        String checkQuery = "SELECT COUNT(*) FROM stocks WHERE brand = ? AND series = ? AND com = ? AND sizes = ?";

        try (Connection conn = SQLConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {

            checkStmt.setString(1, brand);
            checkStmt.setString(2, series);
            checkStmt.setString(3, com);
            checkStmt.setInt(4, sizeVal);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                showAlert("Stock with this size and category already exists!", Alert.AlertType.ERROR);
                return;
            }

            String stocksQuery = "INSERT INTO stocks (id, brand, series, com, sizes, qty, price, status, supplier)" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(stocksQuery)) {
                ps.setInt(1, id);
                ps.setString(2, brand);
                ps.setString(3, series);
                ps.setString(4, com);
                ps.setInt(5, sizeVal);
                ps.setInt(6, qtyVal);
                ps.setDouble(7, priceVal);
                ps.setString(8, status);
                ps.setString(9, supplier);
                ps.executeUpdate();

                showAlert("New Stock Added Successfully!", Alert.AlertType.INFORMATION);
                close();

                if (dashboardController != null){
                    dashboardController.updateLabels();
                    dashboardController.refreshButton();

                }
            }

            String transQuery = """
                                                INSERT INTO transactions (id, type, name, supplier_name, brand, series, qty, price, transaction_date)
                                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)
                                                """;
            try(PreparedStatement trans = conn.prepareStatement(transQuery)){
                trans.setInt(1, transactionID());
                trans.setString(2, "New Stock");
                trans.setString(3, loggedInStaff.getStaffName());
                trans.setString(4, "");
                trans.setString(5, brand);
                trans.setString(6, series);
                trans.setInt(7, qtyVal);
                trans.setDouble(8, 0.0);
                trans.executeUpdate();
            }



        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database error: " + e.getMessage(), Alert.AlertType.ERROR);
        }
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


    @FXML
    void cancelButton() {
        close();
    }

    private int stockID() {
        int randomNum = (int) (Math.random() * 1_000_0000);

        String formattedNumber = String.format("%06d", randomNum);

        return Integer.parseInt("7" + formattedNumber);
    }

    private void close(){
        Stage stage = (Stage) qtyField.getScene().getWindow();
        stage.close();
    }

    public void setLoggedInStaff(LoggedInStaff loggedInStaff) {

        this.loggedInStaff = loggedInStaff;
    }

    public void setDashboardController(DashboardController dashboardController){
        this.dashboardController = dashboardController;
    }

    private static void showAlert(String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle("Notification");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
