package com.ocsms.util;

import com.ocsms.model.BudgetAllocation;
import com.ocsms.model.BudgetBill;
import com.ocsms.model.Society;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * BudgetPdfExporter — generates a professional PDF budget report using PDFBox 3.x.
 */
public class BudgetPdfExporter {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // Page/margin constants
    private static final float MARGIN        = 50f;
    private static final float PAGE_WIDTH    = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT   = PDRectangle.A4.getHeight();
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;

    // Fonts
    private PDType1Font BOLD;
    private PDType1Font REGULAR;
    private PDType1Font ITALIC;

    /**
     * Generates a full budget PDF for a list of allocations (all societies or one).
     *
     * @param allocations  list of allocations to include
     * @param outputFile   where to save the PDF
     * @param reportTitle  headline text e.g. "Society Budget Report"
     */
    public void export(List<BudgetAllocation> allocations, File outputFile,
                       String reportTitle) throws IOException {

        try (PDDocument doc = new PDDocument()) {
            BOLD    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            ITALIC  = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            buildPage(doc, reportTitle, allocations);
            doc.save(outputFile);
        }
    }

    private float buildPage(PDDocument doc, String title,
                            List<BudgetAllocation> allocations) throws IOException {

        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        PDPageContentStream cs = new PDPageContentStream(doc, page);

        float y = PAGE_HEIGHT - MARGIN;

        // ── Header bar ──────────────────────────────────────────────────────────
        cs.setNonStrokingColor(0.11f, 0.14f, 0.26f);   // #1a2040 deep navy
        cs.addRect(0, y - 50, PAGE_WIDTH, 60);
        cs.fill();

        y -= 10;
        drawText(cs, "OCSMS", MARGIN, y, BOLD, 18, 1f, 1f, 1f);
        drawText(cs, "FAST-NUCES Peshawar", MARGIN + 80, y, REGULAR, 10, 0.7f, 0.7f, 0.9f);
        String dateStr = "Generated: " + LocalDate.now().format(FMT);
        float dateX = PAGE_WIDTH - MARGIN - textWidth(REGULAR, 10, dateStr);
        drawText(cs, dateStr, dateX, y, ITALIC, 10, 0.7f, 0.7f, 0.7f);
        y -= 50;

        // ── Title ───────────────────────────────────────────────────────────────
        drawText(cs, title, MARGIN, y, BOLD, 16, 0.31f, 0.39f, 0.78f);
        y -= 8;
        cs.setStrokingColor(0.31f, 0.39f, 0.78f);
        cs.setLineWidth(1.5f);
        cs.moveTo(MARGIN, y); cs.lineTo(PAGE_WIDTH - MARGIN, y); cs.stroke();
        y -= 20;

        // ── Summary row ─────────────────────────────────────────────────────────
        double grandTotal   = allocations.stream().mapToDouble(BudgetAllocation::getTotalBudget).sum();
        double grandUsed    = allocations.stream().mapToDouble(BudgetAllocation::getUsedAmount).sum();
        double grandRemain  = grandTotal - grandUsed;

        float colW = CONTENT_WIDTH / 3;
        drawSummaryCard(cs, MARGIN,            y, colW - 8, "Total Allocated", grandTotal,  0.13f, 0.49f, 0.37f);
        drawSummaryCard(cs, MARGIN + colW,     y, colW - 8, "Total Spent",     grandUsed,   0.54f, 0.10f, 0.10f);
        drawSummaryCard(cs, MARGIN + colW * 2, y, colW - 8, "Remaining",       grandRemain, 0.20f, 0.27f, 0.50f);
        y -= 60;

        // ── Per-allocation detail ────────────────────────────────────────────────
        for (BudgetAllocation alloc : allocations) {
            if (y < 160) {
                cs.close();
                page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                cs = new PDPageContentStream(doc, page);
                y = PAGE_HEIGHT - MARGIN;
            }

            Society soc = alloc.getSociety();
            String socName = soc != null ? soc.getName() : "Unknown Society";

            // Society header
            cs.setNonStrokingColor(0.08f, 0.11f, 0.20f);
            cs.addRect(MARGIN, y - 22, CONTENT_WIDTH, 28);
            cs.fill();
            drawText(cs, socName, MARGIN + 8, y - 8, BOLD, 12, 0.82f, 0.73f, 0.99f);
            drawText(cs, "Allocated: " + LocalDate.now().format(FMT),
                     PAGE_WIDTH - MARGIN - 120, y - 8, REGULAR, 9, 0.56f, 0.62f, 0.72f);
            y -= 32;

            // Allocation note
            if (alloc.getNote() != null && !alloc.getNote().isBlank()) {
                drawText(cs, "Note: " + alloc.getNote(), MARGIN + 4, y, ITALIC, 9, 0.56f, 0.62f, 0.72f);
                y -= 14;
            }

            // Budget bar labels
            drawText(cs, "Total Budget: Rs. " + fmt(alloc.getTotalBudget()), MARGIN + 4, y, BOLD, 10, 0.88f, 0.94f, 1f);
            drawText(cs, "Used: Rs. " + fmt(alloc.getUsedAmount()), MARGIN + 160, y, REGULAR, 10, 0.95f, 0.57f, 0.57f);
            drawText(cs, "Remaining: Rs. " + fmt(alloc.getRemainingBudget()), MARGIN + 280, y, REGULAR, 10, 0.40f, 0.87f, 0.63f);
            y -= 14;

            // Budget bar
            float barW = CONTENT_WIDTH - 8;
            cs.setNonStrokingColor(0.15f, 0.18f, 0.28f);
            cs.addRect(MARGIN + 4, y - 8, barW, 10); cs.fill();
            if (alloc.getTotalBudget() > 0) {
                float used = (float) Math.min(alloc.getUsedAmount() / alloc.getTotalBudget(), 1.0) * barW;
                cs.setNonStrokingColor(0.50f, 0.15f, 0.15f);
                cs.addRect(MARGIN + 4, y - 8, used, 10); cs.fill();
            }
            y -= 22;

            // Bills table header
            if (!alloc.getBills().isEmpty()) {
                drawText(cs, "BILLS / RECEIPTS", MARGIN + 4, y, BOLD, 9, 0.50f, 0.55f, 0.65f);
                y -= 12;
                drawTableRow(cs, y, MARGIN, CONTENT_WIDTH, "Date", "Description", "Amount (Rs.)", true);
                y -= 16;
                for (BudgetBill bill : alloc.getBills()) {
                    if (y < 80) {
                        cs.close();
                        page = new PDPage(PDRectangle.A4);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        y = PAGE_HEIGHT - MARGIN;
                    }
                    drawTableRow(cs, y, MARGIN, CONTENT_WIDTH,
                        bill.getUploadDate().format(FMT),
                        bill.getDescription(),
                        fmt(bill.getAmount()), false);
                    y -= 14;
                }
            } else {
                drawText(cs, "No bills uploaded yet.", MARGIN + 4, y, ITALIC, 9, 0.40f, 0.45f, 0.55f);
                y -= 14;
            }

            cs.setStrokingColor(0.15f, 0.20f, 0.35f);
            cs.setLineWidth(0.5f);
            cs.moveTo(MARGIN, y); cs.lineTo(PAGE_WIDTH - MARGIN, y); cs.stroke();
            y -= 16;
        }

        // ── Footer ───────────────────────────────────────────────────────────────
        cs.setNonStrokingColor(0.25f, 0.25f, 0.25f);
        cs.addRect(0, 20, PAGE_WIDTH, 0.5f); cs.fill();
        drawText(cs, "OCSMS — FAST-NUCES Peshawar  |  Confidential Budget Report",
                 MARGIN, 10, ITALIC, 8, 0.45f, 0.45f, 0.45f);

        cs.close();
        return y;
    }

    // ── Drawing helpers ────────────────────────────────────────────────────────

    private void drawText(PDPageContentStream cs, String text, float x, float y,
                          PDType1Font font, int size, float r, float g, float b) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(r, g, b);
        cs.newLineAtOffset(x, y);
        cs.showText(safe(text));
        cs.endText();
    }

    private void drawSummaryCard(PDPageContentStream cs, float x, float y,
                                  float w, String label, double amount,
                                  float r, float g, float b) throws IOException {
        cs.setNonStrokingColor(r * 0.15f, g * 0.15f, b * 0.15f);
        cs.addRect(x, y - 42, w, 48); cs.fill();
        cs.setNonStrokingColor(r, g, b);
        cs.setLineWidth(1f);
        cs.addRect(x, y - 42, w, 48); cs.stroke();

        drawText(cs, label,              x + 8, y - 12, BOLD,    8, 0.7f, 0.7f, 0.7f);
        drawText(cs, "Rs. " + fmt(amount), x + 8, y - 30, BOLD, 12, r, g, b);
    }

    private void drawTableRow(PDPageContentStream cs, float y, float x, float totalW,
                               String col1, String col2, String col3, boolean isHeader)
            throws IOException {
        float[] widths = { totalW * 0.2f, totalW * 0.55f, totalW * 0.25f };
        PDType1Font font = isHeader ? BOLD : REGULAR;
        float[] rgb = isHeader ? new float[]{0.7f, 0.7f, 0.8f} : new float[]{0.75f, 0.78f, 0.82f};

        if (isHeader) {
            cs.setNonStrokingColor(0.10f, 0.13f, 0.23f);
            cs.addRect(x, y - 10, totalW, 16); cs.fill();
        }

        drawText(cs, col1, x + 4,                    y,     font, 9, rgb[0], rgb[1], rgb[2]);
        drawText(cs, col2, x + widths[0] + 4,        y,     font, 9, rgb[0], rgb[1], rgb[2]);
        drawText(cs, col3, x + widths[0] + widths[1] + 4, y, font, 9, rgb[0], rgb[1], rgb[2]);
    }

    private float textWidth(PDType1Font font, int size, String text) throws IOException {
        return font.getStringWidth(safe(text)) / 1000f * size;
    }

    private String fmt(double v) { return String.format("%,.2f", v); }

    private String safe(String s) {
        if (s == null) return "";
        // Strip non-WinAnsi characters
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c < 256) sb.append(c); else sb.append('?');
        }
        return sb.toString();
    }

}
