"use client";

import { useEffect, useState } from "react";
import { ApiError, recipesApi, shoppingListsApi } from "@/lib/api";
import type { Recipe, ShoppingList, ShoppingListItem } from "@/lib/types";
import { formatQuantity } from "@/lib/format";

export default function ShoppingListPage() {
  const [lists, setLists] = useState<ShoppingList[]>([]);
  const [activeId, setActiveId] = useState<number | null>(null);
  const [active, setActive] = useState<ShoppingList | null>(null);
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [newListName, setNewListName] = useState("");
  const [creating, setCreating] = useState(false);
  const [generatorOpen, setGeneratorOpen] = useState(false);

  useEffect(() => {
    let cancelled = false;
    Promise.all([shoppingListsApi.list(), recipesApi.list()])
      .then(([l, r]) => {
        if (cancelled) return;
        setLists(l);
        setRecipes(r);
        if (l.length > 0) setActiveId(l[0].id);
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

  useEffect(() => {
    if (activeId == null) {
      setActive(null);
      return;
    }
    let cancelled = false;
    shoppingListsApi
      .get(activeId)
      .then((list) => {
        if (!cancelled) setActive(list);
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load list.");
        }
      });
    return () => {
      cancelled = true;
    };
  }, [activeId]);

  async function handleCreateList(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCreating(true);
    setError(null);
    try {
      const created = await shoppingListsApi.create(newListName.trim());
      setLists((prev) => [created, ...prev]);
      setActiveId(created.id);
      setNewListName("");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create list.");
    } finally {
      setCreating(false);
    }
  }

  async function handleDeleteList(id: number) {
    if (!confirm("Delete this shopping list?")) return;
    try {
      await shoppingListsApi.delete(id);
      setLists((prev) => prev.filter((l) => l.id !== id));
      if (activeId === id) {
        setActiveId(null);
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to delete.");
    }
  }

  async function handleToggleItem(item: ShoppingListItem) {
    if (!active) return;
    const next = !item.checked;
    setActive({
      ...active,
      items: active.items.map((i) =>
        i.id === item.id ? { ...i, checked: next } : i
      )
    });
    try {
      await shoppingListsApi.updateItem(active.id, item.id, {
        rawText: item.rawText,
        quantity: item.quantity,
        unitId: item.unitId,
        ingredientId: item.ingredientId,
        recipeId: item.recipeId,
        checked: next
      });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update.");
      setActive((curr) =>
        curr
          ? {
              ...curr,
              items: curr.items.map((i) =>
                i.id === item.id ? { ...i, checked: !next } : i
              )
            }
          : curr
      );
    }
  }

  async function handleAddItem(rawText: string) {
    if (!active || !rawText.trim()) return;
    try {
      const item = await shoppingListsApi.addItem(active.id, {
        rawText: rawText.trim()
      });
      setActive({ ...active, items: [...active.items, item] });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to add item.");
    }
  }

  async function handleDeleteItem(itemId: number) {
    if (!active) return;
    try {
      await shoppingListsApi.deleteItem(active.id, itemId);
      setActive({
        ...active,
        items: active.items.filter((i) => i.id !== itemId)
      });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to delete item.");
    }
  }

  async function handleGenerate(recipeIds: number[]) {
    if (!active || recipeIds.length === 0) return;
    try {
      const updated = await shoppingListsApi.generateFromRecipes(
        active.id,
        recipeIds
      );
      setActive(updated);
      setGeneratorOpen(false);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to generate.");
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-4xl font-semibold">Shopping</h1>
        <p className="text-coffee-light mt-1">
          Keep track of what you need at the market.
        </p>
      </div>

      {loading && <p className="text-coffee-light">Loading…</p>}
      {error && (
        <p className="text-sm text-terracotta-dark bg-terracotta/10 rounded-md px-3 py-2">
          {error}
        </p>
      )}

      {!loading && (
        <div className="grid md:grid-cols-[18rem_1fr] gap-6">
          <aside className="space-y-3">
            <form onSubmit={handleCreateList} className="card p-3 space-y-2">
              <input
                type="text"
                placeholder="New list name…"
                required
                value={newListName}
                onChange={(e) => setNewListName(e.target.value)}
                className="w-full"
              />
              <button
                type="submit"
                disabled={creating || !newListName.trim()}
                className="btn-primary w-full"
              >
                {creating ? "Creating…" : "Create list"}
              </button>
            </form>

            <div className="space-y-1">
              {lists.map((list) => (
                <div
                  key={list.id}
                  className={
                    "flex items-center justify-between gap-2 rounded-md px-3 py-2 cursor-pointer transition-colors " +
                    (activeId === list.id
                      ? "bg-terracotta/10 text-terracotta-dark"
                      : "hover:bg-butter/60 text-coffee")
                  }
                  onClick={() => setActiveId(list.id)}
                >
                  <span className="truncate text-sm font-medium">
                    {list.name}
                  </span>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDeleteList(list.id);
                    }}
                    className="text-xs text-coffee-light hover:text-terracotta-dark"
                  >
                    ✕
                  </button>
                </div>
              ))}
              {lists.length === 0 && (
                <p className="text-sm text-coffee-light italic px-3">
                  No lists yet.
                </p>
              )}
            </div>
          </aside>

          <section className="card p-6 min-h-[20rem]">
            {!active ? (
              <p className="text-coffee-light">
                Create or select a list to start adding items.
              </p>
            ) : (
              <>
                <div className="flex items-center justify-between gap-3 mb-4">
                  <h2 className="font-display text-2xl font-semibold">
                    {active.name}
                  </h2>
                  <button
                    onClick={() => setGeneratorOpen(true)}
                    className="btn-secondary text-sm"
                  >
                    Generate from recipes
                  </button>
                </div>

                <AddItemRow onAdd={handleAddItem} />

                <ul className="mt-4 divide-y divide-butter/70">
                  {active.items.map((item) => (
                    <li
                      key={item.id}
                      className="py-3 flex items-center gap-3 group"
                    >
                      <input
                        type="checkbox"
                        checked={item.checked}
                        onChange={() => handleToggleItem(item)}
                        className="w-5 h-5 accent-terracotta cursor-pointer"
                      />
                      <div className="flex-1 min-w-0">
                        <span
                          className={
                            "text-coffee " +
                            (item.checked
                              ? "line-through text-coffee-light"
                              : "")
                          }
                        >
                          {item.quantity != null && (
                            <span className="font-mono text-terracotta-dark mr-2">
                              {formatQuantity(item.quantity)}
                              {item.unitCode ? " " + item.unitCode : ""}
                            </span>
                          )}
                          {item.rawText}
                        </span>
                      </div>
                      <button
                        onClick={() => handleDeleteItem(item.id)}
                        className="text-xs text-coffee-light hover:text-terracotta-dark opacity-0 group-hover:opacity-100 transition-opacity"
                      >
                        Remove
                      </button>
                    </li>
                  ))}
                </ul>

                {active.items.length === 0 && (
                  <p className="text-coffee-light italic mt-6">
                    Nothing on your list yet — add items above or generate from
                    recipes.
                  </p>
                )}
              </>
            )}
          </section>
        </div>
      )}

      {generatorOpen && active && (
        <GenerateModal
          recipes={recipes}
          onCancel={() => setGeneratorOpen(false)}
          onGenerate={handleGenerate}
        />
      )}
    </div>
  );
}

function AddItemRow({ onAdd }: { onAdd: (text: string) => Promise<void> }) {
  const [text, setText] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!text.trim()) return;
    setBusy(true);
    try {
      await onAdd(text);
      setText("");
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} className="flex gap-2">
      <input
        type="text"
        placeholder="Add an item…"
        value={text}
        onChange={(e) => setText(e.target.value)}
        className="flex-1"
      />
      <button type="submit" disabled={busy || !text.trim()} className="btn-primary">
        Add
      </button>
    </form>
  );
}

function GenerateModal({
  recipes,
  onCancel,
  onGenerate
}: {
  recipes: Recipe[];
  onCancel: () => void;
  onGenerate: (ids: number[]) => Promise<void>;
}) {
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [busy, setBusy] = useState(false);

  function toggle(id: number) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  async function submit() {
    setBusy(true);
    try {
      await onGenerate(Array.from(selected));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="fixed inset-0 z-40 bg-coffee/40 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="card max-w-lg w-full max-h-[80vh] flex flex-col">
        <header className="p-5 border-b border-butter">
          <h2 className="font-display text-xl font-semibold">
            Generate from recipes
          </h2>
          <p className="text-sm text-coffee-light mt-1">
            Pick the recipes you&apos;re cooking — we&apos;ll add every
            ingredient to your list.
          </p>
        </header>
        <div className="flex-1 overflow-auto p-5 space-y-2">
          {recipes.length === 0 && (
            <p className="text-coffee-light italic">
              You don&apos;t have any recipes yet.
            </p>
          )}
          {recipes.map((recipe) => (
            <label
              key={recipe.id}
              className="flex items-center gap-3 px-3 py-2 rounded-md hover:bg-butter/40 cursor-pointer"
            >
              <input
                type="checkbox"
                checked={selected.has(recipe.id)}
                onChange={() => toggle(recipe.id)}
                className="w-5 h-5 accent-terracotta"
              />
              <div className="flex-1 min-w-0">
                <div className="font-medium truncate">{recipe.title}</div>
                <div className="text-xs text-coffee-light">
                  {recipe.ingredients.length} ingredients
                </div>
              </div>
            </label>
          ))}
        </div>
        <footer className="p-5 border-t border-butter flex justify-end gap-3">
          <button onClick={onCancel} className="btn-ghost">
            Cancel
          </button>
          <button
            onClick={submit}
            disabled={busy || selected.size === 0}
            className="btn-primary"
          >
            {busy ? "Adding…" : `Add ${selected.size || ""}`}
          </button>
        </footer>
      </div>
    </div>
  );
}
