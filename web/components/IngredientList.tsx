import type { RecipeIngredient } from "@/lib/types";
import { formatQuantity } from "@/lib/format";

interface Props {
  ingredients: RecipeIngredient[];
  scale: number;
}

export function IngredientList({ ingredients, scale }: Props) {
  if (ingredients.length === 0) {
    return (
      <p className="text-sm text-coffee-light italic">No ingredients listed.</p>
    );
  }
  return (
    <ul className="divide-y divide-butter/70">
      {ingredients.map((ingredient) => (
        <li key={ingredient.id} className="py-3 flex items-baseline gap-3">
          <span className="font-mono text-sm text-terracotta-dark min-w-[5rem]">
            {ingredient.quantity != null
              ? `${formatQuantity(ingredient.quantity * scale)}${
                  ingredient.unitCode ? " " + ingredient.unitCode : ""
                }`
              : ""}
          </span>
          <div className="flex-1">
            <span className="text-coffee">{ingredient.rawText}</span>
            {ingredient.preparation && (
              <span className="text-coffee-light text-sm">
                {" — " + ingredient.preparation}
              </span>
            )}
          </div>
        </li>
      ))}
    </ul>
  );
}
