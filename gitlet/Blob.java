package gitlet;

import java.io.File;
import java.io.Serializable;
import static gitlet.Utils.*;

public class Blob implements Serializable {
    /** The name of the file. */
    private final String name;

    /** The contents of the file. */
    private final String contents;

    /** SHA-1 for the blob. */
    private final String ID;

    /** Folder stores blobs. */
    public static final File BLOB_DIR = join(Commit.OBJECTS_DIR, "blobs");

    /** Initiate a blob to be added. */
    public Blob(String name, String contents) {
        this.name = name;
        this.contents = contents;
        this.ID = sha1(this.name, this.contents);
    }

    /** get SHA-1 of the blob. */
    public String getID() {
        return ID;
    }

    /** get name of the Blob. */
    public String getName() {
        return name;
    }

    /** get contents of the Blob. */
    public String getContents() {
        return contents;
    }

    /**
     * Saves a blob in the specified directory for future use.
     */
    public void saveBlob() {
        File blobFolder = join(BLOB_DIR, ID.substring(0, 2));
        if (!blobFolder.exists()) {
            blobFolder.mkdir();
        }
        writeObject(join(blobFolder, ID), this);
    }

    /**
     * Reads in and deserializes a blob from specified directory with the blobID.
     */
    public static Blob getBlob(String blobID) {
        File inFile = join(BLOB_DIR, blobID.substring(0, 2));
        return readObject(join(inFile, blobID), Blob.class);
    }
}
