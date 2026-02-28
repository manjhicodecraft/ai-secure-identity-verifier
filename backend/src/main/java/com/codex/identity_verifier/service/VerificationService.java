package com.codex.identity_verifier.service;

import com.codex.identity_verifier.dto.VerificationResponse;
import com.codex.identity_verifier.model.VerificationRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class VerificationService {

    private final S3Service s3Service;
    private final RekognitionService rekognitionService;
    private final TextractService textractService;
    private final DynamoDBService dynamoDBService;

    @Autowired
    public VerificationService(S3Service s3Service, RekognitionService rekognitionService, 
                              TextractService textractService, DynamoDBService dynamoDBService) {
        this.s3Service = s3Service;
        this.rekognitionService = rekognitionService;
        this.textractService = textractService;
        this.dynamoDBService = dynamoDBService;
    }

    /**
     * Performs comprehensive document verification using AWS services
     * @param file The document file to verify
     * @return VerificationResponse containing the verification results
     */
    public VerificationResponse verifyDocument(MultipartFile file) {
        try {
            // 1. Upload file to S3
            String s3Key = s3Service.uploadFile(file);
            
            // 2. Download the file from S3 to process locally (in a real scenario, you'd use S3 URI directly)
            byte[] imageData = s3Service.downloadFile(s3Key);
            
            // 3. Perform image analysis with Rekognition
            Map<String, Object> faceDetectionResult = rekognitionService.detectFaces(imageData);
            Map<String, Object> tamperDetectionResult = rekognitionService.detectImageTampering(imageData);
            Map<String, Object> qualityAnalysisResult = rekognitionService.analyzeImageQuality(imageData);
            
            // 4. Extract text from document using Textract
            Map<String, String> identityInfo = textractService.extractIdentityInformation(imageData);
            
            // 5. Calculate risk score based on multiple factors
            int riskScore = calculateRiskScore(faceDetectionResult, tamperDetectionResult, qualityAnalysisResult, identityInfo);
            String riskLevel = determineRiskLevel(riskScore);
            
            // 6. Generate explanations based on analysis
            List<String> explanations = generateExplanations(faceDetectionResult, tamperDetectionResult, 
                                                           qualityAnalysisResult, identityInfo, riskScore);
            
            // 7. Create verification record
            VerificationRecord verificationRecord = createVerificationRecord(
                file.getOriginalFilename(), s3Key, riskLevel, riskScore, 
                explanations.toArray(new String[0]), identityInfo, 
                (Double) faceDetectionResult.get("highestConfidence"), 
                (Boolean) tamperDetectionResult.get("isTampered")
            );
            
            // 8. Save record to DynamoDB
            dynamoDBService.saveVerificationRecord(verificationRecord);
            
            // 9. Build and return response
            return VerificationResponse.builder()
                    .riskLevel(riskLevel)
                    .riskScore(riskScore)
                    .explanation(explanations)
                    .extractedData(VerificationResponse.ExtractedData.builder()
                            .name(identityInfo.get("name"))
                            .idNumber(identityInfo.get("idNumber"))
                            .dob(identityInfo.get("dob"))
                            .build())
                    .build();
                    
        } catch (Exception e) {
            // Log the full stack trace for debugging AWS-related issues
            System.err.println("Verification failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Verification failed: " + e.getMessage(), e);
        }
    }

    /**
     * Calculates the overall risk score based on multiple analysis factors
     */
    private int calculateRiskScore(Map<String, Object> faceDetectionResult, 
                                  Map<String, Object> tamperDetectionResult,
                                  Map<String, Object> qualityAnalysisResult,
                                  Map<String, String> identityInfo) {
        int baseScore = 0;
        
        // Factor 1: Face detection analysis
        if (!(Boolean) faceDetectionResult.getOrDefault("isFaceDetected", false)) {
            baseScore += 35; // High risk if no face detected in ID photo
        } else {
            // Check face quality if face is detected
            Integer faceCount = (Integer) faceDetectionResult.get("faceCount");
            if (faceCount != null && faceCount > 1) {
                baseScore += 15; // Multiple faces may indicate document issues
            }
        }
        
        // Factor 2: Tampering detection (highest weight)
        if ((Boolean) tamperDetectionResult.getOrDefault("isTampered", false)) {
            baseScore += 60; // Very high risk if tampering detected
        }
        
        // Factor 3: Image quality analysis
        @SuppressWarnings("unchecked")
        Map<String, Boolean> qualityIndicators = (Map<String, Boolean>) qualityAnalysisResult.getOrDefault("qualityIndicators", Map.of());
        if (qualityIndicators.getOrDefault("isBlurry", false)) {
            baseScore += 25; // Blur significantly increases risk
        }
        
        if (!qualityIndicators.getOrDefault("hasGoodLighting", false)) {
            baseScore += 10; // Poor lighting affects analysis accuracy
        }
        
        if (qualityIndicators.getOrDefault("isDocument", false)) {
            baseScore -= 15; // Document type decreases risk
        }
        
        // Factor 4: Identity information completeness
        int missingFields = 0;
        if (identityInfo.get("name") == null || identityInfo.get("name").isEmpty()) {
            missingFields++;
            baseScore += 20; // Name is critical
        }
        if (identityInfo.get("idNumber") == null || identityInfo.get("idNumber").isEmpty()) {
            missingFields++;
            baseScore += 25; // ID number is very critical
        }
        if (identityInfo.get("dob") == null || identityInfo.get("dob").isEmpty()) {
            missingFields++;
            baseScore += 10; // DOB is important
        }
        
        // Bonus for complete information
        if (missingFields == 0) {
            baseScore -= 20; // All fields present
        }
        
        // Factor 5: Suspicious content detection
        if ((Boolean) tamperDetectionResult.getOrDefault("hasSuspiciousContent", false)) {
            baseScore += 30;
        }
        
        // Factor 6: Image dimensions (too small/large can be suspicious)
        Integer width = (Integer) tamperDetectionResult.get("width");
        Integer height = (Integer) tamperDetectionResult.get("height");
        if (width != null && height != null) {
            int totalPixels = width * height;
            if (totalPixels < 100000) { // Very low resolution
                baseScore += 20;
            } else if (totalPixels > 20000000) { // Extremely high resolution (suspicious)
                baseScore += 10;
            }
        }
        
        // Ensure score stays within bounds
        return Math.min(100, Math.max(0, baseScore));
    }

    /**
     * Determines the risk level based on the risk score
     */
    private String determineRiskLevel(int riskScore) {
        if (riskScore < 30) {
            return "LOW RISK";
        } else if (riskScore < 70) {
            return "MEDIUM RISK";
        } else {
            return "HIGH RISK";
        }
    }

    /**
     * Generates explanations based on the analysis results
     */
    private List<String> generateExplanations(Map<String, Object> faceDetectionResult,
                                             Map<String, Object> tamperDetectionResult,
                                             Map<String, Object> qualityAnalysisResult,
                                             Map<String, String> identityInfo,
                                             int riskScore) {
        List<String> explanations = new ArrayList<>();
        
        // Face detection results
        Integer faceCount = (Integer) faceDetectionResult.get("faceCount");
        if ((Boolean) faceDetectionResult.getOrDefault("isFaceDetected", false)) {
            if (faceCount != null && faceCount > 1) {
                explanations.add("Face detection: Multiple faces (" + faceCount + ") detected - may indicate document issues");
            } else {
                explanations.add("Face detection: Single face detected - positive indicator");
            }
        } else {
            explanations.add("WARNING: No face detected in document - potential fraud indicator");
        }
        
        // Tampering results
        if ((Boolean) tamperDetectionResult.getOrDefault("isTampered", false)) {
            explanations.add("ALERT: Potential tampering detected in document");
        } else {
            explanations.add("Document integrity check: No obvious signs of tampering detected");
        }
        
        // Quality results
        @SuppressWarnings("unchecked")
        Map<String, Boolean> qualityIndicators = (Map<String, Boolean>) qualityAnalysisResult.getOrDefault("qualityIndicators", Map.of());
        if (qualityIndicators.getOrDefault("isBlurry", false)) {
            explanations.add("Image quality assessment: Low quality detected - may affect analysis accuracy");
        } else {
            explanations.add("Image quality assessment: Good quality for analysis");
        }
        
        if (!qualityIndicators.getOrDefault("hasGoodLighting", false)) {
            explanations.add("Lighting conditions: Poor lighting detected - may impact OCR accuracy");
        }
        
        // Identity info extraction
        if (identityInfo.get("name") != null && !identityInfo.get("name").isEmpty()) {
            explanations.add("Name extracted: " + identityInfo.get("name"));
        } else {
            explanations.add("Name extraction: Failed to extract name from document - critical information missing");
        }
        
        if (identityInfo.get("idNumber") != null && !identityInfo.get("idNumber").isEmpty()) {
            explanations.add("ID number extracted: " + identityInfo.get("idNumber"));
        } else {
            explanations.add("ID number extraction: Failed to extract ID number from document - critical information missing");
        }
        
        if (identityInfo.get("dob") != null && !identityInfo.get("dob").isEmpty()) {
            explanations.add("Date of birth extracted: " + identityInfo.get("dob"));
        } else {
            explanations.add("Date of birth extraction: Failed to extract DOB from document");
        }
        
        // Risk assessment
        if (riskScore >= 80) {
            explanations.add("Final risk assessment: HIGH RISK - Strong indicators of potential fraud detected");
            explanations.add("RECOMMENDATION: Immediate manual review and additional verification required");
        } else if (riskScore >= 60) {
            explanations.add("Final risk assessment: HIGH-MEDIUM RISK - Significant concerns detected");
            explanations.add("RECOMMENDATION: Enhanced verification procedures recommended");
        } else if (riskScore >= 40) {
            explanations.add("Final risk assessment: MEDIUM RISK - Moderate concerns identified");
            explanations.add("RECOMMENDATION: Additional verification may be needed");
        } else if (riskScore >= 20) {
            explanations.add("Final risk assessment: LOW-MEDIUM RISK - Minor concerns identified");
            explanations.add("RECOMMENDATION: Standard verification procedures sufficient");
        } else {
            explanations.add("Final risk assessment: LOW RISK - Document appears authentic and complete");
            explanations.add("RECOMMENDATION: Standard processing approved");
        }
        
        return explanations;
    }

    /**
     * Creates a verification record for storage in DynamoDB
     */
    private VerificationRecord createVerificationRecord(
        String fileName,
        String s3Key,
        String riskLevel,
        int riskScore,
        String[] explanations,
        Map<String, String> identityInfo,
        Double faceMatchConfidence,
        Boolean isTampered) {

    VerificationRecord.ExtractedData extractedData =
            VerificationRecord.ExtractedData.builder()
                    .name(identityInfo.get("name"))
                    .idNumber(identityInfo.get("idNumber"))
                    .dob(identityInfo.get("dob"))
                    .address(identityInfo.get("address"))
                    .expiryDate(identityInfo.get("expiryDate"))
                    .build();

    // Convert String[] → List<String>
    List<String> explanationList = Arrays.asList(explanations);

    return VerificationRecord.builder()
            .fileName(fileName)
            .s3Key(s3Key)
            .s3Bucket(s3Service.getBucketName()) // Use actual bucket name from S3Service
            .riskLevel(riskLevel)
            .riskScore(riskScore)
            .explanation(explanationList)
            .extractedData(extractedData)
            .faceMatchConfidence(faceMatchConfidence)
            .isTampered(isTampered)
            .build();
}

    /**
     * Generates SHA-256 hash of the file content
     */
    private String generateSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}