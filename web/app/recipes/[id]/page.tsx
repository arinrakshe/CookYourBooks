"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ApiError, recipesApi } from "@/lib/api";
import type { Recipe } from "@/lib/types";
import { IngredientList } from "@/components/IngredientList";
import { StepsList } from "@/components/StepsList";
import { ServingScaler } from "@/components/ServingScaler";

export default function RecipeDetailPage() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const [recipe, setRecipe] = useState<Recipe | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentServings, setCurrentServings] = useState<number | null>(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    if (!Number.isFinite(id)) {
      setError("Invalid recipe ID.");
      setLoading(false);
      return;
    }
    let cancelled = false;
    recipesApi
      .get(id)
      .then((data) => {
        if (cancelled) return;
        setRecipe(data);
        setCurrentServings(data.servings ?? 1);
      })
      .catch((err) => {
        if (cancelled) return;
        setError(err instanceof ApiError ? err.message : "Failed to load recipe.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [id]);

  async function handleDelete() {
    if (!recipe) return;
    if (!confirm(`Delete "${recipe.title}"? This cannot be undone.`)) return;
    setDeleting(true);
    try {
      await recipesApi.delete(recipe.id);
      router.push("/");
      router.refresh();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to delete recipe.");
      setDeleting(false);
    }
  }

  if (loading) {
    return <p className="text-coffee-light">Loading recipe…</p>;
  }
  if (error) {
    return (
      <div className="card p-6 max-w-xl">
        <p className="text-terracotta-dark">{error}</p>
        <Link href="/" className="text-sm text-coffee-light hover:underline mt-3 inline-block">
          ← Back to recipes
        </Link>
      </div>
    );
  }
  if (!recipe || currentServings == null) return null;

  const baseServings = recipe.servings ?? 1;
  const scale = currentServings / baseServings;

  return (
    <article className="space-y-8">
      <div>
        <Link href="/" className="text-sm text-coffee-light hover:underline">
          ← All recipes
        </Link>
      </div>

      <header className="grid md:grid-cols-2 gap-8 items-start">
        <div className="aspect-[4/3] rounded-2xl overflow-hidden bg-butter/60 shadow-card">
          {recipe.imageUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={recipe.imageUrl}
              alt={recipe.title}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-7xl text-terracotta/40">
              🥘
            </div>
          )}
        </div>
        <div className="space-y-4">
          <h1 className="font-display text-4xl font-semibold leading-tight">
            {recipe.title}
          </h1>
          {recipe.description && (
            <p className="text-coffee-light leading-relaxed">{recipe.description}</p>
          )}
          <div className="flex flex-wrap items-center gap-3">
            <ServingScaler
              baseServings={baseServings}
              currentServings={currentServings}
              onChange={setCurrentServings}
            />
            {recipe.sourceUrl && (
              <a
                href={recipe.sourceUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="btn-ghost text-sm"
              >
                Source ↗
              </a>
            )}
            <button
              onClick={handleDelete}
              disabled={deleting}
              className="btn-danger ml-auto"
            >
              {deleting ? "Deleting…" : "Delete"}
            </button>
          </div>
        </div>
      </header>

      <div className="grid md:grid-cols-[1fr_2fr] gap-8">
        <section className="card p-6">
          <h2 className="font-display text-xl font-semibold mb-4">Ingredients</h2>
          <IngredientList ingredients={recipe.ingredients} scale={scale} />
        </section>
        <section className="card p-6">
          <h2 className="font-display text-xl font-semibold mb-4">Instructions</h2>
          <StepsList steps={recipe.steps} />
        </section>
      </div>

      {recipe.notes && (
        <section className="card p-6">
          <h2 className="font-display text-xl font-semibold mb-2">Notes</h2>
          <p className="text-coffee-light whitespace-pre-line">{recipe.notes}</p>
        </section>
      )}
    </article>
  );
}
