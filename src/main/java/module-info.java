module com.ocsms {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.pdfbox;
    requires java.sql;
    requires java.desktop;          // AWT/ImageIO for logo resizing


    // ── Supabase HTTP client (OkHttp + Gson) ──────────────────────────────────
    requires okhttp3;
    requires kotlin.stdlib;       // OkHttp 4.x is Kotlin-based
    requires com.google.gson;

    opens com.ocsms to javafx.fxml;
    opens com.ocsms.gui.controllers to javafx.fxml;
    opens com.ocsms.model to javafx.base;
    opens com.ocsms.enums to javafx.base;

    exports com.ocsms;
    exports com.ocsms.model;
    exports com.ocsms.enums;
    exports com.ocsms.service;
    exports com.ocsms.repository;
    exports com.ocsms.util;
    exports com.ocsms.gui.controllers;
}
