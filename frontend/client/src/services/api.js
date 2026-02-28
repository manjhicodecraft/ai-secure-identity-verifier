/**
 * API Service for AI Secure Identity Verifier
 * Handles communication with the backend API
 */

/*
Vite environment variables use import.meta.env
If not defined, fallback to same server API route
*/
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";

/**
 * Check if backend API is healthy
 */
export const healthCheck = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/health`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      throw new Error(`Health check failed with status ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error("Health check error:", error);
    throw error;
  }
};

/**
 * Verify an identity document
 */
export const verifyDocument = async (file) => {
  try {
    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch(`${API_BASE_URL}/verify`, {
      method: "POST",
      body: formData,
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({
        message: `Server error: ${response.status}`,
      }));

      throw new Error(errorData.message || `Server error: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error("Verification error:", error);
    throw error;
  }
};

/**
 * Get API base URL
 */
export const getApiBaseUrl = () => {
  return API_BASE_URL;
};

/**
 * Environment info
 */
export const getEnvironment = () => {
  return {
    isProduction: import.meta.env.PROD,
    apiUrl: API_BASE_URL,
    isLocalhost:
      API_BASE_URL.includes("localhost") ||
      API_BASE_URL.includes("127.0.0.1"),
  };
};