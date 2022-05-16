package gitlet;
import java.io.Serializable;
import java.util.Date;
import java.util.TreeMap;
import java.util.Set;
import java.text.SimpleDateFormat;

public class Commit implements Serializable {
    public Commit(String parent1, String parent2, String message) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE "
                + "MMM d HH:mm:ss yyyy Z");
        _message = message;
        _parent1 = parent1;
        _parent2 = parent2;
        if (parent1 == null) {
            timestamp = dateFormat.format(new Date(0));
        } else {
            timestamp = dateFormat.format(new Date());
        }
        commits = new TreeMap<>();
        this.commitID = Utils.sha1(Utils.serialize(this));
    }

    /**
     * Gets parent 1.
     * @return
     */
    public String getParent1() {
        return _parent1;
    }

    /**
     * Gets parent 2.
     * @return
     */
    public String getParent2() {
        return _parent2;
    }

    /**
     * Gets message.
     * @return
     */
    public String getMessage() {
        return _message;
    }

    /**
     * Gets timestamp.
     * @return
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Gets commitID.
     * @return
     */
    public String getCommitID() {
        return commitID;
    }

    /**
     * Adds file to commits.
     * @param fileName
     * @param blobID
     */
    public void setFile(String fileName, String blobID) {
        commits.put(fileName, blobID);
    }

    /**
     * Initializes committed files.
     * @param commitsFiles
     */
    public void setFiles(TreeMap<String, String> commitsFiles) {
        this.commits = commitsFiles;
    }

    /**
     * Gets blobID.
     * @param fileName
     * @return
     */
    public String getBlob(String fileName) {
        if (!commits.containsKey(fileName)) {
            return "";
        }
        return commits.get(fileName);
    }

    /**
     * Returns committed files.
     * @return
     */
    public TreeMap<String, String> getBlobSet() {
        return commits;
    }

    /**
     * Returns fileNames.
     * @return
     */
    public Set<String> getFileName() {
        return commits.keySet();
    }

    /** Time. */
    private String timestamp;

    /** Message. */
    private String _message;

    /** Gets parent 1. */
    private String _parent1;

    /** Gets parent 2. */
    private String _parent2;

    /** Gets commits. */
    private TreeMap<String, String> commits;

    /** Gets commitID. */
    private String commitID;

}
