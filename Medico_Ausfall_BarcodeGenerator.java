import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Medico_Ausfall_BarcodeGenerator {

    /** Pattern: segment that is only digits, 6–12 chars (person ID in filenames). */
    private static final Pattern PERSON_ID_PATTERN = Pattern.compile("-([0-9]{6,12})(?:-|$)");

    /** Pattern: birth date in format dd.MM.yyyy */
    private static final Pattern BIRTH_DATE_PATTERN = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})");

    public static void main(String[] args) {
        try {
            String configPath = "config.json";
            if (args.length > 0) {
                configPath = args[0];
            }
            JSONObject config = new JSONObject(Files.readString(Paths.get(configPath), StandardCharsets.UTF_8));

            String offlinedokuPathStr = config.optString("offlinedokuPath", "");
            if (offlinedokuPathStr.isEmpty()) {
                System.err.println("config.json must contain 'offlinedokuPath' (e.g. files/OFFLINEDOKU or D:\\OfflineDoku).");
                System.exit(1);
            }
            Path basePath = Paths.get(offlinedokuPathStr);
            if (!Files.isDirectory(basePath)) {
                System.err.println("offlinedokuPath is not a directory: " + basePath.toAbsolutePath());
                System.exit(1);
            }

            String barcodeType = config.optString("barcodeType", "CODE_128");
            int width = config.optInt("barcodeWidth", 200);
            int height = config.optInt("barcodeHeight", 50);
            String pageSize = config.optString("pageSize", "A4");
            int perPage = config.optInt("barcodesPerPage", 8);
            int columns = config.optInt("columns", 0);
            int rows = config.optInt("rows", 0);
            if (columns > 0 && rows > 0) {
                perPage = columns * rows;
            }
            float marginLeft = (float) config.optDouble("marginLeft", 20);
            float marginTop = (float) config.optDouble("marginTop", 20);
            float marginRight = (float) config.optDouble("marginRight", 20);
            float marginBottom = (float) config.optDouble("marginBottom", 20);
            float labelWidth = (float) config.optDouble("labelWidth", 0);
            float labelHeight = (float) config.optDouble("labelHeight", 0);
            String barcodeOutputFilename = config.optString("barcodeOutputFilename", "Barcodes.pdf");
            float nameFontSize = (float) config.optDouble("nameFontSize", 9);
            boolean nameFontBold = config.optBoolean("nameFontBold", true);
            float infoFontSize = (float) config.optDouble("infoFontSize", 8);
            boolean infoFontBold = config.optBoolean("infoFontBold", false);

            // Validate critical numeric config values
            if (width <= 0)        throw new IllegalArgumentException("barcodeWidth must be > 0");
            if (height <= 0)       throw new IllegalArgumentException("barcodeHeight must be > 0");
            if (perPage <= 0)      throw new IllegalArgumentException("barcodesPerPage must be > 0");
            if (columns < 0)       throw new IllegalArgumentException("columns must be >= 0");
            if (rows < 0)          throw new IllegalArgumentException("rows must be >= 0");
            if (nameFontSize <= 0) throw new IllegalArgumentException("nameFontSize must be > 0");
            if (infoFontSize <= 0) throw new IllegalArgumentException("infoFontSize must be > 0");

            Map<Path, String> folderToPersonId = scanPdfFolders(basePath);
            if (folderToPersonId.isEmpty()) {
                System.out.println("No PDF files found under " + basePath.toAbsolutePath());
                return;
            }

            int successCount = 0;
            int errorCount = 0;
            for (Map.Entry<Path, String> e : folderToPersonId.entrySet()) {
                Path folder = e.getKey();
                String personId = e.getValue();
                String personFolderName = folder.getFileName() != null ? folder.getFileName().toString() : personId;
                String stationName = "";
                Path parent = folder.getParent();
                if (parent != null && parent.getFileName() != null) {
                    // Strip parenthetical suffix, e.g. "F-12 (Pflegegruppe 12)" → "F-12"
                    stationName = parent.getFileName().toString()
                            .replaceAll("\\s*\\(.*\\)\\s*$", "").trim();
                }
                Path outputPdf = folder.resolve(barcodeOutputFilename);
                System.out.println("Generating " + outputPdf + " (" + personId + ", " + perPage + " barcodes)");
                try {
                    generateBarcodePDF(personFolderName, stationName, personId, barcodeType, width, height, pageSize,
                            perPage, columns, rows, marginLeft, marginTop, marginRight, marginBottom,
                            labelWidth, labelHeight, nameFontSize, nameFontBold, infoFontSize, infoFontBold, outputPdf);
                    successCount++;
                } catch (Exception ex) {
                    System.err.println("  ERROR: " + ex.getMessage());
                    errorCount++;
                }
            }
            System.out.println("Done. Generated " + successCount + " Barcodes.pdf file(s)."
                    + (errorCount > 0 ? " " + errorCount + " error(s)." : ""));
            if (errorCount > 0) System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Scans basePath for PDF files, extracts person ID from each filename,
     * and returns a map: folder path -> person ID (one entry per folder; ID from filenames in that folder).
     * Inaccessible directories are skipped with a warning. If a folder contains PDFs with
     * different person IDs, a warning is printed and the last found ID is used.
     */
    private static Map<Path, String> scanPdfFolders(Path basePath) throws IOException {
        Map<Path, String> folderToPersonId = new LinkedHashMap<>();
        Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (isPdf(file)) {
                    String id = extractPersonIdFromFilename(file.getFileName().toString());
                    if (id != null) {
                        Path folder = file.getParent();
                        String existing = folderToPersonId.get(folder);
                        if (existing != null && !existing.equals(id)) {
                            System.err.println("Warning: folder " + folder.getFileName()
                                    + " contains PDFs with different IDs (" + existing + ", " + id + "), using " + id);
                        }
                        folderToPersonId.put(folder, id);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                System.err.println("Warning: cannot access " + file + " – " + exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });
        return folderToPersonId;
    }

    private static boolean isPdf(Path p) {
        String name = p.getFileName().toString();
        int i = name.lastIndexOf('.');
        return i > 0 && name.substring(i).equalsIgnoreCase(".pdf");
    }

    /**
     * Extracts person ID from filename like "Name-07.04.2011-250066138-Allgemeine BLA-OFFLINECOMMONDATA.pdf".
     * Returns the first 6–12 digit segment after a dash (the number between date and rest).
     */
    private static String extractPersonIdFromFilename(String filename) {
        String base = filename;
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        Matcher m = PERSON_ID_PATTERN.matcher(base);
        return m.find() ? m.group(1) : null;
    }

    private static void generateBarcodePDF(String personFolderName, String stationName, String number, String type,
            int width, int height, String pageSize, int perPage, int columns, int rows,
            float marginLeft, float marginTop, float marginRight, float marginBottom,
            float labelWidth, float labelHeight,
            float nameFontSize, boolean nameFontBold, float infoFontSize, boolean infoFontBold,
            Path outputPath) throws Exception {
        // Validate barcodeType and generate bytes before opening the document,
        // so invalid config causes a clean error without iText interference.
        byte[] barcodeBytes = generateBarcodeBytes(number, type, width, height);

        Rectangle pageRect = PageSize.getRectangle(pageSize);
        if (pageRect == null) {
            throw new IllegalArgumentException(
                "Invalid pageSize in config.json: '" + pageSize + "'. Valid values are e.g. A4, A5, LETTER.");
        }

        Document document = new Document(pageRect, marginLeft, marginRight, marginTop, marginBottom);
        boolean success = false;
        try (OutputStream out = Files.newOutputStream(outputPath)) {
            PdfWriter.getInstance(document, out);
            document.open();
            try {
                int cols = columns > 0 ? columns : (int) Math.ceil(Math.sqrt(perPage));
                int effectiveRows = rows > 0 ? rows : (int) Math.ceil((double) perPage / cols);
                PdfPTable table = new PdfPTable(cols);
                float maxTableWidth = pageRect.getWidth() - marginLeft - marginRight;
                float desiredTableWidth = (labelWidth > 0 && columns > 0) ? (labelWidth * cols) : maxTableWidth;
                float tableWidth = Math.min(desiredTableWidth, maxTableWidth);
                table.setTotalWidth(tableWidth);
                table.setLockedWidth(true);
                float[] colWidths = new float[cols];
                for (int i = 0; i < cols; i++) colWidths[i] = 1f;
                table.setWidths(colWidths);
                table.setSpacingBefore(0);
                table.setSpacingAfter(0);
                table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

                float cellWidth = tableWidth / cols;
                float cellFixedHeight = labelHeight > 0 ? labelHeight : 54f;
                float barcodeDisplayWidth = cellWidth - 4;
                // PDF display height is taken directly from barcodeHeight in config.json (in points)
                float barcodeDisplayHeight = (float) height;

                Font nameFont = new Font(Font.FontFamily.HELVETICA, nameFontSize, nameFontBold ? Font.BOLD : Font.NORMAL);
                Font infoFont = new Font(Font.FontFamily.HELVETICA, infoFontSize, infoFontBold ? Font.BOLD : Font.NORMAL);

                // Extract name and birth date from folder name once before the loop.
                // Try to detect birth date pattern (dd.MM.yyyy) at the end of the folder name,
                // e.g. "Müller, Annete 31.03.1955" or "Ben Fredj,Mariam-14.10.1999".
                String nameLine = personFolderName;
                String birthDate = "";
                Matcher birthMatcher = BIRTH_DATE_PATTERN.matcher(personFolderName);
                int birthStart = -1;
                while (birthMatcher.find()) {
                    birthStart = birthMatcher.start();
                    birthDate = birthMatcher.group(1);
                }
                if (birthStart >= 0) {
                    nameLine = personFolderName.substring(0, birthStart).replaceAll("[\\s-]+$", "");
                }

                // Build label text lines once for all cells
                String firstLine = nameLine + (birthDate.isEmpty() ? "" : " " + birthDate);
                String secondLine = number + (stationName.isEmpty() ? "" : " - " + stationName);

                for (int i = 0; i < perPage; i++) {
                    Image img = Image.getInstance(barcodeBytes);
                    img.scaleAbsolute(barcodeDisplayWidth, barcodeDisplayHeight);
                    PdfPCell cell = new PdfPCell();
                    cell.setBorder(Rectangle.NO_BORDER);
                    cell.setPaddingLeft(2);
                    cell.setPaddingRight(2);
                    cell.setPaddingTop(2);
                    cell.setPaddingBottom(1);
                    cell.setFixedHeight(cellFixedHeight);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cell.addElement(img);

                    Paragraph namePara = new Paragraph(firstLine, nameFont);
                    namePara.setAlignment(Element.ALIGN_CENTER);
                    namePara.setSpacingBefore(1);
                    cell.addElement(namePara);

                    Paragraph infoPara = new Paragraph(secondLine, infoFont);
                    infoPara.setAlignment(Element.ALIGN_CENTER);
                    infoPara.setSpacingBefore(0);
                    cell.addElement(infoPara);
                    table.addCell(cell);
                }
                int remainder = Math.max(0, (cols * effectiveRows) - perPage);
                for (int i = 0; i < remainder; i++) {
                    PdfPCell empty = new PdfPCell();
                    empty.setBorder(Rectangle.NO_BORDER);
                    empty.setFixedHeight(cellFixedHeight);
                    empty.setMinimumHeight(1);
                    table.addCell(empty);
                }

                document.add(table);
                success = true;
            } finally {
                document.close();
            }
        } finally {
            // Remove incomplete/corrupt output file if an error occurred after file creation
            if (!success) {
                Files.deleteIfExists(outputPath);
            }
        }
    }

    /** Generates a barcode as PNG bytes. Throws IllegalArgumentException for unknown barcodeType. */
    private static byte[] generateBarcodeBytes(String data, String type, int width, int height) throws WriterException, IOException {
        BarcodeFormat format;
        try {
            format = BarcodeFormat.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid barcodeType in config.json: '" + type + "'. Valid values are e.g. CODE_128, QR_CODE, EAN_13.");
        }
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix bitMatrix = writer.encode(data, format, width, height);
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (!ImageIO.write(bufferedImage, "PNG", baos)) {
            throw new IOException("ImageIO could not find a PNG writer – check that the Java ImageIO PNG plugin is available.");
        }
        return baos.toByteArray();
    }
}
