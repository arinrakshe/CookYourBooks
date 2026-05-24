package app.cookyourbooks.cli.commands;

import org.jspecify.annotations.NonNull;

/** Base class for commands with common behavior. */
public abstract class AbstractCommand implements Command {

  private final String name;
  private final String description;
  private final String detailedHelp;
  private final String category;

  /**
   * Constructs an AbstractCommand with the given metadata.
   *
   * @param name the primary command name
   * @param description the short description for the help listing
   * @param detailedHelp the detailed help text including syntax and examples
   * @param category the category for grouping in help output
   */
  protected AbstractCommand(String name, String description, String detailedHelp, String category) {
    this.name = name;
    this.description = description;
    this.detailedHelp = detailedHelp;
    this.category = category;
  }

  @Override
  public @NonNull String getName() {
    return name;
  }

  @Override
  public @NonNull String getDescription() {
    return description;
  }

  @Override
  public @NonNull String getDetailedHelp() {
    return detailedHelp;
  }

  @Override
  public @NonNull String getCategory() {
    return category;
  }
}
