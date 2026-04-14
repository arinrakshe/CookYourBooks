package app.cookyourbooks.cli.commands;

import java.util.List;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.CliContext;

/** Interface for CLI commands. */
public interface Command {

  /**
   * Returns the primary command name (e.g., "show", "shopping-list").
   *
   * @return the command name
   */
  @NonNull String getName();

  /**
   * Returns a brief description for the help listing.
   *
   * @return the short description
   */
  @NonNull String getDescription();

  /**
   * Returns detailed help including syntax and examples.
   *
   * @return the detailed help text
   */
  @NonNull String getDetailedHelp();

  /**
   * Returns the category for grouping in help (Library, Recipe, Tools, General).
   *
   * @return the category name
   */
  @NonNull String getCategory();

  /**
   * Executes the command with the given arguments.
   *
   * @param args the command arguments
   * @param context the shared CLI context
   */
  void execute(@NonNull List<String> args, @NonNull CliContext context);
}
