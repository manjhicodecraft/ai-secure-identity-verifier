@echo off
echo Deploying AI Secure Identity Verifier Backend...

REM Navigate to backend directory
cd /d "%~dp0backend"

REM Build the project
echo Building the project...
call mvnw.cmd clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo Build successful!

REM Check if AWS credentials are set (optional warning)
echo Checking for AWS credentials...
if "%AWS_ACCESS_KEY_ID%"=="" (
    echo Warning: AWS_ACCESS_KEY_ID not set in environment variables.
)
if "%AWS_SECRET_ACCESS_KEY%"=="" (
    echo Warning: AWS_SECRET_ACCESS_KEY not set in environment variables.
)
echo Please ensure AWS credentials are configured before running the application.
echo You can use environment variables, IAM roles, or AWS credentials file.

REM Run the application
echo Starting the application...
java -jar target/identity-verifier-0.0.1-SNAPSHOT.jar --spring.profiles.active=aws

echo Backend deployment completed!
pause