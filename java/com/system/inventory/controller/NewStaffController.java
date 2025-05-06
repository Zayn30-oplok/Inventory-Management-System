package com.system.inventory.controller;

import com.system.inventory.database.SQLConnection;
import com.system.inventory.model.LoggedInStaff;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
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

public class NewStaffController implements Initializable {

    private LoggedInStaff loggedInStaff;
    private DashboardController dashboardController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //ChoiceBoxes
        positionChoiceBox.setItems(FXCollections.observableArrayList(
                "Admin",
                "Manager",
                "Co-Manager",
                "Staff"
        ));

        genderChoiceBox.setItems(FXCollections.observableArrayList(
                "M",
                "F"
        ));

        suffixChoiceBox.setItems(FXCollections.observableArrayList(
                "Jr.",
                "Sr.",
                "I",
                "II",
                "III",
                "IV"
        ));
    }





    @FXML
    private TextField lastNameField;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField middleNameField;
    @FXML
    private TextField ageField;
    @FXML
    private TextField contactField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField birthdateField;

    @FXML
    private ChoiceBox<String> genderChoiceBox;
    @FXML
    private ChoiceBox<String> suffixChoiceBox;
    @FXML
    private ChoiceBox<String> positionChoiceBox;

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField rePasswordField;

    @FXML
    void cancelButton() {
        Stage stage = (Stage) passwordField.getScene().getWindow();
        stage.close();
    }

    @FXML
    void createButton() {

        String fName = firstNameField.getText();
        String mName = middleNameField.getText();
        String lName = lastNameField.getText();
        String age = ageField.getText();
        String contact = contactField.getText();
        String address = addressField.getText();
        String birth = birthdateField.getText();

        String username = usernameField.getText();
        String pass = passwordField.getText();
        String pass2 = rePasswordField.getText();

        String suffix = suffixChoiceBox.getValue();
        String gender = genderChoiceBox.getValue();
        String position = positionChoiceBox.getValue();
        // Check if any of the required fields are empty
        if (fName.isEmpty() || lName.isEmpty() || contact.isEmpty() || address.isEmpty()
                || birth.isEmpty() || age.isEmpty()) {
            showError("Please fill all the required fields of staff information.");
        } else if (position == null || position.isEmpty()) {
            showError("Please select position.");
        } else if (username.isEmpty() || pass.isEmpty() || pass2.isEmpty()) {
            showError("Please fill all the required fields of user account.");
        } else if (!pass.equals(pass2)) {
            showError("Passwords do not match.");
        } else {

            int ageVal;
            try {
                ageVal = Integer.parseInt(age);
                if (ageVal < 18 || ageVal > 100) {
                    showError("Age must be between 18 and 100.");
                    return;
                }
            } catch (NumberFormatException e) {
                showError("Age must be a valid number.");
                return;
            }

            if (!contact.matches("\\d{10}")) {
                showError("Contact number must be a valid 10-digit number.");
                return;
            }

            try (Connection conn = SQLConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM accounts WHERE username = ?")) {

                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();

                if (rs.next() && rs.getInt(1) > 0) {
                    showError("Username already exists. Please choose another one.");
                    return;
                } else {

                    int id = accountID();

                    String accountSQL = "INSERT INTO accounts (id, position, username, password) VALUES (?, ?, ?, ?)";
                    String staffSQL = "INSERT INTO staff (id, last_name, first_name, middle_name, suffix, age, gender, birthdate, contact, address)" +
                            " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                    try (PreparedStatement ps = conn.prepareStatement(accountSQL)) {
                        ps.setInt(1, id);
                        ps.setString(2, position);
                        ps.setString(3, username);
                        ps.setString(4, pass);
                        ps.executeUpdate();

                    }

                    try (PreparedStatement ps = conn.prepareStatement(staffSQL)) {
                        ps.setInt(1, id);
                        ps.setString(2, lName);
                        ps.setString(3, fName);
                        ps.setString(4, mName);
                        ps.setString(5, suffix);
                        ps.setInt(6, ageVal);
                        ps.setString(7, gender);
                        ps.setString(8, birth);
                        ps.setString(9, contact);
                        ps.setString(10, address);
                        ps.executeUpdate();
                    }

                    String transQuery = """
                                                INSERT INTO transactions (id, type, name, supplier_name, brand, series, qty, price, transaction_date)
                                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)
                                                """;
                    try(PreparedStatement trans = conn.prepareStatement(transQuery)){
                        trans.setInt(1, transactionID());
                        trans.setString(2, "New Staff");
                        trans.setString(3, loggedInStaff.getStaffName());
                        trans.setString(4, lName + ", " + fName);
                        trans.setString(5, " ");
                        trans.setString(6, " ");
                        trans.setInt(7, 0);
                        trans.setDouble(8, 0.0);
                        trans.executeUpdate();
                    }

                    if (dashboardController != null){
                        dashboardController.updateLabels();
                        dashboardController.refreshButton();
                    }

                    showAlert("New staff added successfully.", Alert.AlertType.INFORMATION);
                    Stage stage = (Stage) passwordField.getScene().getWindow();
                    stage.close();
                }

            } catch (SQLException e) {
                e.printStackTrace();
                showError("Database error: " + e.getMessage());
            }
        }
    }


    private int accountID() {
        int randomNum = (int) (Math.random() * 1_000_0000);

        String formattedNumber = String.format("%06d", randomNum);

        return Integer.parseInt("08" + formattedNumber);
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


    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setLoggedInStaff(LoggedInStaff staff) {

        this.loggedInStaff = staff;
    }

    public void setDashboardController (DashboardController dashboardController){
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
