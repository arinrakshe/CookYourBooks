import { Link } from "@tanstack/react-router";
import { Clock, Users, ArrowUpRight } from "lucide-react";

export interface RecipeSummary {
  id: string | number;
  title: string;
  description?: string;
  imageUrl?: string;
  photoUrl?: string;
  image?: string;
  servings?: number;
  cookTime?: string | number;
  time?: string | number;
  category?: string;
  tags?: string[];
}

const FALLBACK =
  "https://images.unsplash.com/photo-1495521821757-a1efb6729352?w=1200&q=80";

export function RecipeCard({ recipe }: { recipe: RecipeSummary }) {
  const img = recipe.imageUrl || recipe.photoUrl || recipe.image || FALLBACK;
  return (
    <Link
      to="/recipes/$id"
      params={{ id: String(recipe.id) }}
      className="group block"
    >
      <article className="glass glow-border relative overflow-hidden rounded-3xl transition-all duration-500 hover:-translate-y-2 hover:shadow-glow">
        <div className="relative aspect-[4/5] overflow-hidden">
          <img
            src={img}
            alt={recipe.title}
            loading="lazy"
            className="h-full w-full object-cover transition-transform duration-[1200ms] ease-out group-hover:scale-110"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/30 to-transparent" />

          {recipe.category && (
            <span className="absolute top-4 left-4 inline-flex items-center rounded-full bg-black/40 backdrop-blur-md border border-[var(--gold)]/30 px-3 py-1 text-[10px] uppercase tracking-[0.22em] text-[var(--gold)] font-medium">
              {recipe.category}
            </span>
          )}

          <span className="absolute top-4 right-4 grid h-9 w-9 place-items-center rounded-full bg-white/10 backdrop-blur-md border border-white/20 text-white opacity-0 -translate-y-1 group-hover:opacity-100 group-hover:translate-y-0 transition-all duration-500">
            <ArrowUpRight className="h-4 w-4" />
          </span>

          <div className="absolute inset-x-0 bottom-0 p-6">
            <h3 className="font-display text-2xl sm:text-3xl leading-tight text-white drop-shadow-lg">
              {recipe.title}
            </h3>
            {recipe.description && (
              <p className="mt-2 text-sm text-white/70 line-clamp-2">
                {recipe.description}
              </p>
            )}
            <div className="mt-4 flex items-center gap-5 text-xs text-white/60">
              {recipe.servings != null && (
                <span className="inline-flex items-center gap-1.5">
                  <Users className="h-3.5 w-3.5 text-[var(--gold)]" />
                  {recipe.servings} servings
                </span>
              )}
              {(recipe.cookTime || recipe.time) && (
                <span className="inline-flex items-center gap-1.5">
                  <Clock className="h-3.5 w-3.5 text-[var(--gold)]" />
                  {recipe.cookTime || recipe.time}
                  {typeof (recipe.cookTime || recipe.time) === "number"
                    ? " min"
                    : ""}
                </span>
              )}
            </div>
          </div>
        </div>
      </article>
    </Link>
  );
}
