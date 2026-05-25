// Token + user storage. Token lives in a cookie so middleware can read it;
// user summary lives in localStorage for client-side display.

import type { UserSummary } from "./types";

export const TOKEN_COOKIE = "cyb_token";
const USER_STORAGE_KEY = "cyb_user";
const COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 7;

function isBrowser(): boolean {
  return typeof window !== "undefined";
}

export function getToken(): string | null {
  if (!isBrowser()) return null;
  const match = document.cookie.match(
    new RegExp("(?:^|; )" + TOKEN_COOKIE + "=([^;]*)")
  );
  return match ? decodeURIComponent(match[1]) : null;
}

export function setToken(token: string): void {
  if (!isBrowser()) return;
  const secure = window.location.protocol === "https:" ? "; Secure" : "";
  document.cookie =
    `${TOKEN_COOKIE}=${encodeURIComponent(token)};` +
    ` Max-Age=${COOKIE_MAX_AGE_SECONDS};` +
    ` Path=/; SameSite=Lax${secure}`;
}

export function clearToken(): void {
  if (!isBrowser()) return;
  document.cookie = `${TOKEN_COOKIE}=; Max-Age=0; Path=/; SameSite=Lax`;
}

export function getStoredUser(): UserSummary | null {
  if (!isBrowser()) return null;
  const raw = window.localStorage.getItem(USER_STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as UserSummary;
  } catch {
    return null;
  }
}

export function setStoredUser(user: UserSummary): void {
  if (!isBrowser()) return;
  window.localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
}

export function clearStoredUser(): void {
  if (!isBrowser()) return;
  window.localStorage.removeItem(USER_STORAGE_KEY);
}

export function clearAuth(): void {
  clearToken();
  clearStoredUser();
}
