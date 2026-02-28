/**
 * API Service for AI Secure Identity Verifier
 * Handles communication with the backend API
 */

// Backend API URL
// Production → AWS server
// Development → localhost
const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ||
  (import.meta.env.PROD
    ? "http://18.212.249.8:8080/api"
    : "http://localhost:8080/api");

/**
 * Check backend health
 */
export const healthCheck = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/health`);

    if (!response.ok) {
      throw new Error(`Health check failed: ${response.status}`);
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
      let errorMessage = `Server error ${response.status}`;

      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
      } catch {}

      throw new Error(errorMessage);
    }

    return await response.json();
  } catch (error) {
    console.error("Verification error:", error);
    throw error;
  }
};

/**
 * Get verification records
 */
export const getVerifications = async (limit = 50) => {
  try {
    const response = await fetch(`${API_BASE_URL}/verifications?limit=${limit}`);

    if (!response.ok) {
      throw new Error("Failed to fetch verification records");
    }

    return await response.json();
  } catch (error) {
    console.error("Fetch verifications error:", error);
    throw error;
  }
};

/**
 * Get verification statistics
 */
export const getStats = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/stats`);

    if (!response.ok) {
      throw new Error("Failed to fetch stats");
    }

    return await response.json();
  } catch (error) {
    console.error("Fetch stats error:", error);
    throw error;
  }
};

/**
 * API base URL getter
 */
export const getApiBaseUrl = () => API_BASE_URL;

/**
 * Environment info
 */
export const getEnvironment = () => ({
  isProduction: import.meta.env.PROD,
  apiUrl: API_BASE_URL,
  isLocalhost:
    API_BASE_URL.includes("localhost") ||
    API_BASE_URL.includes("127.0.0.1"),
});