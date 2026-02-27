package com.codex.identity_verifier.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class RekognitionService {

    private final RekognitionClient rekognitionClient;
    @Value("${aws.rekognition.collection-id:identity-docs}")
    private String collectionId;
    @Value("${aws.rekognition.face-match-threshold:80.0}")
    private Double faceMatchThreshold;
    @Value("${aws.rekognition.min-confidence:70.0}")
    private Double minConfidence;

    @Autowired
    public RekognitionService(RekognitionClient rekognitionClient) {
        this.rekognitionClient = rekognitionClient;
    }

    /**
     * Detects faces in an image
     * @param imageData The image data as byte array
     * @return Map containing face detection results
     */
    public Map<String, Object> detectFaces(byte[] imageData) {
        Image image = Image.builder()
                .bytes(SdkBytes.fromByteArray(imageData))
                .build();

        DetectFacesRequest request = DetectFacesRequest.builder()
                .image(image)
                .attributes(Attribute.ALL)
                .build();

        DetectFacesResponse response = rekognitionClient.detectFaces(request);
        
        Map<String, Object> result = new HashMap<>();
        result.put("faceCount", response.faceDetails().size());
        result.put("faces", response.faceDetails());
        result.put("isFaceDetected", !response.faceDetails().isEmpty());
        
        return result;
    }

    /**
     * Compares two faces to determine if they match
     * @param sourceImageData The source image data
     * @param targetImageData The target image data
     * @return Map containing face comparison results
     */
    public Map<String, Object> compareFaces(byte[] sourceImageData, byte[] targetImageData) {
        Image sourceImage = Image.builder()
                .bytes(SdkBytes.fromByteArray(sourceImageData))
                .build();
        
        Image targetImage = Image.builder()
                .bytes(SdkBytes.fromByteArray(targetImageData))
                .build();

        CompareFacesRequest request = CompareFacesRequest.builder()
                .sourceImage(sourceImage)
                .targetImage(targetImage)
                .similarityThreshold(faceMatchThreshold.floatValue())
                .build();

        CompareFacesResponse response = rekognitionClient.compareFaces(request);
        
        Map<String, Object> result = new HashMap<>();
        result.put("faceMatches", response.faceMatches());
        result.put("unmatchedFaces", response.unmatchedFaces());
        result.put("isMatch", !response.faceMatches().isEmpty());
        result.put("highestConfidence", response.faceMatches().stream()
                .mapToDouble(match -> match.similarity())
                .max()
                .orElse(0.0));
        
        return result;
    }

    /**
     * Analyzes an image for potential tampering or forgery indicators
     * @param imageData The image data to analyze
     * @return Map containing tamper detection results
     */
    public Map<String, Object> detectImageTampering(byte[] imageData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Convert image to buffered image to check for basic properties
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            
            // Check image properties that might indicate tampering
            result.put("width", image.getWidth());
            result.put("height", image.getHeight());
            result.put("colorModel", image.getColorModel().toString());
            
            // Check for common tampering indicators
            result.put("isLowQuality", image.getWidth() < 300 || image.getHeight() < 300);
            result.put("hasUniformBackground", checkUniformBackground(image));
            
            // For actual tampering detection, we'll use Rekognition's moderation detection
            Image rekImage = Image.builder()
                    .bytes(SdkBytes.fromByteArray(imageData))
                    .build();

            DetectModerationLabelsRequest moderationRequest = DetectModerationLabelsRequest.builder()
                    .image(rekImage)
                    .minConfidence(minConfidence.floatValue())
                    .build();

            DetectModerationLabelsResponse moderationResponse = rekognitionClient.detectModerationLabels(moderationRequest);
            
            result.put("moderationLabels", moderationResponse.moderationLabels());
            result.put("hasSuspiciousContent", !moderationResponse.moderationLabels().isEmpty());
            
            // Check for text detection which might indicate tampering
            DetectTextRequest textRequest = DetectTextRequest.builder()
                    .image(rekImage)
                    .build();

            DetectTextResponse textResponse = rekognitionClient.detectText(textRequest);
            result.put("detectedTexts", textResponse.textDetections());
            
        } catch (IOException e) {
            result.put("error", "Failed to analyze image: " + e.getMessage());
        }
        
        // Assume tampering if suspicious content is detected
        result.put("isTampered", result.get("hasSuspiciousContent").equals(true));
        
        return result;
    }

    /**
     * Helper method to check for uniform background which might indicate image manipulation
     */
    private boolean checkUniformBackground(BufferedImage image) {
        // This is a simplified check - in reality, you'd want more sophisticated analysis
        // For now, we'll just check corner pixels for similarity
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        if (width < 10 || height < 10) {
            return false; // Too small to analyze
        }
        
        // Get corner pixels
        int topLeft = image.getRGB(5, 5);
        int topRight = image.getRGB(width - 6, 5);
        int bottomLeft = image.getRGB(5, height - 6);
        int bottomRight = image.getRGB(width - 6, height - 6);
        
        // Check if corners are similar (indicating possible uniform background)
        return Math.abs(topLeft - topRight) < 10000 && 
               Math.abs(topLeft - bottomLeft) < 10000 && 
               Math.abs(topLeft - bottomRight) < 10000;
    }

    /**
     * Analyzes image quality
     * @param imageData The image data to analyze
     * @return Map containing image quality metrics
     */
    public Map<String, Object> analyzeImageQuality(byte[] imageData) {
        Image image = Image.builder()
                .bytes(SdkBytes.fromByteArray(imageData))
                .build();

        DetectLabelsRequest request = DetectLabelsRequest.builder()
                .image(image)
                .maxLabels(10)
                .minConfidence(minConfidence.floatValue())
                .build();

        DetectLabelsResponse response = rekognitionClient.detectLabels(request);
        
        Map<String, Object> result = new HashMap<>();
        result.put("labels", response.labels());
        result.put("qualityIndicators", analyzeForQualityIndicators(response.labels()));
        
        return result;
    }
    
    /**
     * Helper method to analyze labels for quality indicators
     */
    private Map<String, Boolean> analyzeForQualityIndicators(java.util.List<Label> labels) {
        Map<String, Boolean> indicators = new HashMap<>();
        
        boolean hasBlur = labels.stream()
                .anyMatch(label -> label.name().toLowerCase().contains("blur") || 
                                 label.name().toLowerCase().contains("out of focus"));
        
        boolean hasGoodLighting = labels.stream()
                .anyMatch(label -> label.name().toLowerCase().contains("light") || 
                                 label.name().toLowerCase().contains("bright"));
        
        boolean hasDocument = labels.stream()
                .anyMatch(label -> label.name().toLowerCase().contains("document") ||
                                 label.name().toLowerCase().contains("paper") ||
                                 label.name().toLowerCase().contains("form"));
        
        indicators.put("isBlurry", hasBlur);
        indicators.put("hasGoodLighting", hasGoodLighting);
        indicators.put("isDocument", hasDocument);
        
        return indicators;
    }
}