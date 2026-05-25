import { useEffect, useState } from "react";
import { clearAuth, getToken } from "./api";

export function useAuth() {
  const [token, setTokenState] = useState<string | null>(() => getToken());

  useEffect(() => {
    // Re-read on mount: lazy useState initializer ran during SSR with null;
    // refresh from localStorage now that we're on the client.
    setTokenState(getToken());

    const sync = () => setTokenState(getToken());
    window.addEventListener("cyb-auth", sync);
    window.addEventListener("storage", sync);
    return () => {
      window.removeEventListener("cyb-auth", sync);
      window.removeEventListener("storage", sync);
    };
  }, []);

  return {
    token,
    isAuthenticated: !!token,
    logout: () => clearAuth(),
  };
}
