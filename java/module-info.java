module com.system.inventory {
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires  java.sql;
    requires layout;
    requires kernel;
    requires java.desktop;
    requires mysql.connector.j;
    requires java.compiler;
    requires de.jensd.fx.glyphs.fontawesome;
    requires javafx.controls;

    opens com.system.inventory.main to javafx.fxml;
    opens com.system.inventory.controller to javafx.fxml;
    opens com.system.inventory.model to javafx.base;


    exports com.system.inventory.main;
}