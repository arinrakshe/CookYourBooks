"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode
} from "react";
import { useRouter } from "next/navigation";
import {
  clearAuth,
  getStoredUser,
  getToken,
  setStoredUser,
  setToken
} from "@/lib/auth";
import type { AuthResponse, UserSummary } from "@/lib/types";

interface AuthState {
  user: UserSummary | null;
  ready: boolean;
  setAuth: (response: AuthResponse) => void;
  signOut: () => void;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [user, setUser] = useState<UserSummary | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const stored = getStoredUser();
    if (stored && getToken()) {
      setUser(stored);
    }
    setReady(true);
  }, []);

  const setAuth = useCallback((response: AuthResponse) => {
    setToken(response.token);
    setStoredUser(response.user);
    setUser(response.user);
  }, []);

  const signOut = useCallback(() => {
    clearAuth();
    setUser(null);
    router.push("/login");
    router.refresh();
  }, [router]);

  const value = useMemo<AuthState>(
    () => ({ user, ready, setAuth, signOut }),
    [user, ready, setAuth, signOut]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}
