package application;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

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
import javafx.scene.control.TextField;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;

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

    @FXML
    private TreeView<LauncherItem> StartupLaunchersTreeView;

    @FXML
    private TextField searchBox;
    @FXML
    private Button searchButton;

    private Image folderIcon;
    private Image fileIcon;
    private Image arrowDownIcon;
    private Image arrowUpIcon;
    private Image startupIcon;

    private Popup addOptionsPopup;
    private VBox addOptionsPopupContent;

    private LauncherDataService dataService;
    // Removed: private StartupManager startupManager; // No longer needed
    // Removed: private boolean isBackgroundMode = false; // No longer needed

    private List<LauncherItem> allOriginalTopLevelLaunchers = new ArrayList<>();
    private List<LauncherItem> allOriginalStartupLaunchers = new ArrayList<>(); // Items explicitly marked as startup

    public Controller() {
        // Default constructor for FXML loading
    }

    // Removed: public void setBackgroundMode(boolean isBackgroundMode) { ... }

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        // Removed: if (isBackgroundMode) { ... return; } block

        System.out.println("Controller: Initializing application.");

        // --- Robust Icon Loading ---
        try {
            folderIcon = new Image(Controller.class.getResourceAsStream("/foldericon.png"));
        } catch (NullPointerException e) {
            System.err.println("Warning: foldericon.png not found. Using a default placeholder.");
            folderIcon = new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=");
        }

        try {
            fileIcon = new Image(Controller.class.getResourceAsStream("/fileicon.png"));
        } catch (NullPointerException e) {
            System.err.println("Warning: fileicon.png not found. Using folderIcon as fallback for files.");
            fileIcon = folderIcon; // Fallback to folder icon or another generic icon
        }

        try {
            startupIcon = new Image(Controller.class.getResourceAsStream("/startupicon.png"));
        } catch (NullPointerException e) {
            System.err.println("Warning: startupicon.png not found. Using folderIcon as fallback for startup items.");
            startupIcon = folderIcon; // Fallback to folder icon or another generic icon
        }

        try {
            arrowDownIcon = new Image(Controller.class.getResourceAsStream("/arrow_down.png"));
        } catch (NullPointerException e) {
            System.err.println("Warning: arrow_down.png not found.");
            arrowDownIcon = null;
        }

        try {
            arrowUpIcon = new Image(Controller.class.getResourceAsStream("/arrow_up.png"));
        } catch (NullPointerException e) {
            System.err.println("Warning: arrow_up.png not found.");
            arrowUpIcon = null;
        }
        // --- End Robust Icon Loading ---


        dataService = new LauncherDataService();
        // Removed: startupManager = new StartupManager(); // No longer needed

        loadLaunchers(); // This will now populate allOriginalTopLevelLaunchers AND allOriginalStartupLaunchers

        AllLaunchersTreeView.setShowRoot(false);
        AllLaunchersTreeView.setCellFactory(tv -> new CustomStringTreeCell(this));

        if (StartupLaunchersTreeView != null) {
            StartupLaunchersTreeView.setShowRoot(false);
            StartupLaunchersTreeView.setCellFactory(tv -> new CustomStringTreeCell(this));
            setupStartupLaunchersTree();
        } else {
            System.err.println("Warning: StartupLaunchersTreeView is null. Check FXML fx:id.");
        }

        createAddOptionsPopup();

        if (searchBox != null) {
            searchBox.textProperty().addListener((obs, oldText, newText) -> {
                if (newText.isEmpty() && !oldText.isEmpty()) {
                    restoreAllLaunchers();
                }
            });
        }

        // --- NEW BEHAVIOR: Launch startup items immediately after app starts normally ---
        System.out.println("Controller: App initialized. Launching startup items now.");
        performAutoStartupLaunch(); // New method for immediate launch
    }

    // NEW METHOD (Renamed from performBackgroundStartupLaunch): For launching startup items when the application itself starts
    private void performAutoStartupLaunch() {
        loadLaunchers(); // Ensure data is loaded to get allOriginalStartupLaunchers

        if (allOriginalStartupLaunchers.isEmpty()) {
            System.out.println("Controller: No startup launchers found to auto-launch on app start.");
        } else {
            System.out.println("Controller: Auto-launching " + allOriginalStartupLaunchers.size() + " top-level startup items.");
            for (LauncherItem item : allOriginalStartupLaunchers) {
                launchRecursive(item); // Use recursive launcher for folders
            }
        }
        // Removed: javafx.application.Platform.exit(); as the app is now always in GUI mode
    }

    // Existing: Recursive launch method
    private void launchRecursive(LauncherItem item) {
        if (item == null) return;

        if (item.isFolder()) {
            System.out.println("Controller: Launching items inside folder: " + item.getName());
            if (item.getChildren() != null) {
                for (LauncherItem child : item.getChildren()) {
                    launchRecursive(child); // Recurse for children
                }
            }
        } else { // It's a non-folder item (URL or shortcut)
            if (item.getUrlOrPath() != null && !item.getUrlOrPath().trim().isEmpty()) {
                System.out.println("Controller: Launching item: " + item.getName() + " (" + item.getUrlOrPath() + ")");
                launchItemPath(item.getUrlOrPath());
            } else {
                System.out.println("Controller: Skipping item with no launch path/URL: " + item.getName());
            }
        }
    }


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

        allOriginalTopLevelLaunchers.clear();
        allOriginalTopLevelLaunchers.addAll(loadedItems);

        // Populate the separate list for startup launchers
        allOriginalStartupLaunchers.clear();
        for (LauncherItem item : allOriginalTopLevelLaunchers) {
            if (item.isStartupLauncher()) {
                allOriginalStartupLaunchers.add(item);
            }
        }

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

    private void setupStartupLaunchersTree() {
        TreeItem<LauncherItem> startupRoot = new TreeItem<>(new LauncherItem("StartupLaunchersRoot"));
        startupRoot.setExpanded(true);

        if (!allOriginalStartupLaunchers.isEmpty()) {
            for (LauncherItem item : allOriginalStartupLaunchers) {
                startupRoot.getChildren().add(deepCopyTreeItem(convertLauncherItemToTreeItem(item)));
            }
        } else {
            System.out.println("Controller: No startup launchers found to display.");
        }
        StartupLaunchersTreeView.setRoot(startupRoot);
    }

    private void restoreAllLaunchers() {
        TreeItem<LauncherItem> rootItem = new TreeItem<>(new LauncherItem("InvisibleRoot"));
        for (LauncherItem item : allOriginalTopLevelLaunchers) {
            rootItem.getChildren().add(convertLauncherItemToTreeItem(item));
        }
        AllLaunchersTreeView.setRoot(rootItem);
        rootItem.setExpanded(true);
        setupStartupLaunchersTree();
    }

    @FXML
    public void searchLaunchers(ActionEvent event) {
        String searchText = searchBox.getText().trim();
        if (searchText.isEmpty()) {
            restoreAllLaunchers();
            return;
        }

        TreeItem<LauncherItem> rootItem = new TreeItem<>(new LauncherItem("InvisibleRoot"));
        List<LauncherItem> matchedItems = findMatchingLaunchers(searchText, allOriginalTopLevelLaunchers);

        if (matchedItems.isEmpty()) {
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
                    LauncherItem folderCopy = new LauncherItem(item.getName(), null, item.isStartupLauncher());
                    if (!matchedChildren.isEmpty()) {
                        for (LauncherItem child : matchedChildren) {
                            folderCopy.addChild(child);
                        }
                    }
                    if(selfMatches && !matched.contains(folderCopy)) {
                        matched.add(folderCopy);
                    } else if (!matchedChildren.isEmpty() && !matched.contains(folderCopy)) {
                        matched.add(folderCopy);
                    }
                }
            } else {
                if (selfMatches) {
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
        if (originalValue.isFolder()) {
            copiedValue = new LauncherItem(originalValue.getName(), null, originalValue.isStartupLauncher());
        } else {
            copiedValue = new LauncherItem(originalValue.getName(), originalValue.getUrlOrPath(), originalValue.isStartupLauncher());
        }

        ImageView iconView = getIconForLauncherItem(copiedValue);

        TreeItem<LauncherItem> copiedTreeItem = new TreeItem<>(copiedValue, iconView);
        copiedTreeItem.setExpanded(original.isExpanded());

        for (TreeItem<LauncherItem> child : original.getChildren()) {
            copiedTreeItem.getChildren().add(deepCopyTreeItem(child));
        }

        return copiedTreeItem;
    }


    public void saveLaunchers() {
        System.out.println("Controller: saveLaunchers() called.");

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

    private LauncherItem convertTreeItemToLauncherItem(TreeItem<LauncherItem> treeItem) {
        LauncherItem launcherItem = treeItem.getValue();
        LauncherItem serializableItem;

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

    private TreeItem<LauncherItem> convertLauncherItemToTreeItem(LauncherItem launcherItem) {
        ImageView iconView = getIconForLauncherItem(launcherItem);
        TreeItem<LauncherItem> treeItem = new TreeItem<>(launcherItem, iconView);
        treeItem.setExpanded(true);

        if (launcherItem.isFolder()) {
            for (LauncherItem childItem : launcherItem.getChildren()) {
                treeItem.getChildren().add(convertLauncherItemToTreeItem(childItem));
            }
        }
        return treeItem;
    }

    private ImageView getIconForLauncherItem(LauncherItem item) {
        Image imageToUse = null;

        if (item.isStartupLauncher() && startupIcon != null) {
            imageToUse = startupIcon;
        } else if (item.isFolder()) {
            imageToUse = folderIcon;
        } else {
            imageToUse = fileIcon;
        }

        if (imageToUse == null) {
            System.err.println("CRITICAL: No suitable icon found for item: " + item.getName() + ". Using folderIcon as last resort.");
            imageToUse = folderIcon;
        }

        ImageView iconView = new ImageView(imageToUse);
        iconView.setFitWidth(24);
        iconView.setFitHeight(20);

        return iconView;
    }


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
            showAddExistingLauncherToStartupDialog();
        });

        addOptionsPopupContent = new VBox(5, createNewLauncherBtn, addStartupLauncherBtn);
        addOptionsPopupContent.getStyleClass().add("add-options-popup");
        addOptionsPopupContent.setAlignment(Pos.CENTER);
        addOptionsPopup.getContent().add(addOptionsPopupContent);

        addOptionsPopupContent.applyCss();
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
        double x = screenCoords.getX() - 140;
        double y = screenCoords.getY();
        Window window = addButton.getScene().getWindow();

        addOptionsPopupContent.applyCss();
        addOptionsPopupContent.layout();

        double popupHeight = addOptionsPopupContent.getHeight();
        double margin = 8;

        addOptionsPopup.show(window, x, y - popupHeight - margin);
    }

    private void showAddExistingLauncherToStartupDialog() {
        List<LauncherItem> topLevelLaunchersToAdd = allOriginalTopLevelLaunchers.stream()
                .filter(item -> !item.isStartupLauncher())
                .collect(Collectors.toList());

        if (topLevelLaunchersToAdd.isEmpty()) {
            if (allOriginalTopLevelLaunchers.isEmpty()) {
                showAlert(AlertType.INFORMATION, "No Launchers Available",
                          "You need to create some launchers (Folder, URL, or Shortcut) first before adding them to startup.");
            } else {
                showAlert(AlertType.INFORMATION, "No New Launchers Available",
                          "All existing top-level launchers are already added to startup.");
            }
            return;
        }

        javafx.stage.Stage dialogStage = new javafx.stage.Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(AllLaunchersTreeView.getScene().getWindow());
        dialogStage.setTitle("Select Launcher for Startup");
        dialogStage.setResizable(false);

        ListView<LauncherItem> launcherListView = new ListView<>();
        launcherListView.getItems().addAll(topLevelLaunchersToAdd);
        launcherListView.setPrefHeight(200);
        launcherListView.setCellFactory(lv -> new ListCell<LauncherItem>() {
            @Override
            protected void updateItem(LauncherItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.getName() + (item != null && item.isFolder() ? " (Folder)" : ""));
                setGraphic(item != null && !empty ? getIconForLauncherItem(item) : null);
            }
        });


        Button addSelectedBtn = new Button("Add Selected to Startup");
        addSelectedBtn.getStyleClass().add("dialog-button");
        addSelectedBtn.setOnAction(e -> {
            LauncherItem selectedItem = launcherListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                LauncherItem itemInMasterList = null;
                for (LauncherItem item : allOriginalTopLevelLaunchers) {
                    if (item == selectedItem) {
                        itemInMasterList = item;
                        break;
                    }
                }

                if (itemInMasterList != null) {
                    itemInMasterList.setStartupLauncher(true);
                    
                    if (!allOriginalStartupLaunchers.contains(itemInMasterList)) {
                        allOriginalStartupLaunchers.add(itemInMasterList);
                    }
                    
                    setupStartupLaunchersTree();
                    saveLaunchers();

                    // Removed: All OS-level startup registration logic from here.
                    // The app no longer registers itself to start with the OS via this action.

                    showAlert(AlertType.INFORMATION, "Added to Startup", "'" + selectedItem.getName() + "' has been added to startup launchers.");
                    dialogStage.close();
                } else {
                    showAlert(AlertType.ERROR, "Error", "Selected launcher not found in master list. Please try again.");
                }
            } else {
                showAlert(AlertType.WARNING, "Selection Required", "Please select a launcher from the list.");
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("dialog-button");
        cancelBtn.setOnAction(e -> dialogStage.close());

        HBox buttonBar = new HBox(10, addSelectedBtn, cancelBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        VBox dialogContent = new VBox(10,
                new Label("Select a top-level launcher (folder or item) to add to startup:"),
                launcherListView,
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
                boolean exists = allOriginalTopLevelLaunchers.stream()
                                   .anyMatch(li -> li.isFolder() && li.getName().equalsIgnoreCase(folderName));
                if (exists) {
                    showAlert(AlertType.WARNING, "Duplicate Name", "A top-level folder with this name already exists. Please choose a different name.");
                    return;
                }

                LauncherItem newFolder = new LauncherItem(folderName);
                allOriginalTopLevelLaunchers.add(newFolder);
                TreeItem<LauncherItem> rootItem = AllLaunchersTreeView.getRoot();
                if (rootItem != null) {
                    addLauncherItemToTreeView(rootItem, newFolder);
                } else {
                    System.err.println("Error: AllLaunchersTreeView root is null. Cannot add new folder.");
                }
                saveLaunchers();
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
                LauncherItem newUrl = new LauncherItem(name, url);
                
                LauncherItem originalParent = findLauncherItemInList(parentFolder.getValue().getName(), true, allOriginalTopLevelLaunchers);
                if (originalParent != null && originalParent.isFolder()) {
                    boolean exists = originalParent.getChildren().stream()
                                       .anyMatch(li -> li.getName().equalsIgnoreCase(name) || (!li.isFolder() && li.getUrlOrPath().equalsIgnoreCase(url)));
                    if (exists) {
                        showAlert(AlertType.WARNING, "Duplicate Item", "An item with this name or URL already exists in this folder. Please choose a different name or URL.");
                        return;
                    }
                    originalParent.addChild(newUrl);
                } else {
                    System.err.println("Error: Original parent folder not found in master list.");
                    showAlert(AlertType.ERROR, "Add Error", "Could not find the parent folder to add the URL.");
                    return;
                }

                addLauncherItemToTreeView(parentFolder, newUrl);
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
                LauncherItem newShortcut = new LauncherItem(name, path);

                LauncherItem originalParent = findLauncherItemInList(parentFolder.getValue().getName(), true, allOriginalTopLevelLaunchers);
                if (originalParent != null && originalParent.isFolder()) {
                    boolean exists = originalParent.getChildren().stream()
                                       .anyMatch(li -> li.getName().equalsIgnoreCase(name) || (!li.isFolder() && li.getUrlOrPath().equalsIgnoreCase(path)));
                    if (exists) {
                        showAlert(AlertType.WARNING, "Duplicate Item", "An item with this name or path already exists in this folder. Please choose a different name or path.");
                        return;
                    }
                    originalParent.addChild(newShortcut);
                } else {
                    System.err.println("Error: Original parent folder not found in master list.");
                    showAlert(AlertType.ERROR, "Add Error", "Could not find the parent folder to add the shortcut.");
                    return;
                }

                addLauncherItemToTreeView(parentFolder, newShortcut);
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

    private void addLauncherItemToTreeView(TreeItem<LauncherItem> parentTreeItem, LauncherItem newLauncherItem) {
        ImageView iconView = getIconForLauncherItem(newLauncherItem);
        TreeItem<LauncherItem> newTreeItem = new TreeItem<>(newLauncherItem, iconView);
        parentTreeItem.getChildren().add(newTreeItem);
        parentTreeItem.setExpanded(true);
        showAlert(AlertType.INFORMATION, "Item Added", "'" + newLauncherItem.getName() + "' added to '" + parentTreeItem.getValue().getName() + "'!");
    }


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
            LauncherItem itemToRemove = itemToDelete.getValue();
            TreeItem<LauncherItem> parentTreeItem = itemToDelete.getParent();

            // Remove from the UI TreeView
            parentTreeItem.getChildren().remove(itemToDelete);

            // Remove from the master data list (allOriginalTopLevelLaunchers)
            if (parentTreeItem.getValue().getName().equals("InvisibleRoot")) {
                allOriginalTopLevelLaunchers.removeIf(item -> item == itemToRemove);
            } else {
                LauncherItem originalParentLauncherItem = findLauncherItemInList(parentTreeItem.getValue().getName(), true, allOriginalTopLevelLaunchers);
                if (originalParentLauncherItem != null && originalParentLauncherItem.isFolder()) {
                    originalParentLauncherItem.getChildren().removeIf(child -> child == itemToRemove);
                } else {
                    System.err.println("Error: Could not find original parent launcher item for deletion.");
                }
            }

            if (itemToRemove.isStartupLauncher()) {
                allOriginalStartupLaunchers.removeIf(startupItem -> startupItem == itemToRemove);
                setupStartupLaunchersTree();
                // Removed: startupManager.unregisterAppFromStartup() logic here.
            }

            showAlert(AlertType.INFORMATION, "Deleted", "'" + itemToDelete.getValue().getName() + "' has been deleted.");
            saveLaunchers();
        }
    }

    private LauncherItem findLauncherItemInList(String identifier, boolean isFolder, List<LauncherItem> items) {
        if (items == null) return null;
        for (LauncherItem item : items) {
            if (isFolder) {
                if (item.isFolder() && item.getName().equals(identifier)) {
                    return item;
                }
            } else {
                if (!item.isFolder() && item.getUrlOrPath() != null && item.getUrlOrPath().equals(identifier)) {
                    return item;
                }
            }
            if (item.isFolder() && item.getChildren() != null) {
                LauncherItem found = findLauncherItemInList(identifier, isFolder, item.getChildren());
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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

    public void launchItemPath(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.trim().isEmpty()) {
            System.err.println("Launch failed: Path or URL is empty.");
            return;
        }

        try {
            if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
                Desktop.getDesktop().browse(new URI(pathOrUrl));
            } else {
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

    // Removed: private String getJarPath() { ... } // No longer needed


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
                        confirmAlert.setHeaderText("Are you sure you want to launch all items in '" + treeItem.getValue().getName() + "'?");
                        confirmAlert.setContentText("This will attempt to open " + treeItem.getChildren().size() + " links/shortcuts.");
                        confirmAlert.getDialogPane().getStylesheets().add(getClass().getResource("application.css").toExternalForm());
                        confirmAlert.getDialogPane().getStyleClass().add("alert-dialog");

                        Optional<ButtonType> result = confirmAlert.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            for (TreeItem<LauncherItem> childItem : treeItem.getChildren()) {
                                launchRecursive(childItem.getValue());
                            }
                        }
                    } else {
                        String pathOrUrl = treeItem.getValue().getUrlOrPath();
                        System.out.println("Launching single item: " + treeItem.getValue().getName() + " (" + pathOrUrl + ")");
                        controller.launchItemPath(pathOrUrl);
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
            contentWrapper.setPadding(new javafx.geometry.Insets(5, 0, 5, 0));

            createContextMenus();
        }

        private void createContextMenus() {
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

            MenuItem toggleFolderStartup = new MenuItem();
            toggleFolderStartup.setOnAction(e -> {
                TreeItem<LauncherItem> selectedItem = getTreeItem();
                if (selectedItem != null && selectedItem.getValue().isFolder()) {
                    boolean currentStatus = selectedItem.getValue().isStartupLauncher();
                    selectedItem.getValue().setStartupLauncher(!currentStatus);

                    controller.allOriginalStartupLaunchers.clear();
                    for (LauncherItem item : controller.allOriginalTopLevelLaunchers) {
                        if (item.isStartupLauncher()) {
                            controller.allOriginalStartupLaunchers.add(item);
                        }
                    }

                    controller.saveLaunchers();
                    controller.setupStartupLaunchersTree();

                    String action = currentStatus ? "removed from" : "added to";
                    controller.showAlert(AlertType.INFORMATION, "Startup Launcher Updated",
                                         "Folder '" + selectedItem.getValue().getName() + "' has been " + action + " startup launchers.");
                    // Removed: OS-level startup registration logic from here.
                }
            });
            folderContextMenu.getItems().addAll(addUrlToFolder, addShortcutToFolder, deleteFolder, toggleFolderStartup);


            leafContextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete Item");
            deleteItem.setOnAction(e -> controller.deleteLauncherItem(getTreeItem()));

            MenuItem toggleLeafStartup = new MenuItem();
            toggleLeafStartup.setOnAction(e -> {
                TreeItem<LauncherItem> selectedItem = getTreeItem();
                if (selectedItem != null && !selectedItem.getValue().isFolder()) {
                    boolean currentStatus = selectedItem.getValue().isStartupLauncher();
                    selectedItem.getValue().setStartupLauncher(!currentStatus);

                    controller.allOriginalStartupLaunchers.clear();
                    for (LauncherItem item : controller.allOriginalTopLevelLaunchers) {
                        if (item.isStartupLauncher()) {
                            controller.allOriginalStartupLaunchers.add(item);
                        }
                    }

                    controller.saveLaunchers();
                    controller.setupStartupLaunchersTree();

                    String action = currentStatus ? "removed from" : "added to";
                    controller.showAlert(AlertType.INFORMATION, "Startup Launcher Updated",
                                         "'" + selectedItem.getValue().getName() + "' has been " + action + " startup launchers.");
                    // Removed: OS-level startup registration logic from here.
                }
            });
            leafContextMenu.getItems().addAll(deleteItem, toggleLeafStartup);


            this.setOnContextMenuRequested(event -> {
                if (isEmpty() || getItem() == null) {
                    folderContextMenu.hide();
                    leafContextMenu.hide();
                    return;
                }
                if (getTreeItem().getValue().isFolder()) {
                    if (getTreeItem().getValue().isStartupLauncher()) {
                        toggleFolderStartup.setText("Remove from Startup");
                    } else {
                        toggleFolderStartup.setText("Add to Startup");
                    }
                    folderContextMenu.show(this, event.getScreenX(), event.getScreenY());
                } else {
                    if (getTreeItem().getValue().isStartupLauncher()) {
                        toggleLeafStartup.setText("Remove from Startup");
                    } else {
                        toggleLeafStartup.setText("Add to Startup");
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
                setContextMenu(null);
                return;
            }

            itemTextLabel.setText(item.getName());

            itemIconView.setImage(controller.getIconForLauncherItem(item).getImage());

            if (item.isFolder()) {
                if (getTreeItem().isExpanded() && controller.arrowUpIcon != null) {
                    customArrowButton.setGraphic(new ImageView(controller.arrowUpIcon));
                } else if (!getTreeItem().isExpanded() && controller.arrowDownIcon != null) {
                    customArrowButton.setGraphic(new ImageView(controller.arrowDownIcon));
                } else {
                    customArrowButton.setGraphic(null);
                }
                customArrowButton.setVisible(true);
                launchButton.setVisible(true);

            } else {
                customArrowButton.setVisible(false);
                launchButton.setVisible(true);
            }

            setGraphic(contentWrapper);
            setContextMenu(item.isFolder() ? folderContextMenu : leafContextMenu);
        }
    }
}