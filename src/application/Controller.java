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
import javafx.scene.control.TextField; // <--- NEW IMPORT

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

    // --- NEW: FXML elements for Search ---
    @FXML
    private TextField searchBox; // This needs to be added in your FXML
    @FXML
    private Button searchButton; // This needs to be added in your FXML

    private Image folderIcon;
    private Image fileIcon;
    private Image arrowDownIcon;
    private Image arrowUpIcon;

    private Popup addOptionsPopup;
    private VBox addOptionsPopupContent;

    private LauncherDataService dataService;

    // --- NEW: Store the original full list of top-level launchers ---
    private List<LauncherItem> allOriginalTopLevelLaunchers = new ArrayList<>();

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
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

        // Load data at startup
        loadLaunchers(); // This will now populate allOriginalTopLevelLaunchers

        AllLaunchersTreeView.setShowRoot(false);
        AllLaunchersTreeView.setCellFactory(tv -> new CustomStringTreeCell(this));
        createAddOptionsPopup();

        // --- NEW: Add listener to searchBox for instant search or clearing ---
        if (searchBox != null) {
            searchBox.textProperty().addListener((obs, oldText, newText) -> {
                if (newText.isEmpty() && !oldText.isEmpty()) {
                    // If text is cleared, restore all launchers
                    restoreAllLaunchers();
                }
            });
        }
    }

    // --- Load Method (updated for search feature) ---
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

        // --- NEW: Store the loaded items in our 'allOriginalTopLevelLaunchers' list ---
        allOriginalTopLevelLaunchers.clear();
        allOriginalTopLevelLaunchers.addAll(loadedItems);

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

    // --- NEW: Method to restore the full list of launchers ---
    private void restoreAllLaunchers() {
        TreeItem<LauncherItem> rootItem = new TreeItem<>(new LauncherItem("InvisibleRoot"));
        for (LauncherItem item : allOriginalTopLevelLaunchers) {
            rootItem.getChildren().add(convertLauncherItemToTreeItem(item));
        }
        AllLaunchersTreeView.setRoot(rootItem);
        rootItem.setExpanded(true);
    }

    // --- NEW: Search Feature Methods ---
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
                // IMPORTANT: We need to create a *new* TreeItem hierarchy for the search results.
                // Otherwise, modifying the expanded state or children of search results
                // would affect the original TreeItem structure.
                rootItem.getChildren().add(deepCopyTreeItem(convertLauncherItemToTreeItem(item)));
            }
        }
        AllLaunchersTreeView.setRoot(rootItem);
        rootItem.setExpanded(true); // Always expand the root to show immediate children
        expandAllTreeItems(rootItem); // Expand all found matching items for visibility
    }

    /**
     * Recursively searches for LauncherItems whose name contains the search text (case-insensitive).
     * @param searchText The text to search for.
     * @param items The list of LauncherItems to search within.
     * @return A new List containing only the matching top-level LauncherItems,
     * where folders include only matching children (if any).
     */
    private List<LauncherItem> findMatchingLaunchers(String searchText, List<LauncherItem> items) {
        List<LauncherItem> matched = new ArrayList<>();
        String lowerCaseSearchText = searchText.toLowerCase();

        for (LauncherItem item : items) {
            // Check if the item itself matches
            boolean selfMatches = item.getName().toLowerCase().contains(lowerCaseSearchText);

            // If it's a folder, search its children
            if (item.isFolder()) {
                List<LauncherItem> matchedChildren = findMatchingLaunchers(searchText, item.getChildren());
                if (selfMatches || !matchedChildren.isEmpty()) {
                    // Create a copy of the folder. Only include matching children.
                    LauncherItem folderCopy = new LauncherItem(item.getName());
                    if (!matchedChildren.isEmpty()) {
                        for (LauncherItem child : matchedChildren) {
                            folderCopy.addChild(child); // Add only matched children
                        }
                    }
                    matched.add(folderCopy);
                }
            } else { // It's a non-folder item (URL/Shortcut)
                if (selfMatches) {
                    matched.add(new LauncherItem(item.getName(), item.getUrlOrPath())); // Add a copy
                }
            }
        }
        return matched;
    }

    /**
     * Recursively expands all TreeItems starting from the given item.
     * Useful for search results where you want all matched folders to be open.
     */
    private void expandAllTreeItems(TreeItem<LauncherItem> item) {
        if (item != null) {
            item.setExpanded(true);
            for (TreeItem<LauncherItem> child : item.getChildren()) {
                expandAllTreeItems(child);
            }
        }
    }

    /**
     * Creates a deep copy of a TreeItem hierarchy. Needed for search results
     * to avoid modifying the original tree structure when expanding/collapsing.
     */
    private TreeItem<LauncherItem> deepCopyTreeItem(TreeItem<LauncherItem> original) {
        if (original == null) {
            return null;
        }

        // Create a copy of the LauncherItem value
        LauncherItem originalValue = original.getValue();
        LauncherItem copiedValue;
        if (originalValue.isFolder()) {
            copiedValue = new LauncherItem(originalValue.getName());
        } else {
            copiedValue = new LauncherItem(originalValue.getName(), originalValue.getUrlOrPath());
        }

        // Create the new TreeItem with an appropriate icon
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
        copiedTreeItem.setExpanded(original.isExpanded()); // Maintain original expanded state

        // Recursively copy children
        for (TreeItem<LauncherItem> child : original.getChildren()) {
            copiedTreeItem.getChildren().add(deepCopyTreeItem(child));
        }

        return copiedTreeItem;
    }


    // --- End of Search Feature Methods ---


    // --- Save Method (using LauncherDataService) ---
    public void saveLaunchers() {
        System.out.println("Controller: saveLaunchers() called.");

        // IMPORTANT: When saving, always save the 'allOriginalTopLevelLaunchers'
        // to ensure changes from adding/deleting are persisted, not just search results.
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
    // (Existing method, ensure it pulls from the current TreeView state if needed for edits,
    // but saveLaunchers now primarily uses allOriginalTopLevelLaunchers directly for the main save)
    private LauncherItem convertTreeItemToLauncherItem(TreeItem<LauncherItem> treeItem) {
        LauncherItem launcherItem = treeItem.getValue();
        LauncherItem serializableItem;

        if (launcherItem.isFolder()) {
            serializableItem = new LauncherItem(launcherItem.getName());
        } else {
            serializableItem = new LauncherItem(launcherItem.getName(), launcherItem.getUrlOrPath());
        }

        if (launcherItem.isFolder()) {
            for (TreeItem<LauncherItem> childTreeItem : treeItem.getChildren()) {
                serializableItem.addChild(convertTreeItemToLauncherItem(childTreeItem));
            }
        }
        return serializableItem;
    }

    // Helper to convert serializable LauncherItem hierarchy back to TreeItem hierarchy
    // (Existing method, used by loadLaunchers and restoreAllLaunchers)
    private TreeItem<LauncherItem> convertLauncherItemToTreeItem(LauncherItem launcherItem) {
        ImageView iconView = null;
        if (launcherItem.isFolder()) {
            if (folderIcon != null) {
                iconView = new ImageView(folderIcon);
                iconView.setFitWidth(24);
                iconView.setFitHeight(20);
            }
        } else { // It's a file/shortcut
            if (fileIcon != null) {
                iconView = new ImageView(fileIcon);
                iconView.setFitWidth(24);
                iconView.setFitHeight(20);
            } else if (folderIcon != null) { // Fallback if no specific file icon
                iconView = new ImageView(folderIcon);
                iconView.setFitWidth(24);
                iconView.setFitHeight(20);
            }
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
            System.out.println("Add Startup Launcher clicked!");
            showAlert(AlertType.INFORMATION, "Feature Not Implemented", "This feature is coming soon!");
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
        double x = screenCoords.getX() - 140;
        double y = screenCoords.getY();
        Window window = addButton.getScene().getWindow();

        addOptionsPopupContent.applyCss();
        addOptionsPopupContent.layout();

        double popupHeight = addOptionsPopupContent.getHeight();
        double margin = 8;

        addOptionsPopup.show(window, x, y - popupHeight - margin);
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
                // --- NEW: Add to allOriginalTopLevelLaunchers directly ---
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
                // --- NEW: Add to parent folder in allOriginalTopLevelLaunchers ---
                LauncherItem newUrl = new LauncherItem(name, url);
                addLauncherItemToFolder(parentFolder, newUrl); // Add to current view

                // Find the actual parent LauncherItem in the original list and add the child
                LauncherItem originalParent = findLauncherItemInList(parentFolder.getValue().getName(), allOriginalTopLevelLaunchers);
                if (originalParent != null && originalParent.isFolder()) {
                    originalParent.addChild(newUrl);
                }
                // --- End NEW ---
                dialogStage.close();
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
                // --- NEW: Add to parent folder in allOriginalTopLevelLaunchers ---
                LauncherItem newShortcut = new LauncherItem(name, path);
                addLauncherItemToFolder(parentFolder, newShortcut); // Add to current view

                // Find the actual parent LauncherItem in the original list and add the child
                LauncherItem originalParent = findLauncherItemInList(parentFolder.getValue().getName(), allOriginalTopLevelLaunchers);
                if (originalParent != null && originalParent.isFolder()) {
                    originalParent.addChild(newShortcut);
                }
                // --- End NEW ---
                dialogStage.close();
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

    // --- NEW: Helper to find a LauncherItem by name in a list (for adding/deleting) ---
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
        saveLaunchers();
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

            // --- NEW: Remove from the allOriginalTopLevelLaunchers as well ---
            LauncherItem itemToRemove = itemToDelete.getValue();
            if (parent.getValue().getName().equals("InvisibleRoot")) { // It's a top-level item
                allOriginalTopLevelLaunchers.remove(itemToRemove);
            } else { // It's a child item, find its parent in the original list and remove
                LauncherItem originalParent = findLauncherItemInList(parent.getValue().getName(), allOriginalTopLevelLaunchers);
                if (originalParent != null && originalParent.isFolder()) {
                    originalParent.getChildren().remove(itemToRemove);
                }
            }
            // --- End NEW ---

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
                                    controller.launchItemPath(pathOrUrl);
                                } else {
                                    System.out.println("Skipping child with no launch path/URL: " + childItem.getValue().getName());
                                }
                            }
                        }
                    } else {
                        String pathOrUrl = treeItem.getValue().getUrlOrPath();
                        if (pathOrUrl != null && !pathOrUrl.trim().isEmpty()) {
                            System.out.println("Launching single: " + pathOrUrl);
                            controller.launchItemPath(pathOrUrl);
                        } else {
                            System.out.println("No launch path/URL defined for: " + treeItem.getValue().getName());
                            controller.showAlert(AlertType.INFORMATION, "Launch Info",
                                "No launch path/URL defined for: " + treeItem.getValue().getName());
                        }
                    }
                }
            });

            customArrowButton = new Button();
            if (controller.arrowDownIcon != null && controller.arrowUpIcon != null) {
                ImageView arrowImageView = new ImageView(controller.arrowDownIcon);
                customArrowButton.setGraphic(arrowImageView);
                customArrowButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            } else {
                customArrowButton.setText(">");
            }
            customArrowButton.getStyleClass().add("custom-arrow-button");
            customArrowButton.setOnAction(event -> {
                TreeItem<LauncherItem> treeItem = getTreeItem();
                if (treeItem != null && treeItem.getValue().isFolder()) {
                    System.out.println("Custom Arrow clicked for: " + treeItem.getValue().getName());
                    treeItem.setExpanded(!treeItem.isExpanded());
                }
            });

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            actionButtonsBox = new HBox(5, launchButton, customArrowButton);
            actionButtonsBox.getStyleClass().add("tree-cell-action-buttons");
            actionButtonsBox.setAlignment(Pos.CENTER_RIGHT);

            contentWrapper = new HBox(5, itemIconView, itemTextLabel, spacer, actionButtonsBox);
            contentWrapper.getStyleClass().add("custom-tree-cell-content");
            contentWrapper.setAlignment(Pos.CENTER_LEFT);

            folderContextMenu = new ContextMenu();
            MenuItem addUrlMenuItem = new MenuItem("Add URL");
            addUrlMenuItem.setOnAction(event -> {
                TreeItem<LauncherItem> selectedFolder = getTreeItem();
                if (selectedFolder != null && selectedFolder.getValue().isFolder()) {
                    controller.showAddUrlDialog(selectedFolder);
                }
            });

            MenuItem addShortcutMenuItem = new MenuItem("Add Shortcut");
            addShortcutMenuItem.setOnAction(event -> {
                TreeItem<LauncherItem> selectedFolder = getTreeItem();
                if (selectedFolder != null && selectedFolder.getValue().isFolder()) {
                    controller.showAddShortcutDialog(selectedFolder);
                }
            });

            MenuItem deleteFolderMenuItem = new MenuItem("Delete Item");
            deleteFolderMenuItem.setOnAction(event -> {
                TreeItem<LauncherItem> selectedItem = getTreeItem();
                if (selectedItem != null) {
                    controller.deleteLauncherItem(selectedItem);
                }
            });
            folderContextMenu.getItems().addAll(addUrlMenuItem, addShortcutMenuItem, deleteFolderMenuItem);

            leafContextMenu = new ContextMenu();
            MenuItem deleteLeafMenuItem = new MenuItem("Delete Item");
            deleteLeafMenuItem.setOnAction(event -> {
                TreeItem<LauncherItem> selectedItem = getTreeItem();
                if (selectedItem != null) {
                    controller.deleteLauncherItem(selectedItem);
                }
            });
            leafContextMenu.getItems().addAll(deleteLeafMenuItem);
        }

        @Override
        protected void updateItem(LauncherItem item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                if (contentWrapper != null) {
                    contentWrapper.setVisible(false);
                    contentWrapper.setManaged(false);
                }
                setDisclosureNode(null);
                setContextMenu(null);
            } else {
                if (contentWrapper != null) {
                    contentWrapper.setVisible(true);
                    contentWrapper.setManaged(true);
                }

                itemTextLabel.setText(item.getName());

                TreeItem<LauncherItem> treeItem = getTreeItem();
                if (treeItem != null) {
                    if (item.isFolder()) {
                        itemIconView.setImage(controller.folderIcon);
                        itemIconView.setVisible(true);
                        customArrowButton.setVisible(true);
                        customArrowButton.setManaged(true);
                        launchButton.setVisible(true);
                        launchButton.setManaged(true);
                        setContextMenu(folderContextMenu);

                        if (treeItem.isExpanded()) {
                            ((ImageView) customArrowButton.getGraphic()).setImage(controller.arrowUpIcon);
                        } else {
                            ((ImageView) customArrowButton.getGraphic()).setImage(controller.arrowDownIcon);
                        }
                    } else {
                        if (controller.fileIcon != null) {
                            itemIconView.setImage(controller.fileIcon);
                        } else {
                            itemIconView.setImage(controller.folderIcon);
                        }
                        itemIconView.setVisible(true);
                        customArrowButton.setVisible(false);
                        customArrowButton.setManaged(false);
                        launchButton.setVisible(true);
                        launchButton.setManaged(true);
                        setContextMenu(leafContextMenu);
                    }
                }

                setGraphic(contentWrapper);
                setText(null);
                setDisclosureNode(null);
            }
        }
    }

    private void launchItemPath(String path) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (path.startsWith("http://") || path.startsWith("https://")) {
                try {
                    desktop.browse(new URI(path));
                } catch (IOException | URISyntaxException e) {
                    showAlert(AlertType.ERROR, "Launch Error", "Failed to open URL: " + path + "\nError: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                File file = new File(path);
                if (file.exists()) {
                    try {
                        desktop.open(file);
                    } catch (IOException e) {
                        showAlert(AlertType.ERROR, "Launch Error", "Failed to open file/shortcut: " + path + "\nError: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    showAlert(AlertType.ERROR, "File Not Found", "The specified file or shortcut does not exist: " + path);
                }
            }
        } else {
            showAlert(AlertType.ERROR, "Unsupported", "Desktop operations are not supported on this platform.");
        }
    }
}