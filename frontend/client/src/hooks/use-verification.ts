import { useMutation } from "@tanstack/react-query";
import { api, type VerificationResultResponse } from "@shared/routes";

/**
 * API Base URL
 * 1. Use VITE_API_BASE_URL if defined
 * 2. Otherwise fallback to AWS server
 */
const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://18.212.249.8:8080";

export function useVerifyIdentity() {
  return useMutation({
    mutationFn: async (file: File): Promise<VerificationResultResponse> => {

      const formData = new FormData();
      formData.append("file", file);

      const response = await fetch(`${API_BASE_URL}/api/verify`, {
        method: "POST",
        body: formData,
      });

      if (!response.ok) {
        let errorMessage = "Verification failed";

        try {
          const errorData = await response.json();
          errorMessage = errorData.message || errorMessage;
        } catch {
          errorMessage = response.statusText || errorMessage;
        }

        throw new Error(errorMessage);
      }

      const data = await response.json();

      // Validate response using shared zod schema
      return api.verification.verify.responses[200].parse(data);
    },
  });
}