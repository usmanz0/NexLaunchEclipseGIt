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

    // --- NEW: Field to identify startup launchers ---
    private boolean isStartupLauncher;

    // Constructor for folders
    public LauncherItem(String name) {
        this.name = name;
        this.isFolder = true;
        this.urlOrPath = null;
        this.children = new ArrayList<>(); // Initialize children list for folders
        this.isStartupLauncher = false; // Default to false for regular folders
    }

    // Constructor for shortcuts/URLs
    public LauncherItem(String name, String urlOrPath) {
        this.name = name;
        this.urlOrPath = urlOrPath;
        this.isFolder = false;
        this.children = null; // No children for leaf nodes
        this.isStartupLauncher = false; // Default to false for regular launchers
    }

    // --- NEW: Overloaded constructor to set isStartupLauncher directly (useful for creation) ---
    public LauncherItem(String name, String urlOrPath, boolean isStartupLauncher) {
        this.name = name;
        this.urlOrPath = urlOrPath;
        this.isFolder = (urlOrPath == null); // If urlOrPath is null, it's a folder
        this.children = this.isFolder ? new ArrayList<>() : null; // Only initialize children for folders
        this.isStartupLauncher = isStartupLauncher;
    }

    // Default constructor for Gson deserialization (mandatory if you have custom constructors)
    public LauncherItem() {
        // Gson will populate fields directly. Initialize children to avoid NPE before Gson sets it.
        this.children = new ArrayList<>();
        this.isStartupLauncher = false; // Default for Gson if not in JSON
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

    // --- NEW: Getter for isStartupLauncher ---
    public boolean isStartupLauncher() {
        return isStartupLauncher;
    }

    public List<LauncherItem> getChildren() {
        // Return an empty list if it's a leaf to avoid NullPointerException when iterating
        return isFolder && children != null ? children : new ArrayList<>();
    }

    public void addChild(LauncherItem child) {
        if (this.isFolder && this.children != null) {
            this.children.add(child);
        } else {
            System.err.println("Cannot add child to a non-folder LauncherItem or uninitialized children list: " + this.name);
        }
    }

    // --- Setters (optional, but good for Gson if fields are private) ---
    public void setName(String name) {
        this.name = name;
    }

    public void setUrlOrPath(String urlOrPath) {
        this.urlOrPath = urlOrPath;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
        // If it becomes a folder, ensure children list is initialized
        if (isFolder && children == null) {
            children = new ArrayList<>();
        } else if (!isFolder) { // If it becomes a leaf, no children
            children = null;
        }
    }

    public void setChildren(List<LauncherItem> children) {
        this.children = children;
    }

    // --- NEW: Setter for isStartupLauncher ---
    public void setStartupLauncher(boolean startupLauncher) {
        isStartupLauncher = startupLauncher;
    }
}