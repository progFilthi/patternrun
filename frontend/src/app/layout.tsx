import type { Metadata } from "next";
import { Geist_Mono, Inter } from "next/font/google";

import { AppHeader } from "@/components/layout/app-header";
import { SessionProvider } from "@/components/layout/session-provider";
import { THEME_INIT_SCRIPT } from "@/lib/theme";
import "./globals.css";

const inter = Inter({
  variable: "--font-sans",
  subsets: ["latin"],
  display: "swap",
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
  display: "swap",
});

export const metadata: Metadata = {
  title: {
    default: "PatternRun",
    template: "%s · PatternRun",
  },
  description:
    "Stop memorizing LeetCode. Learn the patterns behind it through animated breakdowns, progressive hints and pattern recognition.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    // The class is set by the init script below, which the server cannot run, so the
    // attribute it produces differs from what was rendered here.
    <html
      lang="en"
      suppressHydrationWarning
      className={`${inter.variable} ${geistMono.variable} antialiased`}
    >
      <head>
        <script dangerouslySetInnerHTML={{ __html: THEME_INIT_SCRIPT }} />
      </head>
      <body className="flex min-h-screen flex-col">
        {/* One session for the whole app, established once on load. */}
        <SessionProvider>
          <AppHeader />
          <main id="main" className="flex-1">
            {children}
          </main>
        </SessionProvider>
      </body>
    </html>
  );
}