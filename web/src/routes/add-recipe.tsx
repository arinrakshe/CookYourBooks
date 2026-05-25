import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { ocrApi, recipesApi, type RecipeInput } from "@/lib/api";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { toast } from "sonner";
import { Camera, PenLine, Upload } from "lucide-react";

export const Route = createFileRoute("/add-recipe")({
  component: AddRecipe,
});

function AddRecipe() {
  return (
    <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8 py-10 lg:py-14">
      <header className="mb-8">
        <h1 className="font-display text-4xl sm:text-5xl">Add a recipe</h1>
        <p className="mt-2 text-muted-foreground">
          Type it in or let our OCR read it from a photo.
        </p>
      </header>

      <Tabs defaultValue="manual" className="w-full">
        <TabsList className="grid w-full grid-cols-2 rounded-full p-1 h-12 bg-muted">
          <TabsTrigger value="manual" className="rounded-full gap-2">
            <PenLine className="h-4 w-4" /> Manual
          </TabsTrigger>
          <TabsTrigger value="ocr" className="rounded-full gap-2">
            <Camera className="h-4 w-4" /> Scan photo
          </TabsTrigger>
        </TabsList>
        <TabsContent value="manual" className="mt-6">
          <ManualForm />
        </TabsContent>
        <TabsContent value="ocr" className="mt-6">
          <OcrForm />
        </TabsContent>
      </Tabs>
    </div>
  );
}

function ManualForm() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    title: "",
    description: "",
    imageUrl: "",
    servings: 4,
    ingredients: "",
    steps: "",
  });

  const mutation = useMutation({
    mutationFn: (payload: RecipeInput) => recipesApi.create(payload),
    onSuccess: (recipe) => {
      toast.success("Recipe saved!");
      navigate({ to: "/recipes/$id", params: { id: String(recipe.id) } });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.title.trim()) return toast.error("Title is required");

    const ingredients = form.ingredients
      .split("\n")
      .map((line) => line.trim())
      .filter(Boolean)
      .map((rawText) => ({ rawText }));

    const steps = form.steps
      .split("\n")
      .map((line) => line.trim())
      .filter(Boolean);

    mutation.mutate({
      title: form.title.trim(),
      description: form.description.trim() || null,
      imageUrl: form.imageUrl.trim() || null,
      servings: Number(form.servings) || null,
      steps,
      ingredients,
    });
  };

  return (
    <form
      onSubmit={submit}
      className="space-y-5 rounded-3xl border border-border bg-card p-6 sm:p-8 shadow-card"
    >
      <Field label="Title">
        <Input
          value={form.title}
          onChange={(e) => setForm({ ...form, title: e.target.value })}
          placeholder="Tomato basil pasta"
        />
      </Field>
      <Field label="Description">
        <Textarea
          value={form.description}
          onChange={(e) => setForm({ ...form, description: e.target.value })}
          placeholder="A weeknight favorite…"
          rows={2}
        />
      </Field>
      <div className="grid sm:grid-cols-2 gap-5">
        <Field label="Photo URL">
          <Input
            value={form.imageUrl}
            onChange={(e) => setForm({ ...form, imageUrl: e.target.value })}
            placeholder="https://…"
          />
        </Field>
        <Field label="Servings">
          <Input
            type="number"
            min={1}
            step="0.5"
            value={form.servings}
            onChange={(e) =>
              setForm({ ...form, servings: Number(e.target.value) })
            }
          />
        </Field>
      </div>
      <Field label="Ingredients (one per line)">
        <Textarea
          value={form.ingredients}
          onChange={(e) => setForm({ ...form, ingredients: e.target.value })}
          rows={6}
          placeholder={"200g spaghetti\n4 ripe tomatoes\n2 cloves garlic"}
        />
      </Field>
      <Field label="Steps (one per line)">
        <Textarea
          value={form.steps}
          onChange={(e) => setForm({ ...form, steps: e.target.value })}
          rows={6}
          placeholder={"Boil pasta…\nMake sauce…"}
        />
      </Field>

      <Button
        type="submit"
        disabled={mutation.isPending}
        className="w-full h-12 rounded-full text-base"
      >
        {mutation.isPending ? "Saving…" : "Save recipe"}
      </Button>
    </form>
  );
}

function OcrForm() {
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const navigate = useNavigate();

  const mutation = useMutation({
    mutationFn: (f: File) => ocrApi.importImage(f),
    onSuccess: (data) => {
      toast.success("Recipe imported!");
      navigate({ to: "/recipes/$id", params: { id: String(data.recipe.id) } });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  return (
    <div className="rounded-3xl border border-border bg-card p-6 sm:p-8 shadow-card">
      <label className="block">
        <div className="rounded-2xl border-2 border-dashed border-border bg-muted/40 p-10 text-center hover:bg-muted transition cursor-pointer">
          {preview ? (
            <img
              src={preview}
              alt="Preview"
              className="mx-auto max-h-80 rounded-xl object-contain"
            />
          ) : (
            <>
              <Upload className="mx-auto h-10 w-10 text-muted-foreground" />
              <p className="mt-3 font-medium">Drop a recipe photo</p>
              <p className="text-sm text-muted-foreground">
                We'll read the ingredients and steps for you
              </p>
            </>
          )}
          <input
            type="file"
            accept="image/*"
            className="sr-only"
            onChange={(e) => {
              const f = e.target.files?.[0];
              if (!f) return;
              setFile(f);
              setPreview(URL.createObjectURL(f));
            }}
          />
        </div>
      </label>

      <Button
        disabled={!file || mutation.isPending}
        onClick={() => file && mutation.mutate(file)}
        className="w-full h-12 rounded-full text-base mt-6"
      >
        {mutation.isPending ? "Reading photo…" : "Import recipe"}
      </Button>
    </div>
  );
}

function Field({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-2">
      <Label className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
        {label}
      </Label>
      {children}
    </div>
  );
}
