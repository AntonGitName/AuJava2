package ru.mit.spbau.antonpp.vcs.core.utils;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
public class Utils {

    private Utils() {
    }

    public static List<String> listFilesRecursively(String dir, List<String> exclusions) {
        final File directory = new File(dir);
        final List<String> result = new ArrayList<>();

        // get all the files from a directory
        final File[] fList = directory.listFiles();
        if (fList == null) {
            return result;
        }
        for (File file : fList) {
            if (exclusions.contains(file.getName()))
                if (file.isFile()) {
                    result.add(file.getAbsolutePath());
                } else if (file.isDirectory()) {
                    listFilesRecursively(file.getAbsolutePath(), exclusions);
                }
        }
        return result;
    }

    public static HashCode getHashForFile(String path) throws IOException {
        final File file = new File(path);
        return Files.hash(file, Hashing.md5());
    }
}
