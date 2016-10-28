package ru.mit.spbau.antonpp.vcs.core.utils;

import com.google.common.hash.Hashing;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
public class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    private Utils() {
    }

    public static String getCurrentTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static List<Path> listFilesRecursivelyExceptInternls(Path root) {
        return listFilesRecursively(root, Collections.singletonList(getInternals(root)));
    }

    public static List<Path> listFilesRecursively(Path dir, List<Path> exclusions) {
        final List<Path> result = new ArrayList<>();
        SimpleFileVisitor<Path> fileVisitor = new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (exclusions.contains(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile() && !attrs.isDirectory()) {
                    result.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        };
        try {
            java.nio.file.Files.walkFileTree(dir, fileVisitor);
        } catch (IOException e) {
            throw new IllegalStateException("Visitor throw an exception (impossible).");
        }
        return result;
    }

    public static String getFileHash(Path root, Path path) throws IOException {
        final String fileHash = com.google.common.io.Files.hash(path.toFile(), Hashing.md5()).toString();
        final String pathHash = Hashing.md5().hashString(root.relativize(path).toString()).toString();
        return Hashing.md5().hashString(fileHash + pathHash).toString();
    }

    public static String getFileContent(Path path) throws IOException {
        return new String(java.nio.file.Files.readAllBytes(path));
    }

    public static Path getCurrentDir() {
        return Paths.get(System.getProperty("user.dir"));
    }

    public static @Nullable Path getRoot() {
        Path currentPath = getCurrentDir();
        while (currentPath != null && !java.nio.file.Files.exists(getInternals(currentPath))) {
            currentPath = currentPath.getParent();
        }
        return currentPath;
    }

    public static Path getInternals(Path root) {
        return root.resolve(Constants.GLOBAL_INTERNALS);
    }

    public static Path getRepository(Path root) {
        return getInternals(root).resolve(Constants.GLOBAL_REPOSITORY);
    }

    public static Path getLogFile(Path root) {
        return getInternals(root).resolve(Constants.GLOBAL_LOGS);
    }

    public static Path getRevisionsDir(Path root) {
        return getInternals(root).resolve(Constants.GLOBAL_REVISIONS);
    }

    public static Path getRevisionDir(Path root, String hash) {
        return getRevisionsDir(root).resolve(hash);
    }

    public static Path getRevisionParents(Path root, String hash) {
        return getRevisionDir(root, hash).resolve(Constants.REV_PARENTS);
    }

    public static Path getRevisionIndex(Path root, String hash) {
        return getRevisionDir(root, hash).resolve(Constants.REV_INDEX);
    }

    public static Path getRevisionFiles(Path root, String hash) {
        return getRevisionDir(root, hash).resolve(Constants.REV_FILES);
    }

    public static Path getStageDir(Path root) {
        return getInternals(root).resolve(Constants.STAGE);
    }

    public static Path getStageIndex(Path root) {
        return getStageDir(root).resolve(Constants.STAGE_INDEX);
    }

    public static Path getStageFiles(Path root) {
        return getStageDir(root).resolve(Constants.STAGE_FILES);
    }

    public static Path getBranchesFile(Path root) {
        return getInternals(root).resolve(Constants.GLOBAL_BRANCHES);
    }

    public static Map<String, String> readBranches(Path root) throws IOException {
        final Map<String, String> branchesMap = new HashMap<>();
        final List<String> lines = java.nio.file.Files.readAllLines(Utils.getBranchesFile(root));
        for (final String line : lines) {
            final String[] splittedLine = line.split(" ");
            branchesMap.put(splittedLine[0], splittedLine[1]);
        }
        return branchesMap;
    }

    public static void writeBranches(Map<String, String> branchesMap, Path root) throws FileNotFoundException {
        try (final PrintWriter out = new PrintWriter(Utils.getBranchesFile(root).toFile())) {
            for (final Map.Entry<String, String> kv : branchesMap.entrySet()) {
                out.printf("%s %s\n", kv.getKey(), kv.getValue());
            }
        }
    }

    public static void moveToDir(Path file, Path dir) throws IOException {
        java.nio.file.Files.move(file, dir.resolve(file.getFileName()));
    }

    public static void copyToDir(Path file, Path dir) throws IOException {
        java.nio.file.Files.copy(file, dir.resolve(file.getFileName()));
    }

    public static List<Path> findRevisionByHash(Path root, String prefix) throws IOException {
        final List<Path> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(getRevisionsDir(root), prefix + "*")) {
            stream.forEach(result::add);
        }
        return result;
    }

    public static void serializePath(Path path, ObjectOutputStream os) throws IOException {
        os.writeObject(path.toString());
    }

    public static Path deserializePath(ObjectInputStream os) throws IOException, ClassNotFoundException {
        return Paths.get((String) os.readObject());
    }

    public static void serializeListPaths(List<Path> paths, ObjectOutputStream os) throws IOException {
        final List<String> strings = paths.stream().map(Path::toString).collect(Collectors.toList());
        os.writeObject(strings);
    }

    public static List<Path> deserializeListPaths(ObjectInputStream os) throws IOException, ClassNotFoundException {
        final List<String> strings = (List<String>) os.readObject();
        return strings.stream().map(Paths::get).collect(Collectors.toList());
    }

    // Yeah, best approach evah
    public static <T, V> void serializeMapWithPath(Map<T, V> map, ObjectOutputStream os, Class k, Class v) throws IOException {
//        LOGGER.debug("map before serialization: {}", map);
        final ObjectToStringMapper<Object> mK = new ObjectToStringMapper<>(k);
        final ObjectToStringMapper<Object> mV = new ObjectToStringMapper<>(v);
        final Function<Map.Entry, String> mapperK = x -> mK.apply(x.getKey());
        final Function<Map.Entry, String> mapperV = x -> mV.apply(x.getValue());
        os.writeObject(map.entrySet().stream().collect(Collectors.toMap(mapperK, mapperV)));
    }


    public static <K, V> Map<K, V> deserializeMapWithPath(ObjectInputStream os, Class k, Class v) throws IOException, ClassNotFoundException {
        final StringToObjectMapper<K> mK = new StringToObjectMapper<>(k);
        final StringToObjectMapper<V> mV = new StringToObjectMapper<>(v);
        final Function<Map.Entry<String, String>, K> mapperK = x -> mK.apply(x.getKey());
        final Function<Map.Entry<String, String>, V> mapperV = x -> mV.apply(x.getValue());
        final Map<String, String> map = (Map<String, String>) os.readObject();
        final Map<K, V> result = map.entrySet().stream().collect(Collectors.toMap(mapperK, mapperV));
//        LOGGER.debug("deserialized map: {}", result);
        return result;
    }

    private static class ObjectToStringMapper<T> implements Function<T, String> {

        private final Class type;

        private ObjectToStringMapper(Class type) {
            this.type = type;
        }

        @Override
        public String apply(T t) {
            return type.equals(String.class) ? (String) t : t.toString();
        }
    }

    private static class StringToObjectMapper<T> implements Function<String, T> {

        private final Class type;

        private StringToObjectMapper(Class type) {
            this.type = type;
        }

        @Override
        public T apply(String s) {
            return type.equals(String.class) ? (T) s : (T) Paths.get(s);
        }
    }
}
