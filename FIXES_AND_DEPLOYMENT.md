# AI Secure Identity Verifier - Fixes and Deployment Guide

## Issues Fixed

### 1. Express Route Error
**Problem**: The frontend server crashed with the error `PathError [TypeError]: Missing parameter name at index 6: /api/*` due to invalid route pattern in the Express router configuration.

**Solution**: Fixed the invalid route pattern in `frontend/server/static.ts`:
- Changed `app.use("/{*path}", ...)` to `app.use("*", ...)`
- This resolves the path-to-regexp compatibility issue

### 2. Backend 500 Internal Server Error
**Problems Identified and Fixed**:
- Missing detailed error logging in the verification process
- Improper bucket name reference in the verification service
- Hardcoded bucket name in verification record creation

**Solutions Applied**:
1. **Enhanced Error Logging**: Added comprehensive logging to `VerificationController.java`:
   - Added SLF4J logger with proper imports
   - Added detailed log messages for request start/end
   - Added full stack trace logging in exception handlers

2. **Fixed Bucket Name Issue**: Updated `VerificationService.java`:
   - Removed hardcoded bucket name reference `s3Service.getClass().getSimpleName()`
   - Added proper bucket name retrieval via `s3Service.getBucketName()`
   - Added getter method in `S3Service.java` to expose the bucket name

3. **Improved Exception Handling**: Enhanced error reporting in `VerificationService.java`:
   - Added detailed stack trace printing for AWS-related errors
   - Better error propagation for debugging

## Files Modified

### Frontend
- `frontend/server/static.ts` - Fixed Express route pattern
- `frontend/server/routes.ts` - Updated API proxy configuration

### Backend
- `backend/src/main/java/com/codex/identity_verifier/controller/VerificationController.java` - Enhanced logging and error handling
- `backend/src/main/java/com\codex\identity_verifier/service/VerificationService.java` - Fixed bucket name reference and improved error handling
- `backend/src/main/java/com/codex\identity_verifier/service/S3Service.java` - Added bucket name getter method

## Configuration Requirements

### Environment Variables
Ensure the following environment variables are set before running the application:

```bash
# AWS Credentials (required)
export AWS_ACCESS_KEY_ID=your_access_key_here
export AWS_SECRET_ACCESS_KEY=your_secret_key_here
export AWS_REGION=us-east-1

# S3 Configuration (required)
export AWS_S3_BUCKET_NAME=your_bucket_name_here
export AWS_S3_BUCKET_REGION=us-east-1

# DynamoDB Configuration (required)
export AWS_DYNAMODB_TABLE_NAME=your_table_name_here
export AWS_DYNAMODB_REGION=us-east-1

# Rekognition Configuration (optional, defaults provided)
export AWS_REKOGNITION_REGION=us-east-1
export AWS_TEXTRACT_REGION=us-east-1
```

### Alternative: .env file
Create a `.env` file in the backend directory with the above variables.

## Deployment Instructions

### Option 1: Using Provided Scripts

#### Windows
```bash
# For development
.\run-dev.bat

# For production deployment
.\deploy-backend.bat
```

#### Linux/Mac
```bash
# For development
./run-dev.sh

# For production deployment
./deploy-backend.sh
```

### Option 2: Manual Deployment

#### Backend
```bash
cd backend
./mvnw clean package -DskipTests
java -jar target/identity-verifier-0.0.1-SNAPSHOT.jar --spring.profiles.active=aws
```

#### Frontend
```bash
cd frontend
npm install
npm run build
npm start
```

## Verification Steps

1. **Start the backend server** and ensure it connects to AWS services
2. **Check logs** for successful AWS service connections
3. **Test the health endpoint**: `GET /api/health`
4. **Upload a test document** to `/api/verify`
5. **Verify response** contains proper verification results
6. **Check AWS services** (S3, DynamoDB, Rekognition, Textract) for proper usage

## AWS Services Configuration

Ensure the following AWS services are properly configured:

1. **S3 Bucket**: Created with appropriate name and region
2. **DynamoDB Table**: Created with appropriate name and schema
3. **IAM Permissions**: Proper permissions for S3, Rekognition, Textract, and DynamoDB
4. **EC2 Instance**: If running on EC2, ensure IAM role has appropriate permissions

## Troubleshooting

### Common Issues

1. **500 Internal Server Error**: Check application logs for detailed error messages
2. **AWS Service Connection Issues**: Verify credentials and permissions
3. **S3 Upload Failures**: Check bucket name and region configuration
4. **DynamoDB Connection Issues**: Verify table name and region configuration

### Debugging Tips

1. Enable debug logging by adding `-Dlogging.level.com.codex.identity_verifier=DEBUG` to the JVM arguments
2. Check AWS CloudTrail logs for permission issues
3. Verify AWS service quotas have not been exceeded
4. Ensure proper network connectivity to AWS services

## Security Considerations

- Use IAM roles instead of access keys when running on AWS infrastructure
- Enable S3 server-side encryption
- Use DynamoDB encryption at rest
- Implement proper authentication for API endpoints
- Regularly rotate AWS credentials