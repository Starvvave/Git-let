package gitlet;

import java.io.File;
import static gitlet.Utils.*;

/** a class represents a file which contains the current commit in different branches. */
public class Heads {
    /** Create Heads folder to store different branch.
     *  Each branch name represents its current commit.
     */
    public static final File DEFAULTBRANCH_DIR = join(".gitlet", "refs");

    /** Creates Head file stores the current branch. */
    private static final File HEAD = join(".gitlet", "head");

    /** When a new commit happens, heads should be modified:
     *  1. write the commit ID into the head
     *  2. write the commit ID into the ref folder named by current branch
     */
    public static void changeBranch(String branch) {
        writeContents(HEAD, branch);
    }

    /** get the current commit. */
    public static Commit getCurrent() {
        String commitID = getCurrentID();
        return Commit.getCommit(commitID);
    }

    /** get the current commit ID. */
    public static String getCurrentID() {
        return getBranchID(currentBranch());
    }

    /** get the branch commit ID. */
    public static String getBranchID(String branch) {
        File f = join(DEFAULTBRANCH_DIR, branch);
        return readContentsAsString(f);
    }

    /** get the current branch. */
    public static String currentBranch() {
        return readContentsAsString(HEAD);
    }
}
