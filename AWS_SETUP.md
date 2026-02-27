# AWS Deployment Setup Guide

This guide provides instructions for deploying the AI Secure Identity Verifier application on AWS infrastructure.

## Required AWS Services

### Core Services
- **Amazon EC2** - Host the backend application
- **Amazon S3** - Store uploaded identity documents temporarily
- **Amazon DynamoDB** - Store verification records
- **Amazon Rekognition** - Perform face verification and image analysis
- **Amazon Textract** - Extract text from identity documents
- **AWS IAM** - Manage permissions and access control

### Optional Services (Recommended)
- **Application Load Balancer** - Distribute traffic
- **Amazon CloudWatch** - Monitor application performance
- **AWS WAF** - Protect against common web exploits
- **Amazon Route 53** - DNS management
- **AWS Certificate Manager** - SSL/TLS certificates

## AWS Account Setup

### Prerequisites
1. An AWS account with administrator access
2. AWS CLI installed and configured
3. IAM user with necessary permissions

### IAM Permissions
Create an IAM policy with the following permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::ai-identity-verifier-docs",
        "arn:aws:s3:::ai-identity-verifier-docs/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "rekognition:DetectFaces",
        "rekognition:CompareFaces",
        "rekognition:DetectModerationLabels",
        "rekognition:DetectText",
        "rekognition:DetectLabels"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "textract:DetectDocumentText",
        "textract:AnalyzeDocument"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:CreateTable",
        "dynamodb:PutItem",
        "dynamodb:GetItem",
        "dynamodb:UpdateItem",
        "dynamodb:DeleteItem",
        "dynamodb:Query",
        "dynamodb:Scan"
      ],
      "Resource": [
        "arn:aws:dynamodb:*:*:table/verification-records"
      ]
    }
  ]
}
```

## Environment Configuration

### Backend Environment Variables

Create a `.env` file in the backend `src/main/resources/` directory with the following variables:

```bash
# AWS Credentials
AWS_ACCESS_KEY_ID=your_access_key_here
AWS_SECRET_ACCESS_KEY=your_secret_key_here
AWS_SESSION_TOKEN=your_session_token_here # Optional - for temporary credentials

# AWS Configuration
AWS_REGION=us-east-1
AWS_DEFAULT_REGION=us-east-1

# S3 Configuration
AWS_S3_BUCKET_NAME=ai-identity-verifier-docs
AWS_S3_BUCKET_REGION=us-east-1
AWS_S3_PRESIGNED_URL_EXPIRATION_HOURS=24

# DynamoDB Configuration
AWS_DYNAMODB_TABLE_NAME=verification-records
AWS_DYNAMODB_REGION=us-east-1

# Rekognition Configuration
AWS_REKOGNITION_REGION=us-east-1
AWS_REKOGNITION_COLLECTION_ID=identity-docs
AWS_REKOGNITION_FACE_MATCH_THRESHOLD=80.0
AWS_REKOGNITION_MIN_CONFIDENCE=70.0

# Textract Configuration
AWS_TEXTRACT_REGION=us-east-1
AWS_TEXTRACT_MIN_CONFIDENCE=80.0

# Application Configuration
SPRING_PROFILES_ACTIVE=aws
SERVER_PORT=8080

# Security Configuration
JWT_SECRET=your_jwt_secret_here
JWT_EXPIRATION_HOURS=24

# File Upload Configuration
MAX_FILE_SIZE=10MB
MAX_REQUEST_SIZE=10MB
ALLOWED_FILE_TYPES=image/jpeg,image/png,image/webp,application/pdf
```

### Frontend Environment Variables

Create a `.env.production` file in the frontend root directory:

```bash
REACT_APP_API_BASE_URL=https://your-api-gateway-url.amazonaws.com
NODE_ENV=production
```

## Deployment Steps

### 1. Prepare AWS Infrastructure

#### Create S3 Bucket
```bash
aws s3 mb s3://ai-identity-verifier-docs --region us-east-1
```

#### Create DynamoDB Table
```bash
aws dynamodb create-table \
  --table-name verification-records \
  --attribute-definitions \
    AttributeName=id,AttributeType=S \
    AttributeName=createdAt,AttributeType=S \
  --key-schema \
    AttributeName=id,KeyType=HASH \
    AttributeName=createdAt,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

### 2. Build the Application

#### Backend
```bash
cd backend
./mvnw clean package -Paws
```

#### Frontend
```bash
cd frontend
npm install
npm run build
```

### 3. Deploy Backend to EC2

1. Launch an EC2 instance (recommended: t3.medium or larger)
2. Install Java 17 on the instance
3. Copy the built JAR file to the instance
4. Set up environment variables
5. Run the application:

```bash
java -jar -Dspring.profiles.active=aws target/identity-verifier-0.0.1-SNAPSHOT.jar
```

### 4. Alternative: Deploy Backend to Elastic Beanstalk

Create a `Procfile` in the backend root:
```
web: java -Dspring.profiles.active=aws -jar target/identity-verifier-0.0.1-SNAPSHOT.jar
```

Deploy using EB CLI:
```bash
eb init
eb create prod-env
eb deploy
```

### 5. Deploy Frontend

Option 1: S3 Static Hosting
```bash
aws s3 sync dist/ s3://your-frontend-bucket --delete
```

Option 2: CloudFront + S3

### 6. Set Up API Gateway (Optional)

If using API Gateway to expose your backend:
1. Create REST API
2. Set up Lambda proxy integration
3. Deploy to stage

## Local Testing Instructions

### Prerequisites
1. AWS CLI configured with appropriate credentials
2. Docker (for local testing with real AWS services)

### Running Locally with AWS Services
1. Set up environment variables in `.env`
2. Run with AWS profile activated:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=aws
```

### Testing the Application
1. Navigate to the frontend URL
2. Upload a sample identity document
3. Verify that:
   - File uploads to S3 successfully
   - Rekognition processes the image
   - Textract extracts text
   - Results are stored in DynamoDB
   - Response is returned to the frontend

## Security Best Practices

### IAM Roles
- Use IAM roles instead of access keys when running on AWS infrastructure
- Follow principle of least privilege
- Rotate credentials regularly

### Data Protection
- Enable S3 server-side encryption
- Use DynamoDB encryption at rest
- Implement secure API endpoints with authentication

### Network Security
- Use VPC to isolate resources
- Configure security groups properly
- Use SSL/TLS for all communications

## Monitoring and Maintenance

### CloudWatch Metrics
- Monitor API response times
- Track error rates
- Monitor S3 bucket size
- Watch DynamoDB consumed capacity

### Backup Strategy
- Regular DynamoDB backups
- S3 versioning for document storage
- Database export procedures

## Troubleshooting

### Common Issues
1. **Permission Errors**: Verify IAM policies are attached correctly
2. **S3 Access**: Check bucket region and CORS configuration
3. **Rekognition/Textract**: Ensure services are available in selected region
4. **DynamoDB**: Confirm table exists and has correct schema

### Logs
- Check application logs for detailed error messages
- Review CloudWatch logs for AWS service interactions
- Monitor S3 access logs for file operations