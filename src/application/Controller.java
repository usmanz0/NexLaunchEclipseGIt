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

public class Controller implements Initializable {
    @FXML
    private TreeView<String> AllLaunchersTreeView;

    private Image folderIcon;
    private Image fileIcon; // Assuming you might have a file icon for leaf nodes
    private Image arrowDownIcon; // Your custom arrow down icon

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        folderIcon = new Image(Controller.class.getResourceAsStream("/foldericon.png"));
        arrowDownIcon = new Image(Controller.class.getResourceAsStream("/arrow_down.png"));

        TreeItem<String> rootItem = new TreeItem<>("Launchers");
        rootItem.setExpanded(true); // Root is expanded by default

        ImageView iconView1 = new ImageView(folderIcon);
        iconView1.setFitWidth(16); iconView1.setFitHeight(16);
        TreeItem<String> launcher1 = new TreeItem<>("Work", iconView1);

        ImageView iconView2 = new ImageView(folderIcon);
        iconView2.setFitWidth(16); iconView2.setFitHeight(16);
        TreeItem<String> launcher2 = new TreeItem<>("Study", iconView2);

        ImageView iconView3 = new ImageView(folderIcon);
        iconView3.setFitWidth(16); iconView3.setFitHeight(16);
        TreeItem<String> launcher3 = new TreeItem<>("Games", iconView3);

        // Child items (these should be distinct instances for proper tree behavior)
        TreeItem<String> launcherItem1_work = new TreeItem<>("Work Item 1");
        TreeItem<String> launcherItem2_study = new TreeItem<>("Study Item 1");
        TreeItem<String> launcherItem3_game = new TreeItem<>("Game Item 1");

        launcher1.getChildren().addAll(launcherItem1_work, launcherItem2_study);
        launcher2.getChildren().addAll(launcherItem2_study, launcherItem3_game); // Example: reuse if logical
        launcher3.getChildren().addAll(launcherItem1_work, launcherItem2_study, launcherItem3_game);

        rootItem.getChildren().addAll(launcher1, launcher2, launcher3);

        AllLaunchersTreeView.setRoot(rootItem);
        AllLaunchersTreeView.setShowRoot(false); // Hide the "Launchers" root node itself

        // --- IMPORTANT: Set the custom cell factory ---
        AllLaunchersTreeView.setCellFactory(tv -> new CustomStringTreeCell());
    }

    @FXML
    public void showAddMenu(ActionEvent event) {
        System.out.println("Add menu working");
    }

    public void selectItem() {
        TreeItem<String> item = AllLaunchersTreeView.getSelectionModel().getSelectedItem();
        if (item != null) {
            System.out.println("Selected: " + item.getValue());
        }
    }

    // --- Custom TreeCell Implementation ---
    private class CustomStringTreeCell extends TreeCell<String> {

        private HBox contentWrapper;
        private ImageView itemIconView; // Renamed to avoid conflict
        private Label itemTextLabel;
        private Button launchButton;
        private Button customArrowButton; // Your custom arrow button
        private HBox actionButtonsBox; // Contains Launch and custom arrow buttons

        public CustomStringTreeCell() {
            // Initialize elements once per cell
            itemIconView = new ImageView();
            itemIconView.setFitWidth(16);
            itemIconView.setFitHeight(16);

            itemTextLabel = new Label();

            launchButton = new Button("Launch");
            launchButton.getStyleClass().add("launch-button");
            launchButton.setOnAction(event -> {
                String url = getItem(); // Assuming item is a URL or path string
                if (url != null) {
                    System.out.println("Launching: " + url);
                    openUrlInBrowser(url);
                }
            });

            customArrowButton = new Button();
            if (arrowDownIcon != null) {
                ImageView arrowImageView = new ImageView(arrowDownIcon);
                arrowImageView.setFitWidth(12);
                arrowImageView.setFitHeight(12);
                customArrowButton.setGraphic(arrowImageView);
            } else {
                customArrowButton.setText(">"); // Fallback
            }
            customArrowButton.getStyleClass().add("custom-arrow-button"); // Custom CSS class
            customArrowButton.setOnAction(event -> {
                TreeItem<String> treeItem = getTreeItem();
                if (treeItem != null) {
                    System.out.println("Custom Arrow clicked for: " + treeItem.getValue());
                    // Toggle expansion/collapse of the tree item
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
                // Important: Hide the default disclosure node when the cell is empty
                setDisclosureNode(null);
            } else {
                itemTextLabel.setText(item);

                // Determine icon based on TreeItem's properties
                TreeItem<String> treeItem = getTreeItem(); // Get the TreeItem this cell represents
                if (treeItem != null) {
                    if (treeItem.isLeaf()) {
                        // For leaf nodes, use a file icon (or no icon)
                        if (fileIcon != null) {
                            itemIconView.setImage(fileIcon);
                            itemIconView.setVisible(true);
                        } else {
                            itemIconView.setVisible(false);
                        }
                        // Hide custom arrow button for leaf nodes
                        customArrowButton.setVisible(false);
                    } else {
                        // For branch nodes (folders), use folder icon
                        if (folderIcon != null) {
                            itemIconView.setImage(folderIcon);
                            itemIconView.setVisible(true);
                        } else {
                            itemIconView.setVisible(false);
                        }
                        // Show custom arrow button for branch nodes
                        customArrowButton.setVisible(true);
                    }
                }

                // Set the graphic of the TreeCell to our custom HBox
                setGraphic(contentWrapper);
                setText(null); // Crucial: prevent default text rendering

                // Set the TreeCell's disclosure node to null to hide the default triangle
                setDisclosureNode(null); // This is key to remove the default arrow

                // Update the custom arrow button's graphic based on expansion state
                if (customArrowButton.isVisible() && treeItem != null) {
                     if (treeItem.isExpanded()) {
                         // You might want a different icon for expanded state (e.g., arrow pointing up)
                         // For now, it's just the arrowDownIcon.
                         // If you have an "arrow_up.png", you can swap it here.
                         // ((ImageView)customArrowButton.getGraphic()).setImage(arrowUpIcon);
                     } else {
                         // ((ImageView)customArrowButton.getGraphic()).setImage(arrowDownIcon);
                     }
                }
            }
        }
    }

    // Helper method to open a URL in the default browser (unchanged)
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