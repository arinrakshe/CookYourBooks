"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { ApiError, recipesApi } from "@/lib/api";
import type { Recipe } from "@/lib/types";
import { RecipeCard } from "@/components/RecipeCard";

export default function HomePage() {
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    recipesApi
      .list()
      .then((data) => {
        if (!cancelled) setRecipes(data);
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load recipes.");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return recipes;
    return recipes.filter((recipe) => {
      const haystack = [
        recipe.title,
        recipe.description || "",
        ...recipe.ingredients.map((i) => i.rawText)
      ]
        .join(" ")
        .toLowerCase();
      return haystack.includes(q);
    });
  }, [recipes, query]);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4">
        <div>
          <h1 className="font-display text-4xl font-semibold">Your recipes</h1>
          <p className="text-coffee-light mt-1">
            {recipes.length} recipe{recipes.length === 1 ? "" : "s"} in your library.
          </p>
        </div>
        <Link href="/recipes/new" className="btn-primary">
          + Add recipe
        </Link>
      </div>

      <div>
        <input
          type="search"
          placeholder="Search by title, description, or ingredient…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          className="w-full max-w-xl"
        />
      </div>

      {loading && <p className="text-coffee-light">Loading recipes…</p>}
      {error && (
        <p className="text-terracotta-dark bg-terracotta/10 rounded-md px-3 py-2">
          {error}
        </p>
      )}

      {!loading && !error && recipes.length === 0 && (
        <div className="card p-10 text-center">
          <div className="text-5xl mb-3">📖</div>
          <h2 className="font-display text-2xl font-semibold mb-2">
            Your library is empty
          </h2>
          <p className="text-coffee-light mb-6">
            Snap a photo of a recipe to import it, or add one by hand.
          </p>
          <Link href="/recipes/new" className="btn-primary">
            Add your first recipe
          </Link>
        </div>
      )}

      {!loading && filtered.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {filtered.map((recipe) => (
            <RecipeCard key={recipe.id} recipe={recipe} />
          ))}
        </div>
      )}

      {!loading && recipes.length > 0 && filtered.length === 0 && (
        <p className="text-coffee-light">No recipes matched “{query}”.</p>
      )}
    </div>
  );
}
