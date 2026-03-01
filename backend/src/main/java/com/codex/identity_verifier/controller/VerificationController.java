package com.codex.identity_verifier.controller;

import com.codex.identity_verifier.dto.VerificationResponse;
import com.codex.identity_verifier.model.VerificationRecord;
import com.codex.identity_verifier.service.VerificationService;
import com.codex.identity_verifier.service.DynamoDBService;
import com.codex.identity_verifier.util.InputValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class VerificationController {

    @Autowired
    private VerificationService verificationService;
    
    @Autowired
    private DynamoDBService dynamoDBService;
    
    private static final Logger log = LoggerFactory.getLogger(VerificationController.class);

    @PostMapping("/verify")
    public ResponseEntity<VerificationResponse> verifyDocument(
            @RequestParam("file") MultipartFile file) {
        
        try {
            // Validate file using input validator
            InputValidator.ValidationResult fileValidation = InputValidator.validateFile(file);
            if (!fileValidation.isValid()) {
                log.warn("File validation failed: {}", fileValidation.getErrorMessage());
                return ResponseEntity.badRequest()
                    .body(VerificationResponse.builder()
                        .riskLevel("ERROR")
                        .riskScore(100)
                        .explanation(List.of(fileValidation.getErrorMessage()))
                        .extractedData(VerificationResponse.ExtractedData.builder()
                            .name("")
                            .idNumber("")
                            .dob("")
                            .build())
                        .build());
            }

            log.info("Starting verification for file: {} with size: {} bytes", 
                     file.getOriginalFilename(), file.getSize());

            // Process the file using the service
            VerificationResponse response = verificationService.verifyDocument(file);
            log.info("Verification completed successfully for file: {}", file.getOriginalFilename());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Verification failed", e); // Log full stack trace
            return ResponseEntity.internalServerError()
                .body(VerificationResponse.builder()
                    .riskLevel("ERROR")
                    .riskScore(100)
                    .explanation(List.of("Verification failed: " + e.getMessage()))
                    .extractedData(VerificationResponse.ExtractedData.builder()
                        .name("")
                        .idNumber("")
                        .dob("")
                        .build())
                    .build());
        }
    }

    @GetMapping("/verifications")
    public ResponseEntity<List<VerificationRecord>> getAllVerifications(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        try {
            List<VerificationRecord> records = dynamoDBService.getRecentVerifications(limit);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/verifications/{id}")
    public ResponseEntity<VerificationRecord> getVerificationById(@PathVariable String id) {
        try {
            VerificationRecord record = dynamoDBService.getVerificationRecordById(id);
            if (record != null) {
                return ResponseEntity.ok(record);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getVerificationStats() {
        try {
            List<VerificationRecord> records = dynamoDBService.getRecentVerifications(1000);
            
            long totalVerifications = records.size();
            long lowRisk = records.stream().filter(r -> "LOW RISK".equals(r.getRiskLevel())).count();
            long mediumRisk = records.stream().filter(r -> "MEDIUM RISK".equals(r.getRiskLevel())).count();
            long highRisk = records.stream().filter(r -> "HIGH RISK".equals(r.getRiskLevel())).count();
            
            double avgRiskScore = records.stream()
                .mapToInt(VerificationRecord::getRiskScore)
                .average()
                .orElse(0.0);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalVerifications", totalVerifications);
            stats.put("lowRiskCount", lowRisk);
            stats.put("mediumRiskCount", mediumRisk);
            stats.put("highRiskCount", highRisk);
            stats.put("averageRiskScore", Math.round(avgRiskScore * 100.0) / 100.0);
            stats.put("riskDistribution", Map.of(
                "low", lowRisk,
                "medium", mediumRisk,
                "high", highRisk
            ));
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
