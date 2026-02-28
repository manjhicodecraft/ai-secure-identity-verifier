@echo off
echo Starting AI Secure Identity Verifier - Full Stack Application...
echo.

REM Start backend in a separate window
start "Backend Server" cmd /k "cd backend && mvn spring-boot:run"

REM Wait a moment for backend to start
timeout /t 5 /nobreak >nul

REM Start frontend in a separate window
start "Frontend Server" cmd /k "cd frontend/client && npm install && npm run dev"

echo Both servers are starting...
echo Backend: http://18.212.249.8:8080
echo Frontend: http://localhost:5173 (or as shown in the frontend terminal)
echo.
echo Press Ctrl+C in each terminal window to stop the servers.
pause