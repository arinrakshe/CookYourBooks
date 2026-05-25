import { createFileRoute } from "@tanstack/react-router";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Sparkles, ShoppingBasket } from "lucide-react";
import { toast } from "sonner";

export const Route = createFileRoute("/shopping-list")({
  component: ShoppingListPage,
});

interface Item {
  id: string | number;
  name: string;
  quantity?: string | number;
  unit?: string;
  checked?: boolean;
  done?: boolean;
}

interface ShoppingList {
  id?: string | number;
  items: Item[];
}

function ShoppingListPage() {
  const qc = useQueryClient();
  const { data, isLoading, error } = useQuery<ShoppingList>({
    queryKey: ["shopping-list"],
    queryFn: async () => {
      const d: any = await api("/api/shopping-lists");
      const list = Array.isArray(d) ? d[0] : (d.list ?? d.shoppingList ?? d);
      return { items: list?.items ?? [], id: list?.id };
    },
  });

  const [items, setItems] = useState<Item[]>([]);
  useEffect(() => {
    if (data?.items) setItems(data.items);
  }, [data]);

  const toggle = (idx: number) => {
    setItems((prev) =>
      prev.map((it, i) =>
        i === idx ? { ...it, checked: !(it.checked ?? it.done) } : it,
      ),
    );
  };

  const generate = useMutation({
    mutationFn: () =>
      api("/api/shopping-lists/generate", { method: "POST" }).catch(() =>
        api("/api/shopping-lists", { method: "POST" }),
      ),
    onSuccess: () => {
      toast.success("Shopping list generated!");
      qc.invalidateQueries({ queryKey: ["shopping-list"] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const remaining = items.filter((i) => !(i.checked ?? i.done)).length;

  return (
    <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8 py-10 lg:py-14">
      <div className="flex flex-wrap items-end justify-between gap-4 mb-8">
        <div>
          <h1 className="font-display text-4xl sm:text-5xl">Shopping list</h1>
          <p className="mt-2 text-muted-foreground">
            {items.length > 0
              ? `${remaining} of ${items.length} items left`
              : "Build a list from your recipes"}
          </p>
        </div>
        <Button
          onClick={() => generate.mutate()}
          disabled={generate.isPending}
          className="rounded-full gap-2"
        >
          <Sparkles className="h-4 w-4" />
          {generate.isPending ? "Generating…" : "Generate from recipes"}
        </Button>
      </div>

      <div className="rounded-3xl border border-border bg-card shadow-card overflow-hidden">
        {isLoading ? (
          <div className="p-10 text-center text-muted-foreground">Loading…</div>
        ) : error ? (
          <div className="p-10 text-center text-muted-foreground">
            {(error as Error).message}
          </div>
        ) : items.length === 0 ? (
          <div className="p-12 text-center">
            <ShoppingBasket className="mx-auto h-10 w-10 text-muted-foreground" />
            <h3 className="font-display text-2xl mt-3">Empty basket</h3>
            <p className="text-sm text-muted-foreground mt-1">
              Generate a list from your saved recipes.
            </p>
          </div>
        ) : (
          <ul className="divide-y divide-border">
            {items.map((it, i) => {
              const done = it.checked ?? it.done ?? false;
              return (
                <li
                  key={it.id ?? i}
                  className="flex items-center gap-4 px-5 py-4 hover:bg-muted/40 transition"
                >
                  <Checkbox
                    checked={done}
                    onCheckedChange={() => toggle(i)}
                    className="h-5 w-5"
                  />
                  <div
                    className={`flex-1 ${done ? "line-through text-muted-foreground" : ""}`}
                  >
                    <span className="font-medium">{it.name}</span>
                    {(it.quantity != null || it.unit) && (
                      <span className="ml-2 text-sm text-muted-foreground">
                        {it.quantity} {it.unit}
                      </span>
                    )}
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </div>
  );
}
