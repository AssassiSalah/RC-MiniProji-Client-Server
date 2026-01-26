package utils;

import java.io.*;
import java.nio.file.*;

public class FileUtils {

    // Upload a file to the server
    public static byte[] readFile(String localFilePath) throws IOException {
        Path path = Paths.get(localFilePath);
        return Files.readAllBytes(path);
    }

    // Save a downloaded file locally
    public static void saveFile(String localFilePath, byte[] content) throws IOException {
        Path path = Paths.get(localFilePath);
        Files.createDirectories(path.getParent());
        Files.write(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // List files in a local directory
    public static String[] listFiles(String directoryPath) throws IOException {
        return Files.list(Paths.get(directoryPath))
                .map(path -> path.getFileName().toString())
                .toArray(String[]::new);
    }
}
