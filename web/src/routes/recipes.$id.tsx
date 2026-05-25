import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { recipesApi, type Recipe, type RecipeIngredient } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { ArrowLeft, Minus, Plus, Trash2, Users } from "lucide-react";
import { toast } from "sonner";

export const Route = createFileRoute("/recipes/$id")({
  component: RecipeDetail,
});

const FALLBACK_IMG =
  "https://images.unsplash.com/photo-1495521821757-a1efb6729352?w=1200&q=80";

function RecipeDetail() {
  const { id } = Route.useParams();
  const navigate = useNavigate();
  const qc = useQueryClient();

  const { data, isLoading, error } = useQuery<Recipe>({
    queryKey: ["recipe", id],
    queryFn: () => recipesApi.get(id),
  });

  const baseServings = data?.servings && data.servings > 0 ? data.servings : 1;
  const [servings, setServings] = useState<number>(baseServings);

  useEffect(() => {
    if (data?.servings && data.servings > 0) {
      setServings(data.servings);
    }
  }, [data?.id, data?.servings]);

  const deleteMutation = useMutation({
    mutationFn: () => recipesApi.remove(id),
    onSuccess: () => {
      toast.success("Recipe deleted");
      qc.invalidateQueries({ queryKey: ["recipes"] });
      navigate({ to: "/" });
    },
    onError: (e: Error) => toast.error(e.message),
  });

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

  const img = data.imageUrl || FALLBACK_IMG;
  const scale = servings / baseServings;

  return (
    <article className="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8 py-8 lg:py-12">
      <Link
        to="/"
        className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" /> Back
      </Link>

      <header className="mt-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="font-display text-4xl sm:text-5xl lg:text-6xl leading-tight">
            {data.title}
          </h1>
          {data.description && (
            <p className="mt-4 text-lg text-muted-foreground max-w-2xl">
              {data.description}
            </p>
          )}
          <div className="mt-5 flex flex-wrap gap-5 text-sm text-muted-foreground">
            {data.servings != null && (
              <span className="inline-flex items-center gap-1.5">
                <Users className="h-4 w-4" /> {formatQty(data.servings)} servings
              </span>
            )}
          </div>
        </div>
        <Button
          variant="ghost"
          size="sm"
          disabled={deleteMutation.isPending}
          onClick={() => {
            if (confirm(`Delete "${data.title}"? This cannot be undone.`)) {
              deleteMutation.mutate();
            }
          }}
          className="text-muted-foreground hover:text-destructive"
        >
          <Trash2 className="h-4 w-4 mr-1.5" />
          {deleteMutation.isPending ? "Deleting…" : "Delete"}
        </Button>
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
                onClick={() => setServings((s) => Math.max(0.5, s - 0.5))}
                className="h-9 w-9 grid place-items-center rounded-full bg-card shadow hover:bg-accent"
                aria-label="Decrease servings"
              >
                <Minus className="h-4 w-4" />
              </button>
              <div className="text-center">
                <div className="font-display text-2xl leading-none">
                  {formatQty(servings)}
                </div>
                <div className="text-[10px] uppercase tracking-wider text-muted-foreground">
                  servings
                </div>
              </div>
              <button
                onClick={() => setServings((s) => s + 0.5)}
                className="h-9 w-9 grid place-items-center rounded-full bg-card shadow hover:bg-accent"
                aria-label="Increase servings"
              >
                <Plus className="h-4 w-4" />
              </button>
            </div>

            {data.ingredients.length === 0 ? (
              <p className="mt-6 text-sm text-muted-foreground italic">
                No ingredients listed.
              </p>
            ) : (
              <ul className="mt-6 space-y-3">
                {data.ingredients.map((ing) => (
                  <IngredientRow key={ing.id} ingredient={ing} scale={scale} />
                ))}
              </ul>
            )}
          </div>
        </aside>

        {/* Steps */}
        <section>
          <h2 className="font-display text-3xl">Method</h2>
          {data.steps.length === 0 ? (
            <p className="mt-6 text-muted-foreground italic">
              No instructions yet.
            </p>
          ) : (
            <ol className="mt-6 space-y-6">
              {data.steps.map((step, i) => (
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
          )}
        </section>
      </div>

      {data.notes && (
        <section className="mt-12 rounded-2xl border border-border bg-card p-6 shadow-card">
          <h2 className="font-display text-2xl">Notes</h2>
          <p className="mt-3 text-muted-foreground whitespace-pre-line">
            {data.notes}
          </p>
        </section>
      )}
    </article>
  );
}

function IngredientRow({
  ingredient,
  scale,
}: {
  ingredient: RecipeIngredient;
  scale: number;
}) {
  const scaled =
    ingredient.quantity != null ? formatQty(ingredient.quantity * scale) : null;
  return (
    <li className="flex justify-between gap-3 text-sm border-b border-border/60 pb-3 last:border-0">
      <span className="flex-1">
        {ingredient.rawText}
        {ingredient.preparation && (
          <span className="text-muted-foreground"> — {ingredient.preparation}</span>
        )}
      </span>
      {scaled != null && (
        <span className="text-muted-foreground tabular-nums whitespace-nowrap">
          {scaled} {ingredient.unitCode ?? ""}
        </span>
      )}
    </li>
  );
}

function formatQty(n: number): string {
  if (!isFinite(n)) return "";
  const rounded = Math.round(n * 100) / 100;
  return rounded.toString().replace(/\.?0+$/, "");
}
