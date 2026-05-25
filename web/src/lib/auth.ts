import { useEffect, useState } from "react";
import { getToken, setToken } from "./api";

export function useAuth() {
  const [token, setTokenState] = useState<string | null>(() => getToken());
  useEffect(() => {
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
    logout: () => setToken(null),
  };
}
