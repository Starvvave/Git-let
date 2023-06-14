package gitlet;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import static gitlet.Utils.*;
import static gitlet.Utils.plainFilenamesIn;

/** Represents a gitlet repository.
 *.gitlet/ -- top level folder for saved files and other related information
 *  - commits/ -- folder containing all commits
 *  - blobs/ -- file related to current commit
 */
public class Repository {
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");


    /** For init. */
    public static void setup() {
        if (GITLET_DIR.exists()) {
            message("A Gitlet version-control system already exists in the current directory.");
            return;
        } else {
            GITLET_DIR.mkdir();
            Commit.OBJECTS_DIR.mkdir();
            Commit.COMMITS_DIR.mkdir();
            Blob.BLOB_DIR.mkdir();
            Staging.STAGING_DIR.mkdir();
            Staging s = new Staging();
            s.saveStage();
            Heads.DEFAULTBRANCH_DIR.mkdir();
        }
        Commit initialCommit = new Commit();
        initialCommit.saveCommit();
        String initialID = initialCommit.getID();
        writeContents(join(Heads.DEFAULTBRANCH_DIR, "master"), initialID);
        Heads.changeBranch("master");
    }

    /** For Staging. */
    public static void staging(String fileName) {
        // case 1: the file has staged for remove.
        Staging s = Staging.getStage();
        if (s.getToRemoves().contains(fileName)) {
            s.getToRemoves().remove(fileName);
        // case 2: the file is identical to the version in the current commit.
        } else if (checkForCommit(fileName, Heads.getCurrent())) {
            if (checkForStaging(fileName)) {
                s.removeFromStaging(fileName);
            }
        } else {
            s.addStaging(fileName, stagingBlob(fileName));
        }
        s.saveStage();
    }

    /** get the staging file into a blob. */
    private static Blob stagingBlob(String fileName) {
        File f = new File(fileName);
        if (!f.exists()) {
            message("File does not exist.", f);
            System.exit(0);
        }
        return new Blob(fileName, readContentsAsString(f));
    }

    /**
     *  check if current file is identical to the version in the commit
     */
    private static boolean checkForCommit(String fileName, Commit c) {
        Blob toStage = stagingBlob(fileName);      // the blob to be added.
        String toStageID = toStage.getID();

        Map<String, String> m = c.getToBlobs();    // blob in the commits.
        return (m.containsValue(toStageID));
    }

    /** this method is used to check for adding,
     *  needs the file to be existed to check if the file is currently staged for addition.
     */
    private static boolean checkForStaging(String fileName) {
        // get ID from the blob to be staged.
        Blob toStage = stagingBlob(fileName);
        String toStageID = toStage.getID();

        // get ID from staging map with the same name
        Staging s = Staging.getStage();
        Map<String, String> stagingBlobs = s.getToBlobs();
        String blobId = stagingBlobs.get(fileName);
        return (toStageID.equals(blobId));
    }

    /** For Commit.
    /* Move staging files to the Commit File when use the commit command. */
    public static void makeCommit(String message, String branch) {
        Staging s = Staging.getStage();
        if (s.getToBlobs().isEmpty() && s.getToRemoves().isEmpty()) {
            message("No changes added to the commit.");
            System.exit(0);
        }

        Commit c = new Commit(message, branch);
        c.saveCommit();
        String commitID = c.getID();
        writeContents(join(Heads.DEFAULTBRANCH_DIR, Heads.currentBranch()), commitID);

        Staging.clear();
    }

    /** For remove. */
    public static void makeRemove(String fileName) {
        Staging s = Staging.getStage();
        Commit c = Heads.getCurrent();
        // case: file staged for addition.
        if (s.getToBlobs().containsKey(fileName)) {
            s.removeFromStaging(fileName);

        // case: file tracked for the current commit.
        } else if (c.getToBlobs().containsKey(fileName)) {
            List<String> removingBlobs = s.getToRemoves();
            removingBlobs.add(fileName);
            File f = new File(fileName);
            restrictedDelete(f);

        } else {
            message("No reason to remove the file.");
        }
        s.saveStage();
    }

    /** For log. */
    public static void makeLog() {
        Commit c = Heads.getCurrent();
        while (!c.getParent().isEmpty()) {
            printInfo(c);
            List<String> parent = c.getParent();
            c = Commit.getCommit(parent.get(0));
        }
        Commit initialCommit = new Commit();
        printInfo(initialCommit);
    }

    /** format the date presentation. */
    private static String formatDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z");
        formatter.setTimeZone(TimeZone.getDefault());
        return formatter.format(date);
    }

    /** print log information for specified commit. */
    private static void printInfo(Commit c) {
        System.out.println("===");
        System.out.println("commit " + c.getID());
        if (c.getParent().size() > 1) {
            String parent1 = c.getParent().get(0);
            String parent2 = c.getParent().get(1);
            System.out.println("Merge: " + parent1.substring(0, 7) + " "
                               + parent2.substring(0, 7));
        }
        System.out.println("Date: " + formatDate(c.getDate()));
        System.out.println(c.getMessage());
        System.out.println();
    }

    /** For global log. */
    public static void makeGlobalLog() {
        String[] commitFolders = Commit.COMMITS_DIR.list();
        for (String commitFolder: commitFolders) {
            List<String> commits = plainFilenamesIn(join((Commit.COMMITS_DIR), commitFolder));
            for (String commitID: commits) {
                Commit c = Commit.getCommit(commitID);
                printInfo(c);
            }
        }
    }

    /** For find. */
    public static void makeFind(String message) {
        int count = 0;                              // To find out if there are any prints.
        String[] commitFolders = Commit.COMMITS_DIR.list();
        for (String commitFolder: commitFolders) {
            List<String> commits = plainFilenamesIn(join((Commit.COMMITS_DIR), commitFolder));
            for (String commitID: commits) {
                Commit c = Commit.getCommit(commitID);
                String m = c.getMessage();
                if (m.equals(message)) {
                    System.out.println(commitID);
                    count += 1;
                }
            }
        }

        if (count == 0) {
            System.out.println("Found no commit with that message.");
        }
    }

    /** For status. */
    public static void makeStatus() {
        System.out.println("=== Branches ===");
        for (String branches: plainFilenamesIn(Heads.DEFAULTBRANCH_DIR)) {
            if (branches.equals(Heads.currentBranch())) {
                System.out.println("*" + branches);
            } else {
                System.out.println(branches);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        Staging s = Staging.getStage();
        for (String stagings: s.getToBlobs().keySet()) {
            System.out.println(stagings);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String removes: s.getToRemoves()) {
            System.out.println(removes);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    /** For checkout -- [file name] style. */
    public static void checkoutFile(String fileName) {
        String commitID = Heads.getCurrentID();
        checkoutCommit(commitID, fileName);
    }

    /** For checkout -- [commit id] -- [file name] style. */
    public static void checkoutCommit(String commitID, String fileName) {
        File f = join(Commit.COMMITS_DIR, commitID.substring(0, 2));
        if (!f.exists()) {
            message("No commit with that id exists.");
            return;
        }

        commitID = plainFilenamesIn(f).get(0);
        Commit c = Commit.getCommit(commitID);
        Map<String, String> m = c.getToBlobs();
        if (!m.containsKey(fileName)) {
            message("File does not exist in that commit.");
            return;
        }
        String blobID = m.get(fileName);
        Blob b = Blob.getBlob(blobID);
        String blobFile = b.getContents();
        File workingFile = new File(fileName);
        writeContents(workingFile, blobFile);
    }

    /** For checkout [branch name] style. */
    public static void checkoutBranch(String name) {
        //checkout Branch
        File branch = join(Heads.DEFAULTBRANCH_DIR, name);
        if (!branch.exists()) {
            message("No such branch exists.");
            return;
        }
        if (name.equals(Heads.currentBranch())) {
            message("No need to checkout the current branch.");
            return;
        }

        String commitID = readContentsAsString(branch);
        Commit checkout = Commit.getCommit(commitID);
        Map<String, String> checkoutMap = checkout.getToBlobs();
        Commit current = Heads.getCurrent();
        Map<String, String> currentMap = current.getToBlobs();

        // case: working files not tracked in the current branch will be modified in the checkout.
        checkUntracked(checkout);

        // case: put files in the checkout branch into the working directory.
        for (String fileName: checkoutMap.keySet()) {
            checkoutCommit(commitID, fileName);
        }

        // case: delete files tracked in the current branch but not in the checkout branch.
        for (String fileName: currentMap.keySet()) {
            if (!checkoutMap.containsKey(fileName)) {
                File f = new File(fileName);
                f.delete();
            }
        }

        Staging.clear();
        writeContents(join(Heads.DEFAULTBRANCH_DIR, name), commitID);
        Heads.changeBranch(name);
    }

    /** check for working files not tracked in the current branch will be modified. */
    private static void checkUntracked(Commit c) {
        List<String> workingFiles = plainFilenamesIn(CWD);
        if (workingFiles == null) {
            return;
        }
        Commit current = Heads.getCurrent();
        Map<String, String> currentMap = current.getToBlobs();
        Map<String, String> checkMap = c.getToBlobs();
        for (String workingFile: workingFiles) {
            // checks if the current branch track file & if the checkout contains working file name.
            if ((!currentMap.containsKey(workingFile)) && (checkMap.containsKey(workingFile))) {

                // check if the working file is the same as the checkout version
                // a.k.a whether the working file will be overwritten.
                if (!checkForCommit(workingFile, c)) {
                    message("There is an untracked file in the way; delete it, "
                            + "or add and commit it first.");
                    System.exit(0);
                }
            }
        }
    }

    /** For branch. */
    public static void makeBranch(String name) {
        File branch = join(Heads.DEFAULTBRANCH_DIR, name);
        if (branch.exists()) {
            message("A branch with that name already exists.");
            return;
        }
        String currentID = Heads.getCurrentID();
        writeContents(join(Heads.DEFAULTBRANCH_DIR, name), currentID);
    }

    /** For remove branch. */
    public static void removeBranch(String name) {
        File branch = join(Heads.DEFAULTBRANCH_DIR, name);
        if (!branch.exists()) {
            message("A branch with that name does not exist.");
            System.exit(0);
        }
        if (name.equals(Heads.currentBranch())) {
            message("Cannot remove the current branch.");
            System.exit(0);
        }
        branch.delete();
    }

    /** For reset. */
    public static void makeReset(String commitID) {
        File f = join(Commit.COMMITS_DIR, commitID.substring(0, 2));
        if (!f.exists()) {
            message("No commit with that id exists.");
            return;
        }
        commitID = plainFilenamesIn(f).get(0);
        Commit c = readObject(join(f, commitID), Commit.class);
        checkUntracked(c);
        Map<String, String> m = c.getToBlobs();
        for (String fileName: m.keySet()) {
            checkoutCommit(commitID, fileName);
        }
        Staging.clear();
        writeContents(join(Heads.DEFAULTBRANCH_DIR, Heads.currentBranch()), commitID);
    }

    /** For merge. */
    public static void merge(String branchName) {
        Staging s = Staging.getStage();
        if ((!s.getToBlobs().isEmpty()) || (!s.getToRemoves().isEmpty())) {
            message("You have uncommitted changes.");
            System.exit(0);
        }
        File branch = join(Heads.DEFAULTBRANCH_DIR, branchName);
        if (!branch.exists()) {
            message("A branch with that name does not exist.");
            return;
        }
        if (branchName.equals(Heads.currentBranch())) {
            message("Cannot merge a branch with itself.");
            return;
        }

        String givenBranchID = Heads.getBranchID(branchName);
        String splitPointID = splitPoint(branchName);
        String currentBranchID = Heads.getCurrentID();
        if (splitPointID.equals(givenBranchID)) {
            message("Given branch is an ancestor of the current branch.");
            return;
        }
        if (splitPointID.equals(currentBranchID)) {
            checkoutBranch(branchName);
            message("Current branch fast-forwarded.");
            return;
        }

        // to get all the files in three points.
        Commit givenBranch = Commit.getCommit(givenBranchID);
        checkUntracked(givenBranch);
        Commit splitPoint = Commit.getCommit(splitPointID);
        Commit currentBranch = Heads.getCurrent();
        Map<String, String> givenBlobs = givenBranch.getToBlobs();
        Map<String, String> splitBlobs = splitPoint.getToBlobs();
        Map<String, String> currentBlobs = currentBranch.getToBlobs();
        Set<String> modifies = new HashSet<>();
        modifies.addAll(givenBlobs.keySet());
        modifies.addAll(currentBlobs.keySet());

        for (String modifiedGiven: modifies) {
            String givenVersion = givenBlobs.get(modifiedGiven);
            String splitVersion = splitBlobs.get(modifiedGiven);
            String currentVersion = currentBlobs.get(modifiedGiven);

            //case 5: files present only in the given branch
            if (splitVersion == null && currentVersion == null) {
                checkoutCommit(givenBranchID, modifiedGiven);
                staging(modifiedGiven);


            } else if (Objects.equals(splitVersion, currentVersion)) {
                //case 6: files unmodified in the current branch, absent in the given branch
                if (givenVersion == null) {
                    makeRemove(modifiedGiven);

                //case 1: files in given branch modified but current branch unmodified.
                } else if (!currentVersion.equals(givenVersion)) {
                    checkoutCommit(givenBranchID, modifiedGiven);
                    staging(modifiedGiven);
                }

                //case 8: files modified differently in current branch
            } else if (!Objects.equals(givenVersion, currentVersion)) {
                // control for cases 2, 4, 7, like files only modified in current branch.
                if (!Objects.equals(splitVersion, givenVersion)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("<<<<<<< HEAD\n");
                    if (currentVersion != null) {
                        Blob currentBlob = Blob.getBlob(currentVersion);
                        String currentContents = currentBlob.getContents();
                        sb.append(currentContents);
                    }
                    sb.append("=======\n");
                    if (givenVersion != null) {
                        Blob givenBlob = Blob.getBlob(givenVersion);
                        String givenContents = givenBlob.getContents();
                        sb.append(givenContents);
                    }
                    sb.append(">>>>>>>\n");
                    writeContents(join(CWD, modifiedGiven), sb.toString());
                    message("Encountered a merge conflict.");
                    staging(modifiedGiven);
                }
            }
        }
        makeCommit("Merged " + branchName + " into " + Heads.currentBranch() + ".", branchName);
    }

    private static String splitPoint(String branchName) {
        Queue<String> fringe = new LinkedList<>();
        fringe.add(Heads.getCurrent().getID());
        Set<String> commits = new HashSet<>();
        while (!fringe.isEmpty()) {
            String currentBranchID = fringe.remove();
            Commit currentBranch = Commit.getCommit(currentBranchID);
            List<String> parents = currentBranch.getParent();
            commits.add(currentBranchID);
            if (!parents.isEmpty()) {
                for (String parent: parents) {
                    fringe.add(parent);
                }
            }
        }

        Queue<String> branchCommits = new LinkedList<>();
        branchCommits.add(readContentsAsString(join(Heads.DEFAULTBRANCH_DIR, branchName)));
        while (!branchCommits.isEmpty()) {
            String givenBranchID = branchCommits.remove();
            if (commits.contains(givenBranchID)) {
                return givenBranchID;
            }
            Commit givenBranch = Commit.getCommit(givenBranchID);
            List<String> parents = givenBranch.getParent();
            if (!parents.isEmpty()) {
                for (String parent: parents) {
                    branchCommits.add(parent);
                }
            }
        }
        return null;
    }
}





