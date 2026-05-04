package com.ocsms.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * LogoResizeUtil — resizes an image to fit exactly within targetW x targetH pixels.
 *
 * Strategy:
 *  1. Scale proportionally so the image fits within the bounding box (no stretching).
 *  2. Pad with transparent background to fill the full target dimensions.
 *  3. Save as PNG into the application's local logo cache directory.
 */
public class LogoResizeUtil {

    /** Directory where processed logos are stored (created on first use). */
    private static final Path LOGO_DIR = Paths.get(
        System.getProperty("user.home"), ".ocsms", "logos");

    /**
     * Resizes {@code sourceFile} to fit within {@code targetW × targetH} pixels.
     * The result is saved as a PNG in the local logo cache directory.
     *
     * @param sourceFile original image file (any format ImageIO supports)
     * @param targetW    target width in pixels
     * @param targetH    target height in pixels
     * @return the saved output File
     * @throws IOException if the file cannot be read or written
     */
    public static File resizeToFit(File sourceFile, int targetW, int targetH) throws IOException {
        // ── 1. Read source ───────────────────────────────────────────────────
        BufferedImage src = ImageIO.read(sourceFile);
        if (src == null) throw new IOException("Unsupported image format: " + sourceFile.getName());

        int srcW = src.getWidth();
        int srcH = src.getHeight();

        // ── 2. Compute scale (fit within bounding box, keep aspect ratio) ───
        double scaleX = (double) targetW / srcW;
        double scaleY = (double) targetH / srcH;
        double scale  = Math.min(scaleX, scaleY);       // fit, not fill

        int scaledW = (int) Math.round(srcW * scale);
        int scaledH = (int) Math.round(srcH * scale);

        // ── 3. Scale the image with BICUBIC interpolation ───────────────────
        Image scaledImg = src.getScaledInstance(scaledW, scaledH, Image.SCALE_SMOOTH);

        // ── 4. Composite onto transparent canvas of exact target size ────────
        BufferedImage canvas = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,       RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,      RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,          RenderingHints.VALUE_RENDER_QUALITY);

        // Centre the scaled image on the canvas
        int xOffset = (targetW - scaledW) / 2;
        int yOffset = (targetH - scaledH) / 2;
        g.drawImage(scaledImg, xOffset, yOffset, null);
        g.dispose();

        // ── 5. Save output to logo cache ─────────────────────────────────────
        Files.createDirectories(LOGO_DIR);
        String outName = UUID.randomUUID() + "_logo.png";
        File outFile = LOGO_DIR.resolve(outName).toFile();
        ImageIO.write(canvas, "PNG", outFile);
        return outFile;
    }
}
