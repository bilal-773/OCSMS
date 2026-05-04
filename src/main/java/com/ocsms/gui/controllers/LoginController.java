package com.ocsms.gui.controllers;

import com.ocsms.Main;
import com.ocsms.model.User;
import com.ocsms.service.AuthService;
import com.ocsms.util.AlertUtil;
import com.ocsms.util.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * LoginController — login + student self-registration.
 *
 * REGISTRATION:
 *   Only STUDENTS can self-register.
 *   Society Presidents are added by the University Admin (in the admin panel).
 */
public class LoginController implements Initializable {

    // ── Tab buttons ────────────────────────────────────────────────────────────
    @FXML private Button loginTabBtn;
    @FXML private Button registerTabBtn;
    @FXML private VBox   loginPanel;
    @FXML private VBox   registerPanel;

    // ── Login ──────────────────────────────────────────────────────────────────
    @FXML private TextField     loginRollField;
    @FXML private PasswordField loginPasswordField;
    @FXML private Label         loginErrorLabel;
    @FXML private Label         loginErrorIcon;
    @FXML private Button        loginBtn;

    // ── Register (Student only) ────────────────────────────────────────────────
    @FXML private TextField     regNameField;
    @FXML private TextField     regRollField;
    @FXML private TextField     regEmailField;
    @FXML private PasswordField regPasswordField;
    @FXML private Label         regErrorLabel;
    @FXML private Button        registerBtn;

    private final AuthService authService = new AuthService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loginErrorLabel.setText("");
        regErrorLabel.setText("");
        loginPasswordField.setOnAction(e -> handleLogin());
        showLoginPanel();
    }

    // ── Tab switching ──────────────────────────────────────────────────────────

    @FXML private void switchToLogin()    { showLoginPanel(); }
    @FXML private void switchToRegister() { showRegisterPanel(); }

    private void showLoginPanel() {
        loginPanel.setVisible(true);    loginPanel.setManaged(true);
        registerPanel.setVisible(false); registerPanel.setManaged(false);
        applyActiveTab(loginTabBtn);    applyInactiveTab(registerTabBtn);
    }
    private void showRegisterPanel() {
        loginPanel.setVisible(false);   loginPanel.setManaged(false);
        registerPanel.setVisible(true); registerPanel.setManaged(true);
        applyInactiveTab(loginTabBtn);  applyActiveTab(registerTabBtn);
    }

    private static final String ACTIVE_STYLE =
        "-fx-background-color: transparent; -fx-text-fill: #f1f5f9;" +
        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 14 0;" +
        "-fx-cursor: hand; -fx-border-color: transparent transparent #3b82f6 transparent;" +
        "-fx-border-width: 0 0 2 0; -fx-background-radius: 0;";
    private static final String INACTIVE_STYLE =
        "-fx-background-color: transparent; -fx-text-fill: #64748b;" +
        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 14 0;" +
        "-fx-cursor: hand; -fx-border-color: transparent; -fx-border-width: 0;";

    private void applyActiveTab(Button btn)   { btn.setStyle(ACTIVE_STYLE); }
    private void applyInactiveTab(Button btn) { btn.setStyle(INACTIVE_STYLE); }

    // ── Login ──────────────────────────────────────────────────────────────────

    @FXML
    private void handleLogin() {
        loginErrorLabel.setText("");
        String roll = loginRollField.getText().trim();
        String pass = loginPasswordField.getText();
        if (roll.isEmpty()) { showLoginError("Roll number is required."); return; }
        if (pass.isEmpty()) { showLoginError("Password is required."); return; }

        loginBtn.setDisable(true);
        loginBtn.setText("Signing in…");

        Task<User> task = new Task<>() {
            @Override protected User call() { return authService.login(roll, pass); }
        };
        task.setOnSucceeded(ev -> {
            loginBtn.setDisable(false); loginBtn.setText("Login");
            User user = task.getValue();
            if (user == null) {
                showLoginError("Invalid credentials. Please try again.");
            } else {
                SessionManager.getInstance().setCurrentUser(user);
                try { Main.showDashboard(); }
                catch (Exception ex) { AlertUtil.showError("Error", ex.getMessage()); }
            }
        });
        task.setOnFailed(ev -> {
            loginBtn.setDisable(false); loginBtn.setText("Login");
            showLoginError("Connection error. Check network and retry.");
        });
        new Thread(task, "login-thread").start();
    }

    private void showLoginError(String msg) { loginErrorLabel.setText(msg); }

    // ── Register (Student only) ────────────────────────────────────────────────

    @FXML
    private void handleRegister() {
        regErrorLabel.setText("");
        String name  = regNameField.getText().trim();
        String roll  = regRollField.getText().trim();
        String email = regEmailField.getText().trim();
        String pass  = regPasswordField.getText();

        if (name.isEmpty())  { regErrorLabel.setText("Name is required.");          return; }
        if (roll.isEmpty())  { regErrorLabel.setText("Roll number is required.");    return; }
        if (!email.contains("@")) { regErrorLabel.setText("Valid email required."); return; }
        if (pass.length() < 6)   { regErrorLabel.setText("Min 6-char password.");   return; }

        registerBtn.setDisable(true);
        registerBtn.setText("Creating account…");

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                authService.registerStudent(roll, name, email, pass);
                return null;
            }
        };
        task.setOnSucceeded(ev -> {
            registerBtn.setDisable(false); registerBtn.setText("Create Account");
            Platform.runLater(() -> {
                AlertUtil.showInfo("Registered!", "Account created. You can now login.");
                showLoginPanel();
                loginRollField.setText(roll);
            });
        });
        task.setOnFailed(ev -> {
            registerBtn.setDisable(false); registerBtn.setText("Create Account");
            String msg = task.getException() != null ? task.getException().getMessage() : "Registration failed.";
            Platform.runLater(() -> regErrorLabel.setText(msg));
        });
        new Thread(task, "register-thread").start();
    }
}
