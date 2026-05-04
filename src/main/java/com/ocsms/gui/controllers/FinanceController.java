package com.ocsms.gui.controllers;

import com.ocsms.enums.UserRole;
import com.ocsms.model.*;
import com.ocsms.repository.BudgetRepository;
import com.ocsms.repository.SocietyRepository;
import com.ocsms.util.AlertUtil;
import com.ocsms.util.BudgetPdfExporter;
import com.ocsms.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

/**
 * FinanceController — role-based budget management.
 *
 * TREASURER:
 *   - Sees all societies in a table
 *   - Allocates a budget amount to any society with a note + expiry date
 *   - Exports full budget PDF
 *
 * SOCIETY_ADMIN (President):
 *   - Sees only their society's allocated budget
 *   - Uploads bills/receipts against the allocation
 *   - Exports their own budget PDF
 *
 * UNIVERSITY_ADMIN:
 *   - Read-only view of all allocations (same as Treasurer view but no allocate button)
 */
public class FinanceController implements Initializable {

    // ── Treasurer: Allocate Budget pane ───────────────────────────────────────
    @FXML private VBox  treasurerPane;
    @FXML private TableView<Society> societyListTable;
    @FXML private TableColumn<Society, String> colSocName;
    @FXML private TableColumn<Society, String> colSocCategory;
    @FXML private TableColumn<Society, String> colSocAllocated;
    @FXML private TableColumn<Society, String> colSocSpent;
    @FXML private TableColumn<Society, String> colSocRemaining;

    @FXML private VBox      allocateForm;
    @FXML private Label     allocatingSocietyLabel;
    @FXML private TextField allocAmountField;
    @FXML private TextField allocNoteField;
    @FXML private DatePicker allocExpiryPicker;
    @FXML private Label     allocErrorLabel;
    @FXML private Button    confirmAllocateBtn;

    // ── President: My Budget pane ─────────────────────────────────────────────
    @FXML private VBox  presidentPane;
    @FXML private Label budgetSocietyLabel;
    @FXML private Label budgetTotalLabel;
    @FXML private Label budgetUsedLabel;
    @FXML private Label budgetRemainingLabel;
    @FXML private Label budgetExpiryLabel;
    @FXML private Label budgetNoteLabel;
    @FXML private ProgressBar budgetProgressBar;

    @FXML private TableView<BudgetBill> billsTable;
    @FXML private TableColumn<BudgetBill, String> colBillDate;
    @FXML private TableColumn<BudgetBill, String> colBillDesc;
    @FXML private TableColumn<BudgetBill, String> colBillAmount;
    @FXML private TableColumn<BudgetBill, String> colBillFile;

    @FXML private VBox      uploadBillForm;
    @FXML private TextField billDescField;
    @FXML private TextField billAmountField;
    @FXML private Label     billFileLabel;
    @FXML private Label     billErrorLabel;

    @FXML private Button    exportPdfBtn;

    private final BudgetRepository  budgetRepo  = BudgetRepository.getInstance();
    private final SocietyRepository societyRepo = new SocietyRepository();

    private Society          selectedSociety       = null;
    private BudgetAllocation currentAllocation     = null;
    private File             selectedBillFile      = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = SessionManager.getInstance().getCurrentUser();
        UserRole role = user.getRole();

        boolean isTreasurer = role == UserRole.TREASURER;
        boolean isPresident = role == UserRole.SOCIETY_ADMIN;
        boolean isUniAdmin  = role == UserRole.UNIVERSITY_ADMIN;

        // Show correct pane
        if (treasurerPane != null) { treasurerPane.setVisible(isTreasurer || isUniAdmin); treasurerPane.setManaged(isTreasurer || isUniAdmin); }
        if (presidentPane != null) { presidentPane.setVisible(isPresident);               presidentPane.setManaged(isPresident); }

        if (isTreasurer || isUniAdmin) {
            setupTreasurerView(isTreasurer);
        } else if (isPresident) {
            setupPresidentView((SocietyAdmin) user);
        }
    }

    // ── Treasurer view ─────────────────────────────────────────────────────────

    private void setupTreasurerView(boolean canAllocate) {
        setupSocietyTableColumns();
        loadSocietiesAsync();

        // Allocate button only for Treasurer, not UniAdmin read-only
        if (confirmAllocateBtn != null) {
            confirmAllocateBtn.setVisible(canAllocate);
            confirmAllocateBtn.setManaged(canAllocate);
        }

        societyListTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, soc) -> { if (soc != null) showAllocateForm(soc, canAllocate); });
    }

    private void setupSocietyTableColumns() {
        colSocName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colSocCategory.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));

        colSocAllocated.setCellValueFactory(d -> {
            double total = budgetRepo.findBySociety(d.getValue().getSocietyId())
                .stream().mapToDouble(BudgetAllocation::getTotalBudget).sum();
            return new SimpleStringProperty("Rs. " + String.format("%,.2f", total));
        });
        colSocSpent.setCellValueFactory(d -> {
            double used = budgetRepo.findBySociety(d.getValue().getSocietyId())
                .stream().mapToDouble(BudgetAllocation::getUsedAmount).sum();
            return new SimpleStringProperty("Rs. " + String.format("%,.2f", used));
        });
        colSocRemaining.setCellValueFactory(d -> {
            double rem = budgetRepo.findBySociety(d.getValue().getSocietyId())
                .stream().mapToDouble(BudgetAllocation::getRemainingBudget).sum();
            return new SimpleStringProperty("Rs. " + String.format("%,.2f", rem));
        });

        // Color rows: overspent = red tint
        societyListTable.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Society s, boolean empty) {
                super.updateItem(s, empty);
                if (s == null || empty) { setStyle(""); return; }
                double rem = budgetRepo.findBySociety(s.getSocietyId())
                    .stream().mapToDouble(BudgetAllocation::getRemainingBudget).sum();
                setStyle(rem < 0 ? "-fx-background-color: #200808;" : "");
            }
        });
    }

    private void loadSocietiesAsync() {
        Task<List<Society>> task = new Task<>() {
            @Override protected List<Society> call() { return societyRepo.findAll(); }
        };
        task.setOnSucceeded(ev -> javafx.application.Platform.runLater(() ->
            societyListTable.setItems(FXCollections.observableArrayList(task.getValue()))));
        new Thread(task, "load-soc-budget").start();
    }

    private void showAllocateForm(Society soc, boolean canAllocate) {
        selectedSociety = soc;
        if (allocateForm == null) return;

        allocatingSocietyLabel.setText("Allocating budget to: " + soc.getName());
        allocateForm.setVisible(true);
        allocateForm.setManaged(true);
        allocErrorLabel.setText("");

        // Show existing allocation summary
        List<BudgetAllocation> existing = budgetRepo.findBySociety(soc.getSocietyId());
        if (!existing.isEmpty()) {
            double total = existing.stream().mapToDouble(BudgetAllocation::getTotalBudget).sum();
            double used  = existing.stream().mapToDouble(BudgetAllocation::getUsedAmount).sum();
            allocErrorLabel.setStyle("-fx-text-fill: #818cf8; -fx-font-size: 11px;");
            allocErrorLabel.setText(String.format(
                "Existing allocations: Rs. %.2f total, Rs. %.2f used.", total, used));
        } else {
            allocErrorLabel.setText("");
            allocErrorLabel.setStyle("");
        }

        if (!canAllocate) {
            allocAmountField.setEditable(false);
            allocNoteField.setEditable(false);
        }
    }

    @FXML private void handleAllocateBudget() {
        allocErrorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
        if (selectedSociety == null) { allocErrorLabel.setText("Select a society first."); return; }

        String amtStr = allocAmountField.getText().trim();
        String note   = allocNoteField.getText().trim();
        LocalDate exp = allocExpiryPicker.getValue();

        if (amtStr.isEmpty()) { allocErrorLabel.setText("Enter a budget amount."); return; }
        double amount;
        try { amount = Double.parseDouble(amtStr); }
        catch (NumberFormatException e) { allocErrorLabel.setText("Invalid amount."); return; }
        if (amount <= 0) { allocErrorLabel.setText("Amount must be positive."); return; }
        if (exp == null || exp.isBefore(LocalDate.now()))
            { allocErrorLabel.setText("Set a valid future expiry date."); return; }

        BudgetAllocation alloc = new BudgetAllocation(selectedSociety, amount, note, exp);
        budgetRepo.saveAllocation(alloc);

        AlertUtil.showInfo("Budget Allocated",
            String.format("Rs. %,.2f allocated to %s until %s.",
                amount, selectedSociety.getName(), exp));
        allocAmountField.clear(); allocNoteField.clear(); allocExpiryPicker.setValue(null);
        allocErrorLabel.setText("");
        societyListTable.refresh();
    }

    @FXML private void handleCancelAllocate() {
        if (allocateForm != null) { allocateForm.setVisible(false); allocateForm.setManaged(false); }
    }

    // ── President view ─────────────────────────────────────────────────────────

    private void setupPresidentView(SocietyAdmin president) {
        Society soc = president.getManagedSociety();
        if (soc == null) {
            // Try to load from DB
            new SocietyRepository().findByPresidentId(president.getUserId())
                .ifPresent(s -> { president.setManagedSociety(s); refreshPresidentView(president); });
            return;
        }
        refreshPresidentView(president);
    }

    private void refreshPresidentView(SocietyAdmin president) {
        Society soc = president.getManagedSociety();
        if (soc == null || budgetSocietyLabel == null) return;

        budgetSocietyLabel.setText(soc.getName() + " — Budget Overview");

        List<BudgetAllocation> allocs = budgetRepo.findBySociety(soc.getSocietyId());
        double total   = allocs.stream().mapToDouble(BudgetAllocation::getTotalBudget).sum();
        double used    = allocs.stream().mapToDouble(BudgetAllocation::getUsedAmount).sum();
        double remain  = total - used;

        budgetTotalLabel.setText("Rs. " + String.format("%,.2f", total));
        budgetUsedLabel.setText("Rs. " + String.format("%,.2f", used));
        budgetRemainingLabel.setText("Rs. " + String.format("%,.2f", remain));

        if (!allocs.isEmpty()) {
            BudgetAllocation latest = allocs.get(allocs.size() - 1);
            currentAllocation = latest;
            budgetNoteLabel.setText(latest.getNote() != null ? latest.getNote() : "—");
            budgetExpiryLabel.setText(latest.getExpiryDate() != null
                ? "Expires: " + latest.getExpiryDate().toString() : "No expiry");
        } else {
            budgetNoteLabel.setText("No budget allocated yet. Contact the Treasurer.");
            budgetExpiryLabel.setText("");
        }

        budgetProgressBar.setProgress(total > 0 ? Math.min(used / total, 1.0) : 0);

        setupBillsTable();
        refreshBillsTable(soc);
    }

    private void setupBillsTable() {
        if (colBillDate == null) return;
        colBillDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUploadDate().toString()));
        colBillDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
        colBillAmount.setCellValueFactory(d -> new SimpleStringProperty("Rs. " + String.format("%,.2f", d.getValue().getAmount())));
        colBillFile.setCellValueFactory(d -> {
            String fp = d.getValue().getFilePath();
            return new SimpleStringProperty(fp != null && !fp.isEmpty()
                ? new File(fp).getName() : "No file");
        });
    }

    private void refreshBillsTable(Society soc) {
        if (billsTable == null) return;
        List<BudgetBill> bills = budgetRepo.findBillsBySociety(soc.getSocietyId());
        billsTable.setItems(FXCollections.observableArrayList(bills));
    }

    @FXML private void handleShowUploadBillForm() {
        if (currentAllocation == null) {
            AlertUtil.showError("No Allocation", "No budget has been allocated to your society yet.");
            return;
        }
        if (uploadBillForm != null) { uploadBillForm.setVisible(true); uploadBillForm.setManaged(true); }
    }

    @FXML private void handleBrowseBillFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Receipt / Bill");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.png", "*.jpg", "*.jpeg"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        selectedBillFile = chooser.showOpenDialog(null);
        if (billFileLabel != null && selectedBillFile != null)
            billFileLabel.setText(selectedBillFile.getName());
    }

    @FXML private void handleSubmitBill() {
        if (billErrorLabel != null) billErrorLabel.setText("");
        String desc   = billDescField.getText().trim();
        String amtStr = billAmountField.getText().trim();

        if (desc.isEmpty())   { if(billErrorLabel!=null) billErrorLabel.setText("Description is required."); return; }
        if (amtStr.isEmpty()) { if(billErrorLabel!=null) billErrorLabel.setText("Amount is required."); return; }

        double amount;
        try { amount = Double.parseDouble(amtStr); }
        catch (NumberFormatException e) { if(billErrorLabel!=null) billErrorLabel.setText("Invalid amount."); return; }
        if (amount <= 0) { if(billErrorLabel!=null) billErrorLabel.setText("Amount must be positive."); return; }

        User user = SessionManager.getInstance().getCurrentUser();
        String filePath = selectedBillFile != null ? selectedBillFile.getAbsolutePath() : "";

        BudgetBill bill = new BudgetBill(
            currentAllocation.getAllocationId(), desc, amount, filePath, user.getName());
        budgetRepo.saveBill(bill);

        AlertUtil.showInfo("Bill Uploaded", "Bill recorded: Rs. " + String.format("%,.2f", amount));
        billDescField.clear(); billAmountField.clear();
        selectedBillFile = null;
        if (billFileLabel != null) billFileLabel.setText("No file chosen");
        if (uploadBillForm != null) { uploadBillForm.setVisible(false); uploadBillForm.setManaged(false); }

        Society soc = ((SocietyAdmin) user).getManagedSociety();
        if (soc != null) {
            refreshPresidentView((SocietyAdmin) user);
        }
    }

    @FXML private void handleCancelBillUpload() {
        if (uploadBillForm != null) { uploadBillForm.setVisible(false); uploadBillForm.setManaged(false); }
    }

    // ── PDF Export ─────────────────────────────────────────────────────────────

    @FXML private void handleExportPdf() {
        User user = SessionManager.getInstance().getCurrentUser();
        List<BudgetAllocation> toExport;
        String title;

        if (user.getRole() == UserRole.SOCIETY_ADMIN) {
            SocietyAdmin admin = (SocietyAdmin) user;
            if (admin.getManagedSociety() == null) { AlertUtil.showError("No Society", "No society found."); return; }
            toExport = budgetRepo.findBySociety(admin.getManagedSociety().getSocietyId());
            title = admin.getManagedSociety().getName() + " — Budget Report";
        } else {
            toExport = budgetRepo.findAll();
            title = "OCSMS — Full Society Budget Report";
        }

        if (toExport.isEmpty()) { AlertUtil.showError("No Data", "No budget allocations to export."); return; }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Budget Report PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName("budget_report_" + LocalDate.now() + ".pdf");
        File out = chooser.showSaveDialog(null);
        if (out == null) return;

        final List<BudgetAllocation> finalList = toExport;
        final String finalTitle = title;
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                new BudgetPdfExporter().export(finalList, out, finalTitle);
                return null;
            }
        };
        task.setOnSucceeded(ev -> AlertUtil.showInfo("PDF Exported", "Saved to:\n" + out.getAbsolutePath()));
        task.setOnFailed(ev -> AlertUtil.showError("Export Failed",
            task.getException() != null ? task.getException().getMessage() : "Unknown error"));
        new Thread(task, "pdf-export").start();
    }
}
