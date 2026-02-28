/**
 * Centralized API Configuration
 * 
 * This file contains all API-related configuration to ensure
 * consistent and correct API endpoint usage across the application.
 */

/**
 * API Base URL Configuration
 * 
 * Priority order:
 * 1. VITE_API_BASE_URL environment variable (for custom deployments)
 * 2. AWS EC2 production server (default fallback)
 * 
 * IMPORTANT: Never use localhost in production code
 */
export const API_BASE_URL = 
  import.meta.env.VITE_API_BASE_URL || "http://18.212.249.8:8080";

/**
 * API Endpoints
 * 
 * All API endpoints should be defined here to avoid hardcoded paths
 */
export const API_ENDPOINTS = {
  // Health check endpoint
  HEALTH: `${API_BASE_URL}/api/health`,
  
  // Document verification endpoint
  VERIFY: `${API_BASE_URL}/api/verify`,
  
  // Verification records endpoint
  VERIFICATIONS: `${API_BASE_URL}/api/verifications`,
  
  // Statistics endpoint
  STATS: `${API_BASE_URL}/api/stats`,
  
  // Authentication endpoints (if needed)
  LOGIN: `${API_BASE_URL}/api/auth/login`,
  LOGOUT: `${API_BASE_URL}/api/auth/logout`,
} as const;

/**
 * Environment Information
 */
export const getEnvironmentInfo = () => ({
  isProduction: import.meta.env.PROD,
  isDevelopment: import.meta.env.DEV,
  apiUrl: API_BASE_URL,
  isLocalhost: API_BASE_URL.includes("localhost") || API_BASE_URL.includes("127.0.0.1"),
  isAwsServer: API_BASE_URL.includes("18.212.249.8"),
});

/**
 * Utility function to build API URLs with parameters
 */
export const buildApiUrl = (endpoint: string, params?: Record<string, string | number>): string => {
  let url = endpoint;
  
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      url = url.replace(`:${key}`, String(value));
    });
  }
  
  return url;
};

/**
 * Validation function to ensure API configuration is correct
 */
export const validateApiConfig = (): boolean => {
  const envInfo = getEnvironmentInfo();
  
  // In production, should never use localhost
  if (import.meta.env.PROD && envInfo.isLocalhost) {
    console.error("❌ PRODUCTION ERROR: API is configured to use localhost");
    return false;
  }
  
  // Should always have a valid API base URL
  if (!API_BASE_URL || API_BASE_URL.trim() === "") {
    console.error("❌ API Configuration Error: API_BASE_URL is empty");
    return false;
  }
  
  console.log(`✅ API Configuration Valid - Environment: ${import.meta.env.MODE}`);
  console.log(`   API Base URL: ${API_BASE_URL}`);
  console.log(`   Is Production: ${envInfo.isProduction}`);
  console.log(`   Is AWS Server: ${envInfo.isAwsServer}`);
  
  return true;
};

// Run validation on module load in development
if (import.meta.env.DEV) {
  validateApiConfig();
}