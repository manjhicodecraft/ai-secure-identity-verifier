package com.codex.identity_verifier.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.UUID;

@Service
public class S3Service {

    private final S3Client s3Client;
    @Value("${aws.s3.bucket-name:ai-identity-verifier-docs}")
    private String bucketName;

    @Autowired
    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Uploads a file to S3 bucket
     * @param file The multipart file to upload
     * @return The S3 key of the uploaded file
     */
    public String uploadFile(MultipartFile file) throws IOException {
        String key = generateUniqueKey(file.getOriginalFilename());

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        return key;
    }

    /**
     * Downloads a file from S3 bucket
     * @param key The S3 key of the file to download
     * @return The downloaded file as byte array
     */
    public byte[] downloadFile(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
    }

    /**
     * Deletes a file from S3 bucket
     * @param key The S3 key of the file to delete
     */
    public void deleteFile(String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    /**
     * Generates a unique key for the uploaded file
     * @param originalFilename Original filename of the file
     * @return Unique S3 key
     */
    private String generateUniqueKey(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        return "uploads/" + UUID.randomUUID().toString() + extension;
    }

    /**
     * Checks if a file exists in the S3 bucket
     * @param key The S3 key to check
     * @return True if the file exists, false otherwise
     */
    public boolean fileExists(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            s3Client.headObject(headObjectRequest);
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }
    
    public String getBucketName() {
        return bucketName;
    }
}