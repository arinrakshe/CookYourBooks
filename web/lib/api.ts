import { clearAuth, getToken, setStoredUser, setToken } from "./auth";
import type {
  ApiErrorBody,
  AuthResponse,
  Collection,
  CollectionInput,
  CollectionRecipe,
  Ingredient,
  OcrImportResponse,
  Page,
  Recipe,
  RecipeInput,
  ShoppingList,
  ShoppingListItem,
  ShoppingListItemInput
} from "./types";

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

export class ApiError extends Error {
  status: number;
  body: ApiErrorBody | null;

  constructor(status: number, message: string, body: ApiErrorBody | null) {
    super(message);
    this.status = status;
    this.body = body;
  }
}

type Method = "GET" | "POST" | "PUT" | "DELETE";

interface RequestOptions {
  method?: Method;
  body?: unknown;
  formData?: FormData;
  query?: Record<string, string | number | undefined | null>;
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, formData, query } = options;
  const headers: Record<string, string> = {};
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  let payload: BodyInit | undefined;
  if (formData) {
    payload = formData;
  } else if (body !== undefined) {
    headers["Content-Type"] = "application/json";
    payload = JSON.stringify(body);
  }

  const url = new URL(path, API_BASE_URL);
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value === undefined || value === null || value === "") continue;
      url.searchParams.set(key, String(value));
    }
  }

  const response = await fetch(url.toString(), {
    method,
    headers,
    body: payload,
    credentials: "omit"
  });

  if (response.status === 401) {
    clearAuth();
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  const parsed = text ? safeJson(text) : null;

  if (!response.ok) {
    const errorBody = (parsed as ApiErrorBody | null) ?? null;
    const message = errorBody?.message || response.statusText || "Request failed";
    throw new ApiError(response.status, message, errorBody);
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

// ── Auth ──────────────────────────────────────────────────────────────────────

export const authApi = {
  async register(input: {
    email: string;
    password: string;
    displayName?: string;
  }): Promise<AuthResponse> {
    const result = await request<AuthResponse>("/api/auth/register", {
      method: "POST",
      body: input
    });
    persistAuth(result);
    return result;
  },

  async login(input: { email: string; password: string }): Promise<AuthResponse> {
    const result = await request<AuthResponse>("/api/auth/login", {
      method: "POST",
      body: input
    });
    persistAuth(result);
    return result;
  }
};

function persistAuth(result: AuthResponse): void {
  setToken(result.token);
  setStoredUser(result.user);
}

// ── Recipes ───────────────────────────────────────────────────────────────────

export const recipesApi = {
  list: () => request<Recipe[]>("/api/recipes"),
  get: (id: number) => request<Recipe>(`/api/recipes/${id}`),
  create: (input: RecipeInput) =>
    request<Recipe>("/api/recipes", { method: "POST", body: input }),
  update: (id: number, input: RecipeInput) =>
    request<Recipe>(`/api/recipes/${id}`, { method: "PUT", body: input }),
  delete: (id: number) =>
    request<void>(`/api/recipes/${id}`, { method: "DELETE" })
};

// ── Ingredients ───────────────────────────────────────────────────────────────

export const ingredientsApi = {
  search: (query?: string, page = 0, size = 25) =>
    request<Page<Ingredient>>("/api/ingredients", {
      query: { q: query, page, size }
    }),
  get: (id: number) => request<Ingredient>(`/api/ingredients/${id}`)
};

// ── Collections ───────────────────────────────────────────────────────────────

export const collectionsApi = {
  list: () => request<Collection[]>("/api/collections"),
  get: (id: number) => request<Collection>(`/api/collections/${id}`),
  create: (input: CollectionInput) =>
    request<Collection>("/api/collections", { method: "POST", body: input }),
  update: (id: number, input: CollectionInput) =>
    request<Collection>(`/api/collections/${id}`, { method: "PUT", body: input }),
  delete: (id: number) =>
    request<void>(`/api/collections/${id}`, { method: "DELETE" }),
  listRecipes: (id: number) =>
    request<CollectionRecipe[]>(`/api/collections/${id}/recipes`),
  addRecipe: (id: number, recipeId: number) =>
    request<CollectionRecipe>(`/api/collections/${id}/recipes/${recipeId}`, {
      method: "POST"
    }),
  removeRecipe: (id: number, recipeId: number) =>
    request<void>(`/api/collections/${id}/recipes/${recipeId}`, {
      method: "DELETE"
    })
};

// ── Shopping lists ────────────────────────────────────────────────────────────

export const shoppingListsApi = {
  list: () => request<ShoppingList[]>("/api/shopping-lists"),
  get: (id: number) => request<ShoppingList>(`/api/shopping-lists/${id}`),
  create: (name: string) =>
    request<ShoppingList>("/api/shopping-lists", {
      method: "POST",
      body: { name }
    }),
  rename: (id: number, name: string) =>
    request<ShoppingList>(`/api/shopping-lists/${id}`, {
      method: "PUT",
      body: { name }
    }),
  delete: (id: number) =>
    request<void>(`/api/shopping-lists/${id}`, { method: "DELETE" }),
  addItem: (id: number, input: ShoppingListItemInput) =>
    request<ShoppingListItem>(`/api/shopping-lists/${id}/items`, {
      method: "POST",
      body: input
    }),
  updateItem: (id: number, itemId: number, input: ShoppingListItemInput) =>
    request<ShoppingListItem>(`/api/shopping-lists/${id}/items/${itemId}`, {
      method: "PUT",
      body: input
    }),
  deleteItem: (id: number, itemId: number) =>
    request<void>(`/api/shopping-lists/${id}/items/${itemId}`, {
      method: "DELETE"
    }),
  generateFromRecipes: (id: number, recipeIds: number[]) =>
    request<ShoppingList>(`/api/shopping-lists/${id}/generate-from-recipes`, {
      method: "POST",
      body: { recipeIds }
    })
};

// ── OCR ───────────────────────────────────────────────────────────────────────

export const ocrApi = {
  importImage: (file: File) => {
    const fd = new FormData();
    fd.append("image", file);
    return request<OcrImportResponse>("/api/ocr/import", {
      method: "POST",
      formData: fd
    });
  }
};
