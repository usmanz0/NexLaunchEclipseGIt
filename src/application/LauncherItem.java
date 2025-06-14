package application;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LauncherItem implements Serializable {
    private static final long serialVersionUID = 1L; // Recommended for Serializable

    private String name;
    private String urlOrPath; // Null for folders
    private boolean isFolder;
    private List<LauncherItem> children; // To store children if it's a folder

    // Constructor for folders
    public LauncherItem(String name) {
        this.name = name;
        this.isFolder = true;
        this.urlOrPath = null;
        this.children = new ArrayList<>(); // Initialize children list for folders
    }

    // Constructor for shortcuts/URLs
    public LauncherItem(String name, String urlOrPath) {
        this.name = name;
        this.urlOrPath = urlOrPath;
        this.isFolder = false;
        this.children = null; // No children for leaf nodes
    }

    // Default constructor for Gson deserialization (mandatory if you have custom constructors)
    public LauncherItem() {
        this.children = new ArrayList<>(); // Initialize for deserialization, will be nullified for leaves
    }

    public String getName() {
        return name;
    }

    public String getUrlOrPath() {
        return urlOrPath;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public List<LauncherItem> getChildren() {
        // Return an empty list if it's a leaf to avoid NullPointerException when iterating
        return isFolder && children != null ? children : new ArrayList<>();
    }

    // Method to add a child (used during TreeItem to LauncherItem conversion for saving)
    public void addChild(LauncherItem child) {
        if (this.isFolder && this.children != null) {
            this.children.add(child);
        } else {
            System.err.println("Cannot add child to a non-folder LauncherItem or uninitialized children list: " + this.name);
        }
    }

    // --- Setters (optional, but good for Gson if fields are private) ---
    // Gson can access private fields via reflection, but explicit setters don't hurt
    public void setName(String name) {
        this.name = name;
    }

    public void setUrlOrPath(String urlOrPath) {
        this.urlOrPath = urlOrPath;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
    }

    public void setChildren(List<LauncherItem> children) {
        this.children = children;
    }
}
