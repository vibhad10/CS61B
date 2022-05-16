package gitlet;
import java.io.IOException;


/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author V. Dabholkar
 */
public class Main {
    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        Repo repo = new Repo();
        if (args.length == 0) {
            System.out.println("Please enter a command.");
        } else {
            switch (args[0]) {
            case "init":
                repo.init();
                break;
            case "add":
                repo.add(args[1]);
                break;
            case "commit":
                if (correctInput(args, 2)) {
                    if (args[1].equals("")) {
                        System.out.println("Please enter a commit message.");
                        return;
                    }
                    repo.commit(args[1]);
                }
                break;
            case "rm":
                repo.rm(args[1]);
                break;
            case "log":
                repo.log();
                break;
            case "global-log":
                repo.globalLog();
                break;
            case "find":
                repo.find(args[1]);
                break;
            case "status":
                repo.status();
                break;
            case "checkout":
                repo.checkout(args);
                break;
            case "branch":
                repo.branch(args[1]);
                break;
            case "rm-branch":
                repo.rmBranch(args[1]);
                break;
            case "reset":
                repo.reset(args[1]);
                break;
            case "merge":
                repo.merge(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
                break;
            }
        }
    }

    public static boolean correctInput(String[] args, int num2) {
        if (args.length == num2) {
            return true;
        }
        return false;
    }

}
