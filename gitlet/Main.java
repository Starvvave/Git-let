package gitlet;

import static gitlet.Utils.*;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        if (args.length == 0) {
            message("Please enter a command.");
            return;
        }

        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                // TODO: handle the `init` command
                validateNumArgs("init", args, 1);
                Repository.setup();
                break;
            case "add":
                // TODO: handle the `add [filename]` command
                validateNumArgs("add", args, 2);
                validateRepository();
                Repository.staging(args[1]);
                break;
            case "commit":
                validateNumArgs("commit", args, 2);
                validateRepository();
                String message = args[1];
                if (message.equals("")) {
                    message("Please enter a commit message.");
                }
                Repository.makeCommit(message, "");
                break;
            case "rm":
                validateNumArgs("rm", args, 2);
                validateRepository();
                Repository.makeRemove(args[1]);
                break;
            case "log":
                validateNumArgs("log", args, 1);
                validateRepository();
                Repository.makeLog();
                break;
            case "global-log":
                validateNumArgs("global-log", args, 1);
                validateRepository();
                Repository.makeGlobalLog();
                break;
            case "find":
                validateNumArgs("find", args, 2);
                validateRepository();
                Repository.makeFind(args[1]);
                break;
            case "status":
                validateNumArgs("status", args, 1);
                validateRepository();
                Repository.makeStatus();
                break;
            case "checkout":
                validateRepository();
                if (args.length == 2) {
                    Repository.checkoutBranch(args[1]);
                } else if (args.length == 3) {
                    if (!args[1].equals("--")) {
                        message("Incorrect operands");
                        System.exit(0);
                    }
                    Repository.checkoutFile(args[2]);
                } else if (args.length == 4) {
                    if (!args[2].equals("--")) {
                        message("Incorrect operands");
                        System.exit(0);
                    }
                    Repository.checkoutCommit(args[1], args[3]);
                } else {
                    message("Incorrect operands");
                    System.exit(0);
                }
                break;
            case "branch":
                validateNumArgs("branch", args, 2);
                validateRepository();
                Repository.makeBranch(args[1]);
                break;
            case "rm-branch":
                validateNumArgs("rm-branch", args, 2);
                validateRepository();
                Repository.removeBranch(args[1]);
                break;
            case "reset":
                validateNumArgs("reset", args, 2);
                validateRepository();
                Repository.makeReset(args[1]);
                break;
            case "merge":
                validateNumArgs("reset", args, 2);
                validateRepository();
                Repository.merge(args[1]);
                break;
            default:
                message("No command with that name exists.");
                System.exit(0);
        }
    }

    /** check for the number of args. */
    public static void validateNumArgs (String cmd, String[] args, int n) {
        if (args.length != n) {
            message("Incorrect operands", cmd);
            System.exit(0);
        }
    }

    /** check if the user is in an initialized Gitlet working directory. */
    public static void validateRepository() {
        if (!Repository.GITLET_DIR.exists()) {
            message("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
}
