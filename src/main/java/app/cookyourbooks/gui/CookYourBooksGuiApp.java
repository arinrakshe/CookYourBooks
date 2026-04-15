package app.cookyourbooks.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.cookyourbooks.CybLibrary;
import app.cookyourbooks.adapters.NutritionLookupAdapter;
import app.cookyourbooks.adapters.usda.RealUsdaClient;
import app.cookyourbooks.gui.controller.ImportController;
import app.cookyourbooks.gui.view.LibraryViewController;
import app.cookyourbooks.gui.view.MainViewController;
import app.cookyourbooks.gui.view.RecipeEditorViewController;
import app.cookyourbooks.gui.view.SearchViewController;
import app.cookyourbooks.gui.viewmodel.ImportViewModelImpl;
import app.cookyourbooks.gui.viewmodel.LibraryViewModelImpl;
import app.cookyourbooks.gui.viewmodel.RecipeEditorViewModelImpl;
import app.cookyourbooks.gui.viewmodel.SearchViewModelImpl;
import app.cookyourbooks.model.Instruction;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.model.RecipeCollection;
import app.cookyourbooks.model.VagueIngredient;
import app.cookyourbooks.services.LibrarianService;
import app.cookyourbooks.services.LibrarianServiceImpl;
import app.cookyourbooks.services.ocr.RecipeOcrService;

/**
 * JavaFX entry point for CookYourBooks.
 *
 * <p>This class wires up the service layer, creates ViewModels, and launches the main window. It
 * demonstrates the dependency injection pattern you'll use to connect your feature ViewModels to
 * the service layer and navigation.
 *
 * <h2>Wiring pattern</h2>
 *
 * <ol>
 *   <li>Load the library (repositories + conversion registry)
 *   <li>Create service-layer objects
 *   <li>Create the shared {@link NavigationService}
 *   <li>Create your feature ViewModels, injecting services via constructors
 *   <li>Load each feature's FXML, injecting the ViewModel into the controller
 *   <li>Register each feature's view with the {@link MainViewController}
 * </ol>
 *
 * <h2>Adding your feature</h2>
 *
 * <p>Find the TODO comments below and follow the pattern to wire your ViewModel and View.
 */
public class CookYourBooksGuiApp extends Application {

  private static final Logger LOG = LoggerFactory.getLogger(CookYourBooksGuiApp.class);

  /** Creates a new CookYourBooksGuiApp. */
  public CookYourBooksGuiApp() {}

  @Override
  public void start(Stage primaryStage) {
    // ── 1. Load the recipe library (path = current working directory of the JVM process) ──
    Path libraryPath = resolveLibraryPath();
    LOG.info("Library file path: {}", libraryPath.toAbsolutePath());
    CybLibrary library = CybLibrary.load(libraryPath);

    // ── 2. Create services ──
    var librarianService =
        new LibrarianServiceImpl(
            library.getRecipeRepository(), library.getCollectionRepository(), library);
    // var recipeService = new RecipeServiceImpl(
    //     library.getRecipeRepository(), library.getCollectionRepository(),
    //     library.getConversionRegistry());
    // Also available: TransformerServiceImpl, CookingServiceImpl, PlannerServiceImpl

    seedDemoLibraryIfEmpty(librarianService);

    // While librarianService.listCollections() is empty, the UI will show a "No collections found"
    // message.
    // Remove later
    if (librarianService.listCollections().isEmpty()) {
      librarianService.createCollection("Imported Recipes");
    }
    LOG.info("Loaded {} collections", librarianService.listCollections().size());

    // ── 3. Create shared navigation ──
    var navigationService = new NavigationService();

    // ── 4. Create the main layout ──
    var mainController = new MainViewController(navigationService);

    // ── 5. Wire feature ViewModels and Views ──
    var libraryVm =
        new LibraryViewModelImpl(librarianService, navigationService, Duration.ofSeconds(5));
    try {
      // Root FXML must use setController(...) when there is no fx:controller attribute; otherwise
      // the factory is never called and @FXML initialize() never runs (blank content + no
      // bindings).
      FXMLLoader libraryLoader = new FXMLLoader(getClass().getResource("/fxml/LibraryView.fxml"));
      libraryLoader.setController(new LibraryViewController(libraryVm));
      Parent libraryView = libraryLoader.load();
      mainController.setViewNode(NavigationService.View.LIBRARY, libraryView);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load LibraryView.fxml", e);
    }

    // ── Wire Recipe Editor ──
    String usdaKey = System.getenv("USDA_API_KEY");
    if (usdaKey == null || usdaKey.isBlank()) {
      usdaKey = "DEMO_KEY";
    }
    var nutritionService = new NutritionLookupAdapter(new RealUsdaClient(usdaKey));
    var recipeEditorVm =
        new RecipeEditorViewModelImpl(
            library.getRecipeRepository(), navigationService, library::getConversionRegistry);
    FXMLLoader editorLoader = new FXMLLoader(getClass().getResource("/fxml/RecipeEditorView.fxml"));
    editorLoader.setController(
        new RecipeEditorViewController(recipeEditorVm, navigationService, nutritionService));
    try {
      Parent editorView = editorLoader.load();
      mainController.setViewNode(NavigationService.View.RECIPE_EDITOR, editorView);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load RecipeEditorView.fxml", e);
    }

    // Wire Import Interface
    try {
      RecipeOcrService ocrService = new LocalFakeRecipeOcrService();
      ImportViewModelImpl importVm =
          new ImportViewModelImpl(ocrService, librarianService, navigationService);

      FXMLLoader importLoader = new FXMLLoader(getClass().getResource("/fxml/ImportView.fxml"));
      Parent importView = importLoader.load();

      ImportController importController = importLoader.getController();
      importController.setViewModel(importVm);

      mainController.setViewNode(NavigationService.View.IMPORT, importView);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load ImportView.fxml", e);
    }

    // ── Wire Search & Filter ──
    var searchVm = new SearchViewModelImpl(librarianService, navigationService);
    FXMLLoader searchLoader = new FXMLLoader(getClass().getResource("/fxml/SearchView.fxml"));
    searchLoader.setControllerFactory(clazz -> new SearchViewController(searchVm));
    try {
      Parent searchView = searchLoader.load();
      mainController.setViewNode(NavigationService.View.SEARCH, searchView);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load SearchView.fxml", e);
    }

    // ── 6. Load the main layout and show the window ──
    try {
      FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
      mainLoader.setController(mainController);
      Parent root = mainLoader.load();

      Scene scene = new Scene(root, 960, 640);
      primaryStage.setTitle("CookYourBooks");
      primaryStage.setScene(scene);
      primaryStage.show();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load MainView.fxml", e);
    }
  }

  /**
   * Launches the GUI application.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    launch(args);
  }

  /**
   * Resolves the library JSON path. Integration tests set {@code cookyourbooks.test.library.path}
   * to an isolated file so the workspace {@code cyb-library.json} is never read or written.
   */
  static Path resolveLibraryPath() {
    String testPath = System.getProperty("cookyourbooks.test.library.path");
    if (testPath != null && !testPath.isBlank()) {
      return Path.of(testPath).normalize();
    }
    return Path.of(System.getProperty("user.dir", ".")).resolve("cyb-library.json").normalize();
  }

  /**
   * When {@code cyb-library.json} is missing or has no collections, creates sample collections and
   * recipes so the Library view is usable immediately (persists to the library file).
   */
  private static void seedDemoLibraryIfEmpty(LibrarianService librarianService) {
    if (!librarianService.listCollections().isEmpty()) {
      return;
    }
    LOG.info("Library is empty — seeding demo collections and recipes");
    RecipeCollection weeknight = librarianService.createCollection("Weeknight Favorites");
    librarianService.saveRecipe(demoRecipePasta(), weeknight.getId());
    librarianService.saveRecipe(demoRecipeSalad(), weeknight.getId());

    RecipeCollection baking = librarianService.createCollection("Baking Basics (Demo)");
    librarianService.saveRecipe(demoRecipeMuffins(), baking.getId());

    librarianService.createCollection("Saved from the Web (demo — empty)");
  }

  private static Recipe demoRecipePasta() {
    return new Recipe(
        "demo-recipe-pasta",
        "One-Pot Tomato Pasta",
        null,
        List.of(
            new VagueIngredient("dried pasta", null, null, null),
            new VagueIngredient("canned tomatoes", null, null, null)),
        List.of(new Instruction(1, "Simmer tomatoes; cook pasta until tender.", List.of())),
        List.of());
  }

  private static Recipe demoRecipeSalad() {
    return new Recipe(
        "demo-recipe-salad",
        "Quick Garden Salad",
        null,
        List.of(new VagueIngredient("mixed greens", null, null, null)),
        List.of(new Instruction(1, "Toss greens with dressing.", List.of())),
        List.of());
  }

  private static Recipe demoRecipeMuffins() {
    return new Recipe(
        "demo-recipe-muffins",
        "Morning Muffins",
        null,
        List.of(
            new VagueIngredient("flour", null, null, null),
            new VagueIngredient("sugar", null, null, null)),
        List.of(new Instruction(1, "Mix dry and wet; bake at 375°F.", List.of())),
        List.of());
  }
}
