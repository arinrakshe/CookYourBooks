package app.cookyourbooks.cli;

import java.io.PrintWriter;

import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.jspecify.annotations.NonNull;

import app.cookyourbooks.cli.completion.CookModeHolder;
import app.cookyourbooks.services.CookingService;
import app.cookyourbooks.services.LibrarianService;
import app.cookyourbooks.services.PlannerService;
import app.cookyourbooks.services.TransformerService;

/**
 * Shared context for CLI commands: services and I/O.
 *
 * @param librarianService the librarian service for collection and recipe operations
 * @param cookingService the cooking service for cook mode sessions
 * @param plannerService the planner service for shopping lists and export
 * @param transformerService the transformer service for scale and convert operations
 * @param terminal the JLine terminal
 * @param lineReader the JLine line reader for input
 * @param out the print writer for output
 * @param cookModeHolder mutable holder tracking cook mode state
 */
public record CliContext(
    @NonNull LibrarianService librarianService,
    @NonNull CookingService cookingService,
    @NonNull PlannerService plannerService,
    @NonNull TransformerService transformerService,
    @NonNull Terminal terminal,
    @NonNull LineReader lineReader,
    @NonNull PrintWriter out,
    @NonNull CookModeHolder cookModeHolder) {

  /**
   * Writes a line to the terminal output.
   *
   * @param s the string to print
   */
  public void println(String s) {
    out.println(s);
    out.flush();
  }

  /**
   * Writes text to the terminal output (no newline).
   *
   * @param s the string to print
   */
  public void print(String s) {
    out.print(s);
    out.flush();
  }

  /**
   * Reads a line from the user with the given prompt.
   *
   * @param prompt the prompt to display
   * @return the line entered by the user
   */
  public String readLine(String prompt) {
    return lineReader.readLine(prompt);
  }
}
