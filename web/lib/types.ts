// Shared types — mirror backend DTOs.

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

export interface Ingredient {
  id: number;
  name: string;
  defaultUnitId: number | null;
  defaultUnitCode: string | null;
  densityGPerMl: number | null;
  usdaFdcId: number | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
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

export interface CollectionInput {
  name: string;
  description?: string | null;
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

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  fieldErrors?: { field: string; message: string }[] | null;
}
