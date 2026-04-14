package app.cookyourbooks.services;

import org.jspecify.annotations.NonNull;

import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.Recipe;

/**
 * Session for step-by-step cooking mode.
 *
 * <p>Tracks the current step and supports navigation (next, previous). State is held in the session
 * — the Cook actor's workflow.
 */
public interface CookingSession {

  /**
   * Returns the recipe being cooked.
   *
   * @return the recipe
   */
  @NonNull Recipe getRecipe();

  /**
   * Returns the current step index (1-based).
   *
   * @return the current step number
   */
  int getCurrentStep();

  /**
   * Returns the total number of steps.
   *
   * @return the total step count
   */
  int getTotalSteps();

  /**
   * Returns the instruction for the current step.
   *
   * @return the current instruction
   */
  @NonNull Instruction getCurrentInstruction();

  /**
   * Returns true if there is a next step.
   *
   * @return true if a next step exists
   */
  boolean hasNext();

  /**
   * Returns true if there is a previous step.
   *
   * @return true if a previous step exists
   */
  boolean hasPrevious();

  /** Advances to the next step. No-op if already at the last step. */
  void next();

  /** Goes back to the previous step. No-op if already at the first step. */
  void previous();

  /**
   * Returns true if the session has finished (user advanced past the last step).
   *
   * @return true if cooking is finished
   */
  boolean isFinished();
}
