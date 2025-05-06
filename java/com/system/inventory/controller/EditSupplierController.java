package com.system.inventory.controller;

import com.system.inventory.database.SQLConnection;
import com.system.inventory.model.LoggedInStaff;
import com.system.inventory.model.Supplier;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class EditSupplierController implements Initializable {
    private LoggedInStaff loggedInStaff;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadBrands();
    }

    private void loadBrands() {
        ObservableList<String> brands = FXCollections.observableArrayList();

        String query = "SELECT name FROM brands";

        try (Connection conn = SQLConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                brands.add(rs.getString("name"));
            }

            existingBrandChoiceBox.setItems(brands);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private TextField addressField;

    @FXML
    private TextField contactNumField;

    @FXML
    private Group existingBrand;

    @FXML
    private ChoiceBox<String> existingBrandChoiceBox;

    @FXML
    private TextField nameField;

    @FXML
    private TextField numberField;

    @FXML
    void cancelButton() {
        close();
    }

    @FXML
    void existingBrandButton() {
        existingBrand.setVisible(true);
    }

    @FXML
    void updateButton() {
        String name = nameField.getText();
        String contact = contactNumField.getText();
        String address = addressField.getText();
        String brandName = existingBrandChoiceBox.getValue();
        int supplierId = Integer.parseInt(numberField.getText());

        if (name.isEmpty() || contact.isEmpty() || address.isEmpty() || brandName == null) {
            System.out.println("Please fill all fields.");
            return;
        }

        try (Connection conn = SQLConnection.getConnection()) {

            String updateSupplierSql = "UPDATE supplier SET name = ?, contact = ?, address = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateSupplierSql)) {
                ps.setString(1, name);
                ps.setString(2, contact);
                ps.setString(3, address);
                ps.setInt(4, supplierId);
                ps.executeUpdate();
            }

            String transQuery = """
                                                INSERT INTO transactions (id, type, name, supplier_name, brand, series, qty, price, transaction_date)
                                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)
                                                """;
            try(PreparedStatement trans = conn.prepareStatement(transQuery)){
                trans.setInt(1, transactionID());
                trans.setString(2, "Update Supplier \n Add Brand");
                trans.setString(3, loggedInStaff.getStaffName());
                trans.setString(4, name);
                trans.setString(5, brandName);
                trans.setString(6, " ");
                trans.setInt(7, 0);
                trans.setDouble(8, 0.0);
                trans.executeUpdate();
            }

            int brandId = -1;
            String getBrandIdSql = "SELECT id FROM brands WHERE name = ?";
            try (PreparedStatement ps = conn.prepareStatement(getBrandIdSql)) {
                ps.setString(1, brandName);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    brandId = rs.getInt("id");
                }
            }

            if (brandId != -1) {
                String checkSql = "SELECT COUNT(*) FROM supplier_brands WHERE supplier_id = ? AND brand_id = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setInt(1, supplierId);
                    checkStmt.setInt(2, brandId);
                    ResultSet rs = checkStmt.executeQuery();
                    rs.next();

                    if (rs.getInt(1) > 0) {
                        showAlert("Error: Supplier already has this brand assigned.", Alert.AlertType.ERROR);
                        return;
                    }

                    String insertSql = "INSERT INTO supplier_brands (supplier_id, brand_id) VALUES (?, ?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setInt(1, supplierId);
                        insertStmt.setInt(2, brandId);
                        insertStmt.executeUpdate();
                    }
                }
            }

            showAlert("Supplier updated successfully!", Alert.AlertType.ERROR);
            close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void setSupplier(Supplier supplier) {
        loadSupplierDetails(supplier.getId());
    }

    public void loadSupplierDetails(int supplierId) {
        String query = """
        SELECT s.name, s.contact, s.address,
               GROUP_CONCAT(b.name SEPARATOR ', ') AS brands
        FROM supplier s
        JOIN supplier_brands sb ON s.id = sb.supplier_id
        JOIN brands b ON sb.brand_id = b.id
        WHERE s.id = ?
        GROUP BY s.id, s.name, s.contact, s.address
    """;

        try (Connection conn = SQLConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, supplierId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                nameField.setText(rs.getString("name"));
                contactNumField.setText(rs.getString("contact"));
                addressField.setText(rs.getString("address"));
                numberField.setText(String.valueOf(supplierId));

                String brands = rs.getString("brands");
                if (brands != null && !brands.isEmpty()) {
                    existingBrandChoiceBox.setValue(brands.split(",\\s*")[0]);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void close(){
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

    public void setLoggedInStaff(LoggedInStaff staff) {
        this.loggedInStaff = staff;
    }


}
