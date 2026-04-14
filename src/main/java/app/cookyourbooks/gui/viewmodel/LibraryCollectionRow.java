package app.cookyourbooks.gui.viewmodel;

import app.cookyourbooks.model.SourceType;

/**
 * Row displayed in the library's collection list (cards or list view).
 *
 * @param id collection id
 * @param title collection title
 * @param sourceType source type for display
 * @param recipeCount number of recipes in the collection
 */
public record LibraryCollectionRow(
    String id, String title, SourceType sourceType, int recipeCount) {}
