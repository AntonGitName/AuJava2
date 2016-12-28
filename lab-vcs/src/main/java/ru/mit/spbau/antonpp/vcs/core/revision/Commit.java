package ru.mit.spbau.antonpp.vcs.core.revision;

import com.google.common.hash.Hashing;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Set;

/**
 * This class represents revisions name commits. A few things you about commits:
 * <ul>
 *     <li> All commits (possibly except first one) are always created in {@link Stage} class</li>
 *     <li> Any commit should be deserialized only once and it is never updated </li>
 *     <li> Commits hold index about all files they have but files are stored compressed and are spread between commits</li>
 * </ul>
 *
 * @author Anton Mordberg
 * @since 26.10.16
 */
@Slf4j
public final class Commit extends AbstractCommit {

    private static final Random RND = new Random(System.currentTimeMillis());

    private int seed;

    @Nullable
    @Getter
    @Setter
    private String branch;

    @Getter @Setter
    private String filesHash;
    @Getter(lazy = true)
    @NotNull
    private final String revHash = calcRevHash();

    public Commit() {
    }

    private static String getCommitHash(@Nullable String branch, String filesHash, int seed) {
        return Hashing.md5().hashString(branch + filesHash + seed).toString();
    }


    void generateSeed() {
        if (seed != 0) {
            throw new IllegalStateException("generateSeed must be called only once.");
        }
        seed = 1 + RND.nextInt();
    }

    @Override
    public void deserialize(Path path) throws SerializationException {
        try (ObjectInputStream os = new ObjectInputStream(new FileInputStream(path.toFile()))) {
            branch = (String) os.readObject();
            filesHash = (String) os.readObject();
            seed = os.readInt();
            root = Paths.get((String) os.readObject());
            parents = (Set<String>) os.readObject();
            index = Utils.deserializeMapWithPath(os, Path.class, Path.class);
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Could not deserialize revision", e);
        }
    }

    @Override
    public void serialize(Path path) throws SerializationException {
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(path.toFile()))) {
            os.writeObject(branch);
            os.writeObject(filesHash);
            os.writeInt(seed);
            os.writeObject(root.toString());
            os.writeObject(parents);
            Utils.serializeMapWithPath(index, os, Path.class, Path.class);
        } catch (IOException e) {
            throw new SerializationException("Could not serialize revision", e);
        }
    }

    /**
     * Calculates revision hash as described in {@link Revision#getRevHash()}.
     *
     * @return revision hash.
     */
    @NotNull
    private String calcRevHash() {
        return getCommitHash(branch, filesHash, seed);
    }
}
