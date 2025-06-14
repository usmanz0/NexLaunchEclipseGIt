package application;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.control.Label;
import javafx.scene.control.TextField; // <--- Existing Import

import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;

import javafx.stage.Popup;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.stage.Modality;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public class Controller implements Initializable {
    @FXML
    private TreeView<LauncherItem> AllLaunchersTreeView;

    @FXML
    private Button addButton;

    // --- NEW FXML ELEMENTS FOR STARTUP LAUNCHERS ---
    @FXML
    private TreeView<LauncherItem> StartupLaunchersTreeView; // FXML element for Startup Launchers

    // --- FXML elements for Search (already in your provided code) ---
    @FXML
    private TextField searchBox;
    @FXML
    private Button searchButton;

    private Image folderIcon;
    private Image fileIcon;
    private Image arrowDownIcon;
    private Image arrowUpIcon;

    private Popup addOptionsPopup;
    private VBox addOptionsPopupContent;

    private LauncherDataService dataService;
    private StartupManager startupManager; // NEW: Instance of StartupManager

    // --- Store the original full list of top-level launchers (already existing) ---
    private List<LauncherItem> allOriginalTopLevelLaunchers = new ArrayList<>();
    // --- NEW: Separate list for startup launchers (these are also part of allOriginalTopLevelLaunchers) ---
    private List<LauncherItem> allOriginalStartupLaunchers = new ArrayList<>();

    private boolean isBackgroundMode = false; // NEW: Flag to indicate if app is running in background

    // NEW: Constructor to allow Main to set background mode
    public Controller() {
        // Default constructor for FXML loading
    }

    // NEW: Setter for background mode, called by Main.java
    public void setBackgroundMode(boolean isBackgroundMode) {
        this.isBackgroundMode = isBackgroundMode;
        System.out.println("Controller: App running in background mode: " + this.isBackgroundMode);
    }

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        // If in background mode, skip UI initialization and perform background tasks
        if (isBackgroundMode) {
            System.out.println("Controller: Initializing in background mode. Skipping GUI setup.");
            dataService = new LauncherDataService(); // Need data service even in background
            startupManager = new StartupManager(); // Need startup manager
            performBackgroundStartupLaunch(); // Execute startup launchers
            return; // Exit initialization early, do not show GUI
        }

        System.out.println("Controller: Initializing in normal GUI mode.");

        folderIcon = new Image(Controller.class.getResourceAsStream("/foldericon.png"));
        arrowDownIcon = new Image(Controller.class.getResourceAsStream("/arrow_down.png"));
        arrowUpIcon = new Image(Controller.class.getResourceAsStream("/arrow_up.png"));

        try {
            fileIcon = new Image(Controller.class.getResourceAsStream("/fileicon.png"));
        } catch (NullPointerException e) {
            System.err.println("Warning: fileicon.png not found. Leaf nodes will not have a specific file icon.");
            fileIcon = null;
        }

        dataService = new LauncherDataService();
        startupManager = new StartupManager(); // NEW: Initialize StartupManager

        // Load data at startup
        loadLaunchers(); // This will now populate allOriginalTopLevelLaunchers AND allOriginalStartupLaunchers

        AllLaunchersTreeView.setShowRoot(false);
        AllLaunchersTreeView.setCellFactory(tv -> new CustomStringTreeCell(this));

        // NEW: Setup StartupLaunchersTreeView
        if (StartupLaunchersTreeView != null) {
            StartupLaunchersTreeView.setShowRoot(false);
            StartupLaunchersTreeView.setCellFactory(tv -> new CustomStringTreeCell(this)); // Use the same cell factory
            setupStartupLaunchersTree(); // Populate the Startup Launchers TreeView
        } else {
            System.err.println("Warning: StartupLaunchersTreeView is null. Check FXML fx:id.");
        }


        createAddOptionsPopup();

        // Add listener to searchBox for instant search or clearing
        if (searchBox != null) {
            searchBox.textProperty().addListener((obs, oldText, newText) -> {
                if (newText.isEmpty() && !oldText.isEmpty()) {
                    // If text is cleared, restore all launchers
                    restoreAllLaunchers();
                }
            });
        }
    }

    // --- NEW: Method to handle background startup and launching items ---
    private void performBackgroundStartupLaunch() {
        System.out.println("Controller: Executing background startup launchers...");
        // Ensure data is loaded
        loadLaunchers(); // This will populate allOriginalStartupLaunchers

        if (allOriginalStartupLaunchers.isEmpty()) {
            System.out.println("Controller: No startup launchers found to run in background.");
        } else {
            System.out.println("Controller: Launching " + allOriginalStartupLaunchers.size() + " startup items.");
            for (LauncherItem item : allOriginalStartupLaunchers) {
                if (!item.isFolder()) { // Only launch non-folder items
                    launchItemPath(item.getUrlOrPath());
                } else {
                    // If a folder is marked as startup, decide if you want to launch its contents
                    // For now, only direct items will be launched
                    System.out.println("Controller: Skipping folder '" + item.getName() + "' in background startup. Only direct items are launched.");
                }
            }
        }
        System.out.println("Controller: Background startup process completed. Exiting application.");
        // Close the application after background tasks are done
        javafx.application.Platform.exit();
    }


    // --- Load Method (updated for startup launchers) ---
    private void loadLaunchers() {
        System.out.println("Controller: Attempting to load launchers...");
        List<LauncherItem> loadedItems = new ArrayList<>();

        try {
            loadedItems = dataService.loadLaunchers();
            System.out.println("Controller: Successfully loaded " + loadedItems.size() + " top-level launcher items.");
        } catch (IOException e) {
            System.err.println("Controller: !!! ERROR reading launchers data: " + e.getMessage());
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Load Error", "Failed to load launcher data: " + e.getMessage() + "\nStarting with an empty launcher list.");
        } catch (com.google.gson.JsonParseException e) {
            System.err.println("Controller: !!! ERROR parsing launchers data: " + e.getMessage());
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Load Error", "Failed to parse launcher data (file might be corrupted): " + e.getMessage() + "\nStarting with an empty launcher list.");
        }

        // --- Store the loaded items in our 'allOriginalTopLevelLaunchers' list ---
        allOriginalTopLevelLaunchers.clear();
        allOriginalTopLevelLaunchers.addAll(loadedItems);

        // NEW: Populate the separate list for startup launchers
        allOriginalStartupLaunchers.clear();
        populateStartupLaunchersList(allOriginalTopLevelLaunchers); // Recursively find all startup items

        // Always create a root item, even if it's conceptually empty.
        TreeItem<LauncherItem> rootItem = new TreeItem<>(new LauncherItem("InvisibleRoot"));

        if (!allOriginalTopLevelLaunchers.isEmpty()) {
            for (LauncherItem item : allOriginalTopLevelLaunchers) {
                rootItem.getChildren().add(convertLauncherItemToTreeItem(item));
            }
        } else {
            System.out.println("Controller: No existing launcher data found or data was empty/corrupted. Starting with an empty launcher list.");
        }

        AllLaunchersTreeView.setRoot(rootItem);
        rootItem.setExpanded(true);
    }

    // NEW: Recursive method to find all startup launchers from the main list
    private void populateStartupLaunchersList(List<LauncherItem> items) {
        for (LauncherItem item : items) {
            if (item.isStartupLauncher()) {
                allOriginalStartupLaunchers.add(item);
            }
            if (item.isFolder()) {
                populateStartupLaunchersList(item.getChildren()); // Recurse into children
            }
        }
    }

    // NEW: Method to setup and populate the StartupLaunchersTreeView
    private void setupStartupLaunchersTree() {
        TreeItem<LauncherItem> startupRoot = new TreeItem<>(new LauncherItem("StartupLaunchersRoot"));
        startupRoot.setExpanded(true); // Always expand the root for startup items

        // Filter and add only startup items to this tree
        if (!allOriginalStartupLaunchers.isEmpty()) {
            for (LauncherItem item : allOriginalStartupLaunchers) {
                // For simplicity, we'll add a *copy* of the TreeItem.
                // If you want full sync (e.g., changing name in AllLaunchersTreeView updates StartupLaunchersTreeView),
                // you'd need to manage shared TreeItem instances or listeners more carefully.
                // For now, this approach avoids complex synchronization issues.
                startupRoot.getChildren().add(convertLauncherItemToTreeItem(item));
            }
        } else {
            System.out.println("Controller: No startup launchers found to display.");
        }
        StartupLaunchersTreeView.setRoot(startupRoot);
    }

    // --- Method to restore the full list of launchers (already existing) ---
    private void restoreAllLaunchers() {
        TreeItem<LauncherItem> rootItem = new TreeItem<>(new LauncherItem("InvisibleRoot"));
        for (LauncherItem item : allOriginalTopLevelLaunchers) {
            rootItem.getChildren().add(convertLauncherItemToTreeItem(item));
        }
        AllLaunchersTreeView.setRoot(rootItem);
        rootItem.setExpanded(true);
        // Also refresh the startup launchers tree when all launchers are restored
        setupStartupLaunchersTree();
    }

    // --- Search Feature Methods (existing logic, no change needed) ---
    @FXML
    public void searchLaunchers(ActionEvent event) {
        String searchText = searchBox.getText().trim();
        if (searchText.isEmpty()) {
            restoreAllLaunchers(); // If search box is empty, show all
            return;
        }

        TreeItem<LauncherItem> rootItem = new TreeItem<>(new LauncherItem("InvisibleRoot"));
        List<LauncherItem> matchedItems = findMatchingLaunchers(searchText, allOriginalTopLevelLaunchers);

        if (matchedItems.isEmpty()) {
            // If no matches, show an empty tree
            showAlert(AlertType.INFORMATION, "No Results", "No launchers found matching '" + searchText + "'.");
        } else {
            for (LauncherItem item : matchedItems) {
                rootItem.getChildren().add(deepCopyTreeItem(convertLauncherItemToTreeItem(item)));
            }
        }
        AllLaunchersTreeView.setRoot(rootItem);
        rootItem.setExpanded(true);
        expandAllTreeItems(rootItem);
    }

    private List<LauncherItem> findMatchingLaunchers(String searchText, List<LauncherItem> items) {
        List<LauncherItem> matched = new ArrayList<>();
        String lowerCaseSearchText = searchText.toLowerCase();

        for (LauncherItem item : items) {
            boolean selfMatches = item.getName().toLowerCase().contains(lowerCaseSearchText);

            if (item.isFolder()) {
                List<LauncherItem> matchedChildren = findMatchingLaunchers(searchText, item.getChildren());
                if (selfMatches || !matchedChildren.isEmpty()) {
                    // Create a copy of the folder. Only include matching children.
                    LauncherItem folderCopy = new LauncherItem(item.getName());
                    // NEW: Ensure isStartupLauncher property is copied for folders
                    folderCopy.setStartupLauncher(item.isStartupLauncher());
                    if (!matchedChildren.isEmpty()) {
                        for (LauncherItem child : matchedChildren) {
                            folderCopy.addChild(child);
                        }
                    }
                    matched.add(folderCopy);
                }
            } else {
                if (selfMatches) {
                    // NEW: Ensure isStartupLauncher property is copied for items
                    matched.add(new LauncherItem(item.getName(), item.getUrlOrPath(), item.isStartupLauncher()));
                }
            }
        }
        return matched;
    }

    private void expandAllTreeItems(TreeItem<LauncherItem> item) {
        if (item != null) {
            item.setExpanded(true);
            for (TreeItem<LauncherItem> child : item.getChildren()) {
                expandAllTreeItems(child);
            }
        }
    }

    private TreeItem<LauncherItem> deepCopyTreeItem(TreeItem<LauncherItem> original) {
        if (original == null) {
            return null;
        }

        LauncherItem originalValue = original.getValue();
        LauncherItem copiedValue;
        // NEW: Use the constructor that takes isStartupLauncher
        if (originalValue.isFolder()) {
            copiedValue = new LauncherItem(originalValue.getName(), null, originalValue.isStartupLauncher());
        } else {
            copiedValue = new LauncherItem(originalValue.getName(), originalValue.getUrlOrPath(), originalValue.isStartupLauncher());
        }


        ImageView iconView = null;
        if (copiedValue.isFolder()) {
            if (folderIcon != null) {
                iconView = new ImageView(folderIcon);
            }
        } else {
            if (fileIcon != null) {
                iconView = new ImageView(fileIcon);
            } else if (folderIcon != null) {
                iconView = new ImageView(folderIcon);
            }
        }
        if (iconView != null) {
            iconView.setFitWidth(24);
            iconView.setFitHeight(20);
        }

        TreeItem<LauncherItem> copiedTreeItem = new TreeItem<>(copiedValue, iconView);
        copiedTreeItem.setExpanded(original.isExpanded());

        for (TreeItem<LauncherItem> child : original.getChildren()) {
            copiedTreeItem.getChildren().add(deepCopyTreeItem(child));
        }

        return copiedTreeItem;
    }

    // --- End of Search Feature Methods ---


    // --- Save Method (updated for startup launchers) ---
    public void saveLaunchers() {
        System.out.println("Controller: saveLaunchers() called.");

        // IMPORTANT: Always save the 'allOriginalTopLevelLaunchers' as it's the master list.
        if (allOriginalTopLevelLaunchers.isEmpty()) {
            dataService.deleteDataFile();
            System.out.println("Controller: No launchers to save. Data file handled by service.");
            return;
        }

        try {
            dataService.saveLaunchers(allOriginalTopLevelLaunchers);
            System.out.println("Controller: Launchers data successfully saved via service.");
        } catch (IOException e) {
            System.err.println("Controller: !!! ERROR writing launchers data: " + e.getMessage());
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Save Error", "Failed to save launcher data: " + e.getMessage());
        }
    }

    // Helper to convert TreeItem hierarchy to serializable LauncherItem hierarchy
    private LauncherItem convertTreeItemToLauncherItem(TreeItem<LauncherItem> treeItem) {
        LauncherItem launcherItem = treeItem.getValue();
        LauncherItem serializableItem;

        // NEW: Use the constructor that preserves isStartupLauncher
        if (launcherItem.isFolder()) {
            serializableItem = new LauncherItem(launcherItem.getName(), null, launcherItem.isStartupLauncher());
        } else {
            serializableItem = new LauncherItem(launcherItem.getName(), launcherItem.getUrlOrPath(), launcherItem.isStartupLauncher());
        }

        if (launcherItem.isFolder()) {
            for (TreeItem<LauncherItem> childTreeItem : treeItem.getChildren()) {
                serializableItem.addChild(convertTreeItemToLauncherItem(childTreeItem));
            }
        }
        return serializableItem;
    }

    // Helper to convert serializable LauncherItem hierarchy back to TreeItem hierarchy
    private TreeItem<LauncherItem> convertLauncherItemToTreeItem(LauncherItem launcherItem) {
        ImageView iconView = null;
        // NEW: Apply a distinct icon for startup launchers if desired, otherwise use regular icons
        if (launcherItem.isStartupLauncher()) {
            // You could add a specific startup icon here if you have one
            // For now, let's just make it slightly different or add a marker
            if (fileIcon != null) { // Or a specific 'startup' icon
                iconView = new ImageView(fileIcon); // Using file icon as a default for now
            } else if (folderIcon != null) {
                iconView = new ImageView(folderIcon);
            }
        } else if (launcherItem.isFolder()) {
            if (folderIcon != null) {
                iconView = new ImageView(folderIcon);
            }
        } else { // It's a file/shortcut
            if (fileIcon != null) {
                iconView = new ImageView(fileIcon);
            } else if (folderIcon != null) { // Fallback if no specific file icon
                iconView = new ImageView(folderIcon);
            }
        }

        if (iconView != null) {
            iconView.setFitWidth(24);
            iconView.setFitHeight(20);
        }

        TreeItem<LauncherItem> treeItem = new TreeItem<>(launcherItem, iconView);
        treeItem.setExpanded(true); // Keep folders expanded by default on load

        if (launcherItem.isFolder()) {
            for (LauncherItem childItem : launcherItem.getChildren()) {
                treeItem.getChildren().add(convertLauncherItemToTreeItem(childItem));
            }
        }
        return treeItem;
    }


    // --- End of Save and Load Methods ---

    private void createAddOptionsPopup() {
        addOptionsPopup = new Popup();
        addOptionsPopup.setAutoHide(true);

        Button createNewLauncherBtn = new Button("Create New Launcher Folder");
        createNewLauncherBtn.getStyleClass().add("popup-button");
        createNewLauncherBtn.setOnAction(e -> {
            addOptionsPopup.hide();
            showCreateNewLauncherFolderDialog();
        });

        Button addStartupLauncherBtn = new Button("Add Startup Launcher");
        addStartupLauncherBtn.getStyleClass().add("popup-button");
        addStartupLauncherBtn.setOnAction(e -> {
            addOptionsPopup.hide();
            showAddStartupLauncherDialog(); // NEW: Call method for adding startup launcher
        });

        addOptionsPopupContent = new VBox(5, createNewLauncherBtn, addStartupLauncherBtn);
        addOptionsPopupContent.getStyleClass().add("add-options-popup");
        addOptionsPopupContent.setAlignment(Pos.CENTER);
        addOptionsPopup.getContent().add(addOptionsPopupContent);

        addOptionsPopupContent.applyCss();
        addOptionsPopupContent.layout();
    }

    @FXML
    public void showAddMenu(ActionEvent event) {
        if (addOptionsPopup.isShowing()) {
            addOptionsPopup.hide();
            return;
        }

        if (addButton == null) {
            System.err.println("Error: addButton is null. Check FXML fx:id.");
            return;
        }

        Point2D screenCoords = addButton.localToScreen(addButton.getBoundsInLocal().getMinX(), addButton.getBoundsInLocal().getMinY());
        double x = screenCoords.getX() - 140; // Adjust for popup width
        double y = screenCoords.getY();
        Window window = addButton.getScene().getWindow();

        addOptionsPopupContent.applyCss();
        addOptionsPopupContent.layout();

        double popupHeight = addOptionsPopupContent.getHeight();
        double margin = 8;

        addOptionsPopup.show(window, x, y - popupHeight - margin);
    }

    // --- NEW: Method to show dialog for adding Startup Launcher ---
    private void showAddStartupLauncherDialog() {
        javafx.stage.Stage dialogStage = new javafx.stage.Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(AllLaunchersTreeView.getScene().getWindow());
        dialogStage.setTitle("Add Startup Launcher");
        dialogStage.setResizable(false);

        TextField nameField = new TextField();
        nameField.setPromptText("Enter display name");
        nameField.getStyleClass().add("text-field-dark");

        TextField pathField = new TextField();
        pathField.setPromptText("Enter path (e.g., 'C:\\Program Files\\App\\App.exe')");
        pathField.getStyleClass().add("text-field-dark");

        Button addBtn = new Button("Add");
        addBtn.getStyleClass().add("dialog-button");
        addBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String path = pathField.getText().trim();
            if (!name.isEmpty() && !path.isEmpty()) {
                // Check if a launcher with this path already exists as a startup launcher
                if (allOriginalStartupLaunchers.stream().anyMatch(item -> !item.isFolder() && item.getUrlOrPath().equalsIgnoreCase(path))) {
                    showAlert(AlertType.WARNING, "Duplicate", "This path is already registered as a startup launcher.");
                    return;
                }

                // NEW: Create a startup launcher item
                LauncherItem newStartupLauncher = new LauncherItem(name, path, true);

                // Add to the master list (top-level) and the startup list
                allOriginalTopLevelLaunchers.add(newStartupLauncher);
                allOriginalStartupLaunchers.add(newStartupLauncher);

                // Add to the UI TreeView (All Launchers)
                TreeItem<LauncherItem> rootItem = AllLaunchersTreeView.getRoot();
                if (rootItem != null) {
                    addLauncherItemToFolder(rootItem, newStartupLauncher); // Add to the main tree view
                }

                // Refresh the Startup Launchers TreeView
                setupStartupLaunchersTree();

                // ONE-TIME PERMISSION LOGIC
                // Check if the app is already registered for startup. If not, ask permission.
                if (!startupManager.isAppRegisteredForStartup()) {
                    System.out.println("App not registered for startup. Asking user permission...");
                    if (startupManager.askUserForStartupPermission()) {
                        String appExecutablePath = getJarPath(); // Get path to the running JAR/EXE
                        if (appExecutablePath != null) {
                            if (startupManager.registerAppForStartup(appExecutablePath)) {
                                showAlert(AlertType.INFORMATION, "Startup Enabled", "NexLaunch will now start automatically with your computer.");
                            } else {
                                showAlert(AlertType.ERROR, "Startup Failed", "Failed to enable automatic startup for NexLaunch. Please try again or check permissions.");
                            }
                        } else {
                            showAlert(AlertType.ERROR, "Error", "Could not determine application path for startup registration.");
                        }
                    } else {
                        showAlert(AlertType.INFORMATION, "Startup Not Enabled", "Automatic startup for NexLaunch was not enabled. You can enable it later from settings.");
                    }
                } else {
                    System.out.println("App already registered for startup. No need to ask permission again.");
                }

                dialogStage.close();
                saveLaunchers(); // Save changes
            } else {
                showAlert(AlertType.WARNING, "Input Error", "Both display name and path cannot be empty.");
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("dialog-button");
        cancelBtn.setOnAction(e -> dialogStage.close());

        HBox buttonBar = new HBox(10, addBtn, cancelBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        VBox dialogContent = new VBox(10,
                new Label("Display Name:"),
                nameField,
                new Label("Path to Executable/File (for Startup Launcher):"),
                pathField,
                buttonBar);
        dialogContent.setPadding(new javafx.geometry.Insets(20));
        dialogContent.getStyleClass().add("dialog-background");

        Scene dialogScene = new Scene(dialogContent);
        dialogScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        dialogStage.setScene(dialogScene);
        dialogStage.showAndWait();
    }


    private void showCreateNewLauncherFolderDialog() {
        javafx.stage.Stage dialogStage = new javafx.stage.Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(AllLaunchersTreeView.getScene().getWindow());
        dialogStage.setTitle("Create New Launcher Folder");
        dialogStage.setResizable(false);

        TextField folderNameField = new TextField();
        folderNameField.setPromptText("Enter folder name");
        folderNameField.getStyleClass().add("text-field-dark");

        Button createBtn = new Button("Create");
        createBtn.getStyleClass().add("dialog-button");
        createBtn.setOnAction(e -> {
            String folderName = folderNameField.getText().trim();
            if (!folderName.isEmpty()) {
                // Create a regular folder (isStartupLauncher defaults to false)
                LauncherItem newFolder = new LauncherItem(folderName);
                addLauncherItemToFolder(AllLaunchersTreeView.getRoot(), newFolder); // Add to current view
                allOriginalTopLevelLaunchers.add(newFolder); // Add to master list
                dialogStage.close();
            } else {
                showAlert(AlertType.WARNING, "Input Error", "Folder name cannot be empty.");
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("dialog-button");
        cancelBtn.setOnAction(e -> dialogStage.close());

        HBox buttonBar = new HBox(10, createBtn, cancelBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        VBox dialogContent = new VBox(10, new Label("New Launcher Folder Name:"), folderNameField, buttonBar);
        dialogContent.setPadding(new javafx.geometry.Insets(20));
        dialogContent.getStyleClass().add("dialog-background");

        Scene dialogScene = new Scene(dialogContent);
        dialogScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        dialogStage.setScene(dialogScene);
        dialogStage.showAndWait();
    }

    public void showAddUrlDialog(TreeItem<LauncherItem> parentFolder) {
        if (parentFolder == null || !parentFolder.getValue().isFolder()) {
            showAlert(AlertType.ERROR, "Error", "Can only add URLs to folders.");
            return;
        }

        javafx.stage.Stage dialogStage = new javafx.stage.Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(AllLaunchersTreeView.getScene().getWindow());
        dialogStage.setTitle("Add URL to '" + parentFolder.getValue().getName() + "'");
        dialogStage.setResizable(false);

        TextField nameField = new TextField();
        nameField.setPromptText("Enter display name (e.g., 'Google Search')");
        nameField.getStyleClass().add("text-field-dark");

        TextField urlField = new TextField();
        urlField.setPromptText("Enter URL (e.g., 'https://www.google.com')");
        urlField.getStyleClass().add("text-field-dark");

        Button addBtn = new Button("Add");
        addBtn.getStyleClass().add("dialog-button");
        addBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String url = urlField.getText().trim();
            if (!name.isEmpty() && !url.isEmpty()) {
                // Create a regular URL item (isStartupLauncher defaults to false)
                LauncherItem newUrl = new LauncherItem(name, url);
                addLauncherItemToFolder(parentFolder, newUrl); // Add to current view

                LauncherItem originalParent = findLauncherItemInList(parentFolder.getValue().getName(), allOriginalTopLevelLaunchers);
                if (originalParent != null && originalParent.isFolder()) {
                    originalParent.addChild(newUrl);
                }
                dialogStage.close();
                saveLaunchers();
            } else {
                showAlert(AlertType.WARNING, "Input Error", "Both name and URL cannot be empty.");
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("dialog-button");
        cancelBtn.setOnAction(e -> dialogStage.close());

        HBox buttonBar = new HBox(10, addBtn, cancelBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        VBox dialogContent = new VBox(10,
                new Label("Display Name:"),
                nameField,
                new Label("URL:"),
                urlField,
                buttonBar);
        dialogContent.setPadding(new javafx.geometry.Insets(20));
        dialogContent.getStyleClass().add("dialog-background");

        Scene dialogScene = new Scene(dialogContent);
        dialogScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        dialogStage.setScene(dialogScene);
        dialogStage.showAndWait();
    }

    public void showAddShortcutDialog(TreeItem<LauncherItem> parentFolder) {
        if (parentFolder == null || !parentFolder.getValue().isFolder()) {
            showAlert(AlertType.ERROR, "Error", "Can only add shortcuts to folders.");
            return;
        }

        javafx.stage.Stage dialogStage = new javafx.stage.Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(AllLaunchersTreeView.getScene().getWindow());
        dialogStage.setTitle("Add Shortcut to '" + parentFolder.getValue().getName() + "'");
        dialogStage.setResizable(false);

        TextField nameField = new TextField();
        nameField.setPromptText("Enter display name (e.g., 'Calculator')");
        nameField.getStyleClass().add("text-field-dark");

        TextField pathField = new TextField();
        pathField.setPromptText("Enter path (e.g., 'C:\\Windows\\System32\\calc.exe')");
        pathField.getStyleClass().add("text-field-dark");

        Button addBtn = new Button("Add");
        addBtn.getStyleClass().add("dialog-button");
        addBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String path = pathField.getText().trim();
            if (!name.isEmpty() && !path.isEmpty()) {
                // Create a regular shortcut (isStartupLauncher defaults to false)
                LauncherItem newShortcut = new LauncherItem(name, path);
                addLauncherItemToFolder(parentFolder, newShortcut); // Add to current view

                LauncherItem originalParent = findLauncherItemInList(parentFolder.getValue().getName(), allOriginalTopLevelLaunchers);
                if (originalParent != null && originalParent.isFolder()) {
                    originalParent.addChild(newShortcut);
                }
                dialogStage.close();
                saveLaunchers();
            } else {
                showAlert(AlertType.WARNING, "Input Error", "Both name and path cannot be empty.");
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("dialog-button");
        cancelBtn.setOnAction(e -> dialogStage.close());

        HBox buttonBar = new HBox(10, addBtn, cancelBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        VBox dialogContent = new VBox(10,
                new Label("Display Name:"),
                nameField,
                new Label("Path to Executable/File:"),
                pathField,
                buttonBar);
        dialogContent.setPadding(new javafx.geometry.Insets(20));
        dialogContent.getStyleClass().add("dialog-background");

        Scene dialogScene = new Scene(dialogContent);
        dialogScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        dialogStage.setScene(dialogScene);
        dialogStage.showAndWait();
    }

    // --- Helper to find a LauncherItem by name in a list (for adding/deleting) ---
    private LauncherItem findLauncherItemInList(String name, List<LauncherItem> items) {
        for (LauncherItem item : items) {
            if (item.getName().equals(name)) {
                return item;
            }
            if (item.isFolder()) {
                LauncherItem found = findLauncherItemInList(name, item.getChildren());
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }


    private void addLauncherItemToFolder(TreeItem<LauncherItem> parentFolder, LauncherItem newLauncherItem) {
        ImageView iconView = null;
        if (newLauncherItem.isFolder() && folderIcon != null) {
            iconView = new ImageView(folderIcon);
        } else if (!newLauncherItem.isFolder() && fileIcon != null) {
            iconView = new ImageView(fileIcon);
        } else if (folderIcon != null) { // Fallback if fileIcon is null
            iconView = new ImageView(folderIcon);
        }

        if (iconView != null) {
            iconView.setFitWidth(24);
            iconView.setFitHeight(20);
        }

        TreeItem<LauncherItem> newTreeItem = new TreeItem<>(newLauncherItem, iconView);

        parentFolder.getChildren().add(newTreeItem);
        parentFolder.setExpanded(true);
        showAlert(AlertType.INFORMATION, "Item Added", "'" + newLauncherItem.getName() + "' added to '" + parentFolder.getValue().getName() + "'!");
        // saveLaunchers() is called by the dialogs now. Removed from here to avoid double save.
    }

    /**
     * Deletes a specified TreeItem (and its corresponding LauncherItem) from the TreeView.
     * If the item is a folder, it will delete all its contents as well.
     * @param itemToDelete The TreeItem to be deleted.
     */
    public void deleteLauncherItem(TreeItem<LauncherItem> itemToDelete) {
        if (itemToDelete == null || itemToDelete.getParent() == null) {
            showAlert(AlertType.ERROR, "Deletion Error", "Cannot delete the root or a null item.");
            return;
        }

        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Are you sure you want to delete '" + itemToDelete.getValue().getName() + "'?");
        String contentText = itemToDelete.getValue().isFolder() ?
                             "This will permanently remove this folder and ALL its contents." :
                             "This will permanently remove this item.";
        confirmAlert.setContentText(contentText);
        confirmAlert.getDialogPane().getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        confirmAlert.getDialogPane().getStyleClass().add("alert-dialog");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            TreeItem<LauncherItem> parent = itemToDelete.getParent();
            parent.getChildren().remove(itemToDelete);

            // --- Remove from the allOriginalTopLevelLaunchers as well ---
            LauncherItem itemToRemove = itemToDelete.getValue();
            if (parent.getValue().getName().equals("InvisibleRoot")) { // It's a top-level item
                allOriginalTopLevelLaunchers.remove(itemToRemove);
            } else { // It's a child item, find its parent in the original list and remove
                LauncherItem originalParent = findLauncherItemInList(parent.getValue().getName(), allOriginalTopLevelLaunchers);
                if (originalParent != null && originalParent.isFolder()) {
                    originalParent.getChildren().remove(itemToRemove);
                }
            }

            // NEW: If the deleted item was a startup launcher, remove it from that list too
            if (itemToRemove.isStartupLauncher()) {
                allOriginalStartupLaunchers.remove(itemToRemove);
                setupStartupLaunchersTree(); // Refresh startup tree view
            }

            showAlert(AlertType.INFORMATION, "Deleted", "'" + itemToDelete.getValue().getName() + "' has been deleted.");
            saveLaunchers();
        }
    }


    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("alert-dialog");
        alert.showAndWait();
    }

    public void selectItem() {
        TreeItem<LauncherItem> item = AllLaunchersTreeView.getSelectionModel().getSelectedItem();
        if (item != null) {
            System.out.println("Selected: " + item.getValue().getName());
        }
    }

    // NEW: Method to launch a path or URL
    public void launchItemPath(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.trim().isEmpty()) {
            System.err.println("Launch failed: Path or URL is empty.");
            return;
        }

        try {
            if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
                // It's a URL
                Desktop.getDesktop().browse(new URI(pathOrUrl));
            } else {
                // Assume it's a file path
                File file = new File(pathOrUrl);
                if (file.exists()) {
                    Desktop.getDesktop().open(file);
                } else {
                    System.err.println("File not found: " + pathOrUrl);
                    showAlert(AlertType.ERROR, "Launch Error", "File or application not found at: " + pathOrUrl);
                }
            }
        } catch (IOException | URISyntaxException e) {
            System.err.println("Error launching: " + e.getMessage());
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Launch Error", "Could not launch '" + pathOrUrl + "': " + e.getMessage());
        }
    }

    // NEW: Helper to get the path of the running JAR/Executable
    private String getJarPath() {
        try {
            // Get the path of the currently executing JAR file
            String path = Controller.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            // Decode spaces and other URL encoded characters
            String decodedPath = java.net.URLDecoder.decode(path, "UTF-8");

            // Handle running from JAR directly or from an unpacked folder
            File file = new File(decodedPath);
            if (file.isDirectory() && file.getName().endsWith(".jar")) { // If it's a JAR, the path should be to the JAR
                // This case handles running "java -jar myapp.jar", where path is like "/path/to/myapp.jar"
                return file.getAbsolutePath();
            } else if (file.isFile() && decodedPath.endsWith(".jar")) { // If it's a JAR directly
                return file.getAbsolutePath();
            } else if (System.getProperty("os.name").toLowerCase().contains("win") && decodedPath.endsWith(".exe")) {
                // If on Windows and it's an EXE (e.g., from a native打包)
                return file.getAbsolutePath();
            } else {
                // This covers cases where it might be running from IDE or other unpacked forms
                // Attempt to find the executable within the current working directory, or prompt user
                System.out.println("Could not determine direct executable path. Current path: " + decodedPath);
                // For a proper deployed application, this should point to your .exe or .jar
                // You might need to provide specific build instructions for native executables
                showAlert(AlertType.WARNING, "Application Path", "Could not automatically determine application executable path for startup. Please ensure your application is properly packaged (e.g., as a .exe or .jar) for this feature to work reliably, or configure it manually.");
                return null;
            }
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    private class CustomStringTreeCell extends TreeCell<LauncherItem> {
        private HBox contentWrapper;
        private ImageView itemIconView;
        private Label itemTextLabel;
        private Button launchButton;
        private Button customArrowButton;
        private HBox actionButtonsBox;
        private ContextMenu folderContextMenu;
        private ContextMenu leafContextMenu;
        private Controller controller;

        public CustomStringTreeCell(Controller controller) {
            this.controller = controller;

            itemIconView = new ImageView();
            itemIconView.setFitWidth(24);
            itemIconView.setFitHeight(20);

            itemTextLabel = new Label();

            launchButton = new Button("Launch");
            launchButton.getStyleClass().add("launch-button");
            launchButton.setOnAction(event -> {
                TreeItem<LauncherItem> treeItem = getTreeItem();
                if (treeItem != null && treeItem.getValue() != null) {
                    if (treeItem.getValue().isFolder()) {
                        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
                        confirmAlert.setTitle("Launch All Items in Folder");
                        confirmAlert.setHeaderText("Launch all items in '" + treeItem.getValue().getName() + "'?");
                        confirmAlert.setContentText("This will attempt to open " + treeItem.getChildren().size() + " links/shortcuts.");
                        confirmAlert.getDialogPane().getStylesheets().add(getClass().getResource("application.css").toExternalForm());
                        confirmAlert.getDialogPane().getStyleClass().add("alert-dialog");

                        Optional<ButtonType> result = confirmAlert.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            for (TreeItem<LauncherItem> childItem : treeItem.getChildren()) {
                                String pathOrUrl = childItem.getValue().getUrlOrPath();
                                if (pathOrUrl != null && !pathOrUrl.trim().isEmpty()) {
                                    System.out.println("Launching child: " + childItem.getValue().getName() + " (" + pathOrUrl + ")");
                                    controller.launchItemPath(pathOrUrl); // Use the new common launch method
                                } else {
                                    System.out.println("Skipping child with no launch path/URL: " + childItem.getValue().getName());
                                }
                            }
                        }
                    } else { // It's a single item (URL or Shortcut)
                        String pathOrUrl = treeItem.getValue().getUrlOrPath();
                        System.out.println("Launching single item: " + treeItem.getValue().getName() + " (" + pathOrUrl + ")");
                        controller.launchItemPath(pathOrUrl); // Use the new common launch method
                    }
                }
            });

            customArrowButton = new Button();
            customArrowButton.getStyleClass().add("arrow-button");
            customArrowButton.setOnAction(event -> {
                TreeItem<LauncherItem> treeItem = getTreeItem();
                if (treeItem != null && treeItem.getValue().isFolder()) {
                    treeItem.setExpanded(!treeItem.isExpanded());
                }
            });

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            actionButtonsBox = new HBox(5, launchButton, customArrowButton);
            actionButtonsBox.setAlignment(Pos.CENTER_RIGHT);

            contentWrapper = new HBox(5, itemIconView, itemTextLabel, spacer, actionButtonsBox);
            contentWrapper.setAlignment(Pos.CENTER_LEFT);
            contentWrapper.setPadding(new javafx.geometry.Insets(5, 0, 5, 0)); // Add padding

            createContextMenus();
        }

        private void createContextMenus() {
            // Context menu for folders
            folderContextMenu = new ContextMenu();
            MenuItem addUrlToFolder = new MenuItem("Add URL");
            addUrlToFolder.setOnAction(e -> {
                TreeItem<LauncherItem> selectedItem = getTreeItem();
                if (selectedItem != null && selectedItem.getValue().isFolder()) {
                    controller.showAddUrlDialog(selectedItem);
                }
            });
            MenuItem addShortcutToFolder = new MenuItem("Add Shortcut");
            addShortcutToFolder.setOnAction(e -> {
                TreeItem<LauncherItem> selectedItem = getTreeItem();
                if (selectedItem != null && selectedItem.getValue().isFolder()) {
                    controller.showAddShortcutDialog(selectedItem);
                }
            });
            MenuItem deleteFolder = new MenuItem("Delete Folder");
            deleteFolder.setOnAction(e -> controller.deleteLauncherItem(getTreeItem()));
            folderContextMenu.getItems().addAll(addUrlToFolder, addShortcutToFolder, deleteFolder);


            // Context menu for leaf nodes (URLs/Shortcuts)
            leafContextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete Item");
            deleteItem.setOnAction(e -> controller.deleteLauncherItem(getTreeItem()));

            // NEW: Add "Make/Unmake Startup Launcher" option
            MenuItem toggleStartup = new MenuItem();
            toggleStartup.setOnAction(e -> {
                TreeItem<LauncherItem> selectedItem = getTreeItem();
                if (selectedItem != null && !selectedItem.getValue().isFolder()) { // Only for non-folder items
                    boolean currentStatus = selectedItem.getValue().isStartupLauncher();
                    selectedItem.getValue().setStartupLauncher(!currentStatus); // Toggle status
                    controller.saveLaunchers(); // Save the updated status
                    controller.loadLaunchers(); // Reload to refresh both tree views
                    String action = currentStatus ? "removed from" : "added to";
                    controller.showAlert(AlertType.INFORMATION, "Startup Launcher Updated",
                                         "'" + selectedItem.getValue().getName() + "' has been " + action + " startup launchers.");
                }
            });
            leafContextMenu.getItems().addAll(deleteItem, toggleStartup);


            // Set context menu based on item type
            this.setOnContextMenuRequested(event -> {
                if (isEmpty() || getItem() == null) {
                    folderContextMenu.hide();
                    leafContextMenu.hide();
                    return;
                }
                if (getTreeItem().getValue().isFolder()) {
                    folderContextMenu.show(this, event.getScreenX(), event.getScreenY());
                } else {
                    // Before showing leaf context menu, update the text for toggleStartup
                    if (getTreeItem().getValue().isStartupLauncher()) {
                        toggleStartup.setText("Remove from Startup");
                    } else {
                        toggleStartup.setText("Add to Startup");
                    }
                    leafContextMenu.show(this, event.getScreenX(), event.getScreenY());
                }
            });
        }


        @Override
        protected void updateItem(LauncherItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                setContextMenu(null); // Clear context menu
                return;
            }

            itemTextLabel.setText(item.getName());

            // Set appropriate icon
            if (item.isFolder()) {
                itemIconView.setImage(controller.folderIcon);
                // Update arrow button icon based on expansion
                if (getTreeItem().isExpanded()) {
                    customArrowButton.setGraphic(new ImageView(controller.arrowUpIcon));
                } else {
                    customArrowButton.setGraphic(new ImageView(controller.arrowDownIcon));
                }
                customArrowButton.setVisible(true); // Show arrow for folders
                launchButton.setVisible(true); // Always show launch button for folders (to launch all)

            } else { // Leaf node (URL or Shortcut)
                // NEW: Differentiate icon for startup launchers
                if (item.isStartupLauncher()) {
                    // You might want a specific icon for startup launchers, e.g., a lightning bolt
                    // For now, let's use a standard file icon or add a small visual cue.
                    // To show a special icon, you'd load it here: new ImageView(new Image("path/to/startupicon.png"));
                    if (controller.fileIcon != null) {
                        itemIconView.setImage(controller.fileIcon); // Placeholder
                    } else {
                        itemIconView.setImage(controller.folderIcon); // Fallback
                    }
                    // Optional: Add a visual cue like a small overlay icon
                    // You'd need to layer image views or use a custom node for graphic
                } else {
                    if (controller.fileIcon != null) {
                        itemIconView.setImage(controller.fileIcon);
                    } else {
                        itemIconView.setImage(controller.folderIcon); // Fallback
                    }
                }

                customArrowButton.setVisible(false); // Hide arrow for leaf nodes
                launchButton.setVisible(true); // Show launch button for individual items
            }

            setGraphic(contentWrapper);
            setContextMenu(item.isFolder() ? folderContextMenu : leafContextMenu);
        }
    }
}