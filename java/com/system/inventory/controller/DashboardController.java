package com.system.inventory.controller;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.system.inventory.database.SQLConnection;
import com.system.inventory.main.Main;
import com.system.inventory.model.*;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.paint.Color;


import java.awt.*;
import java.io.*;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class DashboardController implements Initializable {

    Stage stage = new Stage();
    private ObservableList<Supplier> suppliers = FXCollections.observableArrayList();
    private ObservableList<Staff> staffs = FXCollections.observableArrayList();
    private ObservableList<Stocks> stocks = FXCollections.observableArrayList();
    private ObservableList<Transactions> transactions = FXCollections.observableArrayList();
    private ObservableList<Report> reports = FXCollections.observableArrayList();

    private boolean isEditSupplierOpen = false;
    private boolean isEditStaffOpen = false;
    private boolean isEditWindowOpen = false;
    private boolean isEditStockOpen = false;

    private LoggedInStaff loggedInStaff;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        applyActiveStyle(dashboard, dashb);
        setupHoverEffect(dashboard, dashb);
        setupHoverEffect(supplier, supB);
        setupHoverEffect(staff, staffB);
        setupHoverEffect(cubes, stockB);
        setupHoverEffect(transaction, transB);
        setupHoverEffect(bar, reportB);

        Platform.runLater(() -> {
            if (loggedInStaff != null) {
                staffName.setText(loggedInStaff.getStaffName());
                staffPosition.setText(loggedInStaff.getPosition());
            }
        });

        //Supplier
        supNoClm.setCellValueFactory(new PropertyValueFactory<>("id"));
        supNameClm.setCellValueFactory(new PropertyValueFactory<>("name"));
        supBrandsClm.setCellValueFactory(new PropertyValueFactory<>("brands"));
        supContactClm.setCellValueFactory(new PropertyValueFactory<>("contact"));
        supActionsClm.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button();
            private final Button deleteButton = new Button();
            private final HBox hBox = new HBox(10);

            {
                ImageView editImageView = new ImageView(new Image(getClass().getResourceAsStream("/com/system/inventory/images/edit.png")));
                editImageView.setFitWidth(15);
                editImageView.setFitHeight(15);
                editButton.setGraphic(editImageView);
                editButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

                editButton.setOnAction(event -> {
                    Supplier selectedSupplier = getTableView().getItems().get(getIndex());
                    try {
                        editSupplier(selectedSupplier);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                ImageView deleteImageView = new ImageView(new Image(getClass().getResourceAsStream("/com/system/inventory/images/delete.png")));
                deleteImageView.setFitWidth(15);
                deleteImageView.setFitHeight(15);
                deleteButton.setGraphic(deleteImageView);
                deleteButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

                deleteButton.setOnAction(event -> {
                    Supplier supplier = getTableView().getItems().get(getIndex());

                    Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmation.setTitle("Delete Confirmation");
                    confirmation.setHeaderText(null);
                    confirmation.setContentText("Are you sure you want to delete this supplier? " +
                            "\n" +
                            "\n" + "ID: " + supplier.getId() +
                            "\n" + "Name: " + supplier.getName() +
                            "\n" + "Brands: " + supplier.getBrands());

                    confirmation.showAndWait().ifPresent(response -> {

                        if (loggedInStaff.getPosition().equals("Staff")) {
                            showAlert("You don't have permission to delete!", Alert.AlertType.ERROR);
                            return;
                        } else {
                            if (response == ButtonType.OK) {

                                String deleteSupplierSql = "DELETE FROM supplier WHERE id = ?";
                                String deleteSupplierBrandsSql = "DELETE FROM supplier_brands WHERE supplier_id = ?";
                                String checkForOrphanedBrandsSql = "SELECT id FROM brands WHERE id NOT IN (SELECT brand_id FROM supplier_brands)";

                                try (Connection conn = SQLConnection.getConnection();
                                     PreparedStatement deleteSupplierPs = conn.prepareStatement(deleteSupplierSql);
                                     PreparedStatement deleteSupplierBrandsPs = conn.prepareStatement(deleteSupplierBrandsSql);
                                     PreparedStatement checkForOrphanedBrandsPs = conn.prepareStatement(checkForOrphanedBrandsSql)) {

                                    deleteSupplierBrandsPs.setInt(1, supplier.getId());
                                    deleteSupplierBrandsPs.executeUpdate();

                                    deleteSupplierPs.setInt(1, supplier.getId());
                                    deleteSupplierPs.executeUpdate();

                                    ResultSet orphanedBrands = checkForOrphanedBrandsPs.executeQuery();
                                    while (orphanedBrands.next()) {
                                        int orphanedBrandId = orphanedBrands.getInt("id");

                                        String deleteBrandSql = "DELETE FROM brands WHERE id = ?";
                                        try (PreparedStatement deleteBrandPs = conn.prepareStatement(deleteBrandSql)) {
                                            deleteBrandPs.setInt(1, orphanedBrandId);
                                            deleteBrandPs.executeUpdate();
                                        }

                                        String deleteSeriesSql = "DELETE FROM series WHERE id NOT IN (SELECT series FROM brands)";
                                        try (PreparedStatement deleteSeriesPs = conn.prepareStatement(deleteSeriesSql)) {
                                            deleteSeriesPs.executeUpdate();
                                        }

                                        String transQuery = """
                                                INSERT INTO transactions (id, type, name, supplier_name, brand, series, qty, price, transaction_date)
                                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)
                                                """;
                                        try (PreparedStatement trans = conn.prepareStatement(transQuery)) {
                                            trans.setInt(1, transactionID());
                                            trans.setString(2, "Remove Supplier");
                                            trans.setString(3, loggedInStaff.getStaffName());
                                            trans.setString(4, supplier.getName());
                                            trans.setString(5, " ");
                                            trans.setString(6, " ");
                                            trans.setInt(7, 0);
                                            trans.setDouble(8, 0.0);
                                            trans.executeUpdate();
                                        }

                                        refreshButton();
                                    }

                                    updateLabels();
                                    suppliers.remove(supplier);
                                    supplierTableView.refresh();

                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    Alert error = new Alert(Alert.AlertType.ERROR);
                                    error.setTitle("Error");
                                    error.setHeaderText(null);
                                    error.setContentText("Failed to delete supplier and related data.");
                                    error.showAndWait();
                                }
                            }
                        }
                    });
                });


                hBox.setAlignment(Pos.CENTER);
                hBox.getChildren().addAll(editButton, deleteButton);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(hBox);
                }
            }
        });

        searchSupplierField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && searchSupplierField.getText().trim().isEmpty()) {
                loadSuppliers();
            }
        });

        supplierTableView.setItems(suppliers);
        loadSuppliers();

        //Staff
        staffNoClm.setCellValueFactory(new PropertyValueFactory<>("id"));
        staffNameClm.setCellValueFactory(new PropertyValueFactory<>("name"));
        staffPositionClm.setCellValueFactory(new PropertyValueFactory<>("position"));
        staffContactClm.setCellValueFactory(new PropertyValueFactory<>("contact"));
        staffAddressClm.setCellValueFactory(new PropertyValueFactory<>("address"));
        staffActionClm.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button();
            private final Button deleteButton = new Button();
            private final HBox hBox = new HBox(10);

            {
                ImageView editImageView = new ImageView(new Image(getClass().getResourceAsStream("/com/system/inventory/images/edit.png")));
                editImageView.setFitWidth(15);
                editImageView.setFitHeight(15);
                editButton.setGraphic(editImageView);
                editButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

                editButton.setOnAction(event -> {

                    Staff selectedStaff = getTableView().getItems().get(getIndex());
                    try {
                        editStaff(selectedStaff);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                ImageView deleteImageView = new ImageView(new Image(getClass().getResourceAsStream("/com/system/inventory/images/delete.png")));
                deleteImageView.setFitWidth(15);
                deleteImageView.setFitHeight(15);
                deleteButton.setGraphic(deleteImageView);
                deleteButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");


                deleteButton.setOnAction(event -> {
                    Staff staff = getTableView().getItems().get(getIndex());

                    Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmation.setTitle("Delete Confirmation");
                    confirmation.setHeaderText(null);
                    confirmation.setContentText("Are you sure you want to remove this staff? " +
                            "\n" +
                            "\n" + "ID: " + staff.getId() +
                            "\n" + "Name: " + staff.getName());
                    confirmation.showAndWait().ifPresent(response -> {

                        if (loggedInStaff.getPosition().equals("Staff")) {
                            showAlert("You don't have permission to delete!", Alert.AlertType.ERROR);
                            return;
                        } else {
                            if (response == ButtonType.OK) {
                                try (Connection conn = SQLConnection.getConnection()) {
                                    String deleteQuery = "DELETE FROM accounts WHERE id = ?";

                                    try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
                                        stmt.setInt(1, staff.getId());

                                        int rowsAffected = stmt.executeUpdate();
                                        if (rowsAffected > 0) {
                                            getTableView().getItems().remove(staff);
                                            showAlert("Staff and account deleted successfully.", Alert.AlertType.INFORMATION);

                                        }
                                    }

                                    String transQuery = """
                                            INSERT INTO transactions (id, type, name, supplier_name, brand, series, qty, price, transaction_date)
                                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)
                                            """;
                                    try (PreparedStatement trans = conn.prepareStatement(transQuery)) {
                                        trans.setInt(1, transactionID());
                                        trans.setString(2, "Remove Staff");
                                        trans.setString(3, loggedInStaff.getStaffName());
                                        trans.setString(4, staff.getName());
                                        trans.setString(5, " ");
                                        trans.setString(6, " ");
                                        trans.setInt(7, 0);
                                        trans.setDouble(8, 0.0);
                                        trans.executeUpdate();
                                    }

                                    updateLabels();
                                    refreshButton();

                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });

                });


                hBox.setAlignment(Pos.CENTER);
                hBox.getChildren().addAll(editButton, deleteButton);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(hBox);
                }
            }
        });

        staffTableView.setItems(staffs);
        loadStaffs();

        //Stocks
        stockNoClm.setCellValueFactory(new PropertyValueFactory<>("id"));
        stockBrandClm.setCellValueFactory(new PropertyValueFactory<>("brand"));
        stockSeriesClm.setCellValueFactory(new PropertyValueFactory<>("series"));
        stockForClm.setCellValueFactory(new PropertyValueFactory<>("com"));
        stockSizesClm.setCellValueFactory(new PropertyValueFactory<>("size"));
        stockQtyClm.setCellValueFactory(new PropertyValueFactory<>("qty"));
        stockPriceClm.setCellValueFactory(new PropertyValueFactory<>("price"));
        stockStatusClm.setCellValueFactory(new PropertyValueFactory<>("status"));
        stockSupplierClm.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        stockActionsClm.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button();
            private final Button deleteButton = new Button();
            private final HBox hBox = new HBox(10);

            {
                ImageView editImageView = new ImageView(new Image(getClass().getResourceAsStream("/com/system/inventory/images/edit.png")));
                editImageView.setFitWidth(15);
                editImageView.setFitHeight(15);
                editButton.setGraphic(editImageView);
                editButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

                editButton.setOnAction(event -> {

                    Stocks selectedStocks = getTableView().getItems().get(getIndex());
                    try {
                        editStock(selectedStocks);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                ;

                ImageView deleteImageView = new ImageView(new Image(getClass().getResourceAsStream("/com/system/inventory/images/delete.png")));
                deleteImageView.setFitWidth(15);
                deleteImageView.setFitHeight(15);
                deleteButton.setGraphic(deleteImageView);
                deleteButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");


                deleteButton.setOnAction(event -> {
                    Stocks stocks1 = getTableView().getItems().get(getIndex());

                    Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmation.setTitle("Delete Confirmation");
                    confirmation.setHeaderText(null);
                    confirmation.setContentText("Are you sure you want to remove this stock? " +
                            "\n" +
                            "\n" + "ID: " + stocks1.getId() +
                            "\n" + "Brand: " + stocks1.getBrand() +
                            "\n" + "Series: " + stocks1.getSeries() +
                            "\n" + "Category: " + stocks1.getCom());
                    confirmation.showAndWait().ifPresent(response -> {
                        if (loggedInStaff.getPosition().equals("Staff")) {
                            showAlert("You don't have permission to delete!", Alert.AlertType.ERROR);
                            return;
                        } else {
                            if (response == ButtonType.OK) {
                                try (Connection conn = SQLConnection.getConnection()) {
                                    String deleteQuery = "DELETE FROM stocks WHERE id = ?";

                                    try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
                                        stmt.setInt(1, stocks1.getId());

                                        int rowsAffected = stmt.executeUpdate();
                                        if (rowsAffected > 0) {
                                            getTableView().getItems().remove(stocks1);
                                            showAlert("Stock deleted successfully.", Alert.AlertType.INFORMATION);
                                            stocks.remove(stocks1);
                                            stocksTableView.refresh();
                                            populateBrandStockChart();
                                        }
                                    }

                                    String transQuery = """
                                            INSERT INTO transactions (id, type, name, supplier_name, brand, series, qty, price, transaction_date)
                                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)
                                            """;
                                    try (PreparedStatement trans = conn.prepareStatement(transQuery)) {
                                        trans.setInt(1, transactionID());
                                        trans.setString(2, "Remove Stock");
                                        trans.setString(3, loggedInStaff.getStaffName());
                                        trans.setString(4, " ");
                                        trans.setString(5, stocks1.getBrand());
                                        trans.setString(6, stocks1.getSeries());
                                        trans.setInt(7, stocks1.getQty());
                                        trans.setDouble(8, stocks1.getPrice());
                                        trans.executeUpdate();
                                    }

                                    refreshButton();

                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });


                });


                hBox.setAlignment(Pos.CENTER);
                hBox.getChildren().addAll(editButton, deleteButton);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(hBox);
                }
            }
        });

        stocksTableView.setItems(stocks);
        loadStocks();

        //Transaction
        transNoClm.setCellValueFactory(new PropertyValueFactory<>("id"));
        typeClm.setCellValueFactory(new PropertyValueFactory<>("type"));
        transNameClm.setCellValueFactory(new PropertyValueFactory<>("name"));
        transSupNameClm.setCellValueFactory(new PropertyValueFactory<>("sup"));
        transBrandClm.setCellValueFactory(new PropertyValueFactory<>("brand"));
        transSeriesClm.setCellValueFactory(new PropertyValueFactory<>("series"));
        transQtyClm.setCellValueFactory(new PropertyValueFactory<>("qty"));
        transPriceClm.setCellValueFactory(new PropertyValueFactory<>("price"));
        transDateClm.setCellValueFactory(new PropertyValueFactory<>("date"));
        transactionTableView.setItems(transactions);
        loadTransactions();


        //Report
        reportClm.setCellValueFactory(new PropertyValueFactory<>("name"));
        reportBrandClm.setCellValueFactory(new PropertyValueFactory<>("brand"));
        reportSeriesClm.setCellValueFactory(new PropertyValueFactory<>("series"));
        reportTotalClm.setCellValueFactory(new PropertyValueFactory<>("total"));
        reportStockQtyClm.setCellValueFactory(new PropertyValueFactory<>("qty"));
        reportPriceClm.setCellValueFactory(new PropertyValueFactory<>("price"));
        reportTableView.setItems(reports);

        updateLabels();
        populateBrandStockChart();
        populateMonthlyStockChart();


    }

    public void setLoggedInStaff(LoggedInStaff loggedInStaff) {
        this.loggedInStaff = loggedInStaff;
    }

    private void loadSuppliers() {
        suppliers.clear();

        String sql = """
                    SELECT s.id, s.name, s.contact,
                           GROUP_CONCAT(b.name SEPARATOR ', ') AS brands
                    FROM supplier s
                    JOIN supplier_brands sb ON s.id = sb.supplier_id
                    JOIN brands b ON sb.brand_id = b.id
                    GROUP BY s.id, s.name, s.contact
                """;

        try (Connection conn = SQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String contact = rs.getString("contact");
                String brands = rs.getString("brands");

                suppliers.add(new Supplier(id, name, brands, contact));
            }

            supplierTableView.refresh();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadStaffs() {
        staffs.clear();

        String sql = """
                SELECT s.id,
                       CONCAT(s.last_name, ', ', s.first_name, 
                              CASE WHEN s.suffix IS NOT NULL THEN CONCAT(' ', s.suffix) ELSE '' END, 
                              ', ', s.middle_name) AS name,
                       s.contact,
                       s.address,
                       a.position
                FROM staff s
                JOIN accounts a ON s.id = a.id
                """;

        try (Connection conn = SQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String fullName = rs.getString("name");
                String contact = rs.getString("contact");
                String address = rs.getString("address");
                String position = rs.getString("position");

                staffs.add(new Staff(id, fullName, position, contact, address));
            }

            staffTableView.setItems(staffs);
            staffTableView.refresh();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        highCheckBox.setOnAction(e -> handleStockFilter("High Level", highCheckBox));
        mediumCheckBox.setOnAction(e -> handleStockFilter("Medium Level", mediumCheckBox));
        lowCheckBox.setOnAction(e -> handleStockFilter("Low Level", lowCheckBox));
        outCheckBox.setOnAction(e -> handleStockFilter("Out of Stocks", outCheckBox));


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

    public void updateLabels() {
        try (Connection conn = SQLConnection.getConnection()) {
            String stockCount = "SELECT COUNT(*) FROM stocks";
            PreparedStatement stockStmt = conn.prepareStatement(stockCount);
            ResultSet rs = stockStmt.executeQuery();

            if (rs.next()) {
                stocksLabel.setText(String.valueOf(rs.getInt(1)));
            }

            String staffCount = "SELECT COUNT(*) FROM staff";
            PreparedStatement staffStmt = conn.prepareStatement(staffCount);
            ResultSet sRs = staffStmt.executeQuery();

            if (sRs.next()) {
                staffCLabel.setText(String.valueOf(sRs.getInt(1)));
            }

            String supplierCount = "SELECT COUNT(*) FROM supplier";
            PreparedStatement supplierStmt = conn.prepareStatement(supplierCount);
            ResultSet sPs = supplierStmt.executeQuery();

            if (sPs.next()) {
                supplierLabel.setText(String.valueOf(sPs.getInt(1)));
            }

            String brandCount = "SELECT COUNT(*)  FROM brands";
            PreparedStatement brandStmt = conn.prepareStatement(brandCount);
            ResultSet sBs = brandStmt.executeQuery();

            if (sBs.next()) {
                brandLabel.setText(String.valueOf(sBs.getInt(1)));
            }

            String outStocks = "SELECT COUNT(*) FROM stocks WHERE status = 'Out of Stocks'";
            PreparedStatement outStmt = conn.prepareStatement(outStocks);
            ResultSet oS = outStmt.executeQuery();

            if (oS.next()) {
                outOfStocksLabel.setText(String.valueOf(oS.getInt(1)));
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    //Scenes
    @FXML
    private AnchorPane dashboardPane;
    @FXML
    private AnchorPane supplierPane;
    @FXML
    private AnchorPane staffPane;
    @FXML
    private AnchorPane stocksPane;
    @FXML
    private AnchorPane transactionPane;
    @FXML
    private AnchorPane reportPane;

    //Changing Label
    @FXML
    private Label sceneLabel;
    @FXML
    private Label staffName, staffLabel, staffPosition;
    @FXML
    private Label supplierLabel, stocksLabel, staffCLabel, outOfStocksLabel, brandLabel;

    @FXML
    private BarChart<String, Number> barChart;
    @FXML
    private BarChart<String, Number> monthlyBarChart;
    @FXML
    private CategoryAxis weeklyAxis;
    @FXML
    private NumberAxis qtyAxis;

    @FXML
    private FontAwesomeIconView dashboard, supplier, staff, cubes, transaction, bar;


    public void populateBrandStockChart() {
        barChart.getData().clear();

        XYChart.Series<String, Number> stockInSeries = new XYChart.Series<>();
        stockInSeries.setName("Stock In");

        XYChart.Series<String, Number> stockOutSeries = new XYChart.Series<>();
        stockOutSeries.setName("Stock Out");

        LocalDate thisMonday = LocalDate.now().with(DayOfWeek.MONDAY);

        String sql = """
        SELECT brand, type, transaction_date, SUM(qty) AS total_qty
        FROM transactions
        WHERE type IN ('Stock In', 'Stock out')
        GROUP BY brand, type, YEARWEEK(transaction_date, 1)
        ORDER BY transaction_date
    """;

        try (Connection conn = SQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy MMM d");
            Map<String, Map<String, Integer>> brandWeekMap = new LinkedHashMap<>();
            Map<String, String> labelToBrandMap = new HashMap<>();

            while (rs.next()) {
                String brand = rs.getString("brand");
                String type = rs.getString("type");
                int qty = rs.getInt("total_qty");

                LocalDate txDate = rs.getDate("transaction_date").toLocalDate();
                LocalDate weekStart = txDate.with(DayOfWeek.MONDAY);
                LocalDate weekEnd = weekStart.plusDays(6);

                if (weekEnd.isBefore(thisMonday)) continue;

                String label = brand + "\n" + weekStart.format(formatter) + " - " + weekEnd.format(formatter);
                labelToBrandMap.put(label, brand);

                brandWeekMap
                        .computeIfAbsent(label, k -> new HashMap<>())
                        .put(type, qty);
            }

            for (Map.Entry<String, Map<String, Integer>> entry : brandWeekMap.entrySet()) {
                String label = entry.getKey();
                String brand = labelToBrandMap.get(label);
                Map<String, Integer> typeMap = entry.getValue();

                int inQty = typeMap.getOrDefault("Stock In", 0);
                int outQty = typeMap.getOrDefault("Stock out", 0);

                if (inQty > 0) {
                    XYChart.Data<String, Number> data = new XYChart.Data<>(label, inQty);
                    data.setExtraValue(brand);
                    stockInSeries.getData().add(data);
                }
                if (outQty > 0) {
                    XYChart.Data<String, Number> data = new XYChart.Data<>(label, outQty);
                    data.setExtraValue(brand);
                    stockOutSeries.getData().add(data);
                }
            }

            barChart.getData().addAll(stockInSeries, stockOutSeries);

            Platform.runLater(() -> {
                applyBarStyleAndTooltip(stockInSeries, "#101d68", "Stock In");
                applyBarStyleAndTooltip(stockOutSeries, "#00b4d8", "Stock Out");

                animateBarGrowth(stockInSeries);
                animateBarGrowth(stockOutSeries);

                Platform.runLater(this::updateLegendColors);
            });

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void applyBarStyleAndTooltip(XYChart.Series<String, Number> series, String color, String label) {
        for (XYChart.Data<String, Number> data : series.getData()) {
            Node node = data.getNode();
            if (node != null) {
                node.setStyle("-fx-bar-fill: " + color + ";");

                String brand = (String) data.getExtraValue();
                String fullLabel = data.getXValue();
                String[] parts = fullLabel.split("\n", 2);
                String dateRange = (parts.length == 2) ? parts[1] : "";

                String qty = String.valueOf(data.getYValue());

                Tooltip tooltip = new Tooltip("Brand: " + brand + "\nQty: " + qty + "\nDate: " + dateRange);
                tooltip.setShowDelay(Duration.ZERO);
                tooltip.setHideDelay(Duration.ZERO);
                tooltip.setShowDuration(Duration.INDEFINITE);
                Tooltip.install(node, tooltip);

                node.setOnMouseEntered(e -> {
                    FadeTransition ft = new FadeTransition(Duration.millis(200), node);
                    ft.setFromValue(1.0);
                    ft.setToValue(0.7);
                    ft.play();
                });

                node.setOnMouseExited(e -> {
                    FadeTransition ft = new FadeTransition(Duration.millis(200), node);
                    ft.setFromValue(0.7);
                    ft.setToValue(1.0);
                    ft.play();
                });
            }
        }
    }

    private void updateLegendColors() {
        for (Node legend : barChart.lookupAll(".chart-legend-item")) {
            Label label = (Label) legend.lookup(".label");
            if (label != null) {
                Node symbol = legend.lookup(".chart-legend-item-symbol");
                if ("Stock In".equals(label.getText()) && symbol != null)
                    symbol.setStyle("-fx-background-color: #101d68;");
                else if ("Stock Out".equals(label.getText()) && symbol != null)
                    symbol.setStyle("-fx-background-color: #00b4d8;");
            }
        }

        for (Node legend : monthlyBarChart.lookupAll(".chart-legend-item")) {
            Label label = (Label) legend.lookup(".label");
            if (label != null) {
                Node symbol = legend.lookup(".chart-legend-item-symbol");
                if ("Stock In".equals(label.getText()) && symbol != null)
                    symbol.setStyle("-fx-background-color: #101d68;");
                else if ("Stock Out".equals(label.getText()) && symbol != null)
                    symbol.setStyle("-fx-background-color: #00b4d8;");
            }
        }
    }

    private void animateBarGrowth(XYChart.Series<String, Number> series) {
        for (XYChart.Data<String, Number> data : series.getData()) {
            Node node = data.getNode();
            if (node != null) {
                node.setScaleY(0);
                ScaleTransition st = new ScaleTransition(Duration.millis(500), node);
                st.setToY(1);
                st.play();
            }
        }
    }

    public void populateMonthlyStockChart() {
        monthlyBarChart.getData().clear();

        XYChart.Series<String, Number> stockInSeries = new XYChart.Series<>();
        stockInSeries.setName("Stock In");

        XYChart.Series<String, Number> stockOutSeries = new XYChart.Series<>();
        stockOutSeries.setName("Stock Out");

        String sql = """
            SELECT brand, type,
                   DATE_FORMAT(transaction_date, '%Y-%m') AS month,
                   SUM(qty) AS total_qty
            FROM transactions
            WHERE type IN ('Stock In', 'Stock out')
            GROUP BY brand, type, month
            ORDER BY month
        """;

        try (Connection conn = SQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("yyyy MMM");

            YearMonth currentMonth = YearMonth.now();

            while (rs.next()) {
                String brand = rs.getString("brand");
                String type = rs.getString("type");
                int qty = rs.getInt("total_qty");
                String monthStr = rs.getString("month");

                YearMonth resultMonth = YearMonth.parse(monthStr);

                if (resultMonth.equals(currentMonth)) {
                    continue;
                }

                LocalDate monthDate = resultMonth.atDay(1);
                String label = brand + "\n" + monthDate.format(displayFormatter);

                XYChart.Data<String, Number> data = new XYChart.Data<>(label, qty);
                data.setExtraValue(brand);

                if ("Stock In".equalsIgnoreCase(type)) {
                    stockInSeries.getData().add(data);
                } else {
                    stockOutSeries.getData().add(data);
                }
            }

            monthlyBarChart.getData().addAll(stockInSeries, stockOutSeries);

            Platform.runLater(() -> {
                applyBarStyleAndTooltip(stockInSeries, "#101d68", "Stock In");
                applyBarStyleAndTooltip(stockOutSeries, "#00b4d8", "Stock Out");

                updateLegendColors();
                animateBarGrowth(stockInSeries);
                animateBarGrowth(stockOutSeries);
            });

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private Button refreshButton, reportB, transB, stockB, staffB, supB, dashb;

    //Side Nav Button
    @FXML
    void dashboardButton() {
        sceneLabel.setText("DASHBOARD");
        dashboardPane.setVisible(true);
        supplierPane.setVisible(false);
        staffPane.setVisible(false);
        stocksPane.setVisible(false);
        transactionPane.setVisible(false);
        reportPane.setVisible(false);

        applyActiveStyle(dashboard, dashb);
        resetStyle(supplier, supB);
        resetStyle(staff, staffB);
        resetStyle(cubes, stockB);
        resetStyle(transaction, transB);
        resetStyle(bar, reportB);
    }

    @FXML
    void supplierButton() {
        sceneLabel.setText("SUPPLIER");
        dashboardPane.setVisible(false);
        supplierPane.setVisible(true);
        staffPane.setVisible(false);
        stocksPane.setVisible(false);
        transactionPane.setVisible(false);
        reportPane.setVisible(false);

        applyActiveStyle(supplier, supB);
        resetStyle(dashboard, dashb);
        resetStyle(staff, staffB);
        resetStyle(cubes, stockB);
        resetStyle(transaction, transB);
        resetStyle(bar, reportB);
    }

    @FXML
    void staffButton() {
        sceneLabel.setText("STAFF");
        dashboardPane.setVisible(false);
        supplierPane.setVisible(false);
        staffPane.setVisible(true);
        stocksPane.setVisible(false);
        transactionPane.setVisible(false);
        reportPane.setVisible(false);

        applyActiveStyle(staff, staffB);
        resetStyle(dashboard, dashb);
        resetStyle(supplier, supB);
        resetStyle(cubes, stockB);
        resetStyle(transaction, transB);
        resetStyle(bar, reportB);
    }

    @FXML
    void stocksButton() {
        sceneLabel.setText("STOCKS");
        dashboardPane.setVisible(false);
        supplierPane.setVisible(false);
        staffPane.setVisible(false);
        stocksPane.setVisible(true);
        transactionPane.setVisible(false);
        reportPane.setVisible(false);

        applyActiveStyle(cubes, stockB);
        resetStyle(dashboard, dashb);
        resetStyle(supplier, supB);
        resetStyle(staff, staffB);
        resetStyle(transaction, transB);
        resetStyle(bar, reportB);
    }

    @FXML
    void transactionButton() {
        sceneLabel.setText("TRANSACTION");
        dashboardPane.setVisible(false);
        supplierPane.setVisible(false);
        staffPane.setVisible(false);
        stocksPane.setVisible(false);
        transactionPane.setVisible(true);
        reportPane.setVisible(false);

        applyActiveStyle(transaction, transB);
        resetStyle(dashboard, dashb);
        resetStyle(supplier, supB);
        resetStyle(staff, staffB);
        resetStyle(cubes, stockB);
        resetStyle(bar, reportB);
    }

    @FXML
    void reportButton() {
        sceneLabel.setText("REPORT");
        dashboardPane.setVisible(false);
        supplierPane.setVisible(false);
        staffPane.setVisible(false);
        stocksPane.setVisible(false);
        transactionPane.setVisible(false);
        reportPane.setVisible(true);

        applyActiveStyle(bar, reportB);
        resetStyle(dashboard, dashb);
        resetStyle(supplier, supB);
        resetStyle(staff, staffB);
        resetStyle(cubes, stockB);
        resetStyle(transaction, transB);
    }

    private Button activeButton;

    private void applyActiveStyle(FontAwesomeIconView icon, Button button) {
        activeButton = button;

        icon.setGlyphSize(20);
        icon.setFill(Color.WHITE);
        button.setTextFill(Color.WHITE);
        button.setStyle("-fx-background-color: #101d68; -fx-font-family: 'Lexend'; -fx-font-size: 15px;");
        button.setGraphic(icon);
        button.setContentDisplay(ContentDisplay.LEFT);
    }


    private void resetStyle(FontAwesomeIconView icon, Button button) {
        icon.setGlyphSize(15);
        icon.setFill(Color.BLACK);
        button.setTextFill(Color.BLACK);
        button.setStyle("-fx-background-color: transparent; -fx-font-family: 'Lexend'; -fx-font-size: 13px;");
        button.setGraphic(icon);
        button.setContentDisplay(ContentDisplay.LEFT);
    }

    private void setupHoverEffect(FontAwesomeIconView icon, Button button) {
        button.setOnMouseEntered(e -> {
            button.setStyle("-fx-background-color: rgba(0,180,216,0.84); -fx-font-family: 'Lexend'; -fx-font-size: 15px;");
            icon.setGlyphSize(20);
            icon.setFill(Color.WHITE);
            button.setTextFill(Color.WHITE);
        });

        button.setOnMouseExited(e -> {
            if (button == activeButton) {
                button.setStyle("-fx-background-color: #101d68; -fx-font-family: 'Lexend'; -fx-font-size: 15px;");
                icon.setGlyphSize(20);
                icon.setFill(Color.WHITE);
                button.setTextFill(Color.WHITE);
            } else {
                button.setStyle("-fx-background-color: transparent; -fx-font-family: 'Lexend'; -fx-font-size: 13px;");
                icon.setGlyphSize(15);
                icon.setFill(Color.BLACK);
                button.setTextFill(Color.BLACK);
            }
        });
    }




    @FXML
    void signOutButton() throws IOException {

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Sign out Confirmation");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Are you sure you want to sign out?");
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Stage stage = (Stage) staffTableView.getScene().getWindow();
                stage.close();
                try {
                    returnLogin();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void returnLogin() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/com/system/inventory/scenes/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setResizable(false);
        stage.setTitle("Inventory Management");
        stage.setScene(scene);
        stage.show();
    }




    //Supplier

    @FXML
    private TableView <Supplier> supplierTableView;
    @FXML
    private TableColumn <Supplier, Integer> supNoClm;
    @FXML
    private TableColumn <Supplier, String> supNameClm, supBrandsClm, supContactClm;
    @FXML
    private TableColumn <Supplier, Void> supActionsClm;


    @FXML
    private TextField searchSupplierField;

    @FXML
    void newSupplierButton() throws IOException {
        newSupplier();
    }

    @FXML
    void searchSupplierButton() {
        String searchText = searchSupplierField.getText().trim();

        if (searchText.isEmpty()) {
            loadSuppliers();
            return;
        }

        try {
            int supplierId = Integer.parseInt(searchText);

            suppliers.clear();

            String sql = """
            SELECT s.id, s.name, s.contact,
                   GROUP_CONCAT(b.name SEPARATOR ', ') AS brands
            FROM supplier s
            JOIN supplier_brands sb ON s.id = sb.supplier_id
            JOIN brands b ON sb.brand_id = b.id
            WHERE s.id = ?
            GROUP BY s.id, s.name, s.contact
        """;

            try (Connection conn = SQLConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, supplierId);
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String contact = rs.getString("contact");
                    String brands = rs.getString("brands");

                    suppliers.add(new Supplier(id, name, brands, contact));
                }

                supplierTableView.refresh();

            } catch (SQLException e) {
                e.printStackTrace();
            }

        } catch (NumberFormatException ignored) {
        }
    }



    @FXML
    void refreshButton() {
        if (isEditWindowOpen) {
            showAlert("Please close the current Edit window before refreshing.", Alert.AlertType.WARNING);
            return;
        }

        if (supplierPane.isVisible()) {
            loadSuppliers();
            supplierTableView.refresh();
        } else if (staffPane.isVisible()) {
            loadStaffs();
            staffTableView.refresh();
        } else if (stocksPane.isVisible()){
            loadStocks();
            stocksTableView.refresh();
        } else if (transactionPane.isVisible()){
            loadTransactions();
            transactionTableView.refresh();
        }
    }

    private void newSupplier() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/system/inventory/scenes/newSupplier.fxml"));
        Parent root = loader.load();

        NewSupplierController controller = loader.getController();
        controller.setLoggedInStaff(loggedInStaff);
        controller.setDashboardController(this);
        controller.updateBarChart(this);

        Stage stage = new Stage();
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/system/inventory/images/logo.png")));
        stage.setScene(new Scene(root));
        stage.show();
    }

    private void editSupplier(Supplier selectedSupplier) throws IOException {
        if (isEditSupplierOpen || isEditStaffOpen) {
            showAlert("Please close the current edit window before opening a new one.", Alert.AlertType.WARNING);
            return;
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/system/inventory/scenes/EditSupplier.fxml"));
        Parent root = loader.load();

        EditSupplierController controller = loader.getController();
        controller.setSupplier(selectedSupplier);
        controller.setLoggedInStaff(loggedInStaff);

        Stage editStage = new Stage();
        editStage.getIcons().add(new Image(getClass().getResourceAsStream("/com/system/inventory/images/logo.png")));

        editStage.setScene(new Scene(root));
        editStage.setTitle("Edit Supplier");

        isEditSupplierOpen = true;
        editStage.setOnHidden(e -> {
            isEditSupplierOpen = false;
            refreshButton.setDisable(false);
        });

        editStage.show();
    }




    //Staff
    @FXML
    private TableView <Staff> staffTableView;
    @FXML
    private TableColumn <Staff, Integer> staffNoClm;
    @FXML
    private TableColumn <Staff, String> staffNameClm, staffPositionClm, staffContactClm, staffAddressClm;
    @FXML
    private TableColumn <Staff, Void> staffActionClm;
    @FXML
    private TextField staffSearchField;

    @FXML
    void newStaffButton() throws IOException {
        if (loggedInStaff.getPosition().equals("Staff")){
            showAlert("You don't have a permission to add a staff!", Alert.AlertType.ERROR);
        } else {
            newStaff();
        }
    }

    @FXML
    void staffSearchButton() {
        String searchText = staffSearchField.getText().trim();

        if (searchText.isEmpty()) {
            loadStaffs();
            return;
        }

        try {
            int staffId = Integer.parseInt(searchText);

            staffs.clear();

            String sql = """
            SELECT s.id,
                   CONCAT(s.last_name, ', ', s.first_name, ' ', s.suffix, ', ', s.middle_name) AS name,
                   s.contact,
                   s.address,
                   a.position
            FROM staff s
            JOIN accounts a ON s.id = a.id
            WHERE s.id = ?
        """;

            try (Connection conn = SQLConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, staffId);
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String contact = rs.getString("contact");
                    String address = rs.getString("address");
                    String position = rs.getString("position");

                    staffs.add(new Staff(id, name, position, contact, address));
                }

                staffTableView.setItems(staffs);
                staffTableView.refresh();

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Database error occurred while searching.", Alert.AlertType.ERROR);
            }

        } catch (NumberFormatException e) {
            showAlert("Please enter a valid numeric Staff ID.", Alert.AlertType.ERROR);
        }
    }




//    @FXML
//    void refreshStaffButton(){
//        loadSuppliers();
//    }

    private void editStaff(Staff selectedStaff) throws IOException {
        if (isEditSupplierOpen || isEditStaffOpen) {
            showAlert("Please close the current edit window before opening a new one.", Alert.AlertType.WARNING);
            return;
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/system/inventory/scenes/editStaff.fxml"));
        Parent root = loader.load();

        EditStaffController controller = loader.getController();
        controller.setStaffData(selectedStaff);
        controller.setLoggedInStaff(loggedInStaff);

        Stage editStage = new Stage();
        editStage.getIcons().add(new Image(getClass().getResourceAsStream("/com/system/inventory/images/logo.png")));
        editStage.setScene(new Scene(root));
        editStage.setTitle("Edit Staff");

        isEditStaffOpen = true;
        editStage.setOnHidden(e -> isEditStaffOpen = false);

        controller.setStage(editStage);

        editStage.show();
    }

    private void newStaff() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/system/inventory/scenes/newStaff.fxml"));
        Parent root = loader.load();

        NewStaffController controller = loader.getController();
        controller.setLoggedInStaff(loggedInStaff);
        controller.setDashboardController(this);
        Stage stage = new Stage();
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/system/inventory/images/logo.png")));
        stage.setScene(new Scene(root));
        stage.show();
    }

    //Stocks
    @FXML
    private TableView <Stocks> stocksTableView;

    @FXML
    private TableColumn <Stocks, String> stockBrandClm, stockSeriesClm, stockForClm, stockStatusClm, stockSupplierClm;

    @FXML
    private TableColumn <Stocks, Integer> stockNoClm, stockSizesClm, stockQtyClm;

    @FXML
    private TableColumn <Stocks, Double> stockPriceClm;

    @FXML
    private TableColumn <Stocks, Void> stockActionsClm;

    @FXML
    private TextField searchStocksField;
    @FXML
    private CheckBox highCheckBox, lowCheckBox, outCheckBox, mediumCheckBox;

    private void loadStocks(){
        ObservableList <Stocks> stocks = FXCollections.observableArrayList();

        String loadQuery = "SELECT * FROM stocks";

        try(Connection conn = SQLConnection.getConnection()){
            PreparedStatement ps = conn.prepareStatement(loadQuery);
            ResultSet rs = ps.executeQuery();

            while (rs.next())
            stocks.add( new Stocks(
                    rs.getInt("id"),
                    rs.getString("brand"),
                    rs.getString("series"),
                    rs.getString("com"),
                    rs.getInt("sizes"),
                    rs.getInt("qty"),
                    rs.getDouble("price"),
                    rs.getString("status"),
                    rs.getString("supplier")

            ));

            stocksTableView.setItems(stocks);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void newStocksButton() throws IOException {
        newStock();
    }

    @FXML
    void searchStocksButton() {
        String searchText = searchStocksField.getText().trim();

        ObservableList<Stocks> stockList = FXCollections.observableArrayList();

        try (Connection conn = SQLConnection.getConnection()) {
            String searchQuery;

            if (searchText.isEmpty()) {
                searchQuery = "SELECT * FROM stocks";
            } else {
                searchQuery = "SELECT * FROM stocks WHERE id LIKE ? OR com LIKE ? OR brand LIKE ? OR sizes LIKE ?";
            }

            try (PreparedStatement stmt = conn.prepareStatement(searchQuery)) {
                if (!searchText.isEmpty()) {
                    String searchTerm = "%" + searchText + "%";
                    stmt.setString(1, searchTerm);
                    stmt.setString(2, searchTerm);
                    stmt.setString(3, searchTerm);
                    stmt.setString(4, searchTerm);
                }

                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    int stockNo = rs.getInt("id");
                    String stockBrand = rs.getString("brand");
                    String stockSeries = rs.getString("series");
                    String stockFor = rs.getString("com");
                    String stockStatus = rs.getString("status");
                    String stockSupplier = rs.getString("supplier");
                    int stockSizes = rs.getInt("sizes");
                    int stockQty = rs.getInt("qty");
                    double stockPrice = rs.getDouble("price");

                    stockList.add(new Stocks(stockNo,
                            stockBrand,
                            stockSeries,
                            stockFor,
                            stockSizes,
                            stockQty,
                            stockPrice,
                            stockStatus,
                            stockSupplier));
                }

                if (stockList.isEmpty()) {
                    showAlert("No stocks found matching the search criteria.", Alert.AlertType.INFORMATION);
                }

                stocksTableView.setItems(stockList);

            }
        } catch (SQLException e) {
            showAlert("Error occurred while searching for stocks: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleStockFilter(String status, CheckBox sourceCheckBox) {
        if (sourceCheckBox.isSelected()) {
            highCheckBox.setSelected(sourceCheckBox == highCheckBox);
            mediumCheckBox.setSelected(sourceCheckBox == mediumCheckBox);
            lowCheckBox.setSelected(sourceCheckBox == lowCheckBox);
            outCheckBox.setSelected(sourceCheckBox == outCheckBox);

            filterStocksByStatus(status);
        } else {
            loadStocks();
        }
    }

    private void filterStocksByStatus(String status) {
        ObservableList<Stocks> filteredList = FXCollections.observableArrayList();

        String query = "SELECT * FROM stocks WHERE status = ?";
        try (Connection conn = SQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Stocks stock = new Stocks(
                        rs.getInt("id"),
                        rs.getString("brand"),
                        rs.getString("series"),
                        rs.getString("com"),
                        rs.getInt("sizes"),
                        rs.getInt("qty"),
                        rs.getDouble("price"),
                        rs.getString("status"),
                        rs.getString("supplier")
                );
                filteredList.add(stock);
            }

            stocksTableView.setItems(filteredList);

        } catch (SQLException e) {
            showAlert("Failed to filter stocks: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }


    private void editStock(Stocks selectedStock) throws IOException {
        if (isEditSupplierOpen || isEditStaffOpen || isEditStockOpen) {
            showAlert("Please close the current edit window before opening a new one.", Alert.AlertType.WARNING);
            return;
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/system/inventory/scenes/updateStock.fxml"));
        Parent root = loader.load();

        EditStocksController controller = loader.getController();
        controller.setStockData(selectedStock);
        controller.setLoggedInStaff(loggedInStaff);
        controller.updateBarChart(this);
        controller.updateLabels(this);

        Stage editStage = new Stage();
        editStage.getIcons().add(new Image(getClass().getResourceAsStream("/com/system/inventory/images/logo.png")));
        editStage.setScene(new Scene(root));
        editStage.setTitle("Edit Stock");

        editStage.setOnHidden(e -> isEditStockOpen = false);

        controller.setStage(editStage);

        isEditStockOpen = true;
        editStage.show();
    }


    private void newStock() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/system/inventory/scenes/newStocks.fxml"));
        Parent root = loader.load();

        newStocksController controller = loader.getController();
        controller.setLoggedInStaff(loggedInStaff);
        controller.setDashboardController(this);

        Stage stage = new Stage();
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/system/inventory/images/logo.png")));
        stage.setScene(new Scene(root));
        stage.show();
    }

    //Transactions
    @FXML
    private TableView <Transactions> transactionTableView;
    @FXML
    private TableColumn <Transactions, Integer> transNoClm, transQtyClm;
    @FXML
    private TableColumn <Transactions, Double> transPriceClm;
    @FXML
    private TableColumn <Transactions, String> typeClm, transNameClm, transSupNameClm, transBrandClm, transSeriesClm, transDateClm;

    @FXML
    private TextField transSearchField;

    private void loadTransactions(){
        transactions.clear();
        String query = "SELECT * FROM transactions";
        try(Connection conn = SQLConnection.getConnection()){
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet resultSet = stmt.executeQuery();

            while(resultSet.next()){
                int id = resultSet.getInt("id");
                String type = resultSet.getString("type");
                String name = resultSet.getString("name");
                String sup = resultSet.getString("supplier_name");
                String brand = resultSet.getString("brand");
                String series = resultSet.getString("series");
                int qty = resultSet.getInt("qty");
                double price = resultSet.getDouble("price");
                String date = resultSet.getString("transaction_date");

                transactions.add(new Transactions(
                        id,
                        type,
                        name,
                        sup,
                        brand,
                        series,
                        qty,
                        price,
                        date
                ));
            }
            transactionTableView.setItems(transactions);
            transactionTableView.refresh();

        } catch (SQLException e){
            e.printStackTrace();
        }
    }


    @FXML
    void transSearchButton() {
        String searchText = transSearchField.getText().trim();

        if (searchText.isEmpty()) {
            loadTransactions();
            return;
        }

        transactions.clear();

        String query = "SELECT * FROM transactions WHERE id = ? OR LOWER(name) = LOWER(?)";

        try (Connection conn = SQLConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            try {
                int id = Integer.parseInt(searchText);
                stmt.setInt(1, id);
            } catch (NumberFormatException e) {
                stmt.setInt(1, -1);
            }

            stmt.setString(2, searchText);

            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String type = resultSet.getString("type");
                String name = resultSet.getString("name");
                String sup = resultSet.getString("supplier_name");
                String brand = resultSet.getString("brand");
                String series = resultSet.getString("series");
                int qty = resultSet.getInt("qty");
                double price = resultSet.getDouble("price");
                String date = resultSet.getString("transaction_date");

                transactions.add(new Transactions(
                        id, type, name, sup, brand, series, qty, price, date
                ));
            }

            transactionTableView.setItems(transactions);
            transactionTableView.refresh();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    //Reports
    @FXML
    private TableView <Report> reportTableView;
    @FXML
    private TableColumn <Report, String> reportClm, reportBrandClm,reportSeriesClm;
    @FXML
    private TableColumn <Report, Integer> reportTotalClm, reportStockQtyClm;
    @FXML
    private TableColumn <Report, Double> reportPriceClm;

    @FXML
    private DatePicker fromDate, toDate;

    @FXML
    void generateReport() {
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();

        if (from == null || to == null) {
            showAlert("Please select both From and To dates.", Alert.AlertType.WARNING);
            return;
        }

        if (from.isAfter(to)) {
            showAlert("From date cannot be after To date.", Alert.AlertType.ERROR);
            return;
        }

        reports.clear();

        String query = """
        SELECT 
            type,
            brand,
            series,
            COUNT(*) AS total_count,
            SUM(qty) AS total_qty,
            SUM(price) AS total_price
        FROM transactions
        WHERE transaction_date BETWEEN ? AND ?
          AND type IN (
              'New Staff', 'New Stock', 'New Supplier', 
              'Remove Stock', 'Remove Staff', 'Remove Supplier', 
              'Remove Brand', 'Stock In', 'Stock Out', 'New Price'
          )
        GROUP BY type, brand, series
        ORDER BY type, brand, series
    """;

        try (Connection conn = SQLConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setDate(1, Date.valueOf(from));
            stmt.setDate(2, Date.valueOf(to));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String type = rs.getString("type");
                String brand = rs.getString("brand");
                String series = rs.getString("series");
                int total = rs.getInt("total_count");
                int qty = rs.getInt("total_qty");
                double price = rs.getDouble("total_price");

                reports.add(new Report(type, brand, series, total, qty, price));
            }

            reportTableView.setItems(reports);
            reportTableView.refresh();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error generating report: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    void downloadReport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report as PDF");
        fileChooser.setInitialFileName("report.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(reportTableView.getScene().getWindow());

        if (file != null) {
            try (OutputStream outputStream = new FileOutputStream(file)) {
                PdfWriter writer = new PdfWriter(outputStream);
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc);

                document.add(new Paragraph("Inventory Report").setBold().setFontSize(16));
                document.add(new Paragraph("From: " + fromDate.getValue() + "   To: " + toDate.getValue()).setFontSize(12));
                document.add(new Paragraph(" "));

                Table table = new Table(UnitValue.createPercentArray(new float[]{2, 2, 2, 1, 1, 1}));
                table.setWidth(UnitValue.createPercentValue(100));
                table.addHeaderCell("Type");
                table.addHeaderCell("Brand");
                table.addHeaderCell("Series");
                table.addHeaderCell("Total");
                table.addHeaderCell("Qty");
                table.addHeaderCell("Price");


                for (Report report : reports) {
                    table.addCell(nullSafe(report.getName()));
                    table.addCell(nullSafe(report.getBrand()));
                    table.addCell(nullSafe(report.getSeries()));
                    table.addCell(String.valueOf(report.getTotal()));
                    table.addCell(String.valueOf(report.getQty()));
                    table.addCell(String.format("%.2f", report.getPrice()));
                }

                document.add(table);
                document.close();

                Desktop.getDesktop().open(file);

                showAlert("PDF report generated and opened successfully!", Alert.AlertType.INFORMATION);

                reports.clear();
                reportTableView.refresh();
                fromDate.setValue(null);
                toDate.setValue(null);

            } catch (Exception e) {
                showAlert("Failed to create or open PDF: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        }
    }





    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    @FXML
    void printReport() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(reportTableView.getScene().getWindow())) {
            boolean success = job.printPage(reportTableView);
            if (success) {
                job.endJob();
                showAlert("Report printed successfully.", Alert.AlertType.INFORMATION);

                reports.clear();
                reportTableView.refresh();
                fromDate.setValue(null);
                toDate.setValue(null);
            } else {
                showAlert("Failed to print report.", Alert.AlertType.ERROR);
            }
        }
    }

    private static void showAlert(String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle("Notification");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
