package application;

import java.net.URL;
import java.util.ResourceBundle;

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
import javafx.util.Callback;
import javafx.scene.control.Label; // Explicitly import Label

// For opening links
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

// NEW IMPORTS FOR POPUP AND DIALOGS
import javafx.stage.Popup;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.stage.Modality;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;


public class Controller implements Initializable {
    @FXML
    private TreeView<String> AllLaunchersTreeView;

    // RE-ADDED: This FXML field is crucial for linking the Add button
    @FXML
    private Button addButton;

    private Image folderIcon;
    private Image fileIcon; // Assuming you might have a file icon for leaf nodes
    private Image arrowDownIcon; // Your custom arrow down icon

    // NEW: Popup for "Create New Launcher" / "Add Startup Launcher" options
    private Popup addOptionsPopup;
    private VBox addOptionsPopupContent;


    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        folderIcon = new Image(Controller.class.getResourceAsStream("/foldericon.png"));
        arrowDownIcon = new Image(Controller.class.getResourceAsStream("/arrow_down.png"));

        // If you have fileicon.png, uncomment the line below:
        // try {
        //     fileIcon = new Image(Controller.class.getResourceAsStream("/fileicon.png"));
        // } catch (NullPointerException e) {
        //     System.err.println("Warning: fileicon.png not found. Leaf nodes will not have a specific file icon.");
        //     fileIcon = null;
        // }


        TreeItem<String> rootItem = new TreeItem<>("Launchers");
        rootItem.setExpanded(true); // Root is expanded by default

        // These ImageView instances are now primarily used to pass the Image reference to the TreeItem.
        // The sizing (setFitWidth/Height) is applied to the ImageView within the CustomStringTreeCell.
        // Note: The iconView.setFitWidth/Height are removed here as CustomStringTreeCell handles it.
        TreeItem<String> launcher1 = new TreeItem<>("Work", new ImageView(folderIcon));
        TreeItem<String> launcher2 = new TreeItem<>("Study", new ImageView(folderIcon));
        TreeItem<String> launcher3 = new TreeItem<>("Games", new ImageView(folderIcon));

        // Child items (these should be distinct instances for proper tree behavior)
        // Changed to include "Link" for demonstration with the launch button
        TreeItem<String> launcherItem1_work = new TreeItem<>("Work Item 1 - Link");
        TreeItem<String> launcherItem2_study = new TreeItem<>("Study Item 1 - Link");
        TreeItem<String> launcherItem3_game = new TreeItem<>("Game Item 1 - Link");

        launcher1.getChildren().addAll(launcherItem1_work, launcherItem2_study);
        launcher2.getChildren().addAll(launcherItem2_study, launcherItem3_game); // Example: reuse if logical
        launcher3.getChildren().addAll(launcherItem1_work, launcherItem2_study, launcherItem3_game);

        rootItem.getChildren().addAll(launcher1, launcher2, launcher3);

        AllLaunchersTreeView.setRoot(rootItem);
        AllLaunchersTreeView.setShowRoot(false); // Hide the "Launchers" root node itself

        // --- IMPORTANT: Set the custom cell factory ---
        AllLaunchersTreeView.setCellFactory(tv -> new CustomStringTreeCell());

        // NEW: Initialize the add options popup
        createAddOptionsPopup();
    }

    // NEW METHOD: To create the initial Add Options Popup
    private void createAddOptionsPopup() {
        addOptionsPopup = new Popup();
        addOptionsPopup.setAutoHide(true); // Popup hides when clicking outside

        Button createNewLauncherBtn = new Button("Create New Launcher");
        createNewLauncherBtn.getStyleClass().add("popup-button"); // Apply CSS for styling
        createNewLauncherBtn.setOnAction(e -> {
            addOptionsPopup.hide(); // Hide this popup first
            showCreateNewLauncherDialog(); // Show the dialog to create a new folder
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
    }

    // MODIFIED: Method to show the "Add" button's popup
    @FXML
    public void showAddMenu(ActionEvent event) {
        if (addOptionsPopup.isShowing()) {
            addOptionsPopup.hide();
            return;
        }

        // Calculate position 8px above the addButton
        // Ensure addButton is not null (should be handled by FXML fx:id="addButton")
        if (addButton == null) {
            System.err.println("Error: addButton is null. Check FXML fx:id.");
            return;
        }

        Window window = addButton.getScene().getWindow();
        double x = window.getX() + addButton.localToScreen(0, 0).getX();
        double y = window.getY() + addButton.localToScreen(0, 0).getY();

        // Force layout if needed to get accurate dimensions before showing
        addOptionsPopupContent.layout();
        double popupHeight = addOptionsPopupContent.getHeight();

        addOptionsPopup.show(window, x, y - popupHeight - 8); // Position 8px above
    }

    // NEW METHOD: To show the "Create New Launcher" dialog
    private void showCreateNewLauncherDialog() {
        // Create a new custom dialog stage
        javafx.stage.Stage dialogStage = new javafx.stage.Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL); // Blocks other windows
        dialogStage.initOwner(AllLaunchersTreeView.getScene().getWindow()); // Parent window
        dialogStage.setTitle("Create New Launcher Folder");
        dialogStage.setResizable(false);

        TextField folderNameField = new TextField();
        folderNameField.setPromptText("Enter folder name");
        folderNameField.getStyleClass().add("text-field-dark"); // Apply custom style

        Button createBtn = new Button("Create");
        createBtn.getStyleClass().add("dialog-button");
        createBtn.setOnAction(e -> {
            String folderName = folderNameField.getText().trim();
            if (!folderName.isEmpty()) {
                createNewLauncherFolder(folderName); // Call method to add folder
                dialogStage.close();
            } else {
                showAlert(AlertType.WARNING, "Input Error", "Folder name cannot be empty.");
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("dialog-button");
        cancelBtn.setOnAction(e -> dialogStage.close());

        HBox buttonBar = new HBox(10, createBtn, cancelBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT); // Align buttons to the right

        VBox dialogContent = new VBox(10, new Label("New Launcher Folder Name:"), folderNameField, buttonBar);
        dialogContent.setPadding(new javafx.geometry.Insets(20));
        dialogContent.getStyleClass().add("dialog-background"); // Apply custom style

        Scene dialogScene = new Scene(dialogContent);
        // Ensure CSS is applied to the dialog scene as well
        dialogScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        dialogStage.setScene(dialogScene);
        dialogStage.showAndWait(); // Show and wait for it to close
    }

    // NEW METHOD: To create and add the new folder to the TreeView
    // Ensures new folders are always added as direct branches of the root
    private void createNewLauncherFolder(String folderName) {
        TreeItem<String> root = AllLaunchersTreeView.getRoot(); // Get the main root item

        ImageView newFolderIcon = new ImageView(folderIcon);
        newFolderIcon.setFitWidth(24);
        newFolderIcon.setFitHeight(20);
        TreeItem<String> newFolder = new TreeItem<>(folderName, newFolderIcon);

        root.getChildren().add(newFolder); // Add to the root's children
        root.setExpanded(true); // Ensure the root is expanded to show the new folder
        AllLaunchersTreeView.getSelectionModel().select(newFolder); // Select the new folder
        showAlert(AlertType.INFORMATION, "Folder Created", "New launcher folder '" + folderName + "' created successfully!");
    }

    // NEW HELPER METHOD: For showing various alerts
    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        // Apply custom CSS to the alert dialog as well
        alert.getDialogPane().getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("alert-dialog");
        alert.showAndWait();
    }


    public void selectItem() {
        TreeItem<String> item = AllLaunchersTreeView.getSelectionModel().getSelectedItem();
        if (item != null) {
            System.out.println("Selected: " + item.getValue());
        }
    }

    // --- Custom TreeCell Implementation (UNCHANGED from last functional version) ---
    private class CustomStringTreeCell extends TreeCell<String> {

        private HBox contentWrapper;
        private ImageView itemIconView;
        private Label itemTextLabel;
        private Button launchButton;
        private Button customArrowButton;
        private HBox actionButtonsBox;

        public CustomStringTreeCell() {
            itemIconView = new ImageView();
            itemIconView.setFitWidth(24);
            itemIconView.setFitHeight(20);

            itemTextLabel = new Label();

            launchButton = new Button("Launch");
            launchButton.getStyleClass().add("launch-button");
            launchButton.setOnAction(event -> {
                String url = getItem();
                // Basic check if the item string looks like a URL before attempting to open
                if (url != null && (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("www."))) {
                    System.out.println("Launching URL: " + url);
                    openUrlInBrowser(url);
                } else {
                    // For demonstration, if not a URL, print to console or handle as local path
                    System.out.println("No URL to launch for: " + url + " (Or not a valid URL format)");
                    showAlert(AlertType.INFORMATION, "Launch Info", "Attempted to launch: " + url + "\n(Assuming it's a link)");
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
                TreeItem<String> treeItem = getTreeItem();
                if (treeItem != null) {
                    System.out.println("Custom Arrow clicked for: " + treeItem.getValue());
                    treeItem.setExpanded(!treeItem.isExpanded());
                }
            });

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            actionButtonsBox = new HBox(5, launchButton, customArrowButton);
            actionButtonsBox.getStyleClass().add("tree-cell-action-buttons");

            contentWrapper = new HBox(5, itemIconView, itemTextLabel, spacer, actionButtonsBox);
            contentWrapper.getStyleClass().add("custom-tree-cell-content");
            contentWrapper.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                // When empty, hide the entire content wrapper to ensure nothing is visible
                if (contentWrapper != null) {
                    contentWrapper.setVisible(false);
                    contentWrapper.setManaged(false);
                }
                setDisclosureNode(null);
            } else {
                // Ensure contentWrapper is visible and managed when the cell is not empty
                if (contentWrapper != null) {
                    contentWrapper.setVisible(true);
                    contentWrapper.setManaged(true);
                }

                itemTextLabel.setText(item);

                TreeItem<String> treeItem = getTreeItem();
                if (treeItem != null) {
                    // Icon logic: For leaf nodes, use fileIcon (if available), otherwise folderIcon.
                    // For branch nodes (folders), use folderIcon.
                    if (treeItem.isLeaf()) {
                        if (fileIcon != null) {
                            itemIconView.setImage(fileIcon);
                            itemIconView.setVisible(true);
                        } else {
                            itemIconView.setImage(folderIcon); // Fallback to folderIcon if no fileIcon
                            itemIconView.setVisible(true);
                        }
                        // For leaf nodes, hide custom arrow but keep launch button visible
                        customArrowButton.setVisible(false);
                        customArrowButton.setManaged(false);
                        launchButton.setVisible(true); // Ensure launch button is visible for leaf nodes
                        launchButton.setManaged(true);
                    } else { // Branch nodes (folders)
                        if (folderIcon != null) {
                            itemIconView.setImage(folderIcon);
                            itemIconView.setVisible(true);
                        } else {
                            itemIconView.setVisible(false); // Hide if no folder icon provided
                        }
                        customArrowButton.setVisible(true); // Show custom arrow for branch nodes
                        customArrowButton.setManaged(true);
                        launchButton.setVisible(true); // Hide launch button for folder nodes by default
                        launchButton.setManaged(true);
                    }
                }

                setGraphic(contentWrapper);
                setText(null);

                setDisclosureNode(null); // Still hide the default disclosure triangle

                // Update custom arrow graphic based on expansion state (if custom arrow is visible)
                if (customArrowButton.isVisible() && treeItem != null) {
                     if (treeItem.isExpanded()) {
                         // If you have an "arrow_up.png" image, you can swap it here
                         // ((ImageView)customArrowButton.getGraphic()).setImage(arrowUpIcon);
                     } else {
                         // If you have an "arrow_down.png" image, you can swap it back
                         // ((ImageView)customArrowButton.getGraphic()).setImage(arrowDownIcon);
                     }
                }
            }
        }
    }

    // Helper method to open a URL in the default browser
    private void openUrlInBrowser(String url) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                URI uri = new URI(url);
                Desktop.getDesktop().browse(uri);
            } catch (URISyntaxException e) {
                System.err.println("Invalid URL syntax: " + url + " - " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Failed to open URL: " + url + " - " + e.getMessage());
            }
        } else {
            System.err.println("Desktop Browse not supported on this system.");
        }
    }
}