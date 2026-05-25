"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { ApiError, ocrApi, recipesApi } from "@/lib/api";
import type { RecipeIngredientInput, RecipeInput } from "@/lib/types";

type Mode = "manual" | "ocr";

interface DraftIngredient {
  rawText: string;
  quantity: string;
}

export default function NewRecipePage() {
  const [mode, setMode] = useState<Mode>("manual");

  return (
    <div className="space-y-6">
      <div>
        <Link href="/" className="text-sm text-coffee-light hover:underline">
          ← All recipes
        </Link>
        <h1 className="font-display text-3xl font-semibold mt-2">Add a recipe</h1>
      </div>

      <div className="flex gap-1 p-1 bg-butter/50 rounded-md w-fit">
        <TabButton active={mode === "manual"} onClick={() => setMode("manual")}>
          Write it out
        </TabButton>
        <TabButton active={mode === "ocr"} onClick={() => setMode("ocr")}>
          Import from photo
        </TabButton>
      </div>

      {mode === "manual" ? <ManualRecipeForm /> : <OcrImportForm />}
    </div>
  );
}

function TabButton({
  active,
  onClick,
  children
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={
        "px-4 py-1.5 rounded-md text-sm font-medium transition-colors " +
        (active
          ? "bg-white shadow-sm text-coffee"
          : "text-coffee-light hover:text-coffee")
      }
    >
      {children}
    </button>
  );
}

function ManualRecipeForm() {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [servings, setServings] = useState("");
  const [sourceUrl, setSourceUrl] = useState("");
  const [imageUrl, setImageUrl] = useState("");
  const [notes, setNotes] = useState("");
  const [stepsText, setStepsText] = useState("");
  const [ingredients, setIngredients] = useState<DraftIngredient[]>([
    { rawText: "", quantity: "" }
  ]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function updateIngredient(index: number, patch: Partial<DraftIngredient>) {
    setIngredients((prev) =>
      prev.map((it, i) => (i === index ? { ...it, ...patch } : it))
    );
  }

  function addIngredient() {
    setIngredients((prev) => [...prev, { rawText: "", quantity: "" }]);
  }

  function removeIngredient(index: number) {
    setIngredients((prev) => prev.filter((_, i) => i !== index));
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const steps = stepsText
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter(Boolean);
      const ingredientPayload: RecipeIngredientInput[] = ingredients
        .filter((it) => it.rawText.trim())
        .map((it) => ({
          rawText: it.rawText.trim(),
          quantity: it.quantity.trim() ? Number(it.quantity) : null
        }));
      const payload: RecipeInput = {
        title: title.trim(),
        description: description.trim() || null,
        servings: servings.trim() ? Number(servings) : null,
        sourceUrl: sourceUrl.trim() || null,
        imageUrl: imageUrl.trim() || null,
        notes: notes.trim() || null,
        steps,
        ingredients: ingredientPayload
      };
      const recipe = await recipesApi.create(payload);
      router.push(`/recipes/${recipe.id}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to save recipe.");
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="card p-6 space-y-5 max-w-3xl">
      <div className="grid md:grid-cols-[2fr_1fr] gap-4">
        <div>
          <label htmlFor="title">Title</label>
          <input
            id="title"
            type="text"
            required
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full"
          />
        </div>
        <div>
          <label htmlFor="servings">Servings</label>
          <input
            id="servings"
            type="number"
            min="0"
            step="0.5"
            value={servings}
            onChange={(e) => setServings(e.target.value)}
            className="w-full"
          />
        </div>
      </div>

      <div>
        <label htmlFor="description">Description</label>
        <textarea
          id="description"
          rows={3}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          className="w-full"
        />
      </div>

      <div className="grid md:grid-cols-2 gap-4">
        <div>
          <label htmlFor="sourceUrl">Source URL</label>
          <input
            id="sourceUrl"
            type="url"
            value={sourceUrl}
            onChange={(e) => setSourceUrl(e.target.value)}
            className="w-full"
          />
        </div>
        <div>
          <label htmlFor="imageUrl">Image URL</label>
          <input
            id="imageUrl"
            type="url"
            value={imageUrl}
            onChange={(e) => setImageUrl(e.target.value)}
            className="w-full"
          />
        </div>
      </div>

      <div>
        <label>Ingredients</label>
        <div className="space-y-2">
          {ingredients.map((item, index) => (
            <div key={index} className="flex gap-2">
              <input
                type="text"
                placeholder="Qty"
                value={item.quantity}
                onChange={(e) =>
                  updateIngredient(index, { quantity: e.target.value })
                }
                className="w-20"
              />
              <input
                type="text"
                placeholder="e.g. 1 cup all-purpose flour"
                value={item.rawText}
                onChange={(e) =>
                  updateIngredient(index, { rawText: e.target.value })
                }
                className="flex-1"
              />
              {ingredients.length > 1 && (
                <button
                  type="button"
                  onClick={() => removeIngredient(index)}
                  className="text-coffee-light hover:text-terracotta-dark px-2"
                  aria-label="Remove ingredient"
                >
                  ✕
                </button>
              )}
            </div>
          ))}
        </div>
        <button
          type="button"
          onClick={addIngredient}
          className="text-sm text-terracotta-dark hover:underline mt-2"
        >
          + Add ingredient
        </button>
      </div>

      <div>
        <label htmlFor="steps">Instructions (one step per line)</label>
        <textarea
          id="steps"
          rows={6}
          value={stepsText}
          onChange={(e) => setStepsText(e.target.value)}
          className="w-full"
        />
      </div>

      <div>
        <label htmlFor="notes">Notes</label>
        <textarea
          id="notes"
          rows={3}
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          className="w-full"
        />
      </div>

      {error && (
        <p className="text-sm text-terracotta-dark bg-terracotta/10 rounded-md px-3 py-2">
          {error}
        </p>
      )}

      <div className="flex justify-end gap-3">
        <Link href="/" className="btn-ghost">
          Cancel
        </Link>
        <button type="submit" disabled={submitting} className="btn-primary">
          {submitting ? "Saving…" : "Save recipe"}
        </button>
      </div>
    </form>
  );
}

function OcrImportForm() {
  const router = useRouter();
  const [file, setFile] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!file) {
      setError("Choose an image first.");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const result = await ocrApi.importImage(file);
      router.push(`/recipes/${result.recipe.id}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Import failed.");
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="card p-6 space-y-5 max-w-2xl">
      <p className="text-coffee-light">
        Upload a photo of a recipe — cookbook page, recipe card, or hand-written
        note — and we&apos;ll extract the title, ingredients, and steps for you.
      </p>

      <div>
        <label htmlFor="image">Recipe image</label>
        <input
          id="image"
          type="file"
          accept="image/*"
          required
          onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          className="w-full"
        />
      </div>

      {file && (
        <div className="rounded-lg overflow-hidden border border-butter">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={URL.createObjectURL(file)}
            alt="Selected recipe"
            className="w-full max-h-80 object-contain bg-butter/40"
          />
        </div>
      )}

      {error && (
        <p className="text-sm text-terracotta-dark bg-terracotta/10 rounded-md px-3 py-2">
          {error}
        </p>
      )}

      <div className="flex justify-end gap-3">
        <Link href="/" className="btn-ghost">
          Cancel
        </Link>
        <button type="submit" disabled={submitting || !file} className="btn-primary">
          {submitting ? "Reading recipe…" : "Import"}
        </button>
      </div>
    </form>
  );
}
