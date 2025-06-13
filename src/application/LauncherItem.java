// application/LauncherItem.java
package application;

public class LauncherItem {
    private String name;
    private String urlOrPath; // Can be a URL or a local file path
    private boolean isFolder; // true if it's a folder, false if it's a shortcut/URL

    // Constructor for shortcut/URL items
    public LauncherItem(String name, String urlOrPath) {
        this.name = name;
        this.urlOrPath = urlOrPath;
        this.isFolder = false; // By default, items with a URL are not folders
    }

    // Constructor for folder items
    public LauncherItem(String name) {
        this.name = name;
        this.urlOrPath = null;
        this.isFolder = true; // By default, items created with just a name are folders
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

    // This method is crucial for the TreeView to display the item's name
    @Override
    public String toString() {
        return name;
    }
}