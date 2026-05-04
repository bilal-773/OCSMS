package com.ocsms.util;

import com.ocsms.gui.controllers.AttendanceController;
import com.ocsms.model.Certificate;
import com.ocsms.model.Event;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * PDFGenerator — generates PDF certificates and attendance reports using Apache PDFBox.
 */
public class PDFGenerator {

    private static final String CERT_OUTPUT_DIR   = "certificates";
    private static final String REPORT_OUTPUT_DIR = "reports";

    // ─── Certificate Generation ───────────────────────────────────────────────

    public static String generateCertificate(Certificate cert) throws IOException {
        File dir = new File(CERT_OUTPUT_DIR);
        if (!dir.exists()) dir.mkdirs();
        String outputPath = CERT_OUTPUT_DIR + "/" + cert.getCertId() + ".pdf";

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float W = PDRectangle.A4.getWidth();
                float H = PDRectangle.A4.getHeight();

                // Background
                cs.setNonStrokingColor(0.05f, 0.05f, 0.15f);
                cs.addRect(0, 0, W, H); cs.fill();

                // Borders
                cs.setStrokingColor(0.4f, 0.3f, 0.9f); cs.setLineWidth(3);
                cs.addRect(20, 20, W - 40, H - 40); cs.stroke();
                cs.setStrokingColor(0.6f, 0.5f, 1.0f); cs.setLineWidth(1);
                cs.addRect(30, 30, W - 60, H - 60); cs.stroke();

                // University
                drawCentered(cs, "FAST-NUCES PESHAWAR", H - 80, W,
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14, 0.6f, 0.5f, 1.0f);

                // Title
                drawCentered(cs, "CERTIFICATE OF PARTICIPATION", H - 140, W,
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 30, 1f, 1f, 1f);

                // Divider
                cs.setStrokingColor(0.6f, 0.5f, 1.0f); cs.setLineWidth(1.5f);
                cs.moveTo(80, H - 160); cs.lineTo(W - 80, H - 160); cs.stroke();

                // Body text
                drawCentered(cs, "This is to certify that", H - 210, W,
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14, 0.75f, 0.75f, 0.85f);

                // Student name
                drawCentered(cs, cert.getRecipient().getName(), H - 260, W,
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE), 26, 1.0f, 0.85f, 0.3f);

                drawCentered(cs, "Roll No: " + cert.getRecipient().getRollNumber(), H - 290, W,
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12, 0.75f, 0.75f, 0.85f);

                drawCentered(cs, "has successfully participated in", H - 340, W,
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14, 0.75f, 0.75f, 0.85f);

                // Event
                drawCentered(cs, cert.getEvent().getTitle(), H - 380, W,
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20, 0.6f, 0.8f, 1.0f);

                if (cert.getEvent().getOrganizer() != null)
                    drawCentered(cs, "Organized by: " + cert.getEvent().getOrganizer().getName(), H - 410, W,
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12, 0.75f, 0.75f, 0.85f);

                drawCentered(cs, "Date: " + cert.getIssuedDate(), H - 450, W,
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12, 0.75f, 0.75f, 0.85f);

                // Divider
                cs.setStrokingColor(0.4f, 0.3f, 0.7f); cs.setLineWidth(1f);
                cs.moveTo(80, H - 470); cs.lineTo(W - 80, H - 470); cs.stroke();

                // Verification code
                drawCentered(cs, "Verification Code: " + cert.getVerificationCode(), H - 495, W,
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10, 0.5f, 0.5f, 0.6f);

                // Footer
                drawCentered(cs, "Online College Society Management System (OCSMS) — FAST-NUCES Peshawar", 50, W,
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9, 0.4f, 0.4f, 0.5f);
            }
            doc.save(outputPath);
        }
        return outputPath;
    }

    // ─── Attendance Report ─────────────────────────────────────────────────────

    public static String generateAttendanceReport(Event event,
            List<AttendanceController.AttendanceRow> rows) throws IOException {
        File dir = new File(REPORT_OUTPUT_DIR);
        if (!dir.exists()) dir.mkdirs();

        String safeTitle  = event.getTitle().replaceAll("[^a-zA-Z0-9]", "_");
        String outputPath = REPORT_OUTPUT_DIR + "/attendance_" + safeTitle + "_" + LocalDate.now() + ".pdf";

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float W = PDRectangle.A4.getWidth();
            float H = PDRectangle.A4.getHeight();

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // Background
                cs.setNonStrokingColor(0.98f, 0.98f, 1.0f); cs.addRect(0, 0, W, H); cs.fill();

                // Header bar
                cs.setNonStrokingColor(0.31f, 0.27f, 0.90f); cs.addRect(0, H - 80, W, 80); cs.fill();

                // Header text
                cs.setNonStrokingColor(1f, 1f, 1f);
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                cs.newLineAtOffset(30, H - 45);
                cs.showText("ATTENDANCE REPORT — " + event.getTitle().toUpperCase());
                cs.endText();

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                cs.newLineAtOffset(30, H - 65);
                cs.showText("OCSMS — FAST-NUCES Peshawar   |   Event Date: " + event.getDateTime()
                    + "   |   Generated: " + LocalDate.now());
                cs.endText();

                // Table header
                float y = H - 105;
                cs.setNonStrokingColor(0.22f, 0.22f, 0.35f); cs.addRect(25, y - 5, W - 50, 22); cs.fill();
                cs.setNonStrokingColor(1f, 1f, 1f);
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
                cs.newLineAtOffset(30, y);
                cs.showText("#     Student Name                       Roll No.          Status");
                cs.endText();

                // Rows
                y -= 30;
                int i = 1;
                for (AttendanceController.AttendanceRow row : rows) {
                    if (y < 60) break;
                    boolean present = row.isPresent();
                    String name   = row.getRegistration().getStudent().getName();
                    String roll   = row.getRegistration().getStudent().getRollNumber();
                    String status = present ? "PRESENT" : "ABSENT";

                    if (i % 2 == 0) {
                        cs.setNonStrokingColor(0.95f, 0.95f, 0.98f);
                        cs.addRect(25, y - 6, W - 50, 20); cs.fill();
                    }

                    if (present) cs.setNonStrokingColor(0.13f, 0.55f, 0.32f);
                    else         cs.setNonStrokingColor(0.80f, 0.18f, 0.18f);
                    cs.addRect(25, y - 4, 5, 14); cs.fill();

                    cs.setNonStrokingColor(0.15f, 0.15f, 0.25f);
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                    cs.newLineAtOffset(36, y);
                    cs.showText(String.format("%-5d %-37s %-18s %s", i, truncate(name, 35), roll, status));
                    cs.endText();

                    y -= 22;
                    i++;
                }

                // Summary
                long presentCount = rows.stream().filter(AttendanceController.AttendanceRow::isPresent).count();
                y -= 10;
                cs.setNonStrokingColor(0.31f, 0.27f, 0.90f); cs.setLineWidth(1f);
                cs.moveTo(25, y); cs.lineTo(W - 25, y); cs.stroke();
                y -= 18;
                cs.setNonStrokingColor(0.15f, 0.15f, 0.25f);
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                cs.newLineAtOffset(30, y);
                cs.showText("Total: " + rows.size() + "   |   Present: " + presentCount
                    + "   |   Absent: " + (rows.size() - presentCount));
                cs.endText();
            }
            doc.save(outputPath);
        }
        return outputPath;
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private static void drawCentered(PDPageContentStream cs, String text, float y, float pageWidth,
            PDType1Font font, float fontSize, float r, float g, float b) throws IOException {
        cs.setNonStrokingColor(r, g, b);
        cs.beginText();
        cs.setFont(font, fontSize);
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        cs.newLineAtOffset((pageWidth - textWidth) / 2, y);
        cs.showText(text);
        cs.endText();
    }

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 2) + ".." : s;
    }
}
