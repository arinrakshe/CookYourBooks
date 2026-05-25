import { createFileRoute } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { recipesApi, type Recipe } from "@/lib/api";
import { Input } from "@/components/ui/input";
import { RecipeCard, type RecipeSummary } from "@/components/RecipeCard";
import { Search, Sparkles, ArrowDown, Flame, BookOpen, ChefHat } from "lucide-react";
import heroImg from "@/assets/hero.jpg";
import { useReveal } from "@/hooks/useReveal";

export const Route = createFileRoute("/")({
  component: HomePage,
});

function HomePage() {
  const [q, setQ] = useState("");
  const { data, isLoading, error } = useQuery<Recipe[]>({
    queryKey: ["recipes"],
    queryFn: () => recipesApi.list(),
  });

  const recipes: Recipe[] = data ?? [];
  const filtered = useMemo(() => {
    const term = q.trim().toLowerCase();
    if (!term) return recipes;
    return recipes.filter((r) => {
      const haystack = [
        r.title,
        r.description || "",
        ...r.ingredients.map((i) => i.rawText),
      ]
        .join(" ")
        .toLowerCase();
      return haystack.includes(term);
    });
  }, [q, recipes]);

  const revealRef = useReveal<HTMLDivElement>();

  return (
    <div ref={revealRef}>
      {/* HERO — fullscreen dramatic */}
      <section className="relative min-h-[100svh] -mt-16 flex items-center overflow-hidden">
        <img
          src={heroImg}
          alt="Plated gourmet dish under golden light"
          width={1920}
          height={1080}
          className="absolute inset-0 h-full w-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-[var(--background)] via-[var(--background)]/70 to-[var(--background)]/40" />
        <div className="absolute inset-0 bg-gradient-to-r from-[var(--background)] via-transparent to-transparent" />

        <div className="relative z-10 mx-auto w-full max-w-7xl px-4 sm:px-6 lg:px-8 pt-32 pb-24">
          <div className="max-w-3xl">
            <span
              className="reveal inline-flex items-center gap-2 rounded-full glass px-4 py-1.5 text-[10px] font-medium uppercase tracking-[0.28em] text-[var(--gold)]"
            >
              <Sparkles className="h-3 w-3" /> A cookbook reimagined
            </span>

            <h1 className="reveal reveal-delay-1 mt-6 font-display text-6xl sm:text-7xl lg:text-[8.5rem] leading-[0.92] text-foreground">
              Cook like
              <br />
              you mean it.
              <span className="block text-gradient-ember italic font-normal">
                Beautifully.
              </span>
            </h1>

            <p className="reveal reveal-delay-2 mt-8 text-lg sm:text-xl text-muted-foreground max-w-xl leading-relaxed">
              A meticulously crafted home for every recipe you bookmark, snap,
              or scribble. Scale by servings, generate shopping lists, and
              return to the dishes you love — in one cinematic place.
            </p>

            <div className="reveal reveal-delay-3 mt-10 relative max-w-lg">
              <Search className="absolute left-5 top-1/2 -translate-y-1/2 h-4 w-4 text-[var(--gold)]" />
              <Input
                placeholder="Search pasta, ramen, tart, biryani…"
                value={q}
                onChange={(e) => setQ(e.target.value)}
                className="pl-12 h-14 rounded-full glass border-0 text-base placeholder:text-muted-foreground/70"
              />
            </div>

            <div className="reveal reveal-delay-4 mt-14 flex items-center gap-10 text-xs uppercase tracking-[0.22em] text-muted-foreground">
              <Stat n={recipes.length || "—"} label="Recipes" />
              <span className="h-8 w-px bg-border" />
              <Stat n="∞" label="Servings scaled" />
              <span className="hidden sm:block h-8 w-px bg-border" />
              <Stat n="1" label="Beautiful kitchen" className="hidden sm:flex" />
            </div>
          </div>

          <a
            href="#recipes"
            className="hidden lg:flex absolute bottom-10 right-10 items-center gap-2 text-xs uppercase tracking-[0.3em] text-muted-foreground hover:text-foreground transition-colors"
          >
            Scroll <ArrowDown className="h-3 w-3 animate-bounce" />
          </a>
        </div>
      </section>

      {/* FEATURE STRIP */}
      <section className="border-y border-border/60 bg-[var(--background)]/60 backdrop-blur-sm">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-10 grid grid-cols-1 sm:grid-cols-3 gap-8">
          <Feature
            icon={<Flame className="h-5 w-5" />}
            title="Scale every serving"
            body="Plus and minus your way to the right portion — quantities recalc instantly."
          />
          <Feature
            icon={<BookOpen className="h-5 w-5" />}
            title="Curated collections"
            body="Group weeknight wins, dinner-party showstoppers, and weekend baking."
          />
          <Feature
            icon={<ChefHat className="h-5 w-5" />}
            title="Snap & import"
            body="Photograph any printed recipe and let OCR do the typing for you."
          />
        </div>
      </section>

      {/* Recipes grid */}
      <section
        id="recipes"
        className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-24"
      >
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-12" data-reveal>
          <div>
            <p className="text-[10px] uppercase tracking-[0.28em] text-[var(--gold)] font-medium">
              The Library
            </p>
            <h2 className="mt-3 font-display text-5xl sm:text-6xl leading-[0.95]">
              {q ? "Search results" : "Every recipe, plated."}
            </h2>
          </div>
          <p className="text-sm text-muted-foreground">
            {filtered.length} {filtered.length === 1 ? "recipe" : "recipes"}
          </p>
        </div>

        {isLoading ? (
          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {Array.from({ length: 6 }).map((_, i) => (
              <div
                key={i}
                className="aspect-[4/5] rounded-3xl glass animate-pulse"
              />
            ))}
          </div>
        ) : error ? (
          <EmptyState
            title="Couldn't reach the kitchen"
            body={(error as Error).message}
          />
        ) : filtered.length === 0 ? (
          <EmptyState
            title="No recipes yet"
            body="Add your first recipe to start your library."
          />
        ) : (
          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6 lg:gap-8">
            {filtered.map((r, i) => (
              <div
                key={r.id}
                data-reveal
                data-reveal-delay={(i % 3) * 100}
              >
                <RecipeCard recipe={r} />
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function Stat({
  n,
  label,
  className = "",
}: {
  n: string | number;
  label: string;
  className?: string;
}) {
  return (
    <div className={`flex flex-col gap-1 ${className}`}>
      <span className="font-display text-3xl text-foreground normal-case tracking-tight">
        {n}
      </span>
      <span>{label}</span>
    </div>
  );
}

function Feature({
  icon,
  title,
  body,
}: {
  icon: React.ReactNode;
  title: string;
  body: string;
}) {
  return (
    <div className="flex gap-4" data-reveal>
      <span className="shrink-0 grid h-11 w-11 place-items-center rounded-full bg-gradient-to-br from-[var(--primary)]/20 to-[var(--gold)]/10 border border-[var(--gold)]/20 text-[var(--gold)]">
        {icon}
      </span>
      <div>
        <h3 className="font-display text-xl">{title}</h3>
        <p className="mt-1 text-sm text-muted-foreground leading-relaxed">
          {body}
        </p>
      </div>
    </div>
  );
}

function EmptyState({ title, body }: { title: string; body: string }) {
  return (
    <div className="glass glow-border rounded-3xl p-16 text-center">
      <h3 className="font-display text-3xl">{title}</h3>
      <p className="mt-3 text-sm text-muted-foreground">{body}</p>
    </div>
  );
}
