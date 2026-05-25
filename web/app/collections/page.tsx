"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ApiError, collectionsApi, recipesApi } from "@/lib/api";
import type { Collection, CollectionRecipe, Recipe } from "@/lib/types";

export default function CollectionsPage() {
  const [collections, setCollections] = useState<Collection[]>([]);
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showNewForm, setShowNewForm] = useState(false);
  const [newName, setNewName] = useState("");
  const [newDescription, setNewDescription] = useState("");
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    let cancelled = false;
    Promise.all([collectionsApi.list(), recipesApi.list()])
      .then(([c, r]) => {
        if (cancelled) return;
        setCollections(c);
        setRecipes(r);
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load.");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  async function handleCreate(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCreating(true);
    setError(null);
    try {
      const collection = await collectionsApi.create({
        name: newName.trim(),
        description: newDescription.trim() || null
      });
      setCollections((prev) => [...prev, collection]);
      setNewName("");
      setNewDescription("");
      setShowNewForm(false);
    } catch (err) {
      setError(
        err instanceof ApiError ? err.message : "Failed to create collection."
      );
    } finally {
      setCreating(false);
    }
  }

  async function handleDelete(id: number) {
    if (!confirm("Delete this collection? Recipes inside it stay safe.")) return;
    try {
      await collectionsApi.delete(id);
      setCollections((prev) => prev.filter((c) => c.id !== id));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to delete.");
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-end justify-between gap-4">
        <div>
          <h1 className="font-display text-4xl font-semibold">Collections</h1>
          <p className="text-coffee-light mt-1">
            Group recipes by theme — weeknight dinners, holiday baking, anything.
          </p>
        </div>
        <button
          onClick={() => setShowNewForm((v) => !v)}
          className="btn-primary"
        >
          {showNewForm ? "Close" : "+ New collection"}
        </button>
      </div>

      {showNewForm && (
        <form onSubmit={handleCreate} className="card p-5 space-y-3 max-w-xl">
          <div>
            <label htmlFor="cName">Name</label>
            <input
              id="cName"
              type="text"
              required
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              className="w-full"
            />
          </div>
          <div>
            <label htmlFor="cDesc">Description (optional)</label>
            <input
              id="cDesc"
              type="text"
              value={newDescription}
              onChange={(e) => setNewDescription(e.target.value)}
              className="w-full"
            />
          </div>
          <div className="flex justify-end">
            <button type="submit" disabled={creating} className="btn-primary">
              {creating ? "Creating…" : "Create"}
            </button>
          </div>
        </form>
      )}

      {error && (
        <p className="text-sm text-terracotta-dark bg-terracotta/10 rounded-md px-3 py-2">
          {error}
        </p>
      )}

      {loading && <p className="text-coffee-light">Loading collections…</p>}

      {!loading && collections.length === 0 && !showNewForm && (
        <div className="card p-10 text-center">
          <div className="text-5xl mb-3">📚</div>
          <h2 className="font-display text-2xl font-semibold mb-2">
            No collections yet
          </h2>
          <p className="text-coffee-light">
            Group your favourites by occasion, season, or anything else.
          </p>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        {collections.map((collection) => (
          <CollectionPanel
            key={collection.id}
            collection={collection}
            allRecipes={recipes}
            onDelete={() => handleDelete(collection.id)}
          />
        ))}
      </div>
    </div>
  );
}

function CollectionPanel({
  collection,
  allRecipes,
  onDelete
}: {
  collection: Collection;
  allRecipes: Recipe[];
  onDelete: () => void;
}) {
  const [items, setItems] = useState<CollectionRecipe[] | null>(null);
  const [open, setOpen] = useState(false);
  const [adding, setAdding] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function toggleOpen() {
    const next = !open;
    setOpen(next);
    if (next && !items) {
      try {
        const data = await collectionsApi.listRecipes(collection.id);
        setItems(data);
      } catch (err) {
        setError(
          err instanceof ApiError ? err.message : "Failed to load recipes."
        );
      }
    }
  }

  async function handleAdd(recipeId: number) {
    setAdding(true);
    setError(null);
    try {
      const added = await collectionsApi.addRecipe(collection.id, recipeId);
      setItems((prev) => [...(prev ?? []), added]);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to add recipe.");
    } finally {
      setAdding(false);
    }
  }

  async function handleRemove(recipeId: number) {
    setError(null);
    try {
      await collectionsApi.removeRecipe(collection.id, recipeId);
      setItems((prev) => (prev ?? []).filter((r) => r.recipeId !== recipeId));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to remove recipe.");
    }
  }

  const inCollection = new Set((items ?? []).map((r) => r.recipeId));
  const available = allRecipes.filter((r) => !inCollection.has(r.id));

  return (
    <div className="card p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="font-display text-xl font-semibold">
            {collection.name}
          </h3>
          {collection.description && (
            <p className="text-sm text-coffee-light mt-1">
              {collection.description}
            </p>
          )}
        </div>
        <button
          onClick={onDelete}
          className="text-sm text-coffee-light hover:text-terracotta-dark"
          aria-label="Delete collection"
        >
          Delete
        </button>
      </div>

      <button
        onClick={toggleOpen}
        className="mt-3 text-sm text-terracotta-dark hover:underline"
      >
        {open ? "Hide recipes" : "Manage recipes"}
      </button>

      {open && (
        <div className="mt-4 space-y-4">
          {error && (
            <p className="text-sm text-terracotta-dark bg-terracotta/10 rounded-md px-3 py-2">
              {error}
            </p>
          )}

          <div>
            <div className="text-xs uppercase tracking-wide text-coffee-light mb-2">
              In this collection
            </div>
            {!items && <p className="text-sm text-coffee-light">Loading…</p>}
            {items && items.length === 0 && (
              <p className="text-sm text-coffee-light italic">No recipes yet.</p>
            )}
            {items && items.length > 0 && (
              <ul className="space-y-1">
                {items.map((item) => (
                  <li
                    key={item.recipeId}
                    className="flex items-center justify-between gap-3 rounded-md px-2 py-1.5 hover:bg-butter/40"
                  >
                    <Link
                      href={`/recipes/${item.recipeId}`}
                      className="text-coffee hover:underline truncate"
                    >
                      {item.title}
                    </Link>
                    <button
                      onClick={() => handleRemove(item.recipeId)}
                      className="text-xs text-coffee-light hover:text-terracotta-dark"
                    >
                      Remove
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div>
            <div className="text-xs uppercase tracking-wide text-coffee-light mb-2">
              Add a recipe
            </div>
            {available.length > 0 ? (
              <select
                disabled={adding}
                onChange={(e) => {
                  if (e.target.value) {
                    handleAdd(Number(e.target.value));
                    e.target.value = "";
                  }
                }}
                className="w-full"
              >
                <option value="">Choose a recipe…</option>
                {available.map((recipe) => (
                  <option key={recipe.id} value={recipe.id}>
                    {recipe.title}
                  </option>
                ))}
              </select>
            ) : allRecipes.length === 0 ? (
              <p className="text-sm text-coffee-light italic">
                You don&apos;t have any recipes yet —{" "}
                <Link href="/recipes/new" className="text-terracotta-dark hover:underline">
                  add one
                </Link>{" "}
                to put it in this collection.
              </p>
            ) : (
              <p className="text-sm text-coffee-light italic">
                Every recipe in your library is already in this collection.{" "}
                <Link href="/recipes/new" className="text-terracotta-dark hover:underline">
                  Add a new recipe
                </Link>{" "}
                to expand it.
              </p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
