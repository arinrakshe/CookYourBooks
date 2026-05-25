interface Props {
  steps: string[];
}

export function StepsList({ steps }: Props) {
  if (steps.length === 0) {
    return (
      <p className="text-sm text-coffee-light italic">
        No instructions provided.
      </p>
    );
  }
  return (
    <ol className="space-y-4">
      {steps.map((step, index) => (
        <li key={index} className="flex gap-4">
          <span className="flex-shrink-0 w-8 h-8 rounded-full bg-terracotta text-white text-sm font-semibold flex items-center justify-center">
            {index + 1}
          </span>
          <p className="text-coffee leading-relaxed pt-1">{step}</p>
        </li>
      ))}
    </ol>
  );
}
