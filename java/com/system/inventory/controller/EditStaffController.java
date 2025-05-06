package com.system.inventory.controller;

import com.system.inventory.database.SQLConnection;
import com.system.inventory.model.LoggedInStaff;
import com.system.inventory.model.Staff;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;



public class EditStaffController implements Initializable {

    private LoggedInStaff loggedInStaff;
    private Stage stage;
    private Runnable onCloseCallback;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        positionChoiceBox.setItems(FXCollections.observableArrayList(
                "Admin",
                "Manager",
                "Co-Manager",
                "Staff"
        ));
        positionChoiceBox.setValue(" ");
    }

    @FXML
    private TextField addressField;

    @FXML
    private TextField ageField;

    @FXML
    private TextField birthdateField;

    @FXML
    private TextField contactField;

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField genderField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField middleNameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ChoiceBox<String> positionChoiceBox;

    @FXML
    private PasswordField rePasswordField;

    @FXML
    private TextField suffixField;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField idField, positionField;

    @FXML
    void newPositionButton(){
        positionChoiceBox.setDisable(false);
    }

    @FXML
    void changePasswordButton(){
        passwordField.setDisable(false);
        rePasswordField.setDisable(false);
    }

    @FXML
    void updateButton() {
        String newPassword = passwordField.getText();
        String rePassword = rePasswordField.getText();
        String newPosition = positionChoiceBox.getValue();
        String currentPosition = positionField.getText();
        int staffId = Integer.parseInt(idField.getText());

        boolean passwordChanged = !passwordField.isDisabled();
        boolean positionChanged = !positionChoiceBox.isDisabled();

        if (!passwordChanged && !positionChanged) {
            showAlert("No changes were made.", Alert.AlertType.INFORMATION);
            return;
        }

        try (Connection conn = SQLConnection.getConnection()) {
            if (passwordChanged) {
                if (newPassword.isEmpty() || rePassword.isEmpty()) {
                    showAlert("Password fields cannot be empty.", Alert.AlertType.ERROR);
                    return;
                }
                if (!newPassword.equals(rePassword)) {
                    showAlert("Passwords do not match.", Alert.AlertType.ERROR);
                    return;
                }

                String updatePasswordSql = "UPDATE accounts SET password = ? WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updatePasswordSql)) {
                    stmt.setString(1, newPassword);
                    stmt.setInt(2, staffId);
                    stmt.executeUpdate();
                }

                logTransaction(conn, "Change Password");
            }

            if (positionChanged) {
                if (newPosition == null || newPosition.trim().isEmpty()) {
                    showAlert("Please select a valid position.", Alert.AlertType.ERROR);
                    return;
                }

                if (newPosition.equals(currentPosition)) {
                    showAlert("The selected position is already assigned to this staff.", Alert.AlertType.ERROR);
                    return;
                }

                String updatePositionSql = "UPDATE accounts SET position = ? WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updatePositionSql)) {
                    stmt.setString(1, newPosition);
                    stmt.setInt(2, staffId);
                    stmt.executeUpdate();
                }

                logTransaction(conn, "Change Position");
            }

            showAlert("Staff updated successfully.", Alert.AlertType.INFORMATION);
            cancelButton();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error while updating staff.", Alert.AlertType.ERROR);
        }
    }

    private void logTransaction(Connection conn, String type) throws SQLException {
        String transQuery = """
        INSERT INTO transactions (id, type, name, supplier_name, brand, series, qty, price, transaction_date)
        VALUES (?, ?, ?, '', '', '', 0, 0.0, CURRENT_DATE)
    """;
        try (PreparedStatement trans = conn.prepareStatement(transQuery)) {
            trans.setInt(1, transactionID());
            trans.setString(2, type);
            trans.setString(3, loggedInStaff.getStaffName());
            trans.executeUpdate();
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

    public void setLoggedInStaff(LoggedInStaff staff){
        this.loggedInStaff = staff;
    }


    public void setStaffData(Staff staff) {
        contactField.setText(staff.getContact());
        addressField.setText(staff.getAddress());
        positionField.setText(staff.getPosition());

        try (Connection conn = SQLConnection.getConnection()) {
            String query = """
            SELECT s.id, s.first_name, s.middle_name, s.last_name, s.suffix,
                   s.birthdate, s.age, s.gender,
                   a.username
            FROM staff s
            JOIN accounts a ON a.id = s.id
            WHERE s.id = ?
        """;

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, staff.getId());

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    idField.setText(String.valueOf(rs.getInt("id")));
                    firstNameField.setText(rs.getString("first_name"));
                    middleNameField.setText(rs.getString("middle_name"));
                    lastNameField.setText(rs.getString("last_name"));
                    suffixField.setText(rs.getString("suffix"));

                    birthdateField.setText(rs.getString("birthdate"));
                    ageField.setText(String.valueOf(rs.getInt("age")));
                    genderField.setText(rs.getString("gender"));
                    usernameField.setText(rs.getString("username"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }


        passwordField.clear();
        rePasswordField.clear();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        this.stage.setOnCloseRequest(e -> {
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
        });
    }

    @FXML
    void cancelButton() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
        stage.close();
    }

    private static void showAlert(String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle("Notification");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


}
