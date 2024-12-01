package may;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PDFProcessorTest {
    
    private static final String TEST_PDF_URL = "http://www.barrylou.com/books/TellingTheStoryInside.pdf";
    
    @BeforeAll
    static void setUp() {
        // Create output directory if it doesn't exist
        new File("output").mkdirs();
    }
    
    @Test
    void testToImageConversion() {
        try {
            PDFProcessor.processPDF("ToImage", TEST_PDF_URL);
            
            // Check if the output file exists
            File outputFile = new File(System.getProperty("user.dir") , "output.png");
            assertTrue(outputFile.exists(), "Image output file should exist");
            assertTrue(outputFile.length() > 0, "Image file should not be empty");
            
         /* */   // Clean up
            //outputFile.delete();
        } 
        catch (IOException e) {
            fail("Should not throw exception: " + e.getMessage());
        } 
    }
    
    @Test
    void testToHTMLConversion() {
        try {
            PDFProcessor.processPDF("ToHTML", TEST_PDF_URL);
            
            // Check if the output file exists
            File outputFile = new File("output/" + TEST_PDF_URL.replaceAll("[^a-zA-Z0-9\\.\\-]", "_") + ".html");
            assertTrue(outputFile.exists(), "HTML output file should exist");
            assertTrue(outputFile.length() > 0, "HTML file should not be empty");
            
            // Verify HTML content
            String content = Files.readString(outputFile.toPath());
            assertTrue(content.startsWith("<html>"), "File should start with HTML tag");
            assertTrue(content.endsWith("</html>"), "File should end with closing HTML tag");
            
            // Clean up
           // outputFile.delete();
        } catch (IOException e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }
    
    @Test
    void testToTextConversion() {
        try {
            PDFProcessor.processPDF("ToText", TEST_PDF_URL);
            
            // Check if the output file exists
            File outputFile = new File("output/" + TEST_PDF_URL.replaceAll("[^a-zA-Z0-9\\.\\-]", "_") + ".txt");
            assertTrue(outputFile.exists(), "Text output file should exist");
            assertTrue(outputFile.length() > 0, "Text file should not be empty");
            
            // Verify content
            String content = Files.readString(outputFile.toPath());
            assertNotNull(content, "Content should not be null");
            assertTrue(content.length() > 0, "Content should not be empty");
            
            // Clean up
           // outputFile.delete();
        } catch (IOException e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }
    
    /*@Test
    void testAllConversionsSequentially() {
        try {
            // Test all conversions in sequence
            PDFProcessor.processPDF("ToImage", TEST_PDF_URL);
            PDFProcessor.processPDF("ToHTML", TEST_PDF_URL);
            PDFProcessor.processPDF("ToText", TEST_PDF_URL);
            
            // Verify all files were created
            String basePath = "output/" + TEST_PDF_URL.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
            assertTrue(new File(basePath + ".png").exists(), "PNG file should exist");
            assertTrue(new File(basePath + ".html").exists(), "HTML file should exist");
            assertTrue(new File(basePath + ".txt").exists(), "Text file should exist");
            
            // Clean up
            new File(basePath + ".png").delete();
            new File(basePath + ".html").delete();
            new File(basePath + ".txt").delete();
        } catch (IOException e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    } */
}
