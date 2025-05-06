package com.system.inventory.controller;

import com.system.inventory.database.SQLConnection;
import com.system.inventory.main.Main;
import com.system.inventory.model.LoggedInStaff;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class NewSupplierController implements Initializable {

    private LoggedInStaff loggedInStaff;
    private DashboardController dashboardController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadBrands();
        loadSeries();
    }

    private void loadBrands() {
        ObservableList<String> brandList = FXCollections.observableArrayList();

        String sql = "SELECT name FROM brands";
        try (Connection conn = SQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                brandList.add(rs.getString("name"));
            }

            existingBrandField.setItems(brandList);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadSeries() {
        ObservableList<String> seriesList = FXCollections.observableArrayList();

        String sql = "SELECT name FROM series";
        try (Connection conn = SQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                seriesList.add(rs.getString("name"));
            }

            existingSeiresField.setItems(seriesList);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private int getSeriesIdByName(String seriesName) {
        String query = "SELECT id FROM series WHERE name = ?";
        try (Connection conn = SQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, seriesName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // not found
    }

    

    private boolean supplierExists(String name) {
        String sql = "SELECT COUNT(*) FROM supplier WHERE name = ?";
        try (Connection conn = SQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @FXML
    private TextField addressField;

    @FXML
    private TextField contactNumField;

    @FXML
    private VBox existingBrand;

    @FXML
    private ChoiceBox<String> existingBrandField;

    @FXML
    private ChoiceBox<String> existingSeiresField;

    @FXML
    private VBox existingSeries;

    @FXML
    private TextField nameField;

    @FXML
    private VBox newBrand;

    @FXML
    private TextField newBrandField;

    @FXML
    private VBox newSeries;

    @FXML
    private TextField newSeriesField;

    @FXML
    void addButton() {
        String name = nameField.getText();
        String contact = contactNumField.getText();
        String address = addressField.getText();

        String newBrand1 = newBrandField.getText();
        String newSeries1 = newSeriesField.getText();
        String existBrand = existingBrandField.getValue();
        String existSeries = existingSeiresField.getValue();

        if (name == null || name.trim().isEmpty()) {
            showAlert("Please enter the supplier name.", Alert.AlertType.ERROR);
            return;
        }

        if (supplierExists(name)) {
            showAlert("Supplier with this name already exists!", Alert.AlertType.ERROR);
            return;
        }

        if (contact == null || contact.trim().isEmpty() || !contact.matches("\\d{11}")) {
            showAlert("Please enter a valid 11-digit contact number.", Alert.AlertType.ERROR);
            return;
        }

        int supId = supplierID();
        int seriesId = -1;
        int brandId = -1;

        if (existingSeiresField.isVisible() && existSeries != null && !existSeries.isEmpty()) {
            seriesId = getSeriesIdByName(existSeries);
        } else if (newSeries.isVisible() && !newSeries1.isEmpty()) {
            seriesId = seriesID();
        }

        if (seriesId == -1) {
            showAlert("Please provide or select a series.", Alert.AlertType.ERROR);
            return;
        }

        if (newBrand.isVisible() && newBrand1.isEmpty()) {
            showAlert("Please fill the brand field", Alert.AlertType.ERROR);
            return;
        } else if (newSeries.isVisible() && newSeries1.isEmpty()) {
            showAlert("Please fill the series field", Alert.AlertType.ERROR);
            return;
        } else if (existingSeries.isVisible() && existSeries == null) {
            showAlert("Please choose a series", Alert.AlertType.ERROR);
            return;
        } else if (existingBrand.isVisible() && existBrand == null) {
            showAlert("Please choose a brand", Alert.AlertType.ERROR);
            return;
        }

        try (Connection conn = SQLConnection.getConnection()) {
            conn.setAutoCommit(false);

            if (newSeries.isVisible() && !newSeries1.isEmpty()) {
                String seriesSQL = "INSERT INTO series(id, name) VALUES (?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(seriesSQL)) {
                    ps.setInt(1, seriesId);
                    ps.setString(2, newSeries1);
                    ps.executeUpdate();
                }
            }

            if (newBrand.isVisible() && !newBrand1.isEmpty()) {
                brandId = brandID();
                String brandSQL = "INSERT INTO brands(id, name, series) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(brandSQL)) {
                    ps.setInt(1, brandId);
                    ps.setString(2, newBrand1);
                    ps.setInt(3, seriesId);
                    ps.executeUpdate();
                }

                String transactionQuery = """
            INSERT INTO transactions (id, type, name, supplier_name, brand, series, qty, price, transaction_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)
            """;
                try (PreparedStatement psr = conn.prepareStatement(transactionQuery)) {
                    psr.setInt(1, transactionID());
                    psr.setString(2, "New Brand");
                    psr.setString(3, loggedInStaff.getStaffName());
                    psr.setString(4, name);
                    psr.setString(5, newBrand1);
                    psr.setString(6, newSeries1);
                    psr.setInt(7, 0);
                    psr.setDouble(8, 0.0);
                    psr.executeUpdate();
                }

            } else if (existingBrand.isVisible() && existBrand != null) {
                brandId = getBrandIdByName(existBrand);
            }

            String supplierSQL = "INSERT INTO supplier(id, name, contact, address) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(supplierSQL)) {
                ps.setInt(1, supId);
                ps.setString(2, name);
                ps.setString(3, contact);
                ps.setString(4, address);
                ps.executeUpdate();
            }

            if (brandId != -1) {
                String supplierBrandsSQL = "INSERT INTO supplier_brands(supplier_id, brand_id) VALUES (?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(supplierBrandsSQL)) {
                    ps.setInt(1, supId);
                    ps.setInt(2, brandId);
                    ps.executeUpdate();
                }
            }

            String transactionQuery = """
        INSERT INTO transactions (id, type, name, supplier_name, brand, series, qty, price, transaction_date)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)
        """;
            try (PreparedStatement ps = conn.prepareStatement(transactionQuery)) {
                ps.setInt(1, transactionID());
                ps.setString(2, "New Supplier");
                ps.setString(3, loggedInStaff.getStaffName());
                ps.setString(4, name);
                ps.setString(5, newBrand1);
                ps.setString(6, newSeries1);
                ps.setInt(7, 0);
                ps.setDouble(8, 0.0);
                ps.executeUpdate();
            }

            conn.commit();

            if (dashboardController != null) {
                dashboardController.updateLabels();
                dashboardController.refreshButton();
            }

            showAlert("New Supplier Added Successfully!", Alert.AlertType.INFORMATION);
            closePage();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database error: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }


    private int getBrandIdByName(String brandName) {
        String sql = "SELECT id FROM brands WHERE name = ?";
        try (Connection conn = SQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, brandName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }


    private int transactionID() {
        int transactionId;
        boolean exists;

        String query = "SELECT 1 FROM transactions WHERE id = ?";

        try (Connection conn = SQLConnection.getConnection()) {
            do {
                int randomNum = (int) (Math.random() * 1_000_000); // up to 999999
                String formattedNumber = String.format("%06d", randomNum); // always 6 digits
                transactionId = Integer.parseInt("05" + formattedNumber);

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, transactionId);
                    ResultSet rs = stmt.executeQuery();
                    exists = rs.next(); // if a result is found, ID exists
                }

            } while (exists);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error generating transaction ID: " + e.getMessage(), Alert.AlertType.ERROR);
            return -1; // handle failure appropriately
        }

        return transactionId;
    }



    @FXML
    void cancelButton() {
        closePage();
    }

    @FXML
    void existingBrandButton() {
        existingBrand.setVisible(true);
        newBrand.setVisible(false);

    }

    @FXML
    void existingSeriesButton() {
        existingSeries.setVisible(true);
        newSeries.setVisible(false);
    }

    @FXML
    void newBrandButton() {
        newBrand.setVisible(true);
        existingBrand.setVisible(false);
    }

    @FXML
    void newSeriesButton() {
        newSeries.setVisible(true);
        existingSeries.setVisible(false);

    }

    public void setLoggedInStaff(LoggedInStaff loggedInStaff) {
        this.loggedInStaff = loggedInStaff;
    }

    private int brandID() {
        int randomNum = (int) (Math.random() * 1_000_0000);

        String formattedNumber = String.format("%06d", randomNum);

        return Integer.parseInt("02" + formattedNumber);
    }


    private int seriesID() {
        int randomNum = (int) (Math.random() * 1_000_0000);

        String formattedNumber = String.format("%06d", randomNum);

        return Integer.parseInt("03" + formattedNumber);
    }

    private int supplierID() {
        int randomNum = (int) (Math.random() * 1_000_0000);

        String formattedNumber = String.format("%06d", randomNum);

        return Integer.parseInt("01" + formattedNumber);
    }


    private void closePage(){
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    private static void showAlert(String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle("Notification");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setDashboardController(DashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    public void updateBarChart (DashboardController dashboardController){
        this.dashboardController = dashboardController;
    }
}
