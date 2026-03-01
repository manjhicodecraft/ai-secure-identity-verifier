import { useMutation } from "@tanstack/react-query";
import { api, type VerificationResultResponse } from "@shared/routes";
import { API_ENDPOINTS } from "@/config/api";

export function useVerifyIdentity() {
  return useMutation({
    mutationFn: async (file: File): Promise<VerificationResultResponse> => {

      const formData = new FormData();
      formData.append("file", file);

      const response = await fetch(API_ENDPOINTS.VERIFY, {
        method: "POST",
        body: formData,
      });

      if (!response.ok) {
        let errorMessage = "Verification failed";

        try {
          const errorData = await response.json();
          if (typeof errorData?.message === "string" && errorData.message.trim().length > 0) {
            errorMessage = errorData.message;
          } else if (Array.isArray(errorData?.explanation) && errorData.explanation.length > 0) {
            errorMessage = String(errorData.explanation[0]);
          }
        } catch {
          errorMessage = response.statusText || errorMessage;
        }

        throw new Error(errorMessage);
      }

      const data = await response.json();
      const normalized = {
        ...data,
        explanation: Array.isArray(data?.explanation) ? data.explanation : [],
        extractedData: {
          name: data?.extractedData?.name ?? "",
          idNumber: data?.extractedData?.idNumber ?? "",
          dob: data?.extractedData?.dob ?? "",
        },
      };

      // Validate response using shared zod schema
      return api.verification.verify.responses[200].parse(normalized);
    },
  });
}
