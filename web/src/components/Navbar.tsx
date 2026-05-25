import { Link, useNavigate } from "@tanstack/react-router";
import { useAuth } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { ChefHat, Menu, X } from "lucide-react";
import { useEffect, useState } from "react";

const links = [
  { to: "/", label: "Recipes" },
  { to: "/collections", label: "Collections" },
  { to: "/shopping-list", label: "Shopping" },
  { to: "/add-recipe", label: "Add" },
] as const;

export function Navbar() {
  const { isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <header
      className={`sticky top-0 z-50 glass-strong transition-all duration-500 ${
        scrolled ? "py-0" : "py-1"
      }`}
    >
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <Link to="/" className="flex items-center gap-2.5 group">
          <span className="grid h-9 w-9 place-items-center rounded-full bg-gradient-to-br from-[var(--gold)] to-[var(--primary)] text-[var(--primary-foreground)] shadow-glow">
            <ChefHat className="h-5 w-5" />
          </span>
          <span className="font-display text-xl tracking-tight">
            Cook<span className="text-gradient-ember">YourBooks</span>
          </span>
        </Link>

        <nav className="hidden md:flex items-center gap-1">
          {links.map((l) => (
            <Link
              key={l.to}
              to={l.to}
              className="relative px-4 py-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
              activeProps={{
                className: "text-foreground after:opacity-100",
              }}
              activeOptions={{ exact: l.to === "/" }}
            >
              <span className="relative z-10">{l.label}</span>
              <span className="absolute left-1/2 -translate-x-1/2 bottom-1 h-px w-6 bg-gradient-to-r from-[var(--gold)] to-[var(--primary)] opacity-0 transition-opacity" />
            </Link>
          ))}
        </nav>

        <div className="hidden md:flex items-center gap-2">
          {isAuthenticated ? (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                logout();
                navigate({ to: "/login" });
              }}
            >
              Sign out
            </Button>
          ) : (
            <>
              <Button variant="ghost" size="sm" asChild>
                <Link to="/login">Sign in</Link>
              </Button>
              <Button
                size="sm"
                asChild
                className="rounded-full bg-gradient-to-r from-[var(--primary)] to-[var(--gold)] text-[var(--primary-foreground)] hover:opacity-90 shadow-glow"
              >
                <Link to="/register">Get started</Link>
              </Button>
            </>
          )}
        </div>

        <button
          className="md:hidden p-2 text-foreground"
          onClick={() => setOpen((v) => !v)}
          aria-label="Menu"
        >
          {open ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
        </button>
      </div>

      {open && (
        <div className="md:hidden glass-strong border-t border-border">
          <div className="px-4 py-3 space-y-1">
            {links.map((l) => (
              <Link
                key={l.to}
                to={l.to}
                onClick={() => setOpen(false)}
                className="block px-3 py-2.5 rounded-md text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-white/5"
              >
                {l.label}
              </Link>
            ))}
            <div className="pt-3 border-t border-border mt-2 flex flex-col gap-2">
              {isAuthenticated ? (
                <Button
                  variant="outline"
                  onClick={() => {
                    logout();
                    setOpen(false);
                    navigate({ to: "/login" });
                  }}
                >
                  Sign out
                </Button>
              ) : (
                <>
                  <Button variant="outline" asChild>
                    <Link to="/login" onClick={() => setOpen(false)}>
                      Sign in
                    </Link>
                  </Button>
                  <Button
                    asChild
                    className="bg-gradient-to-r from-[var(--primary)] to-[var(--gold)] text-[var(--primary-foreground)]"
                  >
                    <Link to="/register" onClick={() => setOpen(false)}>
                      Get started
                    </Link>
                  </Button>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </header>
  );
}
