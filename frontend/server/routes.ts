import type { Express } from "express";
import { type Server } from "http";
import { createProxyMiddleware } from "http-proxy-middleware";

export async function registerRoutes(
  httpServer: Server,
  app: Express
): Promise<Server> {

  const backendUrl = process.env.BACKEND_URL || "http://localhost:8080";

  app.use(
    "/api/*",
    createProxyMiddleware({
      target: backendUrl,
      changeOrigin: true,
      secure: false,
      pathRewrite: {
        "^/api": "/api"
      }
    })
  );

  return httpServer;
}
