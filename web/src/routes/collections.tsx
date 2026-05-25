import { createFileRoute } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { api } from "@/lib/api";
import { RecipeCard, type RecipeSummary } from "@/components/RecipeCard";
import { ArrowLeft, FolderHeart } from "lucide-react";

export const Route = createFileRoute("/collections")({
  component: CollectionsPage,
});

interface Collection {
  id: string | number;
  name: string;
  title?: string;
  description?: string;
  coverUrl?: string;
  imageUrl?: string;
  recipeCount?: number;
  recipes?: RecipeSummary[];
}

function CollectionsPage() {
  const [active, setActive] = useState<Collection | null>(null);
  const { data, isLoading, error } = useQuery<Collection[]>({
    queryKey: ["collections"],
    queryFn: () =>
      api("/api/collections").then(
        (d: any) => d.collections ?? d.data ?? d ?? [],
      ),
  });

  const collections = Array.isArray(data) ? data : [];

  if (active) {
    return (
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-10">
        <button
          onClick={() => setActive(null)}
          className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="h-4 w-4" /> All collections
        </button>
        <h1 className="font-display text-4xl sm:text-5xl mt-6">
          {active.name || active.title}
        </h1>
        {active.description && (
          <p className="mt-2 text-muted-foreground max-w-2xl">
            {active.description}
          </p>
        )}
        <CollectionRecipes collection={active} />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-10 lg:py-14">
      <header className="mb-10">
        <h1 className="font-display text-4xl sm:text-5xl">Collections</h1>
        <p className="mt-2 text-muted-foreground">
          Recipes organized into little cookbooks.
        </p>
      </header>

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
        </div>
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {collections.map((c) => (
            <button
              key={c.id}
              onClick={() => setActive(c)}
              className="group text-left overflow-hidden rounded-2xl bg-card shadow-card hover:shadow-soft hover:-translate-y-1 transition-all"
            >
              <div className="aspect-[4/3] relative overflow-hidden bg-muted">
                <img
                  src={
                    c.coverUrl ||
                    c.imageUrl ||
                    "https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=800&q=80"
                  }
                  alt={c.name || c.title || ""}
                  loading="lazy"
                  className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-105"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
                <div className="absolute bottom-4 left-5 right-5 text-white">
                  <h3 className="font-display text-2xl">
                    {c.name || c.title}
                  </h3>
                  <p className="text-xs opacity-90 mt-0.5">
                    {c.recipeCount ?? c.recipes?.length ?? 0} recipes
                  </p>
                </div>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function CollectionRecipes({ collection }: { collection: Collection }) {
  const { data, isLoading } = useQuery<RecipeSummary[]>({
    queryKey: ["collection", collection.id],
    enabled: !collection.recipes,
    queryFn: () =>
      api(`/api/collections/${collection.id}`).then(
        (d: any) => d.recipes ?? d.data ?? d ?? [],
      ),
  });

  const recipes = collection.recipes ?? (Array.isArray(data) ? data : []);

  if (isLoading) {
    return (
      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6 mt-8">
        {Array.from({ length: 3 }).map((_, i) => (
          <div
            key={i}
            className="aspect-[4/3] rounded-2xl bg-muted animate-pulse"
          />
        ))}
      </div>
    );
  }

  if (recipes.length === 0) {
    return (
      <p className="mt-8 text-muted-foreground">No recipes in this collection.</p>
    );
  }

  return (
    <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6 mt-8">
      {recipes.map((r) => (
        <RecipeCard key={r.id} recipe={r} />
      ))}
    </div>
  );
}
