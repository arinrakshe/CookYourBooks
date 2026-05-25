import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import {
  collectionsApi,
  recipesApi,
  type Collection,
  type CollectionRecipe,
  type Recipe,
} from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { ArrowLeft, FolderHeart, Plus, Trash2, X } from "lucide-react";
import { toast } from "sonner";

export const Route = createFileRoute("/collections")({
  component: CollectionsPage,
});

const COVER_FALLBACK =
  "https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=800&q=80";

function CollectionsPage() {
  const [active, setActive] = useState<Collection | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const qc = useQueryClient();

  const { data, isLoading, error } = useQuery<Collection[]>({
    queryKey: ["collections"],
    queryFn: () => collectionsApi.list(),
  });

  const collections = data ?? [];

  const deleteMutation = useMutation({
    mutationFn: (id: number) => collectionsApi.remove(id),
    onSuccess: () => {
      toast.success("Collection deleted");
      qc.invalidateQueries({ queryKey: ["collections"] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  if (active) {
    return (
      <CollectionDetail
        collection={active}
        onBack={() => setActive(null)}
      />
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-10 lg:py-14">
      <header className="mb-10 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-display text-4xl sm:text-5xl">Collections</h1>
          <p className="mt-2 text-muted-foreground">
            Recipes organized into little cookbooks.
          </p>
        </div>
        <Button
          onClick={() => setShowCreate((v) => !v)}
          className="rounded-full gap-2"
        >
          <Plus className="h-4 w-4" />
          {showCreate ? "Close" : "New collection"}
        </Button>
      </header>

      {showCreate && (
        <CreateCollectionForm onCreated={() => setShowCreate(false)} />
      )}

      {isLoading ? (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array.from({ length: 6 }).map((_, i) => (
            <div
              key={i}
              className="aspect-[4/3] rounded-2xl bg-muted animate-pulse"
            />
          ))}
        </div>
      ) : error ? (
        <p className="text-muted-foreground">{(error as Error).message}</p>
      ) : collections.length === 0 ? (
        <div className="rounded-3xl border border-dashed border-border bg-card/50 p-12 text-center">
          <FolderHeart className="mx-auto h-10 w-10 text-muted-foreground" />
          <h3 className="font-display text-2xl mt-3">No collections yet</h3>
          <p className="mt-1 text-sm text-muted-foreground">
            Click "New collection" above to start one.
          </p>
        </div>
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {collections.map((c) => (
            <CollectionTile
              key={c.id}
              collection={c}
              onOpen={() => setActive(c)}
              onDelete={() => {
                if (
                  confirm(
                    `Delete "${c.name}"? Recipes inside it stay in your library.`,
                  )
                ) {
                  deleteMutation.mutate(c.id);
                }
              }}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function CreateCollectionForm({ onCreated }: { onCreated: () => void }) {
  const qc = useQueryClient();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");

  const mutation = useMutation({
    mutationFn: () =>
      collectionsApi.create({
        name: name.trim(),
        description: description.trim() || null,
      }),
    onSuccess: () => {
      toast.success("Collection created");
      setName("");
      setDescription("");
      qc.invalidateQueries({ queryKey: ["collections"] });
      onCreated();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (!name.trim()) return;
        mutation.mutate();
      }}
      className="mb-10 rounded-2xl border border-border bg-card p-6 shadow-card max-w-xl space-y-4"
    >
      <div className="space-y-2">
        <Label>Name</Label>
        <Input
          required
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Weeknight dinners"
        />
      </div>
      <div className="space-y-2">
        <Label>Description (optional)</Label>
        <Textarea
          rows={2}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Quick mid-week wins."
        />
      </div>
      <div className="flex justify-end">
        <Button type="submit" disabled={mutation.isPending || !name.trim()}>
          {mutation.isPending ? "Creating…" : "Create"}
        </Button>
      </div>
    </form>
  );
}

function CollectionTile({
  collection,
  onOpen,
  onDelete,
}: {
  collection: Collection;
  onOpen: () => void;
  onDelete: () => void;
}) {
  const { data } = useQuery<CollectionRecipe[]>({
    queryKey: ["collection-recipes", collection.id],
    queryFn: () => collectionsApi.recipes(collection.id),
  });
  const items = data ?? [];
  const cover = items.find((r) => r.imageUrl)?.imageUrl || COVER_FALLBACK;

  return (
    <div className="group relative overflow-hidden rounded-2xl bg-card shadow-card hover:shadow-soft hover:-translate-y-1 transition-all">
      <button onClick={onOpen} className="block w-full text-left">
        <div className="aspect-[4/3] relative overflow-hidden bg-muted">
          <img
            src={cover}
            alt={collection.name}
            loading="lazy"
            className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-105"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent" />
          <div className="absolute bottom-4 left-5 right-5 text-white">
            <h3 className="font-display text-2xl">{collection.name}</h3>
            <p className="text-xs opacity-90 mt-0.5">
              {items.length} {items.length === 1 ? "recipe" : "recipes"}
            </p>
          </div>
        </div>
      </button>
      <button
        onClick={onDelete}
        className="absolute top-3 right-3 h-9 w-9 grid place-items-center rounded-full bg-black/40 backdrop-blur-md text-white opacity-0 group-hover:opacity-100 transition-opacity hover:bg-black/60"
        aria-label="Delete collection"
      >
        <Trash2 className="h-4 w-4" />
      </button>
    </div>
  );
}

function CollectionDetail({
  collection,
  onBack,
}: {
  collection: Collection;
  onBack: () => void;
}) {
  const qc = useQueryClient();

  const recipesQ = useQuery<CollectionRecipe[]>({
    queryKey: ["collection-recipes", collection.id],
    queryFn: () => collectionsApi.recipes(collection.id),
  });

  const allRecipesQ = useQuery<Recipe[]>({
    queryKey: ["recipes"],
    queryFn: () => recipesApi.list(),
  });

  const items = recipesQ.data ?? [];
  const allRecipes = allRecipesQ.data ?? [];
  const inCollectionIds = new Set(items.map((r) => r.recipeId));
  const available = allRecipes.filter((r) => !inCollectionIds.has(r.id));

  const addMutation = useMutation({
    mutationFn: (recipeId: number) =>
      collectionsApi.addRecipe(collection.id, recipeId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["collection-recipes", collection.id] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const removeMutation = useMutation({
    mutationFn: (recipeId: number) =>
      collectionsApi.removeRecipe(collection.id, recipeId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["collection-recipes", collection.id] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  return (
    <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-10">
      <button
        onClick={onBack}
        className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" /> All collections
      </button>
      <h1 className="font-display text-4xl sm:text-5xl mt-6">{collection.name}</h1>
      {collection.description && (
        <p className="mt-2 text-muted-foreground max-w-2xl">
          {collection.description}
        </p>
      )}

      <section className="mt-8">
        <h2 className="font-display text-xl mb-4">
          {items.length} {items.length === 1 ? "recipe" : "recipes"}
        </h2>

        {recipesQ.isLoading ? (
          <p className="text-muted-foreground">Loading…</p>
        ) : items.length === 0 ? (
          <p className="text-muted-foreground italic">
            Nothing here yet — add a recipe below.
          </p>
        ) : (
          <ul className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {items.map((r) => (
              <li
                key={r.recipeId}
                className="group relative overflow-hidden rounded-2xl bg-card shadow-card"
              >
                <Link
                  to="/recipes/$id"
                  params={{ id: String(r.recipeId) }}
                  className="block"
                >
                  <div className="aspect-[16/10] bg-muted overflow-hidden">
                    <img
                      src={r.imageUrl || COVER_FALLBACK}
                      alt={r.title}
                      loading="lazy"
                      className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
                    />
                  </div>
                  <div className="p-4">
                    <h3 className="font-display text-lg">{r.title}</h3>
                  </div>
                </Link>
                <button
                  onClick={() => removeMutation.mutate(r.recipeId)}
                  disabled={removeMutation.isPending}
                  className="absolute top-3 right-3 h-8 w-8 grid place-items-center rounded-full bg-black/40 backdrop-blur-md text-white opacity-0 group-hover:opacity-100 transition-opacity hover:bg-black/60"
                  aria-label="Remove from collection"
                >
                  <X className="h-4 w-4" />
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="mt-12 max-w-md">
        <Label className="text-xs uppercase tracking-wider text-muted-foreground">
          Add a recipe
        </Label>
        {allRecipes.length === 0 ? (
          <p className="mt-2 text-sm text-muted-foreground italic">
            You don't have any recipes yet —{" "}
            <Link to="/add-recipe" className="text-primary font-medium">
              add one
            </Link>
            .
          </p>
        ) : available.length === 0 ? (
          <p className="mt-2 text-sm text-muted-foreground italic">
            Every recipe in your library is already in this collection.
          </p>
        ) : (
          <select
            disabled={addMutation.isPending}
            onChange={(e) => {
              if (e.target.value) {
                addMutation.mutate(Number(e.target.value));
                e.target.value = "";
              }
            }}
            className="mt-2 w-full rounded-md border border-border bg-card px-3 py-2 text-sm"
          >
            <option value="">Choose a recipe…</option>
            {available.map((r) => (
              <option key={r.id} value={r.id}>
                {r.title}
              </option>
            ))}
          </select>
        )}
      </section>
    </div>
  );
}
