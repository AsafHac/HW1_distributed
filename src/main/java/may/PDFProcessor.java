package may;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class PDFProcessor {

    /**
     * Processes a single PDF URL with the given command.
     * 
     * @param command The command: ToImage, ToHTML, or ToText.
     * @param pdfUrl The URL of the PDF to process.
     * @throws IOException If an error occurs while reading or processing the PDF.
     */
    public static void processPDF(String command, String pdfUrl) throws IOException {
        // Download the PDF from the URL
        File tempFile = downloadPDF(pdfUrl);
        
        try (PDDocument document = PDDocument.load(tempFile)) {
            if (document.isEncrypted()) {
                System.out.println("Cannot process encrypted PDF: " + pdfUrl);
                return;
            }

            // Process the first page of the PDF
            switch (command) {
                case "ToImage":
                    convertToImage(document, pdfUrl);
                    break;
                case "ToHTML":
                    convertToHTML(document, pdfUrl);
                    break;
                case "ToText":
                    convertToText(document, pdfUrl);
                    break;
                default:
                    System.out.println("Unknown command: " + command);
            }
        } finally {
            // Clean up the temporary file
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Downloads a PDF from a URL and saves it to a temporary file.
     * 
     * @param pdfUrl The URL of the PDF to download.
     * @return A temporary File containing the downloaded PDF.
     * @throws IOException If an error occurs while downloading the file.
     */
    private static File downloadPDF(String pdfUrl) throws IOException {
        File tempFile = File.createTempFile("pdf", ".pdf");
         URLConnection connection = new URL(pdfUrl).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        try (InputStream in = new URL(pdfUrl).openStream();
             FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }

    /**
     * Converts the first page of the PDF to an image (PNG).
     * 
     * @param document The loaded PDDocument.
     * @param pdfUrl The original URL of the PDF.
     * @throws IOException If an error occurs during conversion.
     */
    private static void convertToImage(PDDocument document, String pdfUrl) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);
        BufferedImage image = renderer.renderImage(0); // Render the first page (index 0)

        // Save the image
        File outputFile = new File("output/" + sanitizeFilename(pdfUrl) + ".png");
        ImageIO.write(image, "PNG", outputFile);
        System.out.println("Saved image: " + outputFile.getAbsolutePath());
    }

    /**
     * Converts the first page of the PDF to HTML.
     * 
     * @param document The loaded PDDocument.
     * @param pdfUrl The original URL of the PDF.
     * @throws IOException If an error occurs during conversion.
     */
    private static void convertToHTML(PDDocument document, String pdfUrl) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(1);
        stripper.setEndPage(1);
        String text = stripper.getText(document);

        // Convert to basic HTML (you can enhance this formatting)
        String htmlContent = "<html><body><pre>" + text + "</pre></body></html>";

        // Save the HTML
        File outputFile = new File("output/" + sanitizeFilename(pdfUrl) + ".html");
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(htmlContent);
        }
        System.out.println("Saved HTML: " + outputFile.getAbsolutePath());
    }

    /**
     * Converts the first page of the PDF to plain text.
     * 
     * @param document The loaded PDDocument.
     * @param pdfUrl The original URL of the PDF.
     * @throws IOException If an error occurs during conversion.
     */
    private static void convertToText(PDDocument document, String pdfUrl) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(1);
        stripper.setEndPage(1);
        String text = stripper.getText(document);

        // Save the text
        File outputFile = new File("output/" + sanitizeFilename(pdfUrl) + ".txt");
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(text);
        }
        System.out.println("Saved text: " + outputFile.getAbsolutePath());
    }

    /**
     * Sanitizes a filename by removing illegal characters.
     * 
     * @param url The URL to sanitize.
     * @return A sanitized filename.
     */
    private static String sanitizeFilename(String url) {
        return url.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
    }

    public static void main(String[] args) {
        // Example usage
        try {
            processPDF("ToImage", "http://www.jewishfederations.org/local_includes/downloads/39497.pdf");
            processPDF("ToHTML", "http://www.st.tees.org.uk/assets/Downloads/Passover-service.pdf");
            processPDF("ToText", "http://www.chabad.org/media/pdf/42/kUgi423322.pdf");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
