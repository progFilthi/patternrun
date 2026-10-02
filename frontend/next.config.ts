import type { NextConfig } from "next";

const apiTarget = process.env.API_INTERNAL_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  /**
   * The API is proxied through this origin rather than called from the browser directly.
   *
   * The session cookie is the reason. A cookie is only attached to same-site requests, and in
   * development the frontend is on :3000 while the API is on :8080, which browsers treat as
   * different sites. Calling the API cross-origin from the browser would therefore force
   * SameSite=None, which in turn forces a CSRF token and a credentialed CORS configuration.
   *
   * Proxying makes the browser see a single origin, so the cookie stays SameSite=Lax and CSRF
   * protection comes from the framework default instead of from code we have to get right.
   * It also means the production and development topologies are identical, so the thing tested
   * locally is the thing that ships.
   *
   * Only the browser path is rewritten. Server components keep calling API_INTERNAL_URL
   * directly, which skips a pointless hop for content reads.
   */
  async rewrites() {
    return [
      {
        source: "/api/v1/:path*",
        destination: `${apiTarget}/api/v1/:path*`,
      },
    ];
  },
};

export default nextConfig;