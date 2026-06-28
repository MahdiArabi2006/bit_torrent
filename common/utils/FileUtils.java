package common.utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FileUtils {

    public static Map<String, String> listFilesInFolder(String folderPath) {
        Map<String, String> fileHashMap = new HashMap<>();
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            return fileHashMap;
        }

        File[] files = folder.listFiles();
        if (files == null) return fileHashMap;

        for (File file : files) {
            if (file.isFile()) {
                String hash = MD5Hash.HashFile(file.getPath());
                fileHashMap.put(file.getName(), hash);
            }
        }

        return fileHashMap;
    }
}
