package app.cookyourbooks.cli.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Registry and dispatcher for CLI commands. */
public final class CommandRegistry {

  private final Map<String, Command> commands = new LinkedHashMap<>();
  private final List<Command> commandList = new ArrayList<>();

  /** Creates a new CommandRegistry. */
  public CommandRegistry() {}

  /**
   * Registers a command. The primary name and any aliases are used for lookup.
   *
   * @param command the command to register
   */
  public void register(Command command) {
    commandList.add(command);
    commands.put(command.getName().toLowerCase(Locale.ROOT), command);
  }

  /**
   * Registers an alias for an existing command.
   *
   * @param alias the alias name to register
   * @param command the command to associate with the alias
   */
  public void registerAlias(String alias, Command command) {
    commands.put(alias.toLowerCase(Locale.ROOT), command);
  }

  /**
   * Finds a command by name (case-insensitive).
   *
   * @param name the command name to look up
   * @return an Optional containing the command if found
   */
  public Optional<Command> find(String name) {
    return Optional.ofNullable(commands.get(name.toLowerCase(Locale.ROOT)));
  }

  /** Returns all registered commands in registration order. */
  public List<Command> getAllCommands() {
    return List.copyOf(commandList);
  }

  /** Returns all command names for tab completion. */
  public List<String> getAllCommandNames() {
    return new ArrayList<>(commands.keySet());
  }
}
