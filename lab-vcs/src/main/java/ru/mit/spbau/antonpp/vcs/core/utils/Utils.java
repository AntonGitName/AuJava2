package ru.mit.spbau.antonpp.vcs.core.utils;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
public class Utils {

    private Utils() {
    }

    public static List<Path> listFilesRecursively(Path dir, List<Path> exclusions) throws IOException {
        final List<Path> result = new ArrayList<>();
        SimpleFileVisitor<Path> fileVisitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (exclusions.contains(file)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                if (attrs.isRegularFile() && !attrs.isDirectory()) {
                    result.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        };
        java.nio.file.Files.walkFileTree(dir, fileVisitor);
        return result;
    }

    public static HashCode getHashForFile(Path root, Path path) throws IOException {
        final String fileHash = Files.hash(path.toFile(), Hashing.md5()).toString();
        final String pathHash = Hashing.md5().hashString(root.relativize(path).toString()).toString();
        return Hashing.md5().hashString(fileHash + pathHash);
    }

    public static String getFileContent(Path path) throws IOException {
        return new String(java.nio.file.Files.readAllBytes(path));
    }

    public static Path getInternals(Path workingDir) {
        return workingDir.resolveSibling(Constants.INTERNALS);
    }

    public static Path getHeadHashFile(Path workingDir) {
        return getInternals(workingDir).resolveSibling(Constants.HEAD);
    }

    public static Path getLogFile(Path workingDir) {
        return getInternals(workingDir).resolveSibling(Constants.LOGS);
    }

    public static Path getRevisionsDir(Path workingDir) {
        return getInternals(workingDir).resolveSibling(Constants.REVISIONS);
    }

    public static Path getRevisionDir(Path workingDir, String hash) {
        return getRevisionsDir(workingDir).resolveSibling(hash);
    }

    public static Path getRevisionParents(Path workingDir, String hash) {
        return getRevisionDir(workingDir, hash).resolveSibling(Constants.PATH_REV_PARENTS);
    }

    public static Path getRevisionIndex(Path workingDir, String hash) {
        return getRevisionDir(workingDir, hash).resolveSibling(Constants.PATH_REV_INDEX);
    }

    public static Path getRevisionFiles(Path workingDir, String hash) {
        return getRevisionDir(workingDir, hash).resolveSibling(Constants.PATH_REV_FILES);
    }

    public static Path getStageDir(Path workingDir) {
        return getInternals(workingDir).resolveSibling(Constants.PATH_STAGE);
    }

    public static Path getStageIndex(Path workingDir) {
        return getStageDir(workingDir).resolveSibling(Constants.PATH_STAGE_INDEX);
    }

    public static Path getStageFiles(Path workingDir) {
        return getStageDir(workingDir).resolveSibling(Constants.PATH_STAGE_FILES);
    }
}
