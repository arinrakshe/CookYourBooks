# Dark Mode — Feature Rationale

## Why This Feature

<!-- TODO: Replace this placeholder with your own reason in 1-2 sentences. -->
Dark mode was selected because it is a widely expected quality-of-life feature in modern desktop
applications, and it directly addresses a real usability gap in the current CookYourBooks interface,
which uses a uniformly bright white content area with no alternative theme.

## User Need

The primary persona this feature serves is **Bob Coolio** (age 34, meal-prep enthusiast, finance
professional). Bob commonly uses CookYourBooks in the evening after his 9-to-5 workday — the exact
context in which a bright white interface causes the most eye strain. He has medium technical
comfort and expects apps to behave like other modern tools he uses daily (iPhone, Google Calendar),
nearly all of which offer a dark or low-brightness mode. Without a dark mode, Bob must either
increase his device's brightness to read the recipe content area clearly, or tolerate the glare
while cooking. Neither is acceptable for a tool meant to make cooking easier.

A secondary benefit extends to **Taylor Rodriguez** (college student, apartment cook), who may cook
late at night in a dimly lit kitchen. For Taylor, the bright default theme creates unnecessary
visual discomfort in her primary cooking environment.

## Alternatives Considered

**System-preference detection only (no manual toggle):** JavaFX does not expose the OS
dark-mode preference as a stable, cross-platform API in Java 17. Relying on it would have made the
feature brittle across macOS, Windows, and Linux. A manual toggle is more reliable and gives the
user explicit control.

**Per-view theming:** Applying a dark stylesheet only to individual feature views (Library, Editor,
etc.) was considered but rejected — it would create visual inconsistency across the app and
complicate the CSS significantly. A single Scene-level stylesheet swap is simpler and consistent.

**Separate `ThemeService` class:** Adding dark mode state to a dedicated service class was
considered. We chose to add it to the existing `NavigationService` instead, since that class
already holds other app-wide state (unit system, current view) that is shared across all feature
ViewModels. Keeping shared state in one place reduces the number of objects that must be threaded
through the wiring in `CookYourBooksGuiApp`.
