"use client";

import { formatQuantity } from "@/lib/format";

interface Props {
  baseServings: number;
  currentServings: number;
  onChange: (value: number) => void;
}

export function ServingScaler({ baseServings, currentServings, onChange }: Props) {
  const decrement = () => onChange(Math.max(0.5, currentServings - 0.5));
  const increment = () => onChange(currentServings + 0.5);

  return (
    <div className="flex items-center gap-3 bg-butter/40 rounded-full px-1.5 py-1.5">
      <button
        type="button"
        onClick={decrement}
        aria-label="Decrease servings"
        className="w-8 h-8 rounded-full bg-white text-coffee hover:bg-terracotta hover:text-white transition-colors flex items-center justify-center font-semibold"
      >
        −
      </button>
      <div className="text-center min-w-[6rem]">
        <div className="text-xs uppercase tracking-wide text-coffee-light">
          Servings
        </div>
        <div className="font-display text-lg font-semibold text-coffee leading-tight">
          {formatQuantity(currentServings)}
          {currentServings !== baseServings && (
            <span className="text-xs text-coffee-light font-sans font-normal ml-1">
              (×{formatQuantity(currentServings / baseServings)})
            </span>
          )}
        </div>
      </div>
      <button
        type="button"
        onClick={increment}
        aria-label="Increase servings"
        className="w-8 h-8 rounded-full bg-white text-coffee hover:bg-terracotta hover:text-white transition-colors flex items-center justify-center font-semibold"
      >
        +
      </button>
    </div>
  );
}
