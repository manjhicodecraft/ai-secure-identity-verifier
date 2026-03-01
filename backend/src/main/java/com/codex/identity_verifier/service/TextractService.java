package com.codex.identity_verifier.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TextractService {

    private final TextractClient textractClient;
    @Value("${aws.textract.min-confidence:80.0}")
    private Double minConfidence;

    @Autowired
    public TextractService(TextractClient textractClient) {
        this.textractClient = textractClient;
    }

    /**
     * Extracts text from an image using Amazon Textract
     * @param imageData The image data as byte array
     * @return Map containing extracted text and analysis
     */
    public Map<String, Object> extractText(byte[] imageData) {
        DetectDocumentTextRequest request = DetectDocumentTextRequest.builder()
                .document(Document.builder()
                        .bytes(SdkBytes.fromByteArray(imageData))
                        .build())
                .build();

        DetectDocumentTextResponse response = textractClient.detectDocumentText(request);

        Map<String, Object> result = new HashMap<>();
        result.put("blocks", response.blocks());
        result.put("text", extractTextFromBlocks(response.blocks()));
        result.put("lines", extractLines(response.blocks()));
        result.put("words", extractWords(response.blocks()));
        
        return result;
    }

    /**
     * Analyzes a document using Amazon Textract for structured data extraction
     * @param imageData The image data as byte array
     * @return Map containing analyzed document data
     */
    public Map<String, Object> analyzeDocument(byte[] imageData) {
        AnalyzeDocumentRequest request = AnalyzeDocumentRequest.builder()
                .document(Document.builder()
                        .bytes(SdkBytes.fromByteArray(imageData))
                        .build())
                .featureTypes(FeatureType.TABLES, FeatureType.FORMS)
                .build();

        AnalyzeDocumentResponse response = textractClient.analyzeDocument(request);

        Map<String, Object> result = new HashMap<>();
        result.put("blocks", response.blocks());
        result.put("tables", extractTables(response.blocks()));
        result.put("forms", extractForms(response.blocks()));
        result.put("keyValuePairs", extractKeyValuePairs(response.blocks()));
        
        return result;
    }

    /**
     * Extracts structured identity information from document image
     * @param imageData The image data as byte array
     * @return Map containing extracted identity information
     */
    public Map<String, String> extractIdentityInformation(byte[] imageData) {
        Map<String, String> identityInfo = new HashMap<>();

        // First, extract general text
        Map<String, Object> textResult = extractText(imageData);
        List<String> lines = (List<String>) textResult.get("lines");

        // Look for common identity document fields
        for (int i = 0; i < lines.size(); i++) {
            String originalLine = lines.get(i).trim();
            String normalizedLine = originalLine.toLowerCase();
            
            // Extract name (look for common name patterns)
            if (normalizedLine.contains("name") && !identityInfo.containsKey("name")) {
                // Try to get the next line which might contain the name
                if (i + 1 < lines.size()) {
                    String nextLine = lines.get(i + 1);
                    if (isLikelyName(nextLine)) {
                        identityInfo.put("name", nextLine.trim());
                    }
                }
            }
            
            // Extract ID number (look for common ID patterns)
            if (isLikelyIdNumber(originalLine) && !identityInfo.containsKey("idNumber")) {
                identityInfo.put("idNumber", originalLine.toUpperCase());
            }
            
            // Extract date of birth (look for date patterns)
            if (isLikelyDate(originalLine) && !identityInfo.containsKey("dob")) {
                identityInfo.put("dob", originalLine);
            }
            
            // Extract expiry date
            if ((normalizedLine.contains("expiry") || normalizedLine.contains("expire") || normalizedLine.contains("valid")) && 
                isLikelyDate(originalLine) && !identityInfo.containsKey("expiryDate")) {
                identityInfo.put("expiryDate", originalLine);
            }
            
            // Extract address (if present)
            if (isLikelyAddress(originalLine) && !identityInfo.containsKey("address")) {
                identityInfo.put("address", originalLine);
            }
        }

        // If we couldn't extract from the structured analysis, try a more direct approach
        if (identityInfo.isEmpty()) {
            identityInfo = extractIdentityInformationDirect(lines);
        }

        return identityInfo;
    }

    /**
     * Helper method to extract text from blocks
     */
    private String extractTextFromBlocks(List<Block> blocks) {
        StringBuilder text = new StringBuilder();
        for (Block block : blocks) {
            if (block.blockType() == BlockType.LINE || block.blockType() == BlockType.WORD) {
                text.append(block.text()).append(" ");
            }
        }
        return text.toString().trim();
    }

    /**
     * Helper method to extract lines from blocks
     */
    private List<String> extractLines(List<Block> blocks) {
        List<String> lines = new ArrayList<>();
        for (Block block : blocks) {
            if (block.blockType() == BlockType.LINE && 
                block.confidence() >= minConfidence) {
                lines.add(block.text());
            }
        }
        return lines;
    }

    /**
     * Helper method to extract words from blocks
     */
    private List<String> extractWords(List<Block> blocks) {
        List<String> words = new ArrayList<>();
        for (Block block : blocks) {
            if (block.blockType() == BlockType.WORD && 
                block.confidence() >= minConfidence) {
                words.add(block.text());
            }
        }
        return words;
    }

    /**
     * Helper method to extract tables from blocks
     */
    private List<List<String>> extractTables(List<Block> blocks) {
        List<List<String>> tables = new ArrayList<>();
        // This is a simplified implementation - a full implementation would need to properly
        // parse the relationships between blocks to reconstruct tables
        return tables;
    }

    /**
     * Helper method to extract forms from blocks
     */
    private Map<String, String> extractForms(List<Block> blocks) {
        Map<String, String> forms = new HashMap<>();
        // This is a simplified implementation - a full implementation would need to properly
        // parse the relationships between key-value pairs
        return forms;
    }

    /**
     * Helper method to extract key-value pairs from blocks
     */
    private Map<String, String> extractKeyValuePairs(List<Block> blocks) {
        Map<String, String> keyValuePairs = new HashMap<>();
        
        Block currentKey = null;
        for (Block block : blocks) {
            if (block.blockType() == BlockType.KEY_VALUE_SET && 
                block.entityTypes().contains(EntityType.KEY)) {
                currentKey = block;
            } else if (block.blockType() == BlockType.KEY_VALUE_SET && 
                       block.entityTypes().contains(EntityType.VALUE) && 
                       currentKey != null) {
                // Extract the text for the key and value
                String keyText = extractTextForKey(currentKey, blocks);
                String valueText = extractTextForValue(block, blocks);
                
                if (keyText != null && valueText != null && 
                    block.confidence() >= minConfidence) {
                    keyValuePairs.put(keyText, valueText);
                }
                currentKey = null;
            }
        }
        
        return keyValuePairs;
    }

    /**
     * Helper method to extract text for a key block
     */
    private String extractTextForKey(Block keyBlock, List<Block> allBlocks) {
        // Find child blocks that contain the actual key text
        for (Block block : allBlocks) {
            if (block.id() != null && keyBlock.relationships() != null) {
                for (Relationship rel : keyBlock.relationships()) {
                    if (rel.type() == RelationshipType.CHILD && 
                        rel.ids().contains(block.id())) {
                        return block.text();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Helper method to extract text for a value block
     */
    private String extractTextForValue(Block valueBlock, List<Block> allBlocks) {
        // Find child blocks that contain the actual value text
        for (Block block : allBlocks) {
            if (block.id() != null && valueBlock.relationships() != null) {
                for (Relationship rel : valueBlock.relationships()) {
                    if (rel.type() == RelationshipType.CHILD && 
                        rel.ids().contains(block.id())) {
                        return block.text();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Helper method to check if a string looks like a name
     */
    private boolean isLikelyName(String text) {
        // Simple heuristic: names usually have letters and possibly spaces
        // and are typically 2-3 words long
        String[] words = text.trim().split("\\s+");
        if (words.length < 1 || words.length > 3) {
            return false;
        }
        
        for (String word : words) {
            if (!word.matches("[a-zA-Z]+")) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Helper method to check if a string looks like an ID number
     */
    private boolean isLikelyIdNumber(String text) {
        // Common patterns for ID numbers: alphanumeric sequences
        return text.matches(".*[A-Za-z].*[0-9].*|.*[0-9].*[A-Za-z].*") && 
               text.replaceAll("[^A-Za-z0-9]", "").length() > 4;
    }

    /**
     * Helper method to check if a string looks like a date
     */
    private boolean isLikelyDate(String text) {
        // Common date patterns: DD/MM/YYYY, MM/DD/YYYY, YYYY-MM-DD, etc.
        return text.matches("\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}") ||
               text.matches("\\d{2,4}[/-]\\d{1,2}[/-]\\d{1,2}");
    }

    /**
     * Helper method to check if a string looks like an address
     */
    private boolean isLikelyAddress(String text) {
        // Look for common address indicators
        String lowerText = text.toLowerCase();
        return lowerText.contains("street") || 
               lowerText.contains("st.") || 
               lowerText.contains("road") || 
               lowerText.contains("rd.") || 
               lowerText.contains("ave.") || 
               lowerText.contains("avenue") ||
               lowerText.contains("drive") ||
               lowerText.contains("dr.") ||
               lowerText.contains("lane") ||
               lowerText.contains("ln.") ||
               text.matches(".*\\d+.*"); // Contains numbers (house numbers)
    }

    /**
     * Direct extraction method for identity information
     */
    private Map<String, String> extractIdentityInformationDirect(List<String> lines) {
        Map<String, String> identityInfo = new HashMap<>();
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).toLowerCase().trim();
            
            // Look for common identity document keywords
            if (line.contains("name") || line.contains("surname") || line.contains("given name")) {
                if (i + 1 < lines.size()) {
                    String nextLine = lines.get(i + 1);
                    if (isLikelyName(nextLine)) {
                        identityInfo.put("name", nextLine.trim());
                    }
                }
            } else if (line.matches(".*\\b(id|identification|document)\\b.*") && 
                       isLikelyIdNumber(line.replaceAll(".*\\b(id|identification|document)\\b", "").trim())) {
                identityInfo.put("idNumber", line.replaceAll(".*\\b(id|identification|document)\\b", "").trim());
            } else if (isLikelyDate(line) && !identityInfo.containsKey("dob")) {
                identityInfo.put("dob", line);
            }
        }
        
        return identityInfo;
    }
}
