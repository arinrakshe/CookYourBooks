import Link from "next/link";
import type { Recipe } from "@/lib/types";

interface Props {
  recipe: Recipe;
}

export function RecipeCard({ recipe }: Props) {
  return (
    <Link
      href={`/recipes/${recipe.id}`}
      className="card group overflow-hidden flex flex-col hover:-translate-y-0.5 hover:shadow-lg transition"
    >
      <div className="aspect-[4/3] bg-butter/60 relative overflow-hidden">
        {recipe.imageUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={recipe.imageUrl}
            alt={recipe.title}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-5xl text-terracotta/40">
            🥘
          </div>
        )}
      </div>
      <div className="p-4 flex-1 flex flex-col gap-2">
        <h3 className="font-display text-lg font-semibold line-clamp-2">
          {recipe.title}
        </h3>
        {recipe.description && (
          <p className="text-sm text-coffee-light line-clamp-2">
            {recipe.description}
          </p>
        )}
        <div className="mt-auto flex items-center gap-3 text-xs text-coffee-light pt-2">
          <span>{recipe.ingredients.length} ingredients</span>
          {recipe.servings != null && (
            <span>· serves {formatServings(recipe.servings)}</span>
          )}
        </div>
      </div>
    </Link>
  );
}

function formatServings(value: number): string {
  return Number.isInteger(value) ? value.toString() : value.toFixed(1);
}
