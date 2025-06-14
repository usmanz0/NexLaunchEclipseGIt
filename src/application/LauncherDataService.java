package application;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class LauncherDataService {

    private static final String DATA_FILE_NAME = "launchers_data.json";
    private static final String DATA_FILE_PATH = System.getProperty("user.home") + File.separator + DATA_FILE_NAME;

    /**
     * Saves a list of top-level LauncherItem objects to the JSON data file.
     * @param launchers The list of LauncherItem objects to save.
     * @throws IOException If an I/O error occurs during writing.
     */
    public void saveLaunchers(List<LauncherItem> launchers) throws IOException {
        try (FileWriter writer = new FileWriter(DATA_FILE_PATH)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(launchers, writer);
            System.out.println("LauncherDataService: Launchers data successfully saved to: " + DATA_FILE_PATH);
        }
    }

    /**
     * Loads a list of top-level LauncherItem objects from the JSON data file.
     * Returns an empty list if the file does not exist, is empty, or encounters a parsing error.
     * @return A list of loaded LauncherItem objects.
     * @throws IOException If an I/O error occurs during reading (other than file not found).
     * @throws com.google.gson.JsonParseException If the JSON content is malformed.
     */
    public List<LauncherItem> loadLaunchers() throws IOException {
        File dataFile = new File(DATA_FILE_PATH);
        if (!dataFile.exists() || dataFile.length() == 0) { // Check if file exists AND is not empty
            System.out.println("LauncherDataService: Data file not found or is empty at: " + DATA_FILE_PATH);
            return new ArrayList<>(); // Return empty list if file doesn't exist or is empty
        }

        try (FileReader reader = new FileReader(dataFile)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<LauncherItem>>(){}.getType();
            List<LauncherItem> loadedItems = gson.fromJson(reader, listType);
            if (loadedItems == null) {
                loadedItems = new ArrayList<>(); // Ensure not null even if JSON is empty/malformed
            }
            System.out.println("LauncherDataService: Successfully loaded " + loadedItems.size() + " top-level launcher items.");
            return loadedItems;
        }
    }

    /**
     * Deletes the data file if it exists.
     */
    public void deleteDataFile() {
        File dataFile = new File(DATA_FILE_PATH);
        if (dataFile.exists()) {
            if (dataFile.delete()) {
                System.out.println("LauncherDataService: Data file deleted.");
            } else {
                System.err.println("LauncherDataService: Failed to delete data file: " + DATA_FILE_PATH);
            }
        } else {
            System.out.println("LauncherDataService: No data file to delete.");
        }
    }
}