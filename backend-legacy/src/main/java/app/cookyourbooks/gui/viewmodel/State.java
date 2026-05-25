package app.cookyourbooks.gui.viewmodel;

public enum State {
  IDLE, // No import in progress
  PROCESSING, // Import is in progress
  ERROR, // Import failed with an error
  REVIEW // Import completed and is under review
}
