import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { authApi, setStoredUser, setToken } from "@/lib/api";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import { ChefHat } from "lucide-react";

export const Route = createFileRoute("/login")({
  component: LoginPage,
});

function LoginPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: "", password: "" });

  const mutation = useMutation({
    mutationFn: (payload: typeof form) => authApi.login(payload),
    onSuccess: (data) => {
      setToken(data.token);
      setStoredUser(data.user);
      toast.success("Welcome back!");
      navigate({ to: "/" });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  return (
    <AuthShell title="Welcome back" subtitle="Sign in to keep cooking.">
      <form
        onSubmit={(e) => {
          e.preventDefault();
          mutation.mutate(form);
        }}
        className="space-y-4"
      >
        <div className="space-y-2">
          <Label>Email</Label>
          <Input
            type="email"
            required
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
          />
        </div>
        <div className="space-y-2">
          <Label>Password</Label>
          <Input
            type="password"
            required
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
          />
        </div>
        <Button
          type="submit"
          disabled={mutation.isPending}
          className="w-full h-11 rounded-full"
        >
          {mutation.isPending ? "Signing in…" : "Sign in"}
        </Button>
      </form>
      <p className="text-sm text-muted-foreground text-center mt-6">
        New here?{" "}
        <Link to="/register" className="text-primary font-medium">
          Create account
        </Link>
      </p>
    </AuthShell>
  );
}

export function AuthShell({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-[calc(100vh-4rem)] grid lg:grid-cols-2">
      <div className="hidden lg:flex relative items-end p-12 bg-gradient-to-br from-primary/10 via-accent to-[color:var(--sage)]/15">
        <div>
          <span className="grid h-12 w-12 place-items-center rounded-full bg-primary text-primary-foreground shadow-soft">
            <ChefHat className="h-6 w-6" />
          </span>
          <h2 className="mt-6 font-display text-5xl leading-tight max-w-md">
            Every recipe you love, in one warm place.
          </h2>
          <p className="mt-4 text-muted-foreground max-w-md">
            Scale by servings, build shopping lists, and snap photos to import.
          </p>
        </div>
      </div>
      <div className="flex items-center justify-center px-4 py-12">
        <div className="w-full max-w-md">
          <h1 className="font-display text-4xl">{title}</h1>
          <p className="mt-2 text-muted-foreground">{subtitle}</p>
          <div className="mt-8">{children}</div>
        </div>
      </div>
    </div>
  );
}
