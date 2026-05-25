import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { ArrowLeft, Clock, Minus, Plus, Users } from "lucide-react";

export const Route = createFileRoute("/recipes/$id")({
  component: RecipeDetail,
});

interface Ingredient {
  name: string;
  quantity?: number;
  amount?: number;
  unit?: string;
}

interface Recipe {
  id: string | number;
  title: string;
  description?: string;
  imageUrl?: string;
  photoUrl?: string;
  image?: string;
  servings?: number;
  cookTime?: string | number;
  ingredients?: (Ingredient | string)[];
  steps?: string[];
  instructions?: string[];
}

function RecipeDetail() {
  const { id } = Route.useParams();
  const { data, isLoading, error } = useQuery<Recipe>({
    queryKey: ["recipe", id],
    queryFn: () => api(`/api/recipes/${id}`),
  });

  const baseServings = data?.servings || 4;
  const [servings, setServings] = useState(baseServings);

  // Sync once data loads
  if (data && servings === 4 && baseServings !== 4) {
    // no-op; we'll let user click to adjust
  }

  if (isLoading) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-12">
        <div className="aspect-[16/9] rounded-3xl bg-muted animate-pulse" />
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-20 text-center">
        <h1 className="font-display text-3xl">Recipe not found</h1>
        <p className="mt-2 text-muted-foreground">
          {(error as Error)?.message || "Try another one."}
        </p>
        <Button asChild className="mt-6">
          <Link to="/">Back to recipes</Link>
        </Button>
      </div>
    );
  }

  const img =
    data.imageUrl ||
    data.photoUrl ||
    data.image ||
    "https://images.unsplash.com/photo-1495521821757-a1efb6729352?w=1200&q=80";
  const steps = data.steps || data.instructions || [];
  const scale = servings / (baseServings || 1);

  return (
    <article className="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8 py-8 lg:py-12">
      <Link
        to="/"
        className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" /> Back
      </Link>

      <header className="mt-6">
        <h1 className="font-display text-4xl sm:text-5xl lg:text-6xl leading-tight">
          {data.title}
        </h1>
        {data.description && (
          <p className="mt-4 text-lg text-muted-foreground max-w-2xl">
            {data.description}
          </p>
        )}
        <div className="mt-5 flex flex-wrap gap-5 text-sm text-muted-foreground">
          {data.cookTime && (
            <span className="inline-flex items-center gap-1.5">
              <Clock className="h-4 w-4" /> {data.cookTime}
              {typeof data.cookTime === "number" ? " min" : ""}
            </span>
          )}
          {baseServings && (
            <span className="inline-flex items-center gap-1.5">
              <Users className="h-4 w-4" /> {baseServings} servings
            </span>
          )}
        </div>
      </header>

      <img
        src={img}
        alt={data.title}
        className="mt-8 w-full aspect-[16/9] object-cover rounded-3xl shadow-soft"
      />

      <div className="grid lg:grid-cols-[1fr_1.4fr] gap-10 lg:gap-16 mt-12">
        {/* Ingredients */}
        <aside>
          <div className="sticky top-24 rounded-2xl border border-border bg-card p-6 shadow-card">
            <h2 className="font-display text-2xl">Ingredients</h2>

            <div className="mt-4 flex items-center justify-between rounded-full bg-muted p-1">
              <button
                onClick={() => setServings((s) => Math.max(1, s - 1))}
                className="h-9 w-9 grid place-items-center rounded-full bg-card shadow hover:bg-accent"
                aria-label="Decrease servings"
              >
                <Minus className="h-4 w-4" />
              </button>
              <div className="text-center">
                <div className="font-display text-2xl leading-none">
                  {servings}
                </div>
                <div className="text-[10px] uppercase tracking-wider text-muted-foreground">
                  servings
                </div>
              </div>
              <button
                onClick={() => setServings((s) => s + 1)}
                className="h-9 w-9 grid place-items-center rounded-full bg-card shadow hover:bg-accent"
                aria-label="Increase servings"
              >
                <Plus className="h-4 w-4" />
              </button>
            </div>

            <ul className="mt-6 space-y-3">
              {(data.ingredients ?? []).map((ing, i) => {
                if (typeof ing === "string") {
                  return (
                    <li
                      key={i}
                      className="flex gap-3 text-sm border-b border-border/60 pb-3 last:border-0"
                    >
                      <span className="text-primary">•</span> {ing}
                    </li>
                  );
                }
                const qty = ing.quantity ?? ing.amount;
                const scaled =
                  qty != null ? formatQty(qty * scale) : null;
                return (
                  <li
                    key={i}
                    className="flex justify-between gap-3 text-sm border-b border-border/60 pb-3 last:border-0"
                  >
                    <span>{ing.name}</span>
                    <span className="text-muted-foreground tabular-nums">
                      {scaled} {ing.unit ?? ""}
                    </span>
                  </li>
                );
              })}
            </ul>
          </div>
        </aside>

        {/* Steps */}
        <section>
          <h2 className="font-display text-3xl">Method</h2>
          <ol className="mt-6 space-y-6">
            {steps.map((step, i) => (
              <li key={i} className="flex gap-5">
                <span className="shrink-0 grid place-items-center h-10 w-10 rounded-full bg-primary text-primary-foreground font-display text-lg">
                  {i + 1}
                </span>
                <p className="pt-1.5 text-base leading-relaxed text-foreground/90">
                  {step}
                </p>
              </li>
            ))}
          </ol>
        </section>
      </div>
    </article>
  );
}

function formatQty(n: number): string {
  if (!isFinite(n)) return "";
  const rounded = Math.round(n * 100) / 100;
  return rounded.toString();
}
