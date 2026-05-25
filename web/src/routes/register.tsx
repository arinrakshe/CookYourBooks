import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { authApi, setStoredUser, setToken } from "@/lib/api";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import { AuthShell } from "./login";

export const Route = createFileRoute("/register")({
  component: RegisterPage,
});

function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ displayName: "", email: "", password: "" });

  const mutation = useMutation({
    mutationFn: (payload: typeof form) =>
      authApi.register({
        email: payload.email,
        password: payload.password,
        displayName: payload.displayName.trim() || undefined,
      }),
    onSuccess: (data) => {
      setToken(data.token);
      setStoredUser(data.user);
      toast.success("Account created!");
      navigate({ to: "/" });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  return (
    <AuthShell
      title="Start your cookbook"
      subtitle="Create an account in seconds."
    >
      <form
        onSubmit={(e) => {
          e.preventDefault();
          mutation.mutate(form);
        }}
        className="space-y-4"
      >
        <div className="space-y-2">
          <Label>Name</Label>
          <Input
            value={form.displayName}
            onChange={(e) => setForm({ ...form, displayName: e.target.value })}
            placeholder="Optional"
          />
        </div>
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
            minLength={8}
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
          />
          <p className="text-xs text-muted-foreground">At least 8 characters.</p>
        </div>
        <Button
          type="submit"
          disabled={mutation.isPending}
          className="w-full h-11 rounded-full"
        >
          {mutation.isPending ? "Creating…" : "Create account"}
        </Button>
      </form>
      <p className="text-sm text-muted-foreground text-center mt-6">
        Already have one?{" "}
        <Link to="/login" className="text-primary font-medium">
          Sign in
        </Link>
      </p>
    </AuthShell>
  );
}
