import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./app/**/*.{ts,tsx}",
    "./components/**/*.{ts,tsx}",
    "./lib/**/*.{ts,tsx}"
  ],
  theme: {
    extend: {
      colors: {
        cream: "#FAF6EE",
        butter: "#F1E4C8",
        terracotta: {
          DEFAULT: "#C2531D",
          light: "#E07A3D",
          dark: "#9C3F0F"
        },
        sage: {
          DEFAULT: "#87A878",
          dark: "#557040"
        },
        coffee: {
          DEFAULT: "#3C2A1E",
          light: "#6B5544"
        },
        honey: "#D4A574"
      },
      fontFamily: {
        sans: ["ui-sans-serif", "system-ui", "-apple-system", "Segoe UI", "Roboto", "sans-serif"],
        display: ["ui-serif", "Georgia", "Cambria", "Times New Roman", "serif"]
      },
      boxShadow: {
        card: "0 1px 2px rgba(60, 42, 30, 0.06), 0 4px 12px rgba(60, 42, 30, 0.08)"
      }
    }
  },
  plugins: []
};

export default config;
