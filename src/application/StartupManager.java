package application;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;

public class StartupManager {

    private static final String APP_NAME = "NexLaunch"; // Your application's name
    private static final String BACKGROUND_ARG = "--background-startup"; // Argument to launch in background

    /**
     * Attempts to register the application to run at system startup.
     * On Windows, this creates a shortcut in the user's Startup folder.
     * It also asks for user permission if not already granted.
     *
     * @param appExecutablePath The full path to the application's executable (.exe).
     * @return true if registered successfully or already registered, false otherwise.
     */
    public boolean registerAppForStartup(String appExecutablePath) {
        if (appExecutablePath == null || appExecutablePath.isEmpty()) {
            System.err.println("StartupManager: App executable path cannot be null or empty.");
            return false;
        }

        // --- Windows Specific Implementation ---
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return registerWindowsStartup(appExecutablePath);
        } else {
            // Placeholder for other OSes
            System.out.println("StartupManager: Startup registration not implemented for " + System.getProperty("os.name"));
            return false;
        }
    }

    /**
     * Attempts to remove the application from running at system startup.
     * On Windows, this deletes the shortcut from the user's Startup folder.
     *
     * @return true if removed successfully or not found, false otherwise.
     */
    public boolean removeAppFromStartup() {
        // --- Windows Specific Implementation ---
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return removeWindowsStartup();
        } else {
            // Placeholder for other OSes
            System.out.println("StartupManager: Startup unregistration not implemented for " + System.getProperty("os.name"));
            return false;
        }
    }

    /**
     * Checks if the application is currently registered for startup.
     * @return true if registered, false otherwise.
     */
    public boolean isAppRegisteredForStartup() {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            Path shortcutPath = getWindowsStartupShortcutPath();
            return Files.exists(shortcutPath);
        }
        return false; // Default for other OSes
    }

    // --- Windows Specific Logic ---

    private Path getWindowsStartupShortcutPath() {
        // This is the common path for user-specific startup programs on Windows
        // C:\Users\<UserName>\AppData\Roaming\Microsoft\Windows\Start Menu\Programs\Startup\
        String startupFolderPath = System.getProperty("user.home") + "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup\\";
        return Paths.get(startupFolderPath, APP_NAME + ".lnk");
    }

    private boolean registerWindowsStartup(String appExecutablePath) {
        Path shortcutPath = getWindowsStartupShortcutPath();
        File shortcutFile = shortcutPath.toFile();

        if (shortcutFile.exists()) {
            System.out.println("StartupManager: App is already registered for Windows startup.");
            return true; // Already registered
        }

        // Use PowerShell to create a shortcut, specifying the target and arguments.
        // The -WindowStyle Hidden ensures the cmd/powershell window doesn't flash.
        String targetPath = appExecutablePath;
        String arguments = BACKGROUND_ARG; // Pass the background argument
        String workingDirectory = new File(appExecutablePath).getParent(); // Set working directory to app's folder

        // PowerShell command to create a shortcut
        String command = String.format(
            "powershell.exe -Command \"$shell = New-Object -ComObject WScript.Shell; " +
            "$shortcut = $shell.CreateShortcut('%s'); " +
            "$shortcut.TargetPath = '%s'; " +
            "$shortcut.Arguments = '%s'; " +
            "$shortcut.WorkingDirectory = '%s'; " +
            "$shortcut.Save()\"",
            shortcutPath.toString(), targetPath, arguments, workingDirectory
        );

        System.out.println("StartupManager: Attempting to register app for Windows startup...");
        try {
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("StartupManager: App registered for Windows startup successfully.");
                return true;
            } else {
                String errorOutput = new String(process.getErrorStream().readAllBytes());
                System.err.println("StartupManager: Failed to register app for Windows startup. Exit code: " + exitCode + ", Error: " + errorOutput);
                return false;
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("StartupManager: Exception during Windows startup registration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean removeWindowsStartup() {
        Path shortcutPath = getWindowsStartupShortcutPath();
        File shortcutFile = shortcutPath.toFile();

        if (!shortcutFile.exists()) {
            System.out.println("StartupManager: App is not registered for Windows startup (no shortcut found).");
            return true; // Already removed or never existed
        }

        System.out.println("StartupManager: Attempting to remove app from Windows startup...");
        try {
            Files.delete(shortcutPath);
            System.out.println("StartupManager: App removed from Windows startup successfully.");
            return true;
        } catch (IOException e) {
            System.err.println("StartupManager: Failed to remove app from Windows startup: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Shows a confirmation dialog to the user for allowing app startup.
     * @return true if user confirms, false otherwise.
     */
    public boolean askUserForStartupPermission() {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Enable Automatic Startup");
        alert.setHeaderText("Allow " + APP_NAME + " to start automatically with Windows?");
        alert.setContentText("This will enable " + APP_NAME + " to launch in the background when your computer starts, allowing your 'Startup Launchers' to run automatically. You can disable this later from the app's settings.");

        // Apply custom CSS if available (assuming application.css has an "alert-dialog" style)
        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("application.css").toExternalForm());
            alert.getDialogPane().getStyleClass().add("alert-dialog");
        } catch (Exception e) {
            System.err.println("Could not load application.css for alert dialog: " + e.getMessage());
        }

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}