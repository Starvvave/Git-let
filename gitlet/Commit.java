package gitlet;

import java.io.*;
import java.util.*;
import static gitlet.Utils.*;

public class Commit implements Serializable {
    /** The message of this Commit. */
    private final String message;

    /** Date of this Commit. */
    private final Date date;

    /** reference to parent commits. */
    private final List<String> parent;

    /** a map mapping names to references to blobs. */
    private Map<String, String> toBlobs = new TreeMap<>();

    /** SHA-1 of the commit. */
    private final String ID;

    /** Folder stores blobs and commits. */
    public static final File OBJECTS_DIR = join(".gitlet", "objects");

    /** Folder stores commits. */
    public static final File COMMITS_DIR = join(OBJECTS_DIR, "commits");

    /** constructor */
    public Commit(String message, String branch) {
        this.date = new Date();
        this.message = message;
        this.parent = new LinkedList<>();
        parent.add(Heads.getCurrentID());
        if (!branch.equals("")) {
            parent.add(Heads.getBranchID(branch));
        }
        this.toBlobs = modifyBlobs();
        this.ID = sha1(this.message, this.date.toString(), this.toBlobs.toString(),
                this.parent.toString());
    }

    /** initial commit. */
    public Commit() {
        this.date = new Date(0);
        this.message = "initial commit";
        this.parent = new LinkedList<>();
        this.ID = sha1(this.message, this.date.toString(), this.toBlobs.toString(),
                this.parent.toString());
    }

    /** get SHA-1 for this commit. */
    public final String getID() {
        return ID;
    }

    /** get the date of the current commit. */
    public Date getDate() {
        return date;
    }

    /** get the message of the current commit. */
    public String getMessage() {
        return message;
    }

    /** get the parent of the current commit. */
    public List<String> getParent() {
        return parent;
    }

    /** save the current commit. */
    public void saveCommit() {
        File commitFolder = join(COMMITS_DIR, ID.substring(0, 2));
        if (!commitFolder.exists()) {
            commitFolder.mkdir();
        }
        writeObject(join(commitFolder, ID), this);
    }

    /** get the specified commit using commit ID. */
    public static Commit getCommit(String commitID) {
        File inFile = join(COMMITS_DIR, commitID.substring(0, 2));
        return readObject(join(inFile, commitID), Commit.class);
    }

    /** get the map of name to reference to blobs. */
    public Map<String, String> getToBlobs() {
        return this.toBlobs;
    }

    /** Modify the map of name to reference to blobs in the current commit. */
    public static Map<String, String> modifyBlobs() {
        Commit parent = Heads.getCurrent();
        Staging s = Staging.getStage();
        Map<String, String> newBlobs = parent.getToBlobs();
        Map<String, String> stagingBlobs = s.getToBlobs();
        List<String> removingBlobs = s.getToRemoves();
        if (stagingBlobs != null) {
            for (String key: stagingBlobs.keySet()) {
                newBlobs.put(key, stagingBlobs.get(key));
            }
        }
        if (removingBlobs != null) {
            for (String key: removingBlobs) {
                newBlobs.remove(key);
            }
        }
        return newBlobs;
    }
}
