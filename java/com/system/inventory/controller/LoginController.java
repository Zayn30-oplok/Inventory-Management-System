package com.system.inventory.controller;

import com.system.inventory.database.SQLConnection;
import com.system.inventory.main.Main;
import com.system.inventory.model.LoggedInStaff;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Parent;

import java.io.IOException;
import java.sql.*;

public class LoginController {

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField userNameField;

    private LoggedInStaff loggedInStaff;


    @FXML
    void signInButton() {
        String username = userNameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Username or password must not be empty!", Alert.AlertType.ERROR);
            return;
        }

        try (Connection conn = SQLConnection.getConnection()) {
            String loginQuery = "SELECT a.id, a.position AS position, " +
                    "s.first_name AS name " +
                    "FROM accounts a " +
                    "JOIN staff s ON a.id = s.id " +
                    "WHERE a.username = ? AND a.password = ?";

            try (PreparedStatement stmt = conn.prepareStatement(loginQuery)) {
                stmt.setString(1, username);
                stmt.setString(2, password);

                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String position = rs.getString("position");
                    String staffName = rs.getString("name");

                    loggedInStaff = new LoggedInStaff(staffName, position);

                    openDashboard();
                    close();
                } else {
                    showAlert("Incorrect username or password!", Alert.AlertType.ERROR);
                }

            } catch (SQLException | IOException e) {
                e.printStackTrace();
                showAlert("An error occurred while trying to log in. Please try again later.", Alert.AlertType.ERROR);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database error: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }


    @FXML
    void forgotButton(){
        showAlert("Contact the administration", Alert.AlertType.ERROR);
    }


    private static void showAlert(String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle("Notification");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void openDashboard() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/com/system/inventory/scenes/dashboard.fxml"));

        Parent root = fxmlLoader.load();
        DashboardController controller = fxmlLoader.getController();

        controller.setLoggedInStaff(loggedInStaff);

        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setResizable(false);
        stage.setTitle("Inventory Management");
        stage.setScene(scene);
        stage.show();
    }

    private void close(){
        Stage stage = (Stage) userNameField.getScene().getWindow();
        stage.close();
    }
}
