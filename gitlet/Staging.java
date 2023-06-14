package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import static gitlet.Utils.*;

public class Staging implements Serializable {
    /** tracking file. */
    private Map<String, String> toBlobs;

    /** For remove tracking. */
    private List<String> removeBlobs;

    /** The staging area. */
    public static final File STAGING_DIR = join(".gitlet", "staging");

    public Staging() {
        toBlobs = new TreeMap<>();
        removeBlobs = new LinkedList<>();
    }

    public void saveStage() {
        writeObject(join(STAGING_DIR, "map"), this);
    }

    public static Staging getStage() {
        return readObject(join(STAGING_DIR, "map"), Staging.class);
    }

    /**
     * 1. add relevant name - ID pair to the map.
     * 2. save the blob.
     */
    public void addStaging(String fileName, Blob b) {
        getToBlobs().put(fileName, b.getID());
        b.saveBlob();
    }

    /** remove relevant name - ID pair from the adding map. */
    public void removeFromStaging(String fileName) {
        getToBlobs().remove(fileName);
    }

    public Map<String, String> getToBlobs() {
        return toBlobs;
    }

    public List<String> getToRemoves() {
        return removeBlobs;
    }

    public static void clear() {
        Staging s = getStage();
        s.toBlobs = new TreeMap<>();
        s.removeBlobs = new LinkedList<>();
        s.saveStage();
    }
}

