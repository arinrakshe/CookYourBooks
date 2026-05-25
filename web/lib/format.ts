// Formats a numeric quantity for display. Returns a short, readable string —
// no trailing zeros, max two decimals, with common simple fractions used when
// the value rounds cleanly to halves, thirds, or quarters.

export function formatQuantity(value: number): string {
  if (!Number.isFinite(value)) return "";
  if (Math.abs(value) < 0.001) return "0";

  const whole = Math.trunc(value);
  const fraction = Math.abs(value - whole);
  const fractionLabel = matchFraction(fraction);
  if (fractionLabel !== null) {
    if (whole === 0) return (value < 0 ? "-" : "") + fractionLabel;
    return `${whole} ${fractionLabel}`;
  }

  const rounded = Math.round(value * 100) / 100;
  return rounded
    .toString()
    .replace(/\.?0+$/, "");
}

function matchFraction(fraction: number): string | null {
  const candidates: [number, string][] = [
    [0, ""],
    [1 / 4, "¼"],
    [1 / 3, "⅓"],
    [1 / 2, "½"],
    [2 / 3, "⅔"],
    [3 / 4, "¾"]
  ];
  for (const [value, label] of candidates) {
    if (Math.abs(fraction - value) < 0.02) {
      return label || null;
    }
  }
  return null;
}
