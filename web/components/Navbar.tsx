"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "./AuthProvider";

const NAV_LINKS = [
  { href: "/", label: "Recipes" },
  { href: "/collections", label: "Collections" },
  { href: "/shopping-list", label: "Shopping" }
];

export function Navbar() {
  const { user, ready, signOut } = useAuth();
  const pathname = usePathname();
  const isAuthPage = pathname === "/login" || pathname === "/register";

  return (
    <header className="sticky top-0 z-30 backdrop-blur bg-cream/85 border-b border-butter">
      <div className="w-full max-w-6xl mx-auto px-4 sm:px-6 flex items-center justify-between h-16">
        <Link href="/" className="flex items-center gap-2">
          <span className="text-2xl">🍲</span>
          <span className="font-display text-xl font-semibold text-coffee">
            CookYourBooks
          </span>
        </Link>

        {!isAuthPage && user && (
          <nav className="hidden sm:flex items-center gap-1">
            {NAV_LINKS.map((link) => {
              const active =
                link.href === "/"
                  ? pathname === "/"
                  : pathname.startsWith(link.href);
              return (
                <Link
                  key={link.href}
                  href={link.href}
                  className={
                    "px-3 py-1.5 rounded-md text-sm font-medium transition-colors " +
                    (active
                      ? "bg-terracotta/10 text-terracotta-dark"
                      : "text-coffee-light hover:bg-butter/70 hover:text-coffee")
                  }
                >
                  {link.label}
                </Link>
              );
            })}
          </nav>
        )}

        <div className="flex items-center gap-3">
          {ready && user ? (
            <>
              <span className="hidden sm:inline text-sm text-coffee-light">
                {user.displayName || user.email}
              </span>
              <button
                onClick={signOut}
                className="text-sm font-medium text-coffee-light hover:text-terracotta-dark transition-colors"
              >
                Sign out
              </button>
            </>
          ) : ready && !isAuthPage ? (
            <Link href="/login" className="btn-primary">
              Sign in
            </Link>
          ) : null}
        </div>
      </div>
    </header>
  );
}
