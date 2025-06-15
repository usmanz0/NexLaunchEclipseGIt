package application;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects; // NEW: For Objects.equals and Objects.hash

public class LauncherItem implements Serializable {
    private static final long serialVersionUID = 1L; // For serialization version control

    private String name;
    private String urlOrPath; // Null if it's a folder
    private boolean isFolder;
    private boolean isStartupLauncher; // NEW: Flag for startup launchers
    private List<LauncherItem> children; // Only applicable if isFolder is true

    // Constructor for folders
    public LauncherItem(String name) {
        this.name = name;
        this.urlOrPath = null; // Folders don't have a URL or path
        this.isFolder = true;
        this.isStartupLauncher = false; // Folders themselves are not startup launchers (items inside are)
        this.children = new ArrayList<>();
    }

    // Constructor for URL/Shortcut items
    public LauncherItem(String name, String urlOrPath) {
        this.name = name;
        this.urlOrPath = urlOrPath;
        this.isFolder = false;
        this.isStartupLauncher = false; // Default to not a startup launcher
        this.children = null; // Non-folder items don't have children
    }

    // NEW: Constructor to explicitly set isStartupLauncher
    public LauncherItem(String name, String urlOrPath, boolean isStartupLauncher) {
        this.name = name;
        this.urlOrPath = urlOrPath;
        this.isFolder = (urlOrPath == null); // If urlOrPath is null, it's a folder
        this.isStartupLauncher = isStartupLauncher;
        this.children = this.isFolder ? new ArrayList<>() : null;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrlOrPath() {
        return urlOrPath;
    }

    public void setUrlOrPath(String urlOrPath) {
        this.urlOrPath = urlOrPath;
    }

    public boolean isFolder() {
        return isFolder;
    }

    // For folders, adds a child. For non-folders, this does nothing or throws error.
    public void addChild(LauncherItem child) {
        if (this.isFolder && this.children != null) {
            this.children.add(child);
        } else {
            System.err.println("Cannot add child to a non-folder LauncherItem.");
        }
    }

    // For folders, removes a child.
    public void removeChild(LauncherItem child) {
        if (this.isFolder && this.children != null) {
            this.children.remove(child);
        } else {
            System.err.println("Cannot remove child from a non-folder LauncherItem.");
        }
    }

    public List<LauncherItem> getChildren() {
        return children; // Returns null if not a folder, handle in calling code
    }

    // NEW: Getter and Setter for isStartupLauncher
    public boolean isStartupLauncher() {
        return isStartupLauncher;
    }

    public void setStartupLauncher(boolean startupLauncher) {
        isStartupLauncher = startupLauncher;
    }

    @Override
    public String toString() {
        return name + (isFolder ? " (Folder)" : " (" + urlOrPath + ")");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LauncherItem that = (LauncherItem) o;
        // For equality, check if it's the same type (folder/non-folder)
        // and if names match. For non-folders, paths must also match.
        // For folders, children don't necessarily need to match for simple equality checks.
        // The urlOrPath being null for folders effectively differentiates them.
        if (isFolder != that.isFolder) return false;
        if (!name.equals(that.name)) return false;
        // If both are non-folders, compare their paths.
        // If both are folders, no path to compare.
        // If one is folder and other is not, already handled by isFolder check.
        if (!isFolder) { // It's a non-folder (URL or Shortcut)
            return Objects.equals(urlOrPath, that.urlOrPath);
        }
        // For folders, we only compare name and isFolder property.
        // If you need deeper equality (e.g., same children), you'd need to iterate children.
        // For current use (finding an item based on its path for modification),
        // comparing paths for non-folders is key.
        return true;
    }

    @Override
    public int hashCode() {
        // Hash code should be consistent with equals.
        // For non-folders, include urlOrPath. For folders, only name and isFolder.
        return Objects.hash(name, urlOrPath, isFolder);
    }
}