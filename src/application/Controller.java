package application;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Optional; // NEW: Import Optional for Alert confirmation

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
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

// NEW IMPORTS FOR POPUP, DIALOGS, and CONTEXT MENU
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
import javafx.scene.control.ButtonType; // NEW: Import ButtonType for Alert confirmation
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;


public class Controller implements Initializable {
    @FXML
    private TreeView<LauncherItem> AllLaunchersTreeView;

    @FXML
    private Button addButton;

    private Image folderIcon;
    private Image fileIcon; // For shortcuts/links
    private Image arrowDownIcon;
    private Image arrowUpIcon; // Add this if you have an up arrow image (e.g., arrow_up.png)

    private Popup addOptionsPopup;
    private VBox addOptionsPopupContent;

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        folderIcon = new Image(Controller.class.getResourceAsStream("/foldericon.png"));
        arrowDownIcon = new Image(Controller.class.getResourceAsStream("/arrow_down.png"));
        // Load arrow_up.png if you have it for expansion/collapse visual feedback
        // try {
        //     arrowUpIcon = new Image(Controller.class.getResourceAsStream("/arrow_up.png"));
        // } catch (NullPointerException e) {
        //     System.err.println("Warning: arrow_up.png not found. Expansion will use default arrow_down.");
        //     arrowUpIcon = null;
        // }


        try {
            fileIcon = new Image(Controller.class.getResourceAsStream("/fileicon.png"));
        } catch (NullPointerException e) {
            System.err.println("Warning: fileicon.png not found. Leaf nodes will not have a specific file icon.");
            fileIcon = null; // Ensure it's null if not found
        }


        // Initialize TreeView with LauncherItem
        TreeItem<LauncherItem> rootItem = new TreeItem<>(new LauncherItem("Launchers")); // Root is a folder
        rootItem.setExpanded(true);

        // Example data using LauncherItem
        TreeItem<LauncherItem> launcher1 = new TreeItem<>(new LauncherItem("Work"), new ImageView(folderIcon));
        TreeItem<LauncherItem> launcher2 = new TreeItem<>(new LauncherItem("Study"), new ImageView(folderIcon));
        TreeItem<LauncherItem> launcher3 = new TreeItem<>(new LauncherItem("Games"), new ImageView(folderIcon));

        // Child items (links/shortcuts)
        // Note: The second parameter is the URL/path
        TreeItem<LauncherItem> workLink1 = new TreeItem<>(new LauncherItem("Google", "https://www.google.com"), new ImageView(fileIcon));
        TreeItem<LauncherItem> studyLink1 = new TreeItem<>(new LauncherItem("Stack Overflow", "https://stackoverflow.com"), new ImageView(fileIcon));
        // Example: local executable (adjust path for your system)
        TreeItem<LauncherItem> gameShortcut1 = new TreeItem<>(new LauncherItem("Steam", "C:\\Program Files (x86)\\Steam\\Steam.exe"), new ImageView(fileIcon));
        // Another example shortcut
        TreeItem<LauncherItem> workShortcut1 = new TreeItem<>(new LauncherItem("Notepad", "C:\\Windows\\notepad.exe"), new ImageView(fileIcon));

        launcher1.getChildren().addAll(workLink1, studyLink1, workShortcut1); // Added workShortcut1
        launcher2.getChildren().addAll(studyLink1, gameShortcut1);
        launcher3.getChildren().addAll(workLink1, studyLink1, gameShortcut1);

        rootItem.getChildren().addAll(launcher1, launcher2, launcher3);

        AllLaunchersTreeView.setRoot(rootItem);
        AllLaunchersTreeView.setShowRoot(false);

        // --- IMPORTANT: Set the custom cell factory ---
        // Pass 'this' controller instance so the cell can call dialog methods
        AllLaunchersTreeView.setCellFactory(tv -> new CustomStringTreeCell(this));

        // Initialize the add options popup
        createAddOptionsPopup();
    }

    private void createAddOptionsPopup() {
        addOptionsPopup = new Popup();
        addOptionsPopup.setAutoHide(true); // Popup hides when clicking outside

        Button createNewLauncherBtn = new Button("Create New Launcher Folder"); // Clarified text
        createNewLauncherBtn.getStyleClass().add("popup-button"); // Apply CSS for styling
        createNewLauncherBtn.setOnAction(e -> {
            addOptionsPopup.hide(); // Hide this popup first
            showCreateNewLauncherFolderDialog(); // Show the dialog to create a new folder
        });

        Button addStartupLauncherBtn = new Button("Add Startup Launcher");
        addStartupLauncherBtn.getStyleClass().add("popup-button"); // Apply CSS for styling
        addStartupLauncherBtn.setOnAction(e -> {
            addOptionsPopup.hide(); // Hide this popup first
            System.out.println("Add Startup Launcher clicked!");
            // Implement logic for "Add Startup Launcher" here
            showAlert(AlertType.INFORMATION, "Feature Not Implemented", "This feature is coming soon!");
        });

        addOptionsPopupContent = new VBox(5, createNewLauncherBtn, addStartupLauncherBtn); // 5px spacing
        addOptionsPopupContent.getStyleClass().add("add-options-popup"); // Apply CSS for styling
        addOptionsPopupContent.setAlignment(Pos.CENTER); // Center buttons in the VBox
        addOptionsPopup.getContent().add(addOptionsPopupContent); // Add the content VBox to the popup

        // Force an initial layout pass immediately after content is added
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
        double x = screenCoords.getX() - 140; // Adjust for popup width as needed
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
                // Create a new LauncherItem for the folder
                addLauncherItemToFolder(AllLaunchersTreeView.getRoot(), new LauncherItem(folderName));
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
                addLauncherItemToFolder(parentFolder, new LauncherItem(name, url)); // Create URL LauncherItem
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
                addLauncherItemToFolder(parentFolder, new LauncherItem(name, path)); // Create Shortcut LauncherItem
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
        } else if (folderIcon != null) { // Fallback if no specific icon
            iconView = new ImageView(folderIcon);
        }

        if (iconView != null) {
            iconView.setFitWidth(24);
            iconView.setFitHeight(20);
        }

        TreeItem<LauncherItem> newTreeItem = new TreeItem<>(newLauncherItem, iconView);

        parentFolder.getChildren().add(newTreeItem);
        parentFolder.setExpanded(true); // Ensure the parent folder is expanded
        showAlert(AlertType.INFORMATION, "Item Added", "'" + newLauncherItem.getName() + "' added to '" + parentFolder.getValue().getName() + "'!");
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

    // --- Custom TreeCell Implementation ---
    private class CustomStringTreeCell extends TreeCell<LauncherItem> {

        private HBox contentWrapper;
        private ImageView itemIconView;
        private Label itemTextLabel;
        private Button launchButton;
        private Button customArrowButton;
        private HBox actionButtonsBox;
        private ContextMenu branchContextMenu;

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
                    if (treeItem.getValue().isFolder()) { // If it's a folder (branch)
                        // Show confirmation dialog before launching multiple items
                        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
                        confirmAlert.setTitle("Launch All Items in Folder");
                        confirmAlert.setHeaderText("Launch all items in '" + treeItem.getValue().getName() + "'?");
                        confirmAlert.setContentText("This will attempt to open " + treeItem.getChildren().size() + " links/shortcuts.");
                        confirmAlert.getDialogPane().getStylesheets().add(getClass().getResource("application.css").toExternalForm());
                        confirmAlert.getDialogPane().getStyleClass().add("alert-dialog"); // Apply CSS

                        Optional<ButtonType> result = confirmAlert.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            // Iterate through children and launch them
                            for (TreeItem<LauncherItem> childItem : treeItem.getChildren()) {
                                String pathOrUrl = childItem.getValue().getUrlOrPath();
                                if (pathOrUrl != null && !pathOrUrl.trim().isEmpty()) {
                                    System.out.println("Launching child: " + childItem.getValue().getName() + " (" + pathOrUrl + ")");
                                    controller.launchItemPath(pathOrUrl);
                                } else {
                                    System.out.println("Skipping child with no launch path/URL: " + childItem.getValue().getName());
                                    // Optional: controller.showAlert for individual missing child paths, but might be too many popups
                                }
                            }
                        }
                    } else { // If it's a single shortcut/URL (leaf)
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
            if (arrowDownIcon != null) {
                ImageView arrowImageView = new ImageView(arrowDownIcon);
                customArrowButton.setGraphic(arrowImageView);
                customArrowButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            } else {
                customArrowButton.setText(">");
            }
            customArrowButton.getStyleClass().add("custom-arrow-button");
            customArrowButton.setOnAction(event -> {
                TreeItem<LauncherItem> treeItem = getTreeItem();
                if (treeItem != null && treeItem.getValue().isFolder()) { // Only expand/collapse folders
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

            // --- Context Menu Initialization ---
            branchContextMenu = new ContextMenu();

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

            branchContextMenu.getItems().addAll(addUrlMenuItem, addShortcutMenuItem);
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
                    if (!item.isFolder()) { // If it's a shortcut/link (not a folder)
                        if (fileIcon != null) {
                            itemIconView.setImage(fileIcon);
                            itemIconView.setVisible(true);
                        } else {
                            itemIconView.setImage(folderIcon);
                            itemIconView.setVisible(true);
                        }
                        customArrowButton.setVisible(false);
                        customArrowButton.setManaged(false);
                        launchButton.setVisible(true);
                        launchButton.setManaged(true);
                        setContextMenu(null);
                    } else { // If it's a folder (branch node)
                        if (folderIcon != null) {
                            itemIconView.setImage(folderIcon);
                            itemIconView.setVisible(true);
                        } else {
                            itemIconView.setVisible(false);
                        }
                        customArrowButton.setVisible(true);
                        customArrowButton.setManaged(true);
                        launchButton.setVisible(true); // Launch button always visible for folders
                        launchButton.setManaged(true);

                        setContextMenu(branchContextMenu);
                    }
                }

                setGraphic(contentWrapper);
                setText(null);

                setDisclosureNode(null);

                // Update custom arrow graphic based on expansion state (if custom arrow is visible)
                if (customArrowButton.isVisible() && treeItem != null && treeItem.getValue().isFolder()) {
                    if (treeItem.isExpanded() && arrowUpIcon != null) {
                        ((ImageView)customArrowButton.getGraphic()).setImage(arrowUpIcon);
                    } else if (!treeItem.isExpanded() && arrowDownIcon != null) {
                        ((ImageView)customArrowButton.getGraphic()).setImage(arrowDownIcon);
                    }
                }
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
                            fullUrl = "http://" + path;
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