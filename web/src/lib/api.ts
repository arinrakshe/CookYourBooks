// API client + typed DTOs mirroring the Spring backend.
// Token lives in localStorage; every request auto-attaches the Bearer header.

export const API_BASE =
  (import.meta as any).env?.VITE_API_BASE_URL || "http://localhost:8080";

const TOKEN_KEY = "cyb_token";
const USER_KEY = "cyb_user";

export interface UserSummary {
  id: number;
  email: string;
  displayName: string | null;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  user: UserSummary;
}

export interface RecipeIngredient {
  id: number;
  ingredientId: number | null;
  ingredientName: string | null;
  rawText: string;
  quantity: number | null;
  unitId: number | null;
  unitCode: string | null;
  preparation: string | null;
  notes: string | null;
  position: number;
}

export interface Recipe {
  id: number;
  title: string;
  description: string | null;
  servings: number | null;
  sourceUrl: string | null;
  imageUrl: string | null;
  notes: string | null;
  steps: string[];
  ingredients: RecipeIngredient[];
  createdAt: string;
  updatedAt: string;
}

export interface RecipeIngredientInput {
  ingredientId?: number | null;
  rawText: string;
  quantity?: number | null;
  unitId?: number | null;
  preparation?: string | null;
  notes?: string | null;
}

export interface RecipeInput {
  title: string;
  description?: string | null;
  servings?: number | null;
  sourceUrl?: string | null;
  imageUrl?: string | null;
  notes?: string | null;
  steps?: string[];
  ingredients?: RecipeIngredientInput[];
}

export interface Collection {
  id: number;
  name: string;
  description: string | null;
  createdAt: string;
}

export interface CollectionRecipe {
  recipeId: number;
  title: string;
  imageUrl: string | null;
  position: number;
}

export interface ShoppingListItem {
  id: number;
  rawText: string;
  quantity: number | null;
  unitId: number | null;
  unitCode: string | null;
  ingredientId: number | null;
  ingredientName: string | null;
  recipeId: number | null;
  checked: boolean;
  position: number;
}

export interface ShoppingList {
  id: number;
  name: string;
  createdAt: string;
  updatedAt: string;
  items: ShoppingListItem[];
}

export interface ShoppingListItemInput {
  rawText: string;
  quantity?: number | null;
  unitId?: number | null;
  ingredientId?: number | null;
  recipeId?: number | null;
  checked?: boolean | null;
}

export interface OcrImportResponse {
  recipe: Recipe;
  unmatchedIngredients: string[];
  unmatchedUnits: string[];
}

// ─── Token + user persistence ────────────────────────────────────────────────

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(t: string | null) {
  if (typeof window === "undefined") return;
  if (t) localStorage.setItem(TOKEN_KEY, t);
  else localStorage.removeItem(TOKEN_KEY);
  window.dispatchEvent(new Event("cyb-auth"));
}

export function getStoredUser(): UserSummary | null {
  if (typeof window === "undefined") return null;
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as UserSummary;
  } catch {
    return null;
  }
}

export function setStoredUser(u: UserSummary | null) {
  if (typeof window === "undefined") return;
  if (u) localStorage.setItem(USER_KEY, JSON.stringify(u));
  else localStorage.removeItem(USER_KEY);
}

export function clearAuth() {
  setToken(null);
  setStoredUser(null);
}

// ─── Core request helper ─────────────────────────────────────────────────────

export async function api<T = any>(
  path: string,
  opts: RequestInit = {},
): Promise<T> {
  const headers = new Headers(opts.headers || {});
  if (
    !headers.has("Content-Type") &&
    opts.body &&
    !(opts.body instanceof FormData)
  ) {
    headers.set("Content-Type", "application/json");
  }
  const token = getToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);

  const res = await fetch(`${API_BASE}${path}`, { ...opts, headers });

  if (res.status === 401) {
    clearAuth();
  }
  if (res.status === 204) return undefined as T;

  const text = await res.text();
  const parsed = text ? safeJson(text) : null;

  if (!res.ok) {
    const msg =
      (parsed as any)?.message ||
      (parsed as any)?.error ||
      `Request failed (${res.status})`;
    throw new Error(msg);
  }
  return parsed as T;
}

function safeJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

// ─── Typed endpoints ─────────────────────────────────────────────────────────

export const authApi = {
  register: (body: { email: string; password: string; displayName?: string }) =>
    api<AuthResponse>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  login: (body: { email: string; password: string }) =>
    api<AuthResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(body),
    }),
};

export const recipesApi = {
  list: () => api<Recipe[]>("/api/recipes"),
  get: (id: number | string) => api<Recipe>(`/api/recipes/${id}`),
  create: (body: RecipeInput) =>
    api<Recipe>("/api/recipes", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  update: (id: number | string, body: RecipeInput) =>
    api<Recipe>(`/api/recipes/${id}`, {
      method: "PUT",
      body: JSON.stringify(body),
    }),
  remove: (id: number | string) =>
    api<void>(`/api/recipes/${id}`, { method: "DELETE" }),
};

export const collectionsApi = {
  list: () => api<Collection[]>("/api/collections"),
  get: (id: number | string) => api<Collection>(`/api/collections/${id}`),
  create: (body: { name: string; description?: string | null }) =>
    api<Collection>("/api/collections", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  remove: (id: number | string) =>
    api<void>(`/api/collections/${id}`, { method: "DELETE" }),
  recipes: (id: number | string) =>
    api<CollectionRecipe[]>(`/api/collections/${id}/recipes`),
  addRecipe: (id: number | string, recipeId: number | string) =>
    api<CollectionRecipe>(`/api/collections/${id}/recipes/${recipeId}`, {
      method: "POST",
    }),
  removeRecipe: (id: number | string, recipeId: number | string) =>
    api<void>(`/api/collections/${id}/recipes/${recipeId}`, {
      method: "DELETE",
    }),
};

export const shoppingListsApi = {
  list: () => api<ShoppingList[]>("/api/shopping-lists"),
  get: (id: number | string) => api<ShoppingList>(`/api/shopping-lists/${id}`),
  create: (name: string) =>
    api<ShoppingList>("/api/shopping-lists", {
      method: "POST",
      body: JSON.stringify({ name }),
    }),
  rename: (id: number | string, name: string) =>
    api<ShoppingList>(`/api/shopping-lists/${id}`, {
      method: "PUT",
      body: JSON.stringify({ name }),
    }),
  remove: (id: number | string) =>
    api<void>(`/api/shopping-lists/${id}`, { method: "DELETE" }),
  addItem: (id: number | string, body: ShoppingListItemInput) =>
    api<ShoppingListItem>(`/api/shopping-lists/${id}/items`, {
      method: "POST",
      body: JSON.stringify(body),
    }),
  updateItem: (
    id: number | string,
    itemId: number | string,
    body: ShoppingListItemInput,
  ) =>
    api<ShoppingListItem>(`/api/shopping-lists/${id}/items/${itemId}`, {
      method: "PUT",
      body: JSON.stringify(body),
    }),
  removeItem: (id: number | string, itemId: number | string) =>
    api<void>(`/api/shopping-lists/${id}/items/${itemId}`, { method: "DELETE" }),
  generateFromRecipes: (id: number | string, recipeIds: number[]) =>
    api<ShoppingList>(
      `/api/shopping-lists/${id}/generate-from-recipes`,
      {
        method: "POST",
        body: JSON.stringify({ recipeIds }),
      },
    ),
};

export const ocrApi = {
  importImage: (file: File) => {
    const fd = new FormData();
    fd.append("image", file);
    return api<OcrImportResponse>("/api/ocr/import", {
      method: "POST",
      body: fd,
    });
  },
};
