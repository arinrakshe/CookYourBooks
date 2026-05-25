import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import {
  recipesApi,
  shoppingListsApi,
  type Recipe,
  type ShoppingList,
  type ShoppingListItem,
} from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { ShoppingBasket, Sparkles, Trash2, X } from "lucide-react";
import { toast } from "sonner";

export const Route = createFileRoute("/shopping-list")({
  component: ShoppingListPage,
});

function ShoppingListPage() {
  const qc = useQueryClient();
  const [activeId, setActiveId] = useState<number | null>(null);
  const [generatorOpen, setGeneratorOpen] = useState(false);

  const listsQ = useQuery<ShoppingList[]>({
    queryKey: ["shopping-lists"],
    queryFn: () => shoppingListsApi.list(),
  });

  useEffect(() => {
    if (activeId == null && listsQ.data && listsQ.data.length > 0) {
      setActiveId(listsQ.data[0].id);
    }
  }, [activeId, listsQ.data]);

  const active = listsQ.data?.find((l) => l.id === activeId) ?? null;

  const createMutation = useMutation({
    mutationFn: (name: string) => shoppingListsApi.create(name),
    onSuccess: (list) => {
      toast.success("List created");
      qc.invalidateQueries({ queryKey: ["shopping-lists"] });
      setActiveId(list.id);
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const deleteListMutation = useMutation({
    mutationFn: (id: number) => shoppingListsApi.remove(id),
    onSuccess: () => {
      toast.success("List deleted");
      setActiveId(null);
      qc.invalidateQueries({ queryKey: ["shopping-lists"] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  return (
    <div className="mx-auto max-w-6xl px-4 sm:px-6 lg:px-8 py-10 lg:py-14">
      <header className="mb-8 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-display text-4xl sm:text-5xl">Shopping</h1>
          <p className="mt-2 text-muted-foreground">
            Build a list, tick things off as you shop.
          </p>
        </div>
        {active && (
          <Button
            onClick={() => setGeneratorOpen(true)}
            className="rounded-full gap-2"
          >
            <Sparkles className="h-4 w-4" />
            Generate from recipes
          </Button>
        )}
      </header>

      {listsQ.isLoading ? (
        <p className="text-muted-foreground">Loading…</p>
      ) : (
        <div className="grid lg:grid-cols-[18rem_1fr] gap-6">
          <aside className="space-y-3">
            <CreateListForm
              onCreate={(name) => createMutation.mutate(name)}
              busy={createMutation.isPending}
            />
            <div className="space-y-1">
              {(listsQ.data ?? []).map((list) => (
                <ListSidebarItem
                  key={list.id}
                  list={list}
                  active={list.id === activeId}
                  onOpen={() => setActiveId(list.id)}
                  onDelete={() => {
                    if (confirm(`Delete "${list.name}"?`)) {
                      deleteListMutation.mutate(list.id);
                    }
                  }}
                />
              ))}
              {(listsQ.data ?? []).length === 0 && (
                <p className="text-sm text-muted-foreground italic px-3">
                  No lists yet.
                </p>
              )}
            </div>
          </aside>

          <section className="rounded-3xl border border-border bg-card shadow-card min-h-[24rem]">
            {!active ? (
              <div className="p-12 text-center">
                <ShoppingBasket className="mx-auto h-10 w-10 text-muted-foreground" />
                <h3 className="font-display text-2xl mt-3">
                  Pick or create a list
                </h3>
                <p className="text-sm text-muted-foreground mt-1">
                  Use the sidebar to make one.
                </p>
              </div>
            ) : (
              <ActiveListView list={active} />
            )}
          </section>
        </div>
      )}

      {generatorOpen && active && (
        <GeneratorModal
          listId={active.id}
          onClose={() => setGeneratorOpen(false)}
        />
      )}
    </div>
  );
}

function CreateListForm({
  onCreate,
  busy,
}: {
  onCreate: (name: string) => void;
  busy: boolean;
}) {
  const [name, setName] = useState("");
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (!name.trim()) return;
        onCreate(name.trim());
        setName("");
      }}
      className="rounded-2xl border border-border bg-card p-3 space-y-2 shadow-card"
    >
      <Input
        placeholder="New list name…"
        value={name}
        onChange={(e) => setName(e.target.value)}
      />
      <Button
        type="submit"
        disabled={busy || !name.trim()}
        className="w-full rounded-full"
      >
        {busy ? "Creating…" : "Create list"}
      </Button>
    </form>
  );
}

function ListSidebarItem({
  list,
  active,
  onOpen,
  onDelete,
}: {
  list: ShoppingList;
  active: boolean;
  onOpen: () => void;
  onDelete: () => void;
}) {
  return (
    <div
      onClick={onOpen}
      className={
        "flex items-center justify-between gap-2 px-3 py-2 rounded-md cursor-pointer transition-colors " +
        (active
          ? "bg-primary/10 text-primary"
          : "hover:bg-muted text-foreground")
      }
    >
      <div className="min-w-0">
        <div className="text-sm font-medium truncate">{list.name}</div>
        <div className="text-xs text-muted-foreground">
          {list.items.length} {list.items.length === 1 ? "item" : "items"}
        </div>
      </div>
      <button
        onClick={(e) => {
          e.stopPropagation();
          onDelete();
        }}
        className="text-muted-foreground hover:text-destructive p-1"
        aria-label="Delete list"
      >
        <Trash2 className="h-3.5 w-3.5" />
      </button>
    </div>
  );
}

function ActiveListView({ list }: { list: ShoppingList }) {
  const qc = useQueryClient();

  const addItemMutation = useMutation({
    mutationFn: (rawText: string) =>
      shoppingListsApi.addItem(list.id, { rawText }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["shopping-lists"] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const toggleMutation = useMutation({
    mutationFn: (item: ShoppingListItem) =>
      shoppingListsApi.updateItem(list.id, item.id, {
        rawText: item.rawText,
        quantity: item.quantity,
        unitId: item.unitId,
        ingredientId: item.ingredientId,
        recipeId: item.recipeId,
        checked: !item.checked,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["shopping-lists"] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const deleteItemMutation = useMutation({
    mutationFn: (itemId: number) => shoppingListsApi.removeItem(list.id, itemId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["shopping-lists"] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const remaining = list.items.filter((i) => !i.checked).length;

  return (
    <div>
      <div className="px-6 pt-6 pb-4 flex flex-wrap items-end justify-between gap-3 border-b border-border">
        <div>
          <h2 className="font-display text-2xl">{list.name}</h2>
          <p className="text-sm text-muted-foreground mt-0.5">
            {list.items.length === 0
              ? "Empty — add items below."
              : `${remaining} of ${list.items.length} items left`}
          </p>
        </div>
      </div>

      <AddItemRow
        onAdd={(text) => addItemMutation.mutate(text)}
        busy={addItemMutation.isPending}
      />

      {list.items.length === 0 ? (
        <div className="px-6 pb-10 pt-4">
          <p className="text-muted-foreground italic">
            Nothing on your list yet.
          </p>
        </div>
      ) : (
        <ul className="divide-y divide-border">
          {list.items.map((it) => (
            <li
              key={it.id}
              className="flex items-center gap-4 px-6 py-4 hover:bg-muted/40 transition group"
            >
              <Checkbox
                checked={it.checked}
                onCheckedChange={() => toggleMutation.mutate(it)}
                className="h-5 w-5"
              />
              <div
                className={`flex-1 ${
                  it.checked ? "line-through text-muted-foreground" : ""
                }`}
              >
                {it.quantity != null && (
                  <span className="font-mono text-primary mr-2 tabular-nums">
                    {formatQty(it.quantity)}
                    {it.unitCode ? " " + it.unitCode : ""}
                  </span>
                )}
                <span className="font-medium">{it.rawText}</span>
              </div>
              <button
                onClick={() => deleteItemMutation.mutate(it.id)}
                className="text-muted-foreground hover:text-destructive opacity-0 group-hover:opacity-100 transition-opacity"
                aria-label="Remove item"
              >
                <X className="h-4 w-4" />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function AddItemRow({
  onAdd,
  busy,
}: {
  onAdd: (text: string) => void;
  busy: boolean;
}) {
  const [text, setText] = useState("");
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (!text.trim()) return;
        onAdd(text.trim());
        setText("");
      }}
      className="flex gap-2 px-6 py-4 border-b border-border"
    >
      <Input
        placeholder="Add an item — e.g. 2 lemons, milk, olive oil"
        value={text}
        onChange={(e) => setText(e.target.value)}
        className="flex-1"
      />
      <Button type="submit" disabled={busy || !text.trim()}>
        Add
      </Button>
    </form>
  );
}

function GeneratorModal({
  listId,
  onClose,
}: {
  listId: number;
  onClose: () => void;
}) {
  const qc = useQueryClient();
  const recipesQ = useQuery<Recipe[]>({
    queryKey: ["recipes"],
    queryFn: () => recipesApi.list(),
  });
  const [selected, setSelected] = useState<Set<number>>(new Set());

  const mutation = useMutation({
    mutationFn: () =>
      shoppingListsApi.generateFromRecipes(listId, Array.from(selected)),
    onSuccess: () => {
      toast.success("Added to your list");
      qc.invalidateQueries({ queryKey: ["shopping-lists"] });
      onClose();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  function toggle(id: number) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  const recipes = recipesQ.data ?? [];

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="w-full max-w-lg max-h-[80vh] flex flex-col rounded-3xl border border-border bg-card shadow-card">
        <header className="px-6 py-5 border-b border-border">
          <h2 className="font-display text-2xl">Generate from recipes</h2>
          <p className="text-sm text-muted-foreground mt-1">
            Pick what you're cooking — every ingredient lands on your list.
          </p>
        </header>
        <div className="flex-1 overflow-auto px-3 py-3">
          {recipesQ.isLoading ? (
            <p className="px-3 py-3 text-muted-foreground">Loading recipes…</p>
          ) : recipes.length === 0 ? (
            <p className="px-3 py-3 text-muted-foreground italic">
              You don't have any recipes yet.
            </p>
          ) : (
            recipes.map((r) => (
              <label
                key={r.id}
                className="flex items-center gap-3 px-3 py-2 rounded-md hover:bg-muted cursor-pointer"
              >
                <Checkbox
                  checked={selected.has(r.id)}
                  onCheckedChange={() => toggle(r.id)}
                  className="h-5 w-5"
                />
                <div className="flex-1 min-w-0">
                  <div className="font-medium truncate">{r.title}</div>
                  <div className="text-xs text-muted-foreground">
                    {r.ingredients.length} ingredients
                  </div>
                </div>
              </label>
            ))
          )}
        </div>
        <footer className="px-6 py-4 border-t border-border flex justify-end gap-3">
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button
            onClick={() => mutation.mutate()}
            disabled={mutation.isPending || selected.size === 0}
          >
            {mutation.isPending ? "Adding…" : `Add ${selected.size || ""}`}
          </Button>
        </footer>
      </div>
    </div>
  );
}

function formatQty(n: number): string {
  if (!isFinite(n)) return "";
  return (Math.round(n * 100) / 100).toString().replace(/\.?0+$/, "");
}
