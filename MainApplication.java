package Gitlite;

import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.*;

// ============================================================
//  SIMPLIFIED GIT — DSA Semester Project (JavaFX GUI VERSION)
//  FIXED VERSION — sidebar navigation bug resolved
// ============================================================

// ================================================================
//  SECTION 1 — CUSTOM EXCEPTIONS
// ================================================================
class InvalidUsernameException extends Exception { InvalidUsernameException(String m) { super(m); } }
class InvalidPasswordException extends Exception { InvalidPasswordException(String m) { super(m); } }
class InvalidEmailException extends Exception { InvalidEmailException(String m) { super(m); } }
class UserNotFoundException extends Exception { UserNotFoundException(String m) { super(m); } }
class WrongPasswordException extends Exception { WrongPasswordException(String m) { super(m); } }
class RepositoryNotFoundException extends Exception { RepositoryNotFoundException(String m) { super(m); } }
class EmptyCommitException extends Exception { EmptyCommitException(String m) { super(m); } }
class EmptyCommitMessageException extends Exception { EmptyCommitMessageException(String m) { super(m); } }
class FileNotFoundException2 extends Exception { FileNotFoundException2(String m) { super(m); } }
class BranchNotFoundException extends Exception { BranchNotFoundException(String m) { super(m); } }
class InvalidFileNameException extends Exception { InvalidFileNameException(String m) { super(m); } }
class CommitNotFoundException extends Exception { CommitNotFoundException(String m) { super(m); } }

// ================================================================
//  SECTION 2 — VALIDATOR
// ================================================================
class Validator {
    static void validateUsername(String u) throws InvalidUsernameException {
        if (u == null || u.trim().isEmpty()) throw new InvalidUsernameException("Username cannot be empty.");
        if (u.length() < 3) throw new InvalidUsernameException("Username must be at least 3 characters.");
        if (u.length() > 20) throw new InvalidUsernameException("Username cannot exceed 20 characters.");
        for (char c : u.toCharArray())
            if (!Character.isLetterOrDigit(c) && c != '_')
                throw new InvalidUsernameException("Username can only contain letters, numbers, and underscores.");
    }
    static void validatePassword(String p) throws InvalidPasswordException {
        if (p == null || p.isEmpty()) throw new InvalidPasswordException("Password cannot be empty.");
        if (p.length() < 6) throw new InvalidPasswordException("Password must be at least 6 characters.");
        if (p.length() > 30) throw new InvalidPasswordException("Password cannot exceed 30 characters.");
        boolean hasLetter = false, hasDigit = false;
        for (char c : p.toCharArray()) { if (Character.isLetter(c)) hasLetter = true; if (Character.isDigit(c)) hasDigit = true; }
        if (!hasLetter) throw new InvalidPasswordException("Password must contain at least one letter.");
        if (!hasDigit)  throw new InvalidPasswordException("Password must contain at least one number.");
    }
    static void validateEmail(String e) throws InvalidEmailException {
        if (e == null || e.trim().isEmpty()) throw new InvalidEmailException("Email cannot be empty.");
        if (!e.contains("@")) throw new InvalidEmailException("Email must contain '@'.");
        int at = e.indexOf("@");
        if (at == 0) throw new InvalidEmailException("Email must have characters before '@'.");
        String after = e.substring(at + 1);
        if (!after.contains(".")) throw new InvalidEmailException("Email domain must contain a dot (e.g. gmail.com).");
        if (after.startsWith(".") || after.endsWith(".")) throw new InvalidEmailException("Invalid email format.");
        if (e.contains(" ")) throw new InvalidEmailException("Email cannot contain spaces.");
    }
    static void validateRepoName(String n) throws InvalidFileNameException {
        if (n == null || n.trim().isEmpty()) throw new InvalidFileNameException("Repository name cannot be empty.");
        if (n.length() < 2) throw new InvalidFileNameException("Repo name must be at least 2 characters.");
        if (n.length() > 30) throw new InvalidFileNameException("Repo name cannot exceed 30 characters.");
        if (n.contains(" ")) throw new InvalidFileNameException("Repo name cannot contain spaces.");
        for (char c : n.toCharArray())
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-')
                throw new InvalidFileNameException("Repo name can only contain letters, numbers, - and _");
    }
    static void validateFilename(String f) throws InvalidFileNameException {
        if (f == null || f.trim().isEmpty()) throw new InvalidFileNameException("Filename cannot be empty.");
        if (!f.contains(".")) throw new InvalidFileNameException("Filename must have an extension (e.g. notes.txt).");
        if (f.contains(" ")) throw new InvalidFileNameException("Filename cannot contain spaces.");
        if (f.length() > 50) throw new InvalidFileNameException("Filename too long (max 50 characters).");
    }
    static void validateCommitMessage(String m) throws EmptyCommitMessageException {
        if (m == null || m.trim().isEmpty()) throw new EmptyCommitMessageException("Commit message cannot be empty.");
        if (m.length() < 3) throw new EmptyCommitMessageException("Commit message must be at least 3 characters.");
        if (m.length() > 100) throw new EmptyCommitMessageException("Commit message too long (max 100 characters).");
    }
    static void validateBranchName(String n) throws BranchNotFoundException {
        if (n == null || n.trim().isEmpty()) throw new BranchNotFoundException("Branch name cannot be empty.");
        if (n.contains(" ")) throw new BranchNotFoundException("Branch name cannot contain spaces.");
        if (n.length() > 30) throw new BranchNotFoundException("Branch name too long (max 30 characters).");
    }
    static void validateCommitId(String id) throws CommitNotFoundException {
        if (id == null || id.trim().isEmpty()) throw new CommitNotFoundException("Commit ID cannot be empty.");
        if (!id.matches("c\\d{3,}")) throw new CommitNotFoundException("Invalid commit ID. Must be like c001, c002 etc.");
    }
}

// ================================================================
//  SECTION 3 — STACK
// ================================================================
class StackNode implements Serializable {
    private static final long serialVersionUID = 1L;
    String data; StackNode next;
    StackNode(String data) { this.data = data; }
}
class MyStack implements Serializable {
    private static final long serialVersionUID = 1L;
    private StackNode top;
    void push(String data) { StackNode n = new StackNode(data); n.next = top; top = n; }
    String pop() { if (top == null) return null; String d = top.data; top = top.next; return d; }
    String peek() { return top == null ? null : top.data; }
    boolean isEmpty() { return top == null; }
    void clear() { top = null; }
}

// ================================================================
//  SECTION 4 — HASHMAP
// ================================================================
class HashNode implements Serializable {
    private static final long serialVersionUID = 1L;
    String key; FileEntry value; HashNode next;
    HashNode(String key, FileEntry value) { this.key = key; this.value = value; }
}
class MyHashMap implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int BUCKETS = 16;
    private HashNode[] table; private int size;
    MyHashMap() { table = new HashNode[BUCKETS]; }
    private int hash(String key) { int h = 0; for (char c : key.toCharArray()) h += c; return Math.abs(h % BUCKETS); }
    void put(String key, FileEntry value) {
        int idx = hash(key); HashNode curr = table[idx];
        while (curr != null) { if (curr.key.equals(key)) { curr.value = value; return; } curr = curr.next; }
        HashNode n = new HashNode(key, value); n.next = table[idx]; table[idx] = n; size++;
    }
    FileEntry get(String key) { int idx = hash(key); HashNode curr = table[idx]; while (curr != null) { if (curr.key.equals(key)) return curr.value; curr = curr.next; } return null; }
    void remove(String key) {
        int idx = hash(key); HashNode curr = table[idx], prev = null;
        while (curr != null) { if (curr.key.equals(key)) { if (prev == null) table[idx] = curr.next; else prev.next = curr.next; size--; return; } prev = curr; curr = curr.next; }
    }
    boolean containsKey(String key) { return get(key) != null; }
    int size() { return size; }
    String[] keys() { String[] r = new String[size]; int i = 0; for (HashNode b : table) { HashNode c = b; while (c != null) { r[i++] = c.key; c = c.next; } } return r; }
    FileEntry[] values() { FileEntry[] r = new FileEntry[size]; int i = 0; for (HashNode b : table) { HashNode c = b; while (c != null) { r[i++] = c.value; c = c.next; } } return r; }
}

// ================================================================
//  SECTION 5 — CORE DATA CLASSES
// ================================================================
class FileEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private String filename, content;
    FileEntry(String filename, String content) { this.filename = filename; this.content = content; }
    FileEntry(FileEntry other) { this.filename = other.filename; this.content = other.content; }
    String getFilename() { return filename; }
    String getContent()  { return content; }
    void setContent(String c) { this.content = c; }
    public String toString() { return "[" + filename + "]\n" + content; }
}

class Commit implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id, message, author, timestamp, branch, tag;
    private FileEntry[] files; private int fileCount;
    Commit parent;
    Commit(String id, String message, String author, String branch, FileEntry[] files, int fileCount) {
        this.id = id; this.message = message; this.author = author; this.branch = branch; this.tag = "";
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        this.fileCount = fileCount; this.parent = null;
        this.files = new FileEntry[fileCount];
        for (int i = 0; i < fileCount; i++) this.files[i] = new FileEntry(files[i]);
    }
    String getId()        { return id; }
    String getMessage()   { return message; }
    String getAuthor()    { return author; }
    String getTimestamp() { return timestamp; }
    String getBranch()    { return branch; }
    String getTag()       { return tag; }
    void   setTag(String t) { this.tag = t; }
    int    getFileCount() { return fileCount; }
    FileEntry getFile(int i) { return (i >= 0 && i < fileCount) ? files[i] : null; }
    FileEntry findFile(String filename) { for (int i = 0; i < fileCount; i++) if (files[i].getFilename().equals(filename)) return files[i]; return null; }
    public String toString() { String t = (tag != null && !tag.isEmpty()) ? " <" + tag + ">" : ""; return "* " + id + t + " - " + message + " (" + author + ") [" + branch + "] [" + timestamp + "]"; }
}

// ================================================================
//  SECTION 6 — COMMIT LINKED LIST
// ================================================================
class CommitList implements Serializable {
    private static final long serialVersionUID = 1L;
    Commit head; private int size;
    void add(Commit c) { c.parent = head; head = c; size++; }
    Commit findById(String id) { Commit curr = head; while (curr != null) { if (curr.getId().equals(id)) return curr; curr = curr.parent; } return null; }
    int size() { return size; }
}

// ================================================================
//  SECTION 7 — BST
// ================================================================
class BSTNode implements Serializable {
    private static final long serialVersionUID = 1L;
    String commitId; Commit commit; BSTNode left, right;
    BSTNode(Commit c) { this.commitId = c.getId(); this.commit = c; }
}
class CommitBST implements Serializable {
    private static final long serialVersionUID = 1L;
    private BSTNode root;
    private BSTNode insert(BSTNode node, Commit c) {
        if (node == null) return new BSTNode(c);
        int cmp = c.getId().compareTo(node.commitId);
        if (cmp < 0) node.left = insert(node.left, c); else if (cmp > 0) node.right = insert(node.right, c);
        return node;
    }
    void insert(Commit c) { root = insert(root, c); }
    private BSTNode search(BSTNode node, String id) {
        if (node == null || node.commitId.equals(id)) return node;
        return id.compareTo(node.commitId) < 0 ? search(node.left, id) : search(node.right, id);
    }
    Commit search(String id) { BSTNode n = search(root, id); return n == null ? null : n.commit; }
    private void inorder(BSTNode node, java.util.List<Commit> list) { if (node == null) return; inorder(node.left, list); list.add(node.commit); inorder(node.right, list); }
    java.util.List<Commit> getSorted() { java.util.List<Commit> list = new java.util.ArrayList<>(); inorder(root, list); return list; }
}

// ================================================================
//  SECTION 8 — BRANCH
// ================================================================
class Branch implements Serializable {
    private static final long serialVersionUID = 1L;
    String name; Commit head; Branch next;
    Branch(String name) { this.name = name; }
}
class BranchList implements Serializable {
    private static final long serialVersionUID = 1L;
    Branch head; int size;
    Branch create(String name) { if (find(name) != null) return null; Branch b = new Branch(name); b.next = head; head = b; size++; return b; }
    Branch find(String name) { Branch curr = head; while (curr != null) { if (curr.name.equals(name)) return curr; curr = curr.next; } return null; }
    java.util.List<String> getNames() { java.util.List<String> names = new java.util.ArrayList<>(); Branch curr = head; while (curr != null) { names.add(curr.name); curr = curr.next; } return names; }
}

// ================================================================
//  SECTION 9 — REMOTE
// ================================================================
class RemoteRepo implements Serializable {
    private static final long serialVersionUID = 1L;
    String name; CommitList commits; RemoteRepo next;
    RemoteRepo(String name) { this.name = name; this.commits = new CommitList(); }
}
class RemoteStore implements Serializable {
    private static final long serialVersionUID = 1L;
    private RemoteRepo head;
    RemoteRepo getOrCreate(String name) { RemoteRepo curr = head; while (curr != null) { if (curr.name.equals(name)) return curr; curr = curr.next; } RemoteRepo r = new RemoteRepo(name); r.next = head; head = r; return r; }
    boolean exists(String name) { RemoteRepo curr = head; while (curr != null) { if (curr.name.equals(name)) return curr.commits.head != null; curr = curr.next; } return false; }
}

// ================================================================
//  SECTION 10 — CONTRIBUTOR TRACKER
// ================================================================
class ContribNode implements Serializable {
    private static final long serialVersionUID = 1L;
    String author; int commitCount; ContribNode next;
    ContribNode(String author) { this.author = author; this.commitCount = 1; }
}
class ContributorTracker implements Serializable {
    private static final long serialVersionUID = 1L;
    private ContribNode head;
    void record(String author) { ContribNode curr = head; while (curr != null) { if (curr.author.equals(author)) { curr.commitCount++; return; } curr = curr.next; } ContribNode n = new ContribNode(author); n.next = head; head = n; }
    java.util.List<String[]> getLeaderboard() {
        java.util.List<String[]> list = new java.util.ArrayList<>();
        ContribNode curr = head; while (curr != null) { list.add(new String[]{curr.author, String.valueOf(curr.commitCount)}); curr = curr.next; }
        list.sort((a, b) -> Integer.parseInt(b[1]) - Integer.parseInt(a[1])); return list;
    }
}

// ================================================================
//  SECTION 11 — REPOSITORY
// ================================================================
class Repository implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name, ownerUsername;
    CommitList history; CommitBST commitIndex; private int commitCounter;
    BranchList branches; Branch activeBranch;
    FileEntry[] stagedFiles; int stagedCount;
    private static final int MAX_STAGED = 20;
    private MyStack undoStack, redoStack;
    MyHashMap workingFiles;
    private String[] pushedIds; private int pushedCount;
    private static final int MAX_PUSHED = 200;
    ContributorTracker contributors;

    Repository(String name, String ownerUsername) {
        this.name = name; this.ownerUsername = ownerUsername;
        this.history = new CommitList(); this.commitIndex = new CommitBST(); this.commitCounter = 0;
        this.branches = new BranchList(); this.stagedFiles = new FileEntry[MAX_STAGED]; this.stagedCount = 0;
        this.undoStack = new MyStack(); this.redoStack = new MyStack();
        this.workingFiles = new MyHashMap(); this.pushedIds = new String[MAX_PUSHED]; this.pushedCount = 0;
        this.contributors = new ContributorTracker(); this.activeBranch = branches.create("main");
    }

    String getName()          { return name; }
    String getOwnerUsername() { return ownerUsername; }
    String getActiveBranch()  { return activeBranch == null ? "main" : activeBranch.name; }
    private String nextCommitId() { commitCounter++; return String.format("c%03d", commitCounter); }

    void writeFile(String filename, String content) { workingFiles.put(filename, new FileEntry(filename, content)); }

    void importFile(String path) throws FileNotFoundException2, IOException {
        File f = new File(path);
        if (!f.exists())  throw new FileNotFoundException2("File does not exist: " + path);
        if (!f.isFile())  throw new FileNotFoundException2("Path is not a file: " + path);
        if (!f.canRead()) throw new FileNotFoundException2("Cannot read file: " + path);
        java.util.Scanner sc = null;
        try { sc = new java.util.Scanner(f); StringBuilder sb = new StringBuilder(); while (sc.hasNextLine()) sb.append(sc.nextLine()).append("\n"); workingFiles.put(f.getName(), new FileEntry(f.getName(), sb.toString().trim())); }
        finally { if (sc != null) sc.close(); }
    }

    void exportFile(String filename, String destPath) throws FileNotFoundException2, IOException {
        FileEntry fe = workingFiles.get(filename); if (fe == null) throw new FileNotFoundException2("File not found: " + filename);
        File dest = new File(destPath); if (dest.isDirectory()) dest = new File(destPath + File.separator + filename);
        try (FileWriter fw = new FileWriter(dest)) { fw.write(fe.getContent()); }
    }

    void stageFile(String filename) throws FileNotFoundException2 {
        FileEntry wf = workingFiles.get(filename); if (wf == null) throw new FileNotFoundException2("File not found in working area: " + filename);
        for (int i = 0; i < stagedCount; i++) { if (stagedFiles[i].getFilename().equals(filename)) { stagedFiles[i] = new FileEntry(wf); undoStack.push("unstage:" + filename); redoStack.clear(); return; } }
        if (stagedCount < MAX_STAGED) { stagedFiles[stagedCount++] = new FileEntry(wf); undoStack.push("unstage:" + filename); redoStack.clear(); }
    }

    void unstageFile(String filename) throws FileNotFoundException2 {
        for (int i = 0; i < stagedCount; i++) { if (stagedFiles[i].getFilename().equals(filename)) { for (int j = i; j < stagedCount - 1; j++) stagedFiles[j] = stagedFiles[j + 1]; stagedFiles[--stagedCount] = null; return; } }
        throw new FileNotFoundException2("File not in staging area: " + filename);
    }

    void undoStage() { String a = undoStack.pop(); if (a != null && a.startsWith("unstage:")) { try { unstageFile(a.substring(8)); } catch (Exception e) {} redoStack.push("stage:" + a.substring(8)); } }
    void redoStage() { String a = redoStack.pop(); if (a != null && a.startsWith("stage:")) { try { stageFile(a.substring(6)); } catch (Exception e) {} } }

    String commit(String message, String author) throws EmptyCommitException, EmptyCommitMessageException {
        if (stagedCount == 0) throw new EmptyCommitException("Nothing staged. Stage files before committing.");
        Validator.validateCommitMessage(message);
        String id = nextCommitId();
        Commit c = new Commit(id, message, author, activeBranch.name, stagedFiles, stagedCount);
        c.parent = activeBranch.head; history.add(c); commitIndex.insert(c);
        activeBranch.head = c; contributors.record(author);
        stagedFiles = new FileEntry[MAX_STAGED]; stagedCount = 0; undoStack.clear(); redoStack.clear();
        return "Committed: " + c;
    }

    void tagCommit(String commitId, String tag) throws CommitNotFoundException {
        if (commitId == null || commitId.isEmpty()) throw new CommitNotFoundException("Commit ID cannot be empty.");
        Commit c = commitIndex.search(commitId); if (c == null) throw new CommitNotFoundException("Commit not found: " + commitId);
        c.setTag(tag);
    }

    void createBranch(String name) throws BranchNotFoundException {
        Validator.validateBranchName(name);
        if (branches.find(name) != null) throw new BranchNotFoundException("Branch '" + name + "' already exists.");
        Branch b = branches.create(name); if (b != null) b.head = activeBranch.head;
    }

    String switchBranch(String name) throws BranchNotFoundException {
        Branch b = branches.find(name); if (b == null) throw new BranchNotFoundException("Branch not found: " + name);
        activeBranch = b;
        if (b.head != null) { workingFiles = new MyHashMap(); for (int i = 0; i < b.head.getFileCount(); i++) { FileEntry fe = b.head.getFile(i); workingFiles.put(fe.getFilename(), new FileEntry(fe)); } }
        return "Switched to branch: " + name;
    }

    String mergeBranch(String sourceName, String author) throws BranchNotFoundException, EmptyCommitException {
        if (sourceName.equals(activeBranch.name)) throw new BranchNotFoundException("Cannot merge branch into itself.");
        Branch source = branches.find(sourceName); if (source == null) throw new BranchNotFoundException("Branch not found: " + sourceName);
        if (source.head == null) throw new EmptyCommitException("Source branch has no commits.");
        if (activeBranch.head == null) throw new EmptyCommitException("Current branch has no commits.");
        stagedFiles = new FileEntry[MAX_STAGED]; stagedCount = 0;
        Commit ct = activeBranch.head; for (int i = 0; i < ct.getFileCount(); i++) if (stagedCount < MAX_STAGED) stagedFiles[stagedCount++] = new FileEntry(ct.getFile(i));
        Commit st = source.head;
        for (int i = 0; i < st.getFileCount(); i++) { FileEntry inc = st.getFile(i); boolean found = false; for (int j = 0; j < stagedCount; j++) { if (stagedFiles[j].getFilename().equals(inc.getFilename())) { stagedFiles[j] = new FileEntry(inc); found = true; break; } } if (!found && stagedCount < MAX_STAGED) stagedFiles[stagedCount++] = new FileEntry(inc); }
        String id = nextCommitId(); String msg = "Merge '" + sourceName + "' into '" + activeBranch.name + "'";
        Commit mc = new Commit(id, msg, author, activeBranch.name, stagedFiles, stagedCount);
        mc.parent = activeBranch.head; history.add(mc); commitIndex.insert(mc); activeBranch.head = mc; contributors.record(author);
        stagedFiles = new FileEntry[MAX_STAGED]; stagedCount = 0; undoStack.clear(); redoStack.clear();
        workingFiles = new MyHashMap(); for (int i = 0; i < mc.getFileCount(); i++) { FileEntry fe = mc.getFile(i); workingFiles.put(fe.getFilename(), new FileEntry(fe)); }
        return "Merged successfully. Commit: " + mc.getId();
    }

    java.util.List<Commit> getAllCommits() { java.util.List<Commit> list = new java.util.ArrayList<>(); Commit curr = history.head; while (curr != null) { list.add(curr); curr = curr.parent; } return list; }
    java.util.List<Commit> getBranchCommits() { java.util.List<Commit> list = new java.util.ArrayList<>(); Commit curr = activeBranch.head; while (curr != null) { list.add(curr); curr = curr.parent; } return list; }

    Commit searchCommit(String id) throws CommitNotFoundException { Validator.validateCommitId(id); Commit c = commitIndex.search(id); if (c == null) throw new CommitNotFoundException("Commit not found: " + id); return c; }

    String checkout(String commitId) throws CommitNotFoundException {
        Validator.validateCommitId(commitId); Commit c = commitIndex.search(commitId); if (c == null) throw new CommitNotFoundException("Commit not found: " + commitId);
        workingFiles = new MyHashMap(); for (int i = 0; i < c.getFileCount(); i++) { FileEntry fe = c.getFile(i); workingFiles.put(fe.getFilename(), new FileEntry(fe)); }
        return "Checked out: " + commitId;
    }

    String diff(String id1, String id2, String filename) throws CommitNotFoundException, FileNotFoundException2 {
        Validator.validateCommitId(id1); Validator.validateCommitId(id2);
        Commit c1 = commitIndex.search(id1); if (c1 == null) throw new CommitNotFoundException("Commit not found: " + id1);
        Commit c2 = commitIndex.search(id2); if (c2 == null) throw new CommitNotFoundException("Commit not found: " + id2);
        FileEntry f1 = c1.findFile(filename); if (f1 == null) throw new FileNotFoundException2("File '" + filename + "' not in commit " + id1);
        FileEntry f2 = c2.findFile(filename); if (f2 == null) throw new FileNotFoundException2("File '" + filename + "' not in commit " + id2);
        return "--- " + id1 + " : " + filename + "\n" + f1.getContent() + "\n\n+++ " + id2 + " : " + filename + "\n" + f2.getContent() + "\n\n" + (f1.getContent().equals(f2.getContent()) ? "(No changes)" : "(File differs)");
    }

    private boolean isPushed(String id) { for (int i = 0; i < pushedCount; i++) if (pushedIds[i].equals(id)) return true; return false; }
    private void markPushed(String id) { if (!isPushed(id) && pushedCount < MAX_PUSHED) pushedIds[pushedCount++] = id; }
    private Commit[] collectOldestFirst() { int total = history.size(); if (total == 0) return new Commit[0]; Commit[] arr = new Commit[total]; Commit curr = history.head; for (int i = total - 1; i >= 0; i--) { arr[i] = curr; curr = curr.parent; } return arr; }

    String push(RemoteStore rs) throws EmptyCommitException {
        if (history.head == null) throw new EmptyCommitException("No commits to push.");
        RemoteRepo remote = rs.getOrCreate(name); int count = 0;
        for (Commit c : collectOldestFirst()) { if (!isPushed(c.getId())) { remote.commits.add(c); markPushed(c.getId()); count++; } }
        return count == 0 ? "Already up to date." : "Pushed " + count + " commit(s).";
    }

    String pull(RemoteStore rs) throws EmptyCommitException {
        if (!rs.exists(name)) throw new EmptyCommitException("Remote is empty. Push first.");
        RemoteRepo remote = rs.getOrCreate(name); int total = remote.commits.size();
        Commit[] remoteCm = new Commit[total]; Commit curr = remote.commits.head;
        for (int i = total - 1; i >= 0; i--) { remoteCm[i] = curr; curr = curr.parent; }
        int count = 0;
        for (Commit rc : remoteCm) { if (history.findById(rc.getId()) == null) { history.add(rc); commitIndex.insert(rc); markPushed(rc.getId()); count++; } }
        return count == 0 ? "Already up to date." : "Pulled " + count + " commit(s).";
    }

    java.util.List<String[]> getPushStatus(RemoteStore rs) {
        java.util.List<String[]> list = new java.util.ArrayList<>();
        for (Commit c : collectOldestFirst()) list.add(new String[]{isPushed(c.getId()) ? "PUSHED" : "LOCAL", c.getId(), c.getMessage(), c.getAuthor()});
        return list;
    }

    public String toString() { return name + " [" + activeBranch.name + "] (" + history.size() + " commits)"; }
}

// ================================================================
//  SECTION 12 — USER + USER LINKED LIST
// ================================================================
class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username, password, email;
    private Repository[] repos; private int repoCount;
    private static final int MAX_REPOS = 10;
    User next;
    User(String username, String password, String email) { this.username = username; this.password = password; this.email = email; this.repos = new Repository[MAX_REPOS]; this.repoCount = 0; }
    String getUsername() { return username; }
    String getPassword() { return password; }
    String getEmail()    { return email; }
    void setPassword(String p) { this.password = p; }
    void createRepo(String name) throws InvalidFileNameException {
        Validator.validateRepoName(name);
        if (findRepo(name) != null) throw new InvalidFileNameException("Repository '" + name + "' already exists.");
        if (repoCount >= MAX_REPOS) throw new InvalidFileNameException("Maximum repositories (10) reached.");
        repos[repoCount++] = new Repository(name, username);
    }
    Repository findRepo(String name) { for (int i = 0; i < repoCount; i++) if (repos[i].getName().equals(name)) return repos[i]; return null; }
    Repository[] getRepos() { Repository[] r = new Repository[repoCount]; for (int i = 0; i < repoCount; i++) r[i] = repos[i]; return r; }
    public String toString() { return username + " (" + email + ")"; }
}

class UserList implements Serializable {
    private static final long serialVersionUID = 1L;
    private User head;
    void add(User u) { u.next = head; head = u; }
    boolean exists(String un) { return find(un) != null; }
    User find(String username) { User curr = head; while (curr != null) { if (curr.getUsername().equals(username)) return curr; curr = curr.next; } return null; }
    User login(String username, String password) throws UserNotFoundException, WrongPasswordException {
        User u = find(username); if (u == null) throw new UserNotFoundException("No account found with username '" + username + "'.");
        if (!u.getPassword().equals(password)) throw new WrongPasswordException("Incorrect password. Please try again.");
        return u;
    }
    java.util.List<User> getAll() { java.util.List<User> list = new java.util.ArrayList<>(); User curr = head; while (curr != null) { list.add(curr); curr = curr.next; } return list; }
}

// ================================================================
//  SECTION 13 — MAIN APPLICATION (FIXED SIDEBAR)
// ================================================================
public class MainApplication extends Application {

    // ── State (static so sidebar always reads latest value) ──
    static UserList    users       = new UserList();
    static User        currentUser = null;
    static Repository  currentRepo = null;  // ← this is the key variable
    static RemoteStore remoteStore = new RemoteStore();
    static final String SAVE_FILE  = "simplegit_data.ser";

    // Colors
    static final String BG_DARK    = "#0d1117";
    static final String BG_CARD    = "#161b22";
    static final String BG_SIDEBAR = "#010409";
    static final String ACCENT     = "#238636";
    static final String ACCENT_HVR = "#2ea043";
    static final String BORDER     = "#30363d";
    static final String TEXT_PRI   = "#e6edf3";
    static final String TEXT_SEC   = "#8b949e";
    static final String BLUE       = "#58a6ff";
    static final String RED        = "#f85149";
    static final String ORANGE     = "#d29922";

    Stage      primaryStage;
    BorderPane root;
    StackPane  contentArea;
    Label      statusBar;

    // ── Save / Load ──────────────────────────────────────────
    static void saveData() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(SAVE_FILE))) {
            out.writeObject(users); out.writeObject(remoteStore);
        } catch (IOException e) { System.out.println("Save failed: " + e.getMessage()); }
    }

    static void loadData() {
        File f = new File(SAVE_FILE); if (!f.exists()) return;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(SAVE_FILE))) {
            users = (UserList) in.readObject(); remoteStore = (RemoteStore) in.readObject();
        } catch (Exception e) { users = new UserList(); remoteStore = new RemoteStore(); }
    }

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        loadData();
        primaryStage = stage;
        primaryStage.setTitle("SimpleGit — DSA Version Control");
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
        showLoginScreen();
    }

    // ================================================================
    //  STYLING HELPERS
    // ================================================================
    Button styledBtn(String text, String bg) {
        Button b = new Button(text);
        String base = "-fx-background-color:" + bg + ";-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:13px;-fx-padding:8 18;-fx-background-radius:6;-fx-cursor:hand;-fx-font-family:'Consolas';";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color:" + (bg.equals(ACCENT) ? ACCENT_HVR : "#3a4048") + ";-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:13px;-fx-padding:8 18;-fx-background-radius:6;-fx-cursor:hand;-fx-font-family:'Consolas';"));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }

    TextField styledField(String prompt) {
        TextField tf = new TextField(); tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color:#21262d;-fx-text-fill:" + TEXT_PRI + ";-fx-prompt-text-fill:" + TEXT_SEC + ";-fx-border-color:" + BORDER + ";-fx-border-radius:6;-fx-background-radius:6;-fx-padding:8;-fx-font-size:13px;");
        return tf;
    }

    PasswordField styledPass(String prompt) {
        PasswordField pf = new PasswordField(); pf.setPromptText(prompt);
        pf.setStyle("-fx-background-color:#21262d;-fx-text-fill:" + TEXT_PRI + ";-fx-prompt-text-fill:" + TEXT_SEC + ";-fx-border-color:" + BORDER + ";-fx-border-radius:6;-fx-background-radius:6;-fx-padding:8;-fx-font-size:13px;");
        return pf;
    }

    Label heading(String text) { Label l = new Label(text); l.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:22px;-fx-font-weight:bold;-fx-font-family:'Consolas';"); return l; }
    Label subLabel(String text) { Label l = new Label(text); l.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:12px;-fx-font-family:'Consolas';"); return l; }
    Label fieldLabel(String text) { Label l = new Label(text); l.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:13px;-fx-font-family:'Consolas';"); return l; }
    Label hintLabel(String text) { Label l = new Label(text); l.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:11px;-fx-font-family:'Consolas';"); l.setWrapText(true); return l; }

    Label errorLabel() { Label l = new Label(""); l.setStyle("-fx-text-fill:" + RED + ";-fx-font-size:12px;-fx-font-family:'Consolas';"); l.setWrapText(true); return l; }

    void showError(Label l, String msg) { l.setStyle("-fx-text-fill:" + RED + ";-fx-font-size:12px;-fx-font-family:'Consolas';"); l.setText("✗  " + msg); }
    void showSuccess(Label l, String msg) { l.setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-size:12px;-fx-font-family:'Consolas';"); l.setText("✓  " + msg); }

    void showAlert(String title, String msg, boolean error) {
        Alert a = new Alert(error ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.getDialogPane().setStyle("-fx-background-color:" + BG_CARD + ";"); a.showAndWait();
    }

    void setStatus(String msg) { if (statusBar != null) statusBar.setText("  " + msg); }
    void setContent(javafx.scene.Node node) { contentArea.getChildren().clear(); contentArea.getChildren().add(node); }

    // ================================================================
    //  LOGIN SCREEN
    // ================================================================
    void showLoginScreen() {
        VBox box = new VBox(20); box.setAlignment(Pos.CENTER); box.setStyle("-fx-background-color:" + BG_DARK + ";"); box.setPadding(new Insets(60));
        VBox logo = new VBox(5); logo.setAlignment(Pos.CENTER);
        Label icon = new Label("⬡"); icon.setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-size:48px;");
        Label title = new Label("SimpleGit"); title.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:32px;-fx-font-weight:bold;-fx-font-family:'Consolas';");
        Label sub = new Label("DSA Version Control System"); sub.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:14px;-fx-font-family:'Consolas';");
        logo.getChildren().addAll(icon, title, sub);

        VBox card = new VBox(10); card.setAlignment(Pos.CENTER_LEFT); card.setPadding(new Insets(30)); card.setMaxWidth(400);
        card.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + BORDER + ";-fx-border-radius:10;-fx-background-radius:10;");
        Label loginTitle = new Label("Sign in"); loginTitle.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:20px;-fx-font-weight:bold;-fx-font-family:'Consolas';");
        TextField     userField = styledField("Username"); userField.setPrefWidth(340);
        PasswordField passField = styledPass("Password");  passField.setPrefWidth(340);
        Label errLabel = errorLabel();
        Button loginBtn = styledBtn("Sign in", ACCENT); loginBtn.setPrefWidth(340);
        Label orLabel = new Label("──── or ────"); orLabel.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-family:'Consolas';"); orLabel.setMaxWidth(340); orLabel.setAlignment(Pos.CENTER);
        Button registerBtn = styledBtn("Create new account", "#21262d"); registerBtn.setPrefWidth(340);

        loginBtn.setOnAction(e -> {
            errLabel.setText("");
            try {
                if (userField.getText().trim().isEmpty()) throw new InvalidUsernameException("Please enter your username.");
                if (passField.getText().isEmpty()) throw new InvalidPasswordException("Please enter your password.");
                User u = users.login(userField.getText().trim(), passField.getText());
                currentUser = u; currentRepo = null;
                showMainDashboard();
            } catch (UserNotFoundException | WrongPasswordException | InvalidUsernameException | InvalidPasswordException ex) {
                showError(errLabel, ex.getMessage());
            }
        });

        registerBtn.setOnAction(e -> showRegisterScreen());
        card.getChildren().addAll(loginTitle, fieldLabel("Username"), userField, fieldLabel("Password"), passField, loginBtn, orLabel, registerBtn, errLabel);
        box.getChildren().addAll(logo, card);
        primaryStage.setScene(new Scene(box, 1100, 700)); primaryStage.show();
    }

    // ================================================================
    //  REGISTER SCREEN
    // ================================================================
    void showRegisterScreen() {
        VBox box = new VBox(20); box.setAlignment(Pos.CENTER); box.setStyle("-fx-background-color:" + BG_DARK + ";"); box.setPadding(new Insets(40));
        VBox card = new VBox(8); card.setAlignment(Pos.CENTER_LEFT); card.setPadding(new Insets(30)); card.setMaxWidth(440);
        card.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + BORDER + ";-fx-border-radius:10;-fx-background-radius:10;");
        Label title = new Label("Create Account"); title.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:20px;-fx-font-weight:bold;-fx-font-family:'Consolas';");
        TextField unField = styledField("e.g. ali_coder"); unField.setPrefWidth(380);
        PasswordField pwField  = styledPass("Min 6 chars, include a number"); pwField.setPrefWidth(380);
        PasswordField pw2Field = styledPass("Re-enter password"); pw2Field.setPrefWidth(380);
        TextField emField = styledField("e.g. ali@gmail.com"); emField.setPrefWidth(380);
        Label unErr = errorLabel(), pwErr = errorLabel(), pw2Err = errorLabel(), emErr = errorLabel(), genMsg = errorLabel();
        Button regBtn  = styledBtn("Create account", ACCENT);  regBtn.setPrefWidth(380);
        Button backBtn = styledBtn("← Back to sign in", "#21262d"); backBtn.setPrefWidth(380);

        unField.focusedProperty().addListener((obs, old, f) -> { if (!f) { try { Validator.validateUsername(unField.getText().trim()); unErr.setText(""); } catch (InvalidUsernameException ex) { showError(unErr, ex.getMessage()); } } });
        pwField.focusedProperty().addListener((obs, old, f) -> { if (!f) { try { Validator.validatePassword(pwField.getText()); pwErr.setText(""); } catch (InvalidPasswordException ex) { showError(pwErr, ex.getMessage()); } } });
        pw2Field.focusedProperty().addListener((obs, old, f) -> { if (!f) { if (!pwField.getText().equals(pw2Field.getText())) showError(pw2Err, "Passwords do not match."); else pw2Err.setText(""); } });
        emField.focusedProperty().addListener((obs, old, f) -> { if (!f) { try { Validator.validateEmail(emField.getText().trim()); emErr.setText(""); } catch (InvalidEmailException ex) { showError(emErr, ex.getMessage()); } } });

        regBtn.setOnAction(e -> {
            genMsg.setText("");
            String un = unField.getText().trim(), pw = pwField.getText(), pw2 = pw2Field.getText(), em = emField.getText().trim();
            try {
                Validator.validateUsername(un);
                if (users.exists(un)) throw new InvalidUsernameException("Username '" + un + "' is already taken.");
                Validator.validatePassword(pw);
                if (!pw.equals(pw2)) throw new InvalidPasswordException("Passwords do not match.");
                Validator.validateEmail(em);
                users.add(new User(un, pw, em)); saveData();
                showSuccess(genMsg, "Account created! Redirecting...");
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.2));
                pause.setOnFinished(ev -> showLoginScreen()); pause.play();
            } catch (InvalidUsernameException ex) { showError(unErr, ex.getMessage()); showError(genMsg, "Fix errors above.");
            } catch (InvalidPasswordException ex) { showError(pwErr, ex.getMessage()); showError(genMsg, "Fix errors above.");
            } catch (InvalidEmailException    ex) { showError(emErr, ex.getMessage()); showError(genMsg, "Fix errors above."); }
        });

        backBtn.setOnAction(e -> showLoginScreen());
        card.getChildren().addAll(title,
            fieldLabel("Username"), unField, hintLabel("3–20 chars. Letters, numbers, underscore only."), unErr,
            fieldLabel("Password"), pwField, hintLabel("Min 6 chars. Must include 1 letter and 1 number."), pwErr,
            fieldLabel("Confirm Password"), pw2Field, pw2Err,
            fieldLabel("Email"), emField, hintLabel("Must contain '@' and a valid domain e.g. gmail.com"), emErr,
            regBtn, backBtn, genMsg);
        box.getChildren().add(card);
        primaryStage.setScene(new Scene(box, 1100, 700));
    }

    // ================================================================
    //  MAIN DASHBOARD — FIXED SIDEBAR
    // ================================================================
    void showMainDashboard() {
        root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG_DARK + ";");
        root.setTop(buildTopBar());

        // ── FIXED SIDEBAR ───────────────────────────────────────
        // Sidebar is rebuilt INSIDE each panel refresh so it always
        // reads the latest value of currentRepo
        VBox sb = new VBox(4);
        sb.setPrefWidth(200);
        sb.setPadding(new Insets(16, 8, 16, 8));
        sb.setStyle("-fx-background-color:" + BG_SIDEBAR + ";-fx-border-color:" + BORDER + ";-fx-border-width:0 1 0 0;");

        String[][] items = {{"🏠","Home"},{"📁","Repositories"},{"📄","Files"},{"📦","Staging"},{"🌿","Branches"},{"✅","Commits"},{"📊","Contributors"},{"☁️","Remote"}};

        for (String[] item : items) {
            Button btn = new Button(item[0] + "  " + item[1]);
            btn.setMaxWidth(Double.MAX_VALUE); btn.setAlignment(Pos.CENTER_LEFT);
            String normal = "-fx-background-color:transparent;-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:13px;-fx-padding:8 12;-fx-background-radius:6;-fx-cursor:hand;-fx-font-family:'Consolas';";
            String hover  = "-fx-background-color:#21262d;-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:13px;-fx-padding:8 12;-fx-background-radius:6;-fx-cursor:hand;-fx-font-family:'Consolas';";
            btn.setStyle(normal);
            btn.setOnMouseEntered(e -> btn.setStyle(hover));
            btn.setOnMouseExited(e  -> btn.setStyle(normal));

            String section = item[1];
            btn.setOnAction(e -> {
                // ── KEY FIX: read currentRepo at click time, not build time ──
                boolean needsRepo = section.equals("Files") || section.equals("Staging") ||
                                    section.equals("Branches") || section.equals("Commits") ||
                                    section.equals("Remote") || section.equals("Contributors");

                if (needsRepo && currentRepo == null) {
                    // Show helpful error AND navigate to repos page
                    showAlert("No Repository Selected",
                        "Please select a repository first.\n\nGo to Repositories → click 'Select' on your repo.", true);
                    showRepoPanel();
                    return;
                }

                switch (section) {
                    case "Home":         showHomePanel();         break;
                    case "Repositories": showRepoPanel();         break;
                    case "Files":        showFilesPanel();        break;
                    case "Staging":      showStagingPanel();      break;
                    case "Branches":     showBranchPanel();       break;
                    case "Commits":      showCommitsPanel();      break;
                    case "Contributors": showContributorsPanel(); break;
                    case "Remote":       showRemotePanel();       break;
                }
            });
            sb.getChildren().add(btn);
        }

        root.setLeft(sb);

        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color:" + BG_DARK + ";");
        root.setCenter(contentArea);

        statusBar = new Label("  Ready — select a repository to begin");
        statusBar.setStyle("-fx-background-color:#010409;-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:11px;-fx-font-family:'Consolas';-fx-padding:4 0;");
        statusBar.setMaxWidth(Double.MAX_VALUE);
        root.setBottom(statusBar);

        showRepoPanel(); // ← open Repositories page first so user selects repo immediately
        primaryStage.setScene(new Scene(root, 1100, 700));
    }

    HBox buildTopBar() {
        HBox bar = new HBox(12); bar.setAlignment(Pos.CENTER_LEFT); bar.setPadding(new Insets(10, 20, 10, 20));
        bar.setStyle("-fx-background-color:" + BG_SIDEBAR + ";-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;");
        Label logo = new Label("⬡ SimpleGit"); logo.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:16px;-fx-font-weight:bold;-fx-font-family:'Consolas';");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label userInfo = new Label("👤 " + currentUser.getUsername()); userInfo.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:13px;-fx-font-family:'Consolas';");
        Button logoutBtn = styledBtn("Sign out", "#21262d");
        logoutBtn.setOnAction(e -> { saveData(); currentUser = null; currentRepo = null; showLoginScreen(); });
        bar.getChildren().addAll(logo, spacer, userInfo, logoutBtn); return bar;
    }

    // ================================================================
    //  HOME PANEL
    // ================================================================
    void showHomePanel() {
        ScrollPane scroll = new ScrollPane(); scroll.setStyle("-fx-background-color:" + BG_DARK + ";-fx-background:" + BG_DARK + ";"); scroll.setFitToWidth(true);
        VBox panel = new VBox(20); panel.setPadding(new Insets(30)); panel.setStyle("-fx-background-color:" + BG_DARK + ";");
        panel.getChildren().add(heading("Welcome, " + currentUser.getUsername() + " 👋"));
        panel.getChildren().add(subLabel("Select a repository from the sidebar to get started."));

        HBox stats = new HBox(16);
        Repository[] repos = currentUser.getRepos();
        int totalCommits = 0; for (Repository r : repos) totalCommits += r.history.size();
        stats.getChildren().addAll(
            statCard("📁", String.valueOf(repos.length), "Repositories"),
            statCard("✅", String.valueOf(totalCommits), "Total Commits"),
            statCard("🌿", currentRepo == null ? "none" : currentRepo.getActiveBranch(), "Active Branch"),
            statCard("📦", currentRepo == null ? "0" : String.valueOf(currentRepo.stagedCount), "Staged Files")
        );
        panel.getChildren().add(stats);

        if (currentRepo != null) {
            Label activeLabel = new Label("✓  Active repo: " + currentRepo.getName() + "  [" + currentRepo.getActiveBranch() + "]");
            activeLabel.setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-size:14px;-fx-font-family:'Consolas';");
            panel.getChildren().add(activeLabel);
        }

        scroll.setContent(panel); setContent(scroll); setStatus("Home");
    }

    VBox statCard(String icon, String value, String label) {
        VBox card = new VBox(4); card.setAlignment(Pos.CENTER); card.setPadding(new Insets(20)); card.setPrefWidth(180);
        card.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + BORDER + ";-fx-border-radius:8;-fx-background-radius:8;");
        Label ic = new Label(icon); ic.setStyle("-fx-font-size:24px;");
        Label val = new Label(value); val.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:22px;-fx-font-weight:bold;-fx-font-family:'Consolas';");
        Label lbl = new Label(label); lbl.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:12px;-fx-font-family:'Consolas';");
        card.getChildren().addAll(ic, val, lbl); return card;
    }

    // ================================================================
    //  REPOSITORIES PANEL — FIXED select logic
    // ================================================================
    void showRepoPanel() {
        VBox panel = new VBox(20); panel.setPadding(new Insets(30)); panel.setStyle("-fx-background-color:" + BG_DARK + ";");
        HBox header = new HBox(12); header.setAlignment(Pos.CENTER_LEFT);
        Label title = heading("Repositories");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        TextField nameField = styledField("my-project"); nameField.setPrefWidth(220);
        Label repoErr = errorLabel();
        Button createBtn = styledBtn("+ New Repository", ACCENT);
        createBtn.setOnAction(e -> {
            repoErr.setText("");
            try { String name = nameField.getText().trim(); currentUser.createRepo(name); saveData(); nameField.clear(); showSuccess(repoErr, "Repository created!"); showRepoPanel(); }
            catch (InvalidFileNameException ex) { showError(repoErr, ex.getMessage()); }
        });
        header.getChildren().addAll(title, sp, nameField, createBtn);

        // Show currently selected repo info
        if (currentRepo != null) {
            Label activeInfo = new Label("▶  Currently working in: " + currentRepo.getName() + " [" + currentRepo.getActiveBranch() + "]");
            activeInfo.setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-size:13px;-fx-font-family:'Consolas';-fx-padding:0 0 8 0;");
            panel.getChildren().addAll(header, activeInfo, repoErr);
        } else {
            Label hint = new Label("⚠  Select a repository below to start working");
            hint.setStyle("-fx-text-fill:" + ORANGE + ";-fx-font-size:13px;-fx-font-family:'Consolas';");
            panel.getChildren().addAll(header, hint, repoErr);
        }

        VBox list = new VBox(10);
        Repository[] repos = currentUser.getRepos();
        if (repos.length == 0) {
            list.getChildren().add(subLabel("No repositories yet. Create one above."));
        } else {
            for (Repository r : repos) {
                HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(14, 16, 14, 16));
                // Highlight selected repo with green border
                boolean isActive = currentRepo != null && currentRepo.getName().equals(r.getName());
                row.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + (isActive ? ACCENT : BORDER) + ";-fx-border-radius:8;-fx-background-radius:8;-fx-border-width:2;");

                VBox info = new VBox(3);
                Label name = new Label("📁 " + r.getName()); name.setStyle("-fx-text-fill:" + BLUE + ";-fx-font-size:15px;-fx-font-weight:bold;-fx-font-family:'Consolas';");
                Label meta = new Label("Owner: " + r.getOwnerUsername() + "  ·  " + r.history.size() + " commits  ·  branch: " + r.getActiveBranch());
                meta.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:12px;-fx-font-family:'Consolas';");
                info.getChildren().addAll(name, meta);
                Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

                Button selBtn = styledBtn(isActive ? "✓ Selected" : "Select", isActive ? ACCENT : "#21262d");
                selBtn.setOnAction(ev -> {
                    currentRepo = r;                         // ← set global currentRepo
                    setStatus("Active repo: " + r.getName() + " [" + r.getActiveBranch() + "]");
                    showRepoPanel();                         // ← refresh to show green border
                });
                row.getChildren().addAll(info, spacer, selBtn);
                list.getChildren().add(row);
            }
        }
        panel.getChildren().add(list);
        ScrollPane scroll = new ScrollPane(panel); scroll.setStyle("-fx-background-color:" + BG_DARK + ";-fx-background:" + BG_DARK + ";"); scroll.setFitToWidth(true);
        setContent(scroll); setStatus(currentRepo == null ? "Select a repository to begin" : "Repo: " + currentRepo.getName());
    }

    // ================================================================
    //  FILES PANEL
    // ================================================================
    void showFilesPanel() {
        VBox panel = new VBox(20); panel.setPadding(new Insets(30)); panel.setStyle("-fx-background-color:" + BG_DARK + ";");
        panel.getChildren().add(heading("Files  —  " + currentRepo.getName() + " [" + currentRepo.getActiveBranch() + "]"));

        VBox createCard = new VBox(8); createCard.setPadding(new Insets(16));
        createCard.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + BORDER + ";-fx-border-radius:8;-fx-background-radius:8;");
        Label createTitle = new Label("✏️  Create / Edit File"); createTitle.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:14px;-fx-font-weight:bold;-fx-font-family:'Consolas';");
        TextField fnField = styledField("notes.txt");
        Label fnErr = errorLabel();
        TextArea contentArea2 = new TextArea(); contentArea2.setPromptText("File content..."); contentArea2.setPrefRowCount(5);
        contentArea2.setStyle("-fx-background-color:#21262d;-fx-text-fill:" + TEXT_PRI + ";-fx-font-family:'Consolas';-fx-font-size:13px;-fx-border-color:" + BORDER + ";-fx-border-radius:6;-fx-background-radius:6;");
        Button saveBtn = styledBtn("💾 Save File", ACCENT);
        saveBtn.setOnAction(e -> {
            fnErr.setText("");
            try { Validator.validateFilename(fnField.getText().trim()); currentRepo.writeFile(fnField.getText().trim(), contentArea2.getText()); saveData(); setStatus("Saved: " + fnField.getText().trim()); showSuccess(fnErr, "File saved!"); showFilesPanel(); }
            catch (InvalidFileNameException ex) { showError(fnErr, ex.getMessage()); }
        });
        createCard.getChildren().addAll(createTitle, fieldLabel("Filename"), fnField, hintLabel("Must have extension e.g. .txt .java .py — no spaces"), fnErr, fieldLabel("Content"), contentArea2, saveBtn);

        HBox ioRow = new HBox(12);
        VBox importCard = new VBox(8); importCard.setPadding(new Insets(16)); importCard.setPrefWidth(400);
        importCard.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + BORDER + ";-fx-border-radius:8;-fx-background-radius:8;");
        Label importTitle = new Label("📥  Import from Computer"); importTitle.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:14px;-fx-font-weight:bold;-fx-font-family:'Consolas';");
        TextField importPath = styledField("C:\\Users\\Ali\\Desktop\\notes.txt");
        Label importErr = errorLabel();
        Button importBtn = styledBtn("Import File", "#21262d");
        importBtn.setOnAction(e -> {
            importErr.setText("");
            String path = importPath.getText().trim();
            if (path.isEmpty()) { showError(importErr, "File path cannot be empty."); return; }
            try { currentRepo.importFile(path); saveData(); showSuccess(importErr, "Imported!"); showFilesPanel(); }
            catch (FileNotFoundException2 | IOException ex) { showError(importErr, ex.getMessage()); }
        });
        importCard.getChildren().addAll(importTitle, fieldLabel("File path"), importPath, importBtn, importErr);

        VBox exportCard = new VBox(8); exportCard.setPadding(new Insets(16)); exportCard.setPrefWidth(400);
        exportCard.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + BORDER + ";-fx-border-radius:8;-fx-background-radius:8;");
        Label exportTitle = new Label("📤  Export File"); exportTitle.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:14px;-fx-font-weight:bold;-fx-font-family:'Consolas';");
        TextField exportFn = styledField("notes.txt"); TextField exportDest = styledField("C:\\Users\\Ali\\Desktop");
        Label exportErr = errorLabel();
        Button exportBtn = styledBtn("Export File", "#21262d");
        exportBtn.setOnAction(e -> {
            exportErr.setText("");
            try { if (exportFn.getText().trim().isEmpty()) throw new FileNotFoundException2("Filename cannot be empty."); currentRepo.exportFile(exportFn.getText().trim(), exportDest.getText().trim()); showSuccess(exportErr, "Exported!"); }
            catch (FileNotFoundException2 | IOException ex) { showError(exportErr, ex.getMessage()); }
        });
        exportCard.getChildren().addAll(exportTitle, fieldLabel("Filename"), exportFn, fieldLabel("Destination"), exportDest, exportBtn, exportErr);
        ioRow.getChildren().addAll(importCard, exportCard);

        Label wfTitle = new Label("📂  Working Files (HashMap)"); wfTitle.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:15px;-fx-font-weight:bold;-fx-font-family:'Consolas';");
        VBox fileList = new VBox(6);
        String[] keys = currentRepo.workingFiles.keys();
        if (keys == null || currentRepo.workingFiles.size() == 0) {
            fileList.getChildren().add(subLabel("No files yet. Create or import a file above."));
        } else {
            for (String key : keys) {
                if (key == null) continue;
                FileEntry fe = currentRepo.workingFiles.get(key);
                HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(10, 14, 10, 14));
                row.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + BORDER + ";-fx-border-radius:6;-fx-background-radius:6;");
                Label fname = new Label("📄 " + key); fname.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:13px;-fx-font-family:'Consolas';");
                Label fsize = new Label(fe.getContent().length() + " chars"); fsize.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:12px;-fx-font-family:'Consolas';");
                Region spr = new Region(); HBox.setHgrow(spr, Priority.ALWAYS);
                Button viewBtn = styledBtn("View", "#21262d");
                viewBtn.setOnAction(ev -> { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(key); a.setHeaderText(null); TextArea ta = new TextArea(fe.getContent()); ta.setEditable(false); ta.setStyle("-fx-font-family:'Consolas';"); a.getDialogPane().setContent(ta); a.getDialogPane().setPrefSize(500, 300); a.showAndWait(); });
                row.getChildren().addAll(fname, spr, fsize, viewBtn);
                fileList.getChildren().add(row);
            }
        }
        panel.getChildren().addAll(createCard, ioRow, wfTitle, fileList);
        ScrollPane scroll = new ScrollPane(panel); scroll.setStyle("-fx-background-color:" + BG_DARK + ";-fx-background:" + BG_DARK + ";"); scroll.setFitToWidth(true);
        setContent(scroll); setStatus("Files — " + currentRepo.workingFiles.size() + " file(s) in working area");
    }

    // ================================================================
    //  STAGING PANEL
    // ================================================================
    void showStagingPanel() {
        VBox panel = new VBox(20); panel.setPadding(new Insets(30)); panel.setStyle("-fx-background-color:" + BG_DARK + ";");
        panel.getChildren().add(heading("Staging  —  " + currentRepo.getName()));

        HBox controls = new HBox(10); controls.setAlignment(Pos.CENTER_LEFT);
        ComboBox<String> fileCombo = new ComboBox<>();
        fileCombo.setStyle("-fx-background-color:#21262d;-fx-text-fill:" + TEXT_PRI + ";-fx-font-family:'Consolas';");
        fileCombo.setPromptText("Select file...");
        String[] keys = currentRepo.workingFiles.keys();
        if (keys != null) for (String k : keys) if (k != null) fileCombo.getItems().add(k);

        Label stageErr = errorLabel();
        Button stageBtn   = styledBtn("+ Stage",    ACCENT);
        Button unstageBtn = styledBtn("− Unstage",  "#21262d");
        Button undoBtn    = styledBtn("↩ Undo",     "#21262d");
        Button redoBtn    = styledBtn("↪ Redo",     "#21262d");

        stageBtn.setOnAction(e -> {
            stageErr.setText("");
            String f = fileCombo.getValue();
            if (f == null) { showError(stageErr, "Select a file first."); return; }
            try { currentRepo.stageFile(f); saveData(); setStatus("Staged: " + f); showStagingPanel(); }
            catch (FileNotFoundException2 ex) { showError(stageErr, ex.getMessage()); }
        });
        unstageBtn.setOnAction(e -> {
            stageErr.setText("");
            String f = fileCombo.getValue();
            if (f == null) { showError(stageErr, "Select a file first."); return; }
            try { currentRepo.unstageFile(f); saveData(); showStagingPanel(); }
            catch (FileNotFoundException2 ex) { showError(stageErr, ex.getMessage()); }
        });
        undoBtn.setOnAction(e -> { currentRepo.undoStage(); saveData(); showStagingPanel(); });
        redoBtn.setOnAction(e -> { currentRepo.redoStage(); saveData(); showStagingPanel(); });
        controls.getChildren().addAll(fileCombo, stageBtn, unstageBtn, undoBtn, redoBtn);

        Label stagedTitle = new Label("📦  Staged Files (" + currentRepo.stagedCount + ")");
        stagedTitle.setStyle("-fx-text-fill:" + ORANGE + ";-fx-font-size:15px;-fx-font-weight:bold;-fx-font-family:'Consolas';");
        VBox stagedList = new VBox(6);
        if (currentRepo.stagedCount == 0) {
            stagedList.getChildren().add(subLabel("Nothing staged. Select a file above and click + Stage."));
        } else {
            for (int i = 0; i < currentRepo.stagedCount; i++) {
                FileEntry fe = currentRepo.stagedFiles[i];
                HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(10, 14, 10, 14));
                row.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + ORANGE + ";-fx-border-radius:6;-fx-background-radius:6;");
                Label dot = new Label("●"); dot.setStyle("-fx-text-fill:" + ORANGE + ";");
                Label fname = new Label(fe.getFilename()); fname.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:13px;-fx-font-family:'Consolas';");
                row.getChildren().addAll(dot, fname);
                stagedList.getChildren().add(row);
            }
        }
        panel.getChildren().addAll(controls, stageErr, stagedTitle, stagedList);
        ScrollPane scroll = new ScrollPane(panel); scroll.setStyle("-fx-background-color:" + BG_DARK + ";-fx-background:" + BG_DARK + ";"); scroll.setFitToWidth(true);
        setContent(scroll); setStatus("Staging — " + currentRepo.stagedCount + " file(s) staged");
    }

    // ================================================================
    //  BRANCHES PANEL
    // ================================================================
    void showBranchPanel() {
        VBox panel = new VBox(20); panel.setPadding(new Insets(30)); panel.setStyle("-fx-background-color:" + BG_DARK + ";");
        panel.getChildren().addAll(heading("Branches  —  " + currentRepo.getName()), new Label("Active: " + currentRepo.getActiveBranch()) {{ setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-family:'Consolas';"); }});

        HBox createRow = new HBox(10); createRow.setAlignment(Pos.CENTER_LEFT);
        TextField branchName = styledField("feature-login"); branchName.setPrefWidth(220);
        Label branchErr = errorLabel();
        Button createBtn = styledBtn("+ Create Branch", ACCENT);
        createBtn.setOnAction(e -> { branchErr.setText(""); try { currentRepo.createBranch(branchName.getText().trim()); saveData(); showSuccess(branchErr, "Branch created!"); showBranchPanel(); } catch (BranchNotFoundException ex) { showError(branchErr, ex.getMessage()); } });
        createRow.getChildren().addAll(fieldLabel("Branch name:"), branchName, createBtn);

        HBox actionRow = new HBox(10); actionRow.setAlignment(Pos.CENTER_LEFT);
        ComboBox<String> branchCombo = new ComboBox<>();
        branchCombo.setStyle("-fx-background-color:#21262d;-fx-text-fill:" + TEXT_PRI + ";-fx-font-family:'Consolas';");
        branchCombo.setPromptText("Select branch...");
        for (String bn : currentRepo.branches.getNames()) branchCombo.getItems().add(bn);
        Label actionErr = errorLabel();
        Button switchBtn = styledBtn("⇄ Switch", "#21262d");
        Button mergeBtn  = styledBtn("⊕ Merge into " + currentRepo.getActiveBranch(), "#21262d");
        switchBtn.setOnAction(e -> { actionErr.setText(""); String b = branchCombo.getValue(); if (b == null) { showError(actionErr, "Select a branch."); return; } try { String r = currentRepo.switchBranch(b); saveData(); setStatus(r); showBranchPanel(); } catch (BranchNotFoundException ex) { showError(actionErr, ex.getMessage()); } });
        mergeBtn.setOnAction(e -> { actionErr.setText(""); String b = branchCombo.getValue(); if (b == null) { showError(actionErr, "Select a branch."); return; } try { String r = currentRepo.mergeBranch(b, currentUser.getUsername()); saveData(); showAlert("Merge", r, false); showBranchPanel(); } catch (BranchNotFoundException | EmptyCommitException ex) { showError(actionErr, ex.getMessage()); } });
        actionRow.getChildren().addAll(fieldLabel("Branch:"), branchCombo, switchBtn, mergeBtn);

        VBox branchList = new VBox(8);
        for (String bn : currentRepo.branches.getNames()) {
            Branch b = currentRepo.branches.find(bn);
            boolean isActive = bn.equals(currentRepo.getActiveBranch());
            HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(12, 16, 12, 16));
            row.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + (isActive ? ACCENT : BORDER) + ";-fx-border-radius:8;-fx-background-radius:8;");
            Label dot   = new Label(isActive ? "●" : "○"); dot.setStyle("-fx-text-fill:" + (isActive ? ACCENT : TEXT_SEC) + ";-fx-font-size:16px;");
            Label bname = new Label(bn + (isActive ? "  ← HEAD" : "")); bname.setStyle("-fx-text-fill:" + (isActive ? ACCENT : TEXT_PRI) + ";-fx-font-size:14px;-fx-font-weight:bold;-fx-font-family:'Consolas';");
            Label tip   = new Label(b.head == null ? "no commits" : b.head.getId() + " - " + b.head.getMessage()); tip.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:12px;-fx-font-family:'Consolas';");
            Region spr = new Region(); HBox.setHgrow(spr, Priority.ALWAYS);
            row.getChildren().addAll(dot, bname, spr, tip);
            branchList.getChildren().add(row);
        }
        panel.getChildren().addAll(createRow, branchErr, actionRow, actionErr, new Label("🌿  All Branches") {{ setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:15px;-fx-font-weight:bold;-fx-font-family:'Consolas';"); }}, branchList);
        ScrollPane scroll = new ScrollPane(panel); scroll.setStyle("-fx-background-color:" + BG_DARK + ";-fx-background:" + BG_DARK + ";"); scroll.setFitToWidth(true);
        setContent(scroll); setStatus("Branches — " + currentRepo.branches.size + " branch(es)");
    }

    // ================================================================
    //  COMMITS PANEL
    // ================================================================
    void showCommitsPanel() {
        VBox panel = new VBox(20); panel.setPadding(new Insets(30)); panel.setStyle("-fx-background-color:" + BG_DARK + ";");
        panel.getChildren().add(heading("Commits  —  " + currentRepo.getName() + " [" + currentRepo.getActiveBranch() + "]"));

        // Make commit card
        VBox commitCard = new VBox(8); commitCard.setPadding(new Insets(16));
        commitCard.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + (currentRepo.stagedCount > 0 ? ACCENT : BORDER) + ";-fx-border-radius:8;-fx-background-radius:8;");
        Label commitTitle = new Label("✅  Make a Commit  [" + currentRepo.stagedCount + " file(s) staged]");
        commitTitle.setStyle("-fx-text-fill:" + (currentRepo.stagedCount > 0 ? ACCENT : TEXT_SEC) + ";-fx-font-size:14px;-fx-font-weight:bold;-fx-font-family:'Consolas';");
        TextField msgField = styledField("e.g. Add login feature");
        Label commitErr = errorLabel();
        Button commitBtn = styledBtn(currentRepo.stagedCount > 0 ? "Commit" : "Stage files first →", currentRepo.stagedCount > 0 ? ACCENT : "#3a4048");
        commitBtn.setOnAction(e -> {
            commitErr.setText("");
            if (currentRepo.stagedCount == 0) { showError(commitErr, "Nothing staged. Go to Staging and stage files first."); return; }
            try { String result = currentRepo.commit(msgField.getText().trim(), currentUser.getUsername()); saveData(); msgField.clear(); setStatus(result); showSuccess(commitErr, result); showCommitsPanel(); }
            catch (EmptyCommitException | EmptyCommitMessageException ex) { showError(commitErr, ex.getMessage()); }
        });
        commitCard.getChildren().addAll(commitTitle, fieldLabel("Commit message"), msgField, hintLabel("3–100 characters. Describe what changed."), commitErr, commitBtn);
        if (currentRepo.stagedCount == 0) commitCard.getChildren().add(subLabel("→ Go to Staging first to stage files before committing."));

        // Tools row
        HBox toolsRow = new HBox(10); toolsRow.setAlignment(Pos.CENTER_LEFT);
        Label toolsErr = errorLabel(); toolsErr.setPrefWidth(Double.MAX_VALUE);
        TextField searchField = styledField("c001"); searchField.setPrefWidth(90);
        Button searchBtn = styledBtn("🔍 BST Search", "#21262d");
        searchBtn.setOnAction(e -> { toolsErr.setText(""); try { Commit c = currentRepo.searchCommit(searchField.getText().trim()); showCommitDetail(c); } catch (CommitNotFoundException ex) { showError(toolsErr, ex.getMessage()); } });
        TextField tagId = styledField("c001"); tagId.setPrefWidth(65);
        TextField tagVal = styledField("v1.0"); tagVal.setPrefWidth(65);
        Button tagBtn = styledBtn("🏷 Tag", "#21262d");
        tagBtn.setOnAction(e -> { toolsErr.setText(""); try { if (tagVal.getText().trim().isEmpty()) throw new CommitNotFoundException("Tag label cannot be empty."); currentRepo.tagCommit(tagId.getText().trim(), tagVal.getText().trim()); saveData(); showSuccess(toolsErr, "Tagged!"); showCommitsPanel(); } catch (CommitNotFoundException ex) { showError(toolsErr, ex.getMessage()); } });
        TextField diffId1 = styledField("c001"); diffId1.setPrefWidth(60); TextField diffId2 = styledField("c002"); diffId2.setPrefWidth(60); TextField diffFn = styledField("file.txt"); diffFn.setPrefWidth(90);
        Button diffBtn = styledBtn("⟷ Diff", "#21262d");
        diffBtn.setOnAction(e -> { toolsErr.setText(""); try { if (diffFn.getText().trim().isEmpty()) throw new FileNotFoundException2("Filename required."); String result = currentRepo.diff(diffId1.getText().trim(), diffId2.getText().trim(), diffFn.getText().trim()); Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle("Diff"); a.setHeaderText(null); TextArea ta = new TextArea(result); ta.setEditable(false); ta.setStyle("-fx-font-family:'Consolas';"); a.getDialogPane().setContent(ta); a.getDialogPane().setPrefSize(560, 360); a.showAndWait(); } catch (CommitNotFoundException | FileNotFoundException2 ex) { showError(toolsErr, ex.getMessage()); } });
        TextField checkoutId = styledField("c001"); checkoutId.setPrefWidth(70);
        Button checkoutBtn = styledBtn("⏮ Checkout", "#21262d");
        checkoutBtn.setOnAction(e -> { toolsErr.setText(""); try { String r = currentRepo.checkout(checkoutId.getText().trim()); saveData(); setStatus(r); showSuccess(toolsErr, r); showCommitsPanel(); } catch (CommitNotFoundException ex) { showError(toolsErr, ex.getMessage()); } });
        toolsRow.getChildren().addAll(searchField, searchBtn, tagId, tagVal, tagBtn, diffId1, diffId2, diffFn, diffBtn, checkoutId, checkoutBtn);

        Label histTitle = new Label("📜  Commit History"); histTitle.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:15px;-fx-font-weight:bold;-fx-font-family:'Consolas';");
        VBox commitList = new VBox(6);
        java.util.List<Commit> commits = currentRepo.getAllCommits();
        if (commits.isEmpty()) { commitList.getChildren().add(subLabel("No commits yet. Stage a file then commit.")); }
        else {
            for (Commit c : commits) {
                HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(10, 14, 10, 14));
                row.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + BORDER + ";-fx-border-radius:6;-fx-background-radius:6;-fx-cursor:hand;");
                Label cid = new Label(c.getId()); cid.setStyle("-fx-text-fill:" + BLUE + ";-fx-font-size:13px;-fx-font-weight:bold;-fx-font-family:'Consolas';-fx-min-width:50;");
                String tagStr = (c.getTag() != null && !c.getTag().isEmpty()) ? " 🏷 " + c.getTag() : "";
                Label cmsg = new Label(c.getMessage() + tagStr); cmsg.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:13px;-fx-font-family:'Consolas';");
                Region spr = new Region(); HBox.setHgrow(spr, Priority.ALWAYS);
                Label cauthor = new Label("👤 " + c.getAuthor()); cauthor.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:12px;-fx-font-family:'Consolas';");
                Label cbranch = new Label("🌿 " + c.getBranch()); cbranch.setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-size:12px;-fx-font-family:'Consolas';");
                Label ctime   = new Label(c.getTimestamp()); ctime.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:11px;-fx-font-family:'Consolas';");
                row.getChildren().addAll(cid, cmsg, spr, cauthor, cbranch, ctime);
                row.setOnMouseClicked(e -> showCommitDetail(c));
                commitList.getChildren().add(row);
            }
        }
        panel.getChildren().addAll(commitCard, toolsRow, toolsErr, histTitle, commitList);
        ScrollPane scroll = new ScrollPane(panel); scroll.setStyle("-fx-background-color:" + BG_DARK + ";-fx-background:" + BG_DARK + ";"); scroll.setFitToWidth(true);
        setContent(scroll); setStatus("Commits — " + commits.size() + " total | " + currentRepo.stagedCount + " staged");
    }

    void showCommitDetail(Commit c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle("Commit: " + c.getId()); a.setHeaderText(c.toString());
        StringBuilder sb = new StringBuilder("Files:\n"); for (int i = 0; i < c.getFileCount(); i++) sb.append("  • ").append(c.getFile(i).getFilename()).append("\n");
        TextArea ta = new TextArea(sb.toString()); ta.setEditable(false); ta.setStyle("-fx-font-family:'Consolas';");
        a.getDialogPane().setContent(ta); a.getDialogPane().setPrefSize(500, 260); a.showAndWait();
    }

    // ================================================================
    //  CONTRIBUTORS PANEL
    // ================================================================
    void showContributorsPanel() {
        VBox panel = new VBox(20); panel.setPadding(new Insets(30)); panel.setStyle("-fx-background-color:" + BG_DARK + ";");
        panel.getChildren().add(heading("Contributors  —  " + currentRepo.getName()));
        java.util.List<String[]> lb = currentRepo.contributors.getLeaderboard();
        String[] medals = {"🥇","🥈","🥉"};
        VBox list = new VBox(10);
        if (lb.isEmpty()) { list.getChildren().add(subLabel("No contributors yet. Make some commits first.")); }
        else { for (int i = 0; i < lb.size(); i++) { String[] entry = lb.get(i); HBox row = new HBox(16); row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(16,20,16,20)); row.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + BORDER + ";-fx-border-radius:10;-fx-background-radius:10;"); Label rank = new Label(i < 3 ? medals[i] : String.valueOf(i+1)); rank.setStyle("-fx-font-size:24px;"); Label name = new Label(entry[0]); name.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:16px;-fx-font-weight:bold;-fx-font-family:'Consolas';"); Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS); Label count = new Label(entry[1] + " commits"); count.setStyle("-fx-text-fill:" + BLUE + ";-fx-font-size:15px;-fx-font-weight:bold;-fx-font-family:'Consolas';"); row.getChildren().addAll(rank, name, sp, count); list.getChildren().add(row); } }
        panel.getChildren().add(list);
        ScrollPane scroll = new ScrollPane(panel); scroll.setStyle("-fx-background-color:" + BG_DARK + ";-fx-background:" + BG_DARK + ";"); scroll.setFitToWidth(true);
        setContent(scroll); setStatus("Contributors");
    }

    // ================================================================
    //  REMOTE PANEL
    // ================================================================
    void showRemotePanel() {
        VBox panel = new VBox(20); panel.setPadding(new Insets(30)); panel.setStyle("-fx-background-color:" + BG_DARK + ";");
        panel.getChildren().add(heading("Remote  —  " + currentRepo.getName()));
        HBox btnRow = new HBox(12); Label remoteErr = errorLabel();
        Button pushBtn = styledBtn("📤  Push to Remote", ACCENT);
        Button pullBtn = styledBtn("📥  Pull from Remote", "#21262d");
        pushBtn.setOnAction(e -> { remoteErr.setText(""); try { String r = currentRepo.push(remoteStore); saveData(); showAlert("Push", r, false); showRemotePanel(); } catch (EmptyCommitException ex) { showError(remoteErr, ex.getMessage()); } });
        pullBtn.setOnAction(e -> { remoteErr.setText(""); try { String r = currentRepo.pull(remoteStore); saveData(); showAlert("Pull", r, false); showRemotePanel(); } catch (EmptyCommitException ex) { showError(remoteErr, ex.getMessage()); } });
        btnRow.getChildren().addAll(pushBtn, pullBtn);
        Label statusTitle = new Label("📡  Push Status"); statusTitle.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:15px;-fx-font-weight:bold;-fx-font-family:'Consolas';");
        VBox statusList = new VBox(6);
        java.util.List<String[]> ps = currentRepo.getPushStatus(remoteStore);
        if (ps.isEmpty()) { statusList.getChildren().add(subLabel("No commits yet.")); }
        else { for (String[] entry : ps) { HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(10,14,10,14)); boolean pushed = entry[0].equals("PUSHED"); row.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + (pushed ? ACCENT : ORANGE) + ";-fx-border-radius:6;-fx-background-radius:6;"); Label status = new Label(pushed ? "✓ PUSHED" : "⬆ LOCAL"); status.setStyle("-fx-text-fill:" + (pushed ? ACCENT : ORANGE) + ";-fx-font-size:12px;-fx-font-weight:bold;-fx-font-family:'Consolas';-fx-min-width:80;"); Label cid = new Label(entry[1]); cid.setStyle("-fx-text-fill:" + BLUE + ";-fx-font-family:'Consolas';-fx-font-size:13px;"); Label cmsg = new Label(entry[2]); cmsg.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-family:'Consolas';-fx-font-size:13px;"); Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS); Label cauth = new Label(entry[3]); cauth.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-family:'Consolas';-fx-font-size:12px;"); row.getChildren().addAll(status, cid, cmsg, sp, cauth); statusList.getChildren().add(row); } }
        panel.getChildren().addAll(btnRow, remoteErr, statusTitle, statusList);
        ScrollPane scroll = new ScrollPane(panel); scroll.setStyle("-fx-background-color:" + BG_DARK + ";-fx-background:" + BG_DARK + ";"); scroll.setFitToWidth(true);
        setContent(scroll); setStatus("Remote — " + (remoteStore.exists(currentRepo.getName()) ? "Connected" : "Empty"));
    }
}