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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Medico_Ausfall_BarcodeGenerator {

    /** Pattern: segment that is only digits, 6–12 chars (person ID in filenames). */
    private static final Pattern PERSON_ID_PATTERN = Pattern.compile("-([0-9]{6,12})(?:-|$)");

    public static void main(String[] args) {
        try {
            String configPath = "config.json";
            if (args.length > 0) {
                configPath = args[0];
            }
            JSONObject config = new JSONObject(readFile(configPath));

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
            float marginLeft = (float) config.optDouble("marginLeft", 20);
            float marginTop = (float) config.optDouble("marginTop", 20);
            float marginRight = (float) config.optDouble("marginRight", 20);
            float marginBottom = (float) config.optDouble("marginBottom", 20);
            String barcodeOutputFilename = config.optString("barcodeOutputFilename", "Barcodes.pdf");

            Map<Path, String> folderToPersonId = scanPdfFolders(basePath);
            if (folderToPersonId.isEmpty()) {
                System.out.println("No PDF files found under " + basePath.toAbsolutePath());
                return;
            }

            for (Map.Entry<Path, String> e : folderToPersonId.entrySet()) {
                Path folder = e.getKey();
                String personId = e.getValue();
                String personFolderName = folder.getFileName() != null ? folder.getFileName().toString() : personId;
                Path outputPdf = folder.resolve(barcodeOutputFilename);
                System.out.println("Generating " + outputPdf + " (" + personId + ", " + perPage + " barcodes)");
                generateBarcodePDF(personFolderName, personId, barcodeType, width, height, pageSize, perPage,
                        marginLeft, marginTop, marginRight, marginBottom, outputPdf);
            }
            System.out.println("Done. Generated " + folderToPersonId.size() + " Barcodes.pdf file(s).");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Scans basePath for PDF files, extracts person ID from each filename,
     * and returns a map: folder path -> person ID (one entry per folder; ID from filenames in that folder).
     */
    private static Map<Path, String> scanPdfFolders(Path basePath) throws IOException {
        Map<Path, String> folderToPersonId = new LinkedHashMap<>();
        try (var stream = Files.walk(basePath)) {
            stream.filter(p -> Files.isRegularFile(p) && isPdf(p))
                    .forEach(pdfPath -> {
                        String id = extractPersonIdFromFilename(pdfPath.getFileName().toString());
                        if (id != null) {
                            Path folder = pdfPath.getParent();
                            folderToPersonId.put(folder, id);
                        }
                    });
        }
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

    private static void generateBarcodePDF(String personFolderName, String number, String type, int width, int height,
            String pageSize, int perPage, float marginLeft, float marginTop, float marginRight, float marginBottom,
            Path outputPath) throws Exception {
        Rectangle pageRect = PageSize.getRectangle(pageSize);
        Document document = new Document(pageRect, marginLeft, marginRight, marginTop, marginBottom);
        try (OutputStream out = Files.newOutputStream(outputPath)) {
            PdfWriter.getInstance(document, out);
            document.open();

            // Person folder name at the top
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
            Paragraph title = new Paragraph(personFolderName, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(12);
            document.add(title);

            int cols = (int) Math.ceil(Math.sqrt(perPage));
            int rows = (int) Math.ceil((double) perPage / cols);
            PdfPTable table = new PdfPTable(cols);
            float tableWidth = pageRect.getWidth() - marginLeft - marginRight;
            table.setTotalWidth(tableWidth);
            table.setLockedWidth(true);
            table.setWidthPercentage(100f);
            float[] colWidths = new float[cols];
            for (int i = 0; i < cols; i++) colWidths[i] = 1f;
            table.setWidths(colWidths);
            table.setSpacingBefore(0);
            table.setSpacingAfter(0);
            table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            float cellWidth = tableWidth / cols;
            float barcodeDisplayWidth = cellWidth - 8;
            float barcodeDisplayHeight = 36;
            float cellFixedHeight = barcodeDisplayHeight + 18;

            Font numberFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);

            for (int i = 0; i < perPage; i++) {
                BufferedImage barcodeImage = generateBarcode(number, type, width, height);
                Image img = imageFromBufferedImage(barcodeImage);
                img.scaleAbsolute(barcodeDisplayWidth, barcodeDisplayHeight);
                img.setAlignment(Element.ALIGN_CENTER);
                PdfPCell cell = new PdfPCell();
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setPadding(4);
                cell.setFixedHeight(cellFixedHeight);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.addElement(img);
                Paragraph numPara = new Paragraph(number, numberFont);
                numPara.setAlignment(Element.ALIGN_CENTER);
                numPara.setSpacingBefore(2);
                cell.addElement(numPara);
                table.addCell(cell);
            }
            int remainder = (cols * rows) - perPage;
            for (int i = 0; i < remainder; i++) {
                PdfPCell empty = new PdfPCell();
                empty.setBorder(Rectangle.NO_BORDER);
                empty.setFixedHeight(cellFixedHeight);
                empty.setMinimumHeight(1);
                table.addCell(empty);
            }

            document.add(table);
            document.close();
        }
    }

    private static Image imageFromBufferedImage(BufferedImage bufferedImage) throws IOException, BadElementException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "PNG", baos);
        return Image.getInstance(baos.toByteArray());
    }

    private static BufferedImage generateBarcode(String data, String type, int width, int height) throws WriterException {
        BarcodeFormat format;
        try {
            format = BarcodeFormat.valueOf(type);
        } catch (IllegalArgumentException e) {
            format = BarcodeFormat.CODE_128;
        }
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix bitMatrix = writer.encode(data, format, width, height);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    private static String readFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
