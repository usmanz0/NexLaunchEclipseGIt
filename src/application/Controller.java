package application;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

// Removed Gson imports as data handling is moved to LauncherDataService
// import com.google.gson.Gson;
// import com.google.gson.GsonBuilder;
// import com.google.gson.reflect.TypeToken;
import java.io.File; // Still needed for Desktop.open(File)
import java.io.IOException; // Still needed for Desktop operations and LauncherDataService exceptions
// import java.io.FileReader;
// import java.io.FileWriter;
// import java.lang.reflect.Type;

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

// For opening links/shortcuts
import java.awt.Desktop;
// import java.io.IOException; // Already imported above
import java.net.URI;
import java.net.URISyntaxException;

// IMPORTS FOR POPUP, DIALOGS, and CONTEXT MENU
import javafx.stage.Popup;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.stage.Modality;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
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

    private Image folderIcon;
    private Image fileIcon;
    private Image arrowDownIcon;
    private Image arrowUpIcon;

    private Popup addOptionsPopup;
    private VBox addOptionsPopupContent;

    // NEW: Instance of LauncherDataService
    private LauncherDataService dataService;

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

        // Initialize the data service
        dataService = new LauncherDataService();

        // Load data at startup
        loadLaunchers();

        AllLaunchersTreeView.setShowRoot(false);
        // Pass 'this' (the Controller instance) to the cell factory
        AllLaunchersTreeView.setCellFactory(tv -> new CustomStringTreeCell(this));
        createAddOptionsPopup();
    }

    // --- Load Method (using LauncherDataService) ---
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
            // loadedItems remains an empty ArrayList if an exception occurs
        } catch (com.google.gson.JsonParseException e) { // Catch Gson parsing issues
            System.err.println("Controller: !!! ERROR parsing launchers data: " + e.getMessage());
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Load Error", "Failed to parse launcher data (file might be corrupted): " + e.getMessage() + "\nStarting with an empty launcher list.");
        }

        // Always create a root item, even if it's conceptually empty.
        // This 'root' LauncherItem is just a placeholder for the invisible root of the TreeView.
        TreeItem<LauncherItem> rootItem = new TreeItem<>(new LauncherItem("InvisibleRoot")); // Name it anything, it won't be seen

        if (!loadedItems.isEmpty()) {
            for (LauncherItem item : loadedItems) {
                // Add loaded items as children to the newly created rootItem
                rootItem.getChildren().add(convertLauncherItemToTreeItem(item));
            }
        } else {
            System.out.println("Controller: No existing launcher data found or data was empty/corrupted. Starting with an empty launcher list.");
        }

        AllLaunchersTreeView.setRoot(rootItem);
        // It's good practice to expand the root, even if setShowRoot(false) is true,
        // so that its children (your top-level folders/launchers) are visible.
        rootItem.setExpanded(true);
    }

    // --- Save Method (using LauncherDataService) ---
    // Call this method from Main.java when the primary stage closes or after modifications
    public void saveLaunchers() {
        System.out.println("Controller: saveLaunchers() called.");
        
        List<LauncherItem> itemsToSave = new ArrayList<>();
        if (AllLaunchersTreeView.getRoot() != null) {
            // Convert top-level TreeItems to LauncherItems for serialization
            for (TreeItem<LauncherItem> treeItem : AllLaunchersTreeView.getRoot().getChildren()) {
                itemsToSave.add(convertTreeItemToLauncherItem(treeItem));
            }
        }

        if (itemsToSave.isEmpty()) {
            dataService.deleteDataFile(); // Delete file if nothing to save
            System.out.println("Controller: No launchers to save. Data file handled by service.");
            return;
        }

        try {
            dataService.saveLaunchers(itemsToSave);
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
        double x = screenCoords.getX() - 140; // Adjust position as needed
        double y = screenCoords.getY();
        Window window = addButton.getScene().getWindow();

        addOptionsPopupContent.applyCss();
        addOptionsPopupContent.layout();

        double popupHeight = addOptionsPopupContent.getHeight();
        double margin = 8; // Small margin above the button

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
                TreeItem<LauncherItem> currentRoot = AllLaunchersTreeView.getRoot();
                // This check is now less likely to be null due to loadLaunchers fix
                if (currentRoot != null) {
                    addLauncherItemToFolder(currentRoot, new LauncherItem(folderName));
                    dialogStage.close();
                } else {
                    // This scenario should now ideally not happen, but kept as a fallback.
                    showAlert(AlertType.ERROR, "Internal Error", "Root folder is missing. Cannot add new folder.");
                    dialogStage.close();
                }
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
                addLauncherItemToFolder(parentFolder, new LauncherItem(name, url));
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
                addLauncherItemToFolder(parentFolder, new LauncherItem(name, path));
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
        parentFolder.setExpanded(true); // Automatically expand the folder when adding
        showAlert(AlertType.INFORMATION, "Item Added", "'" + newLauncherItem.getName() + "' added to '" + parentFolder.getValue().getName() + "'!");
        saveLaunchers(); // Save changes after adding an item
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
            showAlert(AlertType.INFORMATION, "Deleted", "'" + itemToDelete.getValue().getName() + "' has been deleted.");
            saveLaunchers(); // Save changes after deletion
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
        private ContextMenu folderContextMenu; // Context menu for folders
        private ContextMenu leafContextMenu;   // Context menu for non-folder items (URLs/shortcuts)
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
            // Ensure arrowDownIcon and arrowUpIcon are not null before setting graphic
            if (controller.arrowDownIcon != null && controller.arrowUpIcon != null) {
                ImageView arrowImageView = new ImageView(controller.arrowDownIcon); // Default to down arrow
                customArrowButton.setGraphic(arrowImageView);
                customArrowButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            } else {
                customArrowButton.setText(">"); // Fallback text if icons are missing
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

            // --- Define Folder Context Menu ---
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

            MenuItem deleteFolderMenuItem = new MenuItem("Delete Item"); // "Delete Item" applies to folder itself
            deleteFolderMenuItem.setOnAction(event -> {
                TreeItem<LauncherItem> selectedItem = getTreeItem();
                if (selectedItem != null) {
                    controller.deleteLauncherItem(selectedItem);
                }
            });
            folderContextMenu.getItems().addAll(addUrlMenuItem, addShortcutMenuItem, deleteFolderMenuItem);

            // --- Define Leaf (URL/Shortcut) Context Menu ---
            leafContextMenu = new ContextMenu();
            MenuItem deleteLeafMenuItem = new MenuItem("Delete Item"); // "Delete Item" applies to URL/shortcut
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
                setDisclosureNode(null); // Hide default disclosure triangle
                setContextMenu(null);   // Remove context menu when empty
            } else {
                if (contentWrapper != null) {
                    contentWrapper.setVisible(true);
                    contentWrapper.setManaged(true);
                }

                itemTextLabel.setText(item.getName());

                TreeItem<LauncherItem> treeItem = getTreeItem();
                if (treeItem != null) {
                    // Set icon based on whether it's a folder or not
                    if (item.isFolder()) {
                        itemIconView.setImage(controller.folderIcon);
                        itemIconView.setVisible(true);
                        customArrowButton.setVisible(true);
                        customArrowButton.setManaged(true);
                        launchButton.setVisible(true); // Launch button for folders (launch all)
                        launchButton.setManaged(true);
                        setContextMenu(folderContextMenu); // Assign folder context menu

                        // Update arrow button graphic based on expanded state
                        if (treeItem.isExpanded()) {
                            ((ImageView) customArrowButton.getGraphic()).setImage(controller.arrowUpIcon);
                        } else {
                            ((ImageView) customArrowButton.getGraphic()).setImage(controller.arrowDownIcon);
                        }
                    } else { // It's a URL or Shortcut
                        if (controller.fileIcon != null) {
                            itemIconView.setImage(controller.fileIcon);
                        } else {
                            itemIconView.setImage(controller.folderIcon); // Fallback to folder icon
                        }
                        itemIconView.setVisible(true);
                        customArrowButton.setVisible(false); // Hide arrow for leaf nodes
                        customArrowButton.setManaged(false);
                        launchButton.setVisible(true); // Launch button for single items
                        launchButton.setManaged(true);
                        setContextMenu(leafContextMenu); // Assign leaf context menu
                    }
                }

                setGraphic(contentWrapper);
                setText(null); // Clear default text
                setDisclosureNode(null); // Ensure default disclosure triangle is not shown
            }
        }
    }

    private void launchItemPath(String path) {
        if (Desktop.isDesktopSupported()) {
            try {
                if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("ftp://") || path.startsWith("www.")) {
                    if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        String fullUrl = path;
                        if (path.startsWith("www.") && !path.contains("://")) {
                            fullUrl = "http://" + path; // Prepend http if only www. is present
                        }
                        Desktop.getDesktop().browse(new URI(fullUrl));
                    } else {
                        showAlert(AlertType.ERROR, "Launch Error", "Browser launch not supported on this system.");
                    }
                } else {
                    File file = new File(path);
                    if (file.exists()) {
                        if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                            Desktop.getDesktop().open(file);
                        } else {
                            showAlert(AlertType.ERROR, "Launch Error", "File open not supported on this system.");
                        }
                    } else {
                        showAlert(AlertType.ERROR, "Launch Error", "File or path not found: " + path);
                    }
                }
            } catch (URISyntaxException e) {
                showAlert(AlertType.ERROR, "Launch Error", "Invalid URL/URI syntax: " + path + "\n" + e.getMessage());
            } catch (IOException e) {
                showAlert(AlertType.ERROR, "Launch Error", "Failed to launch: " + path + "\n" + e.getMessage());
            } catch (SecurityException e) {
                showAlert(AlertType.ERROR, "Launch Error", "Security exception while launching: " + path + "\n" + e.getMessage());
            }
        } else {
            showAlert(AlertType.ERROR, "Launch Error", "Desktop actions are not supported on this system.");
        }
    }
}