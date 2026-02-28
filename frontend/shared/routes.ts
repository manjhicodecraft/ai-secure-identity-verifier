import { z } from 'zod';
import { verificationResultSchema } from './schema';

// API Configuration for shared routes
const API_BASE_URL = process.env.VITE_API_BASE_URL || "http://18.212.249.8:8080";

export const errorSchemas = {
  validation: z.object({ message: z.string(), field: z.string().optional() }),
  internal: z.object({ message: z.string() }),
};

export const api = {
  verification: {
    verify: {
      method: 'POST' as const,
      path: '/api/verify' as const,
      responses: {
        200: verificationResultSchema,
        400: errorSchemas.validation,
        500: errorSchemas.internal,
      },
    },
  },
};

export function buildUrl(path: string, params?: Record<string, string | number>): string {
  // Ensure path is properly formatted
  let url = path;
  
  // If path doesn't start with http, prepend API base URL
  if (!url.startsWith('http')) {
    url = `${API_BASE_URL}${url.startsWith('/') ? url : `/${url}`}`;
  }
  
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (url.includes(`:${key}`)) {
        url = url.replace(`:${key}`, String(value));
      }
    });
  }
  return url;
}

export type VerificationResultResponse = z.infer<typeof api.verification.verify.responses[200]>;
