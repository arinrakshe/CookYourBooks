import type { Metadata } from "next";
import "./globals.css";
import { AuthProvider } from "@/components/AuthProvider";
import { Navbar } from "@/components/Navbar";

export const metadata: Metadata = {
  title: "CookYourBooks",
  description: "Your kitchen library — recipes, collections, and shopping lists."
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className="min-h-screen flex flex-col">
        <AuthProvider>
          <Navbar />
          <main className="flex-1 w-full max-w-6xl mx-auto px-4 sm:px-6 py-8">
            {children}
          </main>
          <footer className="text-center text-xs text-coffee-light py-6">
            CookYourBooks · cook well, eat well
          </footer>
        </AuthProvider>
      </body>
    </html>
  );
}
