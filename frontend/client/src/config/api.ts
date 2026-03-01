/**
 * Centralized API configuration.
 * If VITE_API_BASE_URL is omitted, requests use same-origin paths.
 */
const rawApiBaseUrl = (import.meta.env.VITE_API_BASE_URL || "").trim();
export const API_BASE_URL = rawApiBaseUrl.replace(/\/+$/, "");

const apiUrl = (path: string) => (API_BASE_URL ? `${API_BASE_URL}${path}` : path);

export const API_ENDPOINTS = {
  HEALTH: apiUrl("/api/health"),
  VERIFY: apiUrl("/api/verify"),
  VERIFICATIONS: apiUrl("/api/verifications"),
  STATS: apiUrl("/api/stats"),
  LOGIN: apiUrl("/api/auth/login"),
  LOGOUT: apiUrl("/api/auth/logout"),
} as const;

export const getEnvironmentInfo = () => ({
  isProduction: import.meta.env.PROD,
  isDevelopment: import.meta.env.DEV,
  apiUrl: API_BASE_URL || "(same-origin)",
  isLocalhost: API_BASE_URL.includes("localhost") || API_BASE_URL.includes("127.0.0.1"),
  isAwsServer: API_BASE_URL.includes("amazonaws.com") || API_BASE_URL.includes("ec2"),
});

export const buildApiUrl = (endpoint: string, params?: Record<string, string | number>): string => {
  let url = endpoint.startsWith("http") ? endpoint : apiUrl(endpoint.startsWith("/") ? endpoint : `/${endpoint}`);

  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      url = url.replace(`:${key}`, String(value));
    });
  }

  return url;
};

export const validateApiConfig = (): boolean => {
  const envInfo = getEnvironmentInfo();

  if (import.meta.env.PROD && envInfo.isLocalhost) {
    console.error("PRODUCTION ERROR: API is configured to use localhost");
    return false;
  }

  if (!API_BASE_URL || API_BASE_URL.trim() === "") {
    console.log("API Configuration: using same-origin /api routes");
    return true;
  }

  console.log(`API Configuration Valid - Environment: ${import.meta.env.MODE}`);
  console.log(`   API Base URL: ${API_BASE_URL}`);
  console.log(`   Is Production: ${envInfo.isProduction}`);
  console.log(`   Is AWS Server: ${envInfo.isAwsServer}`);

  return true;
};

if (import.meta.env.DEV) {
  validateApiConfig();
}
