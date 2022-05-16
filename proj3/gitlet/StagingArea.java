package gitlet;

import java.io.Serializable;
import java.util.TreeMap;
import java.util.TreeSet;

public class StagingArea implements Serializable {
    public StagingArea() {
        stagedFile = new TreeMap<>();
        delete = new TreeSet<>();
    }

    /**
     * Adds a file into staging.
     * @param fileName
     * @param blobID
     */
    public void add(String fileName, String blobID) {
        stagedFile.put(fileName, blobID);
    }

    /**
     * Gets files stored for removal.
     * @return
     */
    public TreeSet<String> getDelete() {
        return delete;
    }

    /**
     *  Gets Files stored for addition.
     * @return
     */
    public TreeMap<String, String> getStagedFile() {
        return stagedFile;
    }

    /** Files stored for addition. */
    private TreeMap<String, String> stagedFile;

    /** Files stored for removal. */
    private TreeSet<String> delete;
}
