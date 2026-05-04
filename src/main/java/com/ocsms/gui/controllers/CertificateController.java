package com.ocsms.gui.controllers;

import com.ocsms.model.*;
import com.ocsms.service.CertificateService;
import com.ocsms.util.AlertUtil;
import com.ocsms.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * CertificateController — dedicated certificates screen.
 * Shows earned certificates, allows verify by code, and PDF download.
 */
public class CertificateController implements Initializable {

    @FXML private Label totalCertsLabel;
    @FXML private TextField verifyCodeField;
    @FXML private Label     verifyResult;

    @FXML private TableView<Certificate>           certsTable;
    @FXML private TableColumn<Certificate, String> colCertEvent;
    @FXML private TableColumn<Certificate, String> colCertSociety;
    @FXML private TableColumn<Certificate, String> colCertDate;
    @FXML private TableColumn<Certificate, String> colCertCode;
    @FXML private TableColumn<Certificate, String> colCertDownload;

    private final CertificateService certService = new CertificateService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        loadCertificates();
    }

    private void setupColumns() {
        colCertEvent.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEvent().getTitle()));
        colCertSociety.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getEvent().getOrganizer() != null ?
            d.getValue().getEvent().getOrganizer().getName() : "—"));
        colCertDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getIssuedDate().toString()));
        colCertCode.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getVerificationCode()));

        // Download button column
        colCertDownload.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("⬇ PDF");
            {
                btn.setStyle("-fx-background-color: #1e2a4a; -fx-text-fill: #818cf8; -fx-font-size: 11px;" +
                             "-fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 4 10;");
                btn.setOnAction(e -> {
                    Certificate cert = getTableView().getItems().get(getIndex());
                    handleDownloadCert(cert);
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void loadCertificates() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (!(user instanceof Student student)) {
            totalCertsLabel.setText("0");
            return;
        }
        List<Certificate> certs = certService.getCertificatesForStudent(student.getUserId());
        certsTable.setItems(FXCollections.observableArrayList(certs));
        totalCertsLabel.setText(String.valueOf(certs.size()));
    }

    private void handleDownloadCert(Certificate cert) {
        try {
            String path = certService.generateAndSaveSingle(cert);
            AlertUtil.showInfo("Certificate Downloaded",
                "PDF saved to:\n" + path + "\n\nVerification Code: " + cert.getVerificationCode());
        } catch (Exception ex) {
            AlertUtil.showError("Download Failed", ex.getMessage());
        }
    }

    @FXML private void handleVerify() {
        String code = verifyCodeField.getText().trim();
        if (code.isEmpty()) {
            verifyResult.setStyle("-fx-text-fill: #475569;");
            verifyResult.setText("Please enter a verification code.");
            return;
        }
        if (certService.verify(code)) {
            verifyResult.setStyle("-fx-text-fill: #34d399; -fx-font-weight: bold;");
            verifyResult.setText("✔  Valid certificate — authenticity confirmed.");
        } else {
            verifyResult.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            verifyResult.setText("✘  Not found — certificate may be invalid or not yet generated.");
        }
    }
}
