package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Command Center.
 *
 * @author V. Dabholkar
 */

public class Repo {

    public void init() throws IOException {
        if (gitlet.isDirectory()) {
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
        } else {
            gitlet.mkdir();
            branch.mkdir();
            commits.mkdir();
            blobs.mkdir();

            StagingArea stagingArea = new StagingArea();
            stage.createNewFile();
            Utils.writeObject(stage, stagingArea);

            branchName = Utils.join(gitlet, "branchName");
            branchName.createNewFile();
            _branch = Utils.readContentsAsString(branchName);
            Utils.writeContents(branchName, "master");
            _branch = "master";
            File mBranch = Utils.join(branch, "master");
            mBranch.createNewFile();

            Commit initial = new Commit(null, null, "initial commit");
            String commitID = initial.getCommitID();
            Utils.writeObject(Utils.join(commits, commitID), initial);
            Utils.writeContents(mBranch, commitID);
        }
    }

    public void add(String fileName) throws IOException {
        branchName = Utils.join(gitlet, "branchName");
        _branch = Utils.readContentsAsString(branchName);

        File addition = Utils.join(cWD, fileName);

        if (!addition.exists()) {
            System.out.println("File does not exist");
            return;
        }

        File branchFile = Utils.join(branch, _branch);
        String commitID = Utils.readContentsAsString(branchFile);
        File commitFile = Utils.join(commits, commitID);
        Commit commit = Utils.readObject(commitFile, Commit.class);

        String addedContent = Utils.readContentsAsString(addition);

        String blobID = Utils.sha1(addedContent);
        StagingArea stageObject = Utils.readObject(stage, StagingArea.class);

        stageObject.getDelete().remove(fileName);

        if (commit.getFileName().contains(fileName)) {
            if (blobID.equals(commit.getBlob(fileName))) {
                Utils.writeObject(stage, stageObject);
                return;
            }
        }

        stageObject.add(fileName, blobID);
        Utils.writeObject(stage, stageObject);

        if (stageObject.getDelete().contains(fileName)) {
            stageObject.getDelete().remove(fileName);
            Utils.writeObject(stage, stageObject);
            return;
        }

        File blobFile = Utils.join(blobs, blobID);
        blobFile.createNewFile();
        Utils.writeContents(blobFile, addedContent);
    }

    public void commit(String message) throws IOException {
        branchName = Utils.join(gitlet, "branchName");
        _branch = Utils.readContentsAsString(branchName);

        String branchFile = Utils.readContentsAsString(branchName);
        File bNameFile = Utils.join(branch, branchFile);
        String bNameID = Utils.readContentsAsString(bNameFile);
        File commitFile = Utils.join(commits, bNameID);
        Commit parentCommit = Utils.readObject(commitFile, Commit.class);

        StagingArea objStage = Utils.readObject(stage, StagingArea.class);

        if (objStage.getStagedFile().isEmpty()
                && objStage.getDelete().isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }

        Commit commit = new Commit(parentCommit.getCommitID(), null, message);
        commit.setFiles(objStage.getStagedFile());

        for (String fileName : parentCommit.getFileName()) {
            if (!objStage.getStagedFile().containsKey(fileName)
                    && !objStage.getDelete().contains(fileName)) {
                commit.setFile(fileName, parentCommit.getBlob(fileName));
            }
        }
        Set<String> clone = new HashSet<>();
        for (String fileName : objStage.getDelete()) {
            clone.add(fileName);
        }
        for (String file : clone) {
            objStage.getDelete().remove(file);
        }

        String commitID = commit.getCommitID();
        File commitFileX = Utils.join(commits, commitID);
        commitFile.createNewFile();
        Utils.writeObject(commitFileX, commit);

        objStage.getStagedFile().clear();
        objStage.getDelete().clear();
        Utils.writeObject(stage, objStage);

        Utils.writeContents(bNameFile, commitID);
    }

    public void rm(String fileName) {
        branchName = Utils.join(gitlet, "branchName");
        _branch = Utils.readContentsAsString(branchName);

        StagingArea objStage = Utils.readObject(stage, StagingArea.class);
        File branchFile = Utils.join(branch, _branch);
        String commitID = Utils.readContentsAsString(branchFile);
        File commitFiles = Utils.join(commits, commitID);
        Commit commit = Utils.readObject(commitFiles, Commit.class);

        if (!commit.getFileName().contains(fileName)) {
            if (!objStage.getStagedFile().containsKey(fileName)) {
                System.out.println("No reason to remove the file.");
                return;
            }
        }
        if (objStage.getStagedFile().containsKey(fileName)) {
            objStage.getStagedFile().remove(fileName);
        }
        if (commit.getFileName().contains(fileName)) {
            File deleteFile = Utils.join(cWD, fileName);
            deleteFile.delete();
            objStage.getDelete().add(fileName);
            commit.getFileName().remove(fileName);
        }
        Utils.writeObject(stage, objStage);
    }

    public void log() {
        branchName = Utils.join(gitlet, "branchName");
        _branch = Utils.readContentsAsString(branchName);
        File branchFile = Utils.join(branch, _branch);
        String headID = Utils.readContentsAsString(branchFile);
        File commitFile = Utils.join(commits, headID);
        Commit headCommit = Utils.readObject(commitFile, Commit.class);

        int counter = 0;
        while (headCommit.getParent1() != null) {
            if (counter != 0) {
                System.out.println();
            }
            File currFile = Utils.join(commits, headID);
            headCommit = Utils.readObject(currFile, Commit.class);
            System.out.println("===\ncommit " + headCommit.getCommitID());
            if (headCommit.getParent1() != null
                    && headCommit.getParent2() != null) {
                System.out.println("Merge: "
                        + headCommit.getParent1().substring(0, 7)
                        + " " + headCommit.getParent2().substring(0, 7));
            }
            System.out.println("Date: " + headCommit.getTimestamp());
            System.out.println(headCommit.getMessage());
            headID = headCommit.getParent1();
            counter++;
        }
    }

    public void globalLog() {
        int counter = 0;
        for (File commitFile : commits.listFiles()) {
            if (counter != 0) {
                System.out.println();
            }
            Commit commit = Utils.readObject(commitFile, Commit.class);
            System.out.println("===");
            System.out.println("commit " + commit.getCommitID());
            System.out.println("Date: " + commit.getTimestamp());
            System.out.println(commit.getMessage());
            counter++;
        }
    }

    public void find(String message) {
        int counter = 0;
        for (File commitFile : commits.listFiles()) {
            Commit commit = Utils.readObject(commitFile, Commit.class);
            if (commit.getMessage().equals(message)) {
                System.out.println(commit.getCommitID());
                counter++;
            }
        }
        if (counter == 0) {
            System.out.println("Found no commit with that message.");
        }
    }

    public void status() {
        if (!gitlet.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        branchName = Utils.join(gitlet, "branchName");
        _branch = Utils.readContentsAsString(branchName);

        System.out.println("=== Branches ===");
        File[] branchLst = branch.listFiles();
        Arrays.sort(branchLst);
        for (File branchOptn : branchLst) {
            String[] bName = branchOptn.toString().split("/");
            String name = bName[bName.length - 1];
            if (_branch.equals(name)) {
                System.out.println("*" + name);
            } else {
                System.out.println(name);
            }
        }
        System.out.println();
        StagingArea objStage = Utils.readObject(stage, StagingArea.class);
        System.out.println("=== Staged Files ===");
        for (String fileName : objStage.getStagedFile().keySet()) {
            System.out.println(fileName);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String fileName : objStage.getDelete()) {
            System.out.println(fileName);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        modificationsStatus(objStage);
    }

    public void modificationsStatus(StagingArea objStage) {
        File branchFile = Utils.join(branch, _branch);
        String commitID = Utils.readContentsAsString(branchFile);
        File commitFile = Utils.join(commits, commitID);
        Commit headCommit = Utils.readObject(commitFile, Commit.class);

        ArrayList<String> modifiedFiles = new ArrayList<>();
        for (File file : cWD.listFiles()) {
            if (file.isFile()) {
                String[] fileName = file.toString().split("/");
                String name = fileName[fileName.length - 1];
                modifiedFiles.add(name);
            }
        }

        for (String commitFileName : headCommit.getFileName()) {
            if ((modifiedFiles.contains(commitFileName)
                    && objStage.getStagedFile().containsKey(commitFileName))) {
                System.out.println(commitFileName + " (deleted)");
            } else {
                String blobFile = headCommit.getBlob(commitFileName);
                File commitContents = Utils.join(blobs, blobFile);
                String lastCommit = Utils.readContentsAsString(commitContents);

                if (lastCommit.equals(commitFileName)) {
                    System.out.println(commitFileName + " (modified)");
                }
            }
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        for (String fileName : modifiedFiles) {
            if (!objStage.getStagedFile().containsKey(fileName)
                    && !headCommit.getFileName().contains(fileName)) {
                System.out.println(fileName);
            }
        }
        System.out.println();
    }

    public void checkout(String[] args) {
        if (Main.correctInput(args, 3)) {
            checkout1(args[2]);
        } else if (Main.correctInput(args, 4)) {
            if (!args[2].equals("--")) {
                System.out.println("Incorrect operands.");
                return;
            }
            checkout2(args[1], args[3]);
        } else if (Main.correctInput(args, 2)) {
            checkout3(args[1]);
        }
    }

    public void checkout1(String fileName) {
        branchName = Utils.join(gitlet, "branchName");
        _branch = Utils.readContentsAsString(branchName);

        File branchFile = Utils.join(branch, _branch);
        String commitID = Utils.readContentsAsString(branchFile);
        checkout2(commitID, fileName);
    }

    public void checkout2(String commitID, String fileName) {
        branchName = Utils.join(gitlet, "branchName");
        _branch = Utils.readContentsAsString(branchName);

        String[] commitLst = commits.list();
        if (commitID.length() != commitLst[0].length()) {
            commitID = findAbbreviation(commitLst, commitID);
        }
        int counter = 0;
        for (String commitName : commitLst) {
            if (commitName.equals(commitID)) {
                counter++;
            }
        }
        if (counter == 0) {
            System.out.println("No commit with that id exists.");
            return;
        }
        File commitFile = Utils.join(commits, commitID);
        Commit commit = Utils.readObject(commitFile, Commit.class);
        String blobID = commit.getBlob(fileName);
        if (blobID.equals("")) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        File blobFile = Utils.join(blobs, blobID);
        String blobContent = Utils.readContentsAsString(blobFile);

        Utils.writeContents(Utils.join(cWD, fileName), blobContent);
    }

    public void checkout3(String branchNameX) {
        branchName = Utils.join(gitlet, "branchName");
        _branch = Utils.readContentsAsString(branchName);

        if (_branch.equals(branchNameX)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        if (!Utils.join(branch, branchNameX).exists()) {
            System.out.println("No such branch exists.");
            return;
        }
        StagingArea objStage = Utils.readObject(stage, StagingArea.class);
        File branchFileM = Utils.join(branch, _branch);
        String commitID = Utils.readContentsAsString(branchFileM);
        File commitFileM = Utils.join(commits, commitID);
        Commit commit = Utils.readObject(commitFileM, Commit.class);

        for (File file : cWD.listFiles()) {
            if (file.isFile()) {
                String fullFile = file.toString();
                String[] splitFile = fullFile.split("/");
                String fileName = splitFile[splitFile.length - 1];
                if (!commit.getFileName().contains(fileName)) {
                    if (!objStage.getStagedFile().containsKey(fileName)) {
                        System.out.println("There is an untracked file in the "
                                + "way; delete it, or "
                                + "add and commit it first.");
                        return;
                    }
                }
            }
        }
        for (String branchFileName : commit.getFileName()) {
            File branchFile = Utils.join(cWD, branchFileName);
            branchFile.delete();
        }
        File branchFileX = Utils.join(branch, branchNameX);
        String branchCommitID = Utils.readContentsAsString(branchFileX);
        File commitFile = Utils.join(commits, branchCommitID);
        Commit branchCommit = Utils.readObject(commitFile, Commit.class);
        for (String branchFile : branchCommit.getFileName()) {
            String blobID = branchCommit.getBlob(branchFile);
            File blobFile = Utils.join(blobs, blobID);
            String filler = Utils.readContentsAsString(blobFile);
            File branchDirectory = Utils.join(cWD, branchFile);
            Utils.writeContents(branchDirectory, filler);
        }
        Utils.writeContents(branchName, branchNameX);
    }

    public void branch(String branchNameX) throws IOException {
        File newBranch = Utils.join(branch, branchNameX);
        if (newBranch.exists()) {
            System.out.println("A branch with that name already exists.");
            return;
        } else {
            newBranch.createNewFile();
        }

        branchName = Utils.join(gitlet, "branchName");
        _branch = Utils.readContentsAsString(branchName);
        File branchFile = Utils.join(branch, _branch);
        String branchHead = Utils.readContentsAsString(branchFile);
        Utils.writeContents(newBranch, branchHead);
    }

    public void rmBranch(String branchNameX) {
        branchName = Utils.join(gitlet, "branchName");
        _branch = Utils.readContentsAsString(branchName);

        File removedBranch = Utils.join(branch, branchNameX);

        if (!removedBranch.exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (branchNameX.equals(_branch)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        removedBranch.delete();
    }

    public void reset(String commitID) throws IOException {
        branchName = Utils.join(gitlet, "branchName");
        _branch = Utils.readContentsAsString(branchName);
        File commitFile = Utils.join(commits, commitID);
        if (!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        String[] commitsLst = commits.list();
        if (commitID.length() < commitsLst[0].length()) {
            commitID = findAbbreviation(commitsLst, commitID);
        }
        StagingArea objStage = Utils.readObject(stage, StagingArea.class);
        File branchFile = Utils.join(branch, _branch);
        String hCommitID = Utils.readContentsAsString(branchFile);
        File commitFileX = Utils.join(commits, hCommitID);
        Commit hCommit = Utils.readObject(commitFileX, Commit.class);
        File commitFileY = Utils.join(commits, commitID);
        Commit commit = Utils.readObject(commitFileY, Commit.class);

        for (File file : cWD.listFiles()) {
            if (file.isFile()) {
                String fullFile = file.toString();
                String[] splitFile = fullFile.split("/");
                String fileName = splitFile[splitFile.length - 1];
                if (!hCommit.getFileName().contains(fileName)) {
                    if (!objStage.getStagedFile().containsKey(fileName)) {
                        System.out.println("There is an untracked file in the "
                                + "way; delete it, or "
                                + "add and commit it first.");
                        return;
                    }
                }
            }
        }
        for (File file : cWD.listFiles()) {
            if (!commit.getFileName().contains(file.getName())) {
                if (hCommit.getFileName().contains(file.getName())) {
                    file.delete();
                }
            }
        }
        for (String stagedFileName : objStage.getStagedFile().keySet()) {
            Utils.restrictedDelete(stagedFileName);
        }
        Set<String> commitsList = commit.getFileName();
        for (String fileName : commitsList) {
            File blobFile = Utils.join(blobs, commit.getBlob(fileName));
            String filler = Utils.readContentsAsString(blobFile);
            File checkoutFile = Utils.join(cWD, fileName);
            checkoutFile.createNewFile();
            Utils.writeContents(checkoutFile, filler);
        }
        objStage.getStagedFile().clear();
        objStage.getDelete().clear();
        Utils.writeObject(stage, objStage);
        Utils.writeContents(Utils.join(branch, _branch), commitID);
    }

    public void merge(String branchNameX) throws IOException {
        if (checkExceptions(branchNameX)) {
            return;
        }
        File currBranch = Utils.join(branch, _branch);
        File otherBranch = Utils.join(branch, branchNameX);
        String currCommitID = Utils.readContentsAsString(currBranch);
        String otherCommitID = Utils.readContentsAsString(otherBranch);
        File currFile = Utils.join(commits, currCommitID);
        Commit cuCommit = Utils.readObject(currFile, Commit.class);
        File otherFile = Utils.join(commits, otherCommitID);
        Commit otherCommit = Utils.readObject(otherFile, Commit.class);
        Commit splitCommit = idek(cuCommit, otherCommit);
        StagingArea objStage = Utils.readObject(stage, StagingArea.class);
        if (specialErrors(cuCommit, otherCommit, splitCommit, branchNameX)) {
            return;
        }
        Set<String> allBlobSet = addBlobSet(cuCommit, otherCommit, splitCommit);
        boolean mergeConflict = false;
        for (String fileName : allBlobSet) {
            String currBlob = cuCommit.getBlob(fileName);
            String otherBlob = otherCommit.getBlob(fileName);
            String splitBlob = splitCommit.getBlob(fileName);

            if (currBlob.equals(splitBlob)
                    && !currBlob.equals(otherBlob)) {
                if (otherBlob.equals("")) {
                    objStage.getDelete().add(fileName);
                } else {
                    objStage.add(fileName, otherBlob);
                }
            } else if (!otherBlob.equals(splitBlob)
                    && !currBlob.equals(splitBlob)) {
                if (!otherBlob.equals(currBlob)) {
                    String newBlobID = mergeConflict(currBlob, otherBlob);
                    objStage.add(fileName, newBlobID); mergeConflict = true;
                }
            } else if (splitBlob.equals("") && splitBlob.equals(currBlob)
                    && !splitBlob.equals(otherBlob)) {
                objStage.add(fileName, otherBlob);
            } else if (otherBlob.equals("") && currBlob.equals(splitBlob)) {
                objStage.getDelete().add(fileName);
            }
        }
        Utils.writeObject(stage, objStage);
        for (String fileName : objStage.getStagedFile().keySet()) {
            String blobID = objStage.getStagedFile().get(fileName);
            File blobFile = Utils.join(blobs, blobID);
            String filler = Utils.readContentsAsString(blobFile);
            Utils.writeContents(Utils.join(cWD, fileName), filler);
        }
        String message = "Merged " + branchNameX
                + " into " + _branch + ".";
        if (mergeConflict) {
            System.out.println("Encountered a merge conflict.");
        }
        String currCommID = cuCommit.getCommitID();
        String otherCommID = otherCommit.getCommitID();
        mergeCommit(message, currCommID, otherCommID);
    }

    public Set<String> addBlobSet(Commit currCommit,
                                  Commit otherCommit, Commit splitCommit) {
        Set<String> allBlobSet = new HashSet<>();
        allBlobSet.addAll(currCommit.getFileName());
        allBlobSet.addAll(otherCommit.getFileName());
        allBlobSet.addAll(splitCommit.getFileName());
        return allBlobSet;
    }

    public boolean specialErrors(Commit currCommit, Commit otherCommit,
                                 Commit splitCommit, String branchNameX) {
        if (splitCommit.getCommitID().equals(otherCommit.getCommitID())) {
            System.out.println("Given branch is an "
                    + "ancestor of the current branch.");
            return true;
        }

        if (splitCommit.getCommitID().equals(currCommit.getCommitID())) {
            checkout3(branchNameX);
            System.out.println("Current branch fast-forwarded.");
            return true;
        }
        return false;
    }

    public void mergeCommit(String message, String parent1, String parent2)
            throws IOException {
        branchName = Utils.join(gitlet, "branchName");
        _branch = Utils.readContentsAsString(branchName);

        String contents = Utils.readContentsAsString(branchName);
        File bNameFile = Utils.join(branch, contents);
        String bNameID = Utils.readContentsAsString(bNameFile);
        File parentFile = Utils.join(commits, bNameID);
        Commit parentCommit = Utils.readObject(parentFile, Commit.class);

        StagingArea objStage = Utils.readObject(stage, StagingArea.class);

        if (objStage.getStagedFile().isEmpty()
                && objStage.getDelete().isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }

        Commit commit = new Commit(parent1, parent2, message);
        commit.setFiles(objStage.getStagedFile());

        for (String fileName : parentCommit.getFileName()) {
            if (!objStage.getStagedFile().containsKey(fileName)
                    && !objStage.getDelete().contains(fileName)) {
                commit.setFile(fileName, parentCommit.getBlob(fileName));
            }
        }

        for (String fileName : objStage.getDelete()) {
            Utils.join(cWD, fileName).delete();
        }

        String commitID = commit.getCommitID();
        File commitFile = Utils.join(commits, commitID);
        commitFile.createNewFile();
        Utils.writeObject(commitFile, commit);

        objStage.getStagedFile().clear();
        objStage.getDelete().clear();
        Utils.writeObject(stage, objStage);

        File branchFile = Utils.join(branch, _branch);
        Utils.writeContents(branchFile, commitID);
    }

    public String mergeConflict(String currBlob, String otherBlob)
            throws IOException {
        String currFiller = "";
        String otherFiller = "";
        if (!currBlob.equals("")) {
            File blobFile = Utils.join(blobs, currBlob);
            currFiller = Utils.readContentsAsString(blobFile);
        }
        if (!otherBlob.equals("")) {
            File blobFile = Utils.join(blobs, otherBlob);
            otherFiller = Utils.readContentsAsString(blobFile);
        }
        String filler = "<<<<<<< HEAD\n" + currFiller
                + "=======\n" + otherFiller + ">>>>>>>\n";
        String newBlobID = Utils.sha1(filler);
        File newBlobIDFile = Utils.join(blobs, newBlobID);
        newBlobIDFile.createNewFile();
        Utils.writeContents(newBlobIDFile, filler);
        return newBlobID;
    }

    public Commit idek(Commit currCommit, Commit otherCommit) {
        ArrayList<String> otherList = new ArrayList<>();
        otherList.add(otherCommit.getCommitID());
        while (otherCommit.getParent1() != null) {
            otherList.add(otherCommit.getParent1());
            if (otherCommit.getParent2() != null) {
                otherList.add(otherCommit.getParent2());
            }
            File otherFile = Utils.join(commits, otherCommit.getParent1());
            otherCommit = Utils.readObject(otherFile, Commit.class);
        }

        if (otherList.contains(currCommit.getCommitID())) {
            File commitFile = Utils.join(commits, currCommit.getCommitID());
            Commit finalCommit = Utils.readObject(commitFile, Commit.class);
            return finalCommit;
        }

        while (currCommit.getParent1() != null) {
            if (otherList.contains(currCommit.getParent1())) {
                File commitFile = Utils.join(commits, currCommit.getParent1());
                Commit fCommit = Utils.readObject(commitFile, Commit.class);
                return fCommit;
            }
            if (currCommit.getParent2() != null) {
                if (otherList.contains(currCommit.getParent2())) {
                    File cFile = Utils.join(commits, currCommit.getParent2());
                    Commit fCommit = Utils.readObject(cFile, Commit.class);
                    return fCommit;
                }
            }
            File commitFile = Utils.join(commits, currCommit.getParent1());
            currCommit = Utils.readObject(commitFile, Commit.class);
        }
        return null;
    }

    public boolean checkExceptions(String branchNameX) {
        boolean incorrect = false;
        branchName = Utils.join(gitlet, "branchName");
        _branch = Utils.readContentsAsString(branchName);

        StagingArea objStage = Utils.readObject(stage, StagingArea.class);
        File branchFile = Utils.join(branch, _branch);
        String commitID = Utils.readContentsAsString(branchFile);
        File commitFile = Utils.join(commits, commitID);
        Commit commit = Utils.readObject(commitFile, Commit.class);

        if (!objStage.getStagedFile().isEmpty()
                || !objStage.getDelete().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            incorrect = true;
        }

        File mergeBranch = Utils.join(branch, branchNameX);
        if (!mergeBranch.exists()) {
            System.out.println("A branch with that name does not exist.");
            incorrect = true;
        }

        File currBranch = Utils.join(branch, _branch);
        if (mergeBranch.equals(currBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            incorrect = true;
        }

        for (File file : cWD.listFiles()) {
            if (file.isFile()) {
                String fullFile = file.toString();
                String[] splitFile = fullFile.split("/");
                String fileName = splitFile[splitFile.length - 1];
                if (!commit.getFileName().contains(fileName)) {
                    if (!objStage.getStagedFile().containsKey(fileName)) {
                        System.out.println("There is an untracked file in the "
                                + "way; delete it, or add "
                                + "and commit it first.");
                        incorrect = true;
                    }
                }
            }
        }
        return incorrect;
    }

    public String findAbbreviation(String[] commitLst, String abbrevID) {
        String substringCommit = abbrevID.substring(0, 4);
        for (String commitStr : commitLst) {
            String commitLstSubString = commitStr.substring(0, 4);
            if (substringCommit.equals(commitLstSubString)) {
                return commitStr;
            }
        }
        return null;
    }
    /** Current Directory. */

    private static File cWD = new File(System.getProperty("user.dir"));

    /** Gitlet Directory. */
    private static File gitlet = Utils.join(cWD, ".gitlet");

    /** Commits Directory. */
    private static File commits = Utils.join(gitlet, "commits");

    /** Blobs Directory. */
    private static File blobs = Utils.join(gitlet, "blob");

    /** Stage Directory. */
    private static File stage = Utils.join(gitlet, "Stage");

    /** Branch Directory. */
    private static File branch = Utils.join(gitlet, "branch");

    /** Current Branch. */
    private static String _branch;

    /** Current Branch Name. */
    private static File branchName;

}
