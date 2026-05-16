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
//  GitLite — DSA Semester Project (JavaFX GUI VERSION)
//  Data Structures:
//    1. Stack       : Undo/Redo staging actions
//    2. LinkedList  : Commit history, User list, Branch list
//    3. BST         : Fast commit search by ID
//    4. Custom Array: Staging area, repos
//    5. HashMap     : File tracking (filename → FileEntry)
// ============================================================


// ================================================================
//  SECTION 1 — CUSTOM EXCEPTIONS
// ================================================================

class InvalidUsernameException extends Exception {
    InvalidUsernameException(String m) {
        super(m);
    }
}

class InvalidPasswordException extends Exception {
    InvalidPasswordException(String m) {
        super(m);
    }
}

class InvalidEmailException extends Exception {
    InvalidEmailException(String m) {
        super(m);
    }
}

class UserNotFoundException extends Exception {
    UserNotFoundException(String m) {
        super(m);
    }
}

class WrongPasswordException extends Exception {
    WrongPasswordException(String m) {
        super(m);
    }
}

class RepositoryNotFoundException extends Exception {
    RepositoryNotFoundException(String m) {
        super(m);
    }
}

class EmptyCommitException extends Exception {
    EmptyCommitException(String m) {
        super(m);
    }
}

class EmptyCommitMessageException extends Exception {
    EmptyCommitMessageException(String m) {
        super(m);
    }
}

class FileNotFoundException2 extends Exception {
    FileNotFoundException2(String m) {
        super(m);
    }
}

class BranchNotFoundException extends Exception {
    BranchNotFoundException(String m) {
        super(m);
    }
}

class InvalidFileNameException extends Exception {
    InvalidFileNameException(String m) {
        super(m);
    }
}

class CommitNotFoundException extends Exception {
    CommitNotFoundException(String m) {
        super(m);
    }
}


// ================================================================
//  SECTION 2 — VALIDATOR
// ================================================================

class Validator {

    // Username: 3-20 chars, letters/numbers/underscore only
    static void validateUsername(String u) throws InvalidUsernameException {
        if (u == null || u.trim().isEmpty()) {
            throw new InvalidUsernameException("Username cannot be empty.");
        }
        if (u.length() < 3) {
            throw new InvalidUsernameException("Username must be at least 3 characters.");
        }
        if (u.length() > 20) {
            throw new InvalidUsernameException("Username cannot exceed 20 characters.");
        }
        for (char c : u.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_') {
                throw new InvalidUsernameException("Username can only contain letters, numbers, and underscores.");
            }
        }
    }

    // Password: min 6 chars, at least 1 letter and 1 number
    static void validatePassword(String p) throws InvalidPasswordException {
        if (p == null || p.isEmpty()) {
            throw new InvalidPasswordException("Password cannot be empty.");
        }
        if (p.length() < 6) {
            throw new InvalidPasswordException("Password must be at least 6 characters.");
        }
        if (p.length() > 30) {
            throw new InvalidPasswordException("Password cannot exceed 30 characters.");
        }
        boolean hasLetter = false;
        boolean hasDigit  = false;
        for (char c : p.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c))  hasDigit  = true;
        }
        if (!hasLetter) {
            throw new InvalidPasswordException("Password must contain at least one letter.");
        }
        if (!hasDigit) {
            throw new InvalidPasswordException("Password must contain at least one number.");
        }
    }

    // Email: must contain @ and a dot after @
    static void validateEmail(String e) throws InvalidEmailException {
        if (e == null || e.trim().isEmpty()) {
            throw new InvalidEmailException("Email cannot be empty.");
        }
        if (!e.contains("@")) {
            throw new InvalidEmailException("Email must contain '@'.");
        }
        int at = e.indexOf("@");
        if (at == 0) {
            throw new InvalidEmailException("Email must have characters before '@'.");
        }
        String after = e.substring(at + 1);
        if (!after.contains(".")) {
            throw new InvalidEmailException("Email domain must contain a dot (e.g. gmail.com).");
        }
        if (after.startsWith(".") || after.endsWith(".")) {
            throw new InvalidEmailException("Invalid email format.");
        }
        if (e.contains(" ")) {
            throw new InvalidEmailException("Email cannot contain spaces.");
        }
    }

    // Repo name: 2-30 chars, no spaces, letters/numbers/dash/underscore
    static void validateRepoName(String n) throws InvalidFileNameException {
        if (n == null || n.trim().isEmpty()) {
            throw new InvalidFileNameException("Repository name cannot be empty.");
        }
        if (n.length() < 2) {
            throw new InvalidFileNameException("Repo name must be at least 2 characters.");
        }
        if (n.length() > 30) {
            throw new InvalidFileNameException("Repo name cannot exceed 30 characters.");
        }
        if (n.contains(" ")) {
            throw new InvalidFileNameException("Repo name cannot contain spaces.");
        }
        for (char c : n.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') {
                throw new InvalidFileNameException("Repo name can only contain letters, numbers, - and _");
            }
        }
    }

    // Filename: must have extension, no spaces
    static void validateFilename(String f) throws InvalidFileNameException {
        if (f == null || f.trim().isEmpty()) {
            throw new InvalidFileNameException("Filename cannot be empty.");
        }
        if (!f.contains(".")) {
            throw new InvalidFileNameException("Filename must have an extension (e.g. notes.txt).");
        }
        if (f.contains(" ")) {
            throw new InvalidFileNameException("Filename cannot contain spaces.");
        }
        if (f.length() > 50) {
            throw new InvalidFileNameException("Filename too long (max 50 characters).");
        }
    }

    // Commit message: 3-100 chars
    static void validateCommitMessage(String m) throws EmptyCommitMessageException {
        if (m == null || m.trim().isEmpty()) {
            throw new EmptyCommitMessageException("Commit message cannot be empty.");
        }
        if (m.length() < 3) {
            throw new EmptyCommitMessageException("Commit message must be at least 3 characters.");
        }
        if (m.length() > 100) {
            throw new EmptyCommitMessageException("Commit message too long (max 100 characters).");
        }
    }

    // Branch name: no spaces, max 30 chars
    static void validateBranchName(String n) throws BranchNotFoundException {
        if (n == null || n.trim().isEmpty()) {
            throw new BranchNotFoundException("Branch name cannot be empty.");
        }
        if (n.contains(" ")) {
            throw new BranchNotFoundException("Branch name cannot contain spaces.");
        }
        if (n.length() > 30) {
            throw new BranchNotFoundException("Branch name too long (max 30 characters).");
        }
    }

    // Commit ID: must match pattern cXXX
    static void validateCommitId(String id) throws CommitNotFoundException {
        if (id == null || id.trim().isEmpty()) {
            throw new CommitNotFoundException("Commit ID cannot be empty.");
        }
        if (!id.matches("c\\d{3,}")) {
            throw new CommitNotFoundException("Invalid commit ID. Must be like c001, c002 etc.");
        }
    }
}


// ================================================================
//  SECTION 3 — STACK (Data Structure 1)
//  Used for: Undo/Redo staging actions
// ================================================================

class StackNode implements Serializable {
    private static final long serialVersionUID = 1L;
    String    data;
    StackNode next;

    StackNode(String data) {
        this.data = data;
    }
}

class MyStack implements Serializable {
    private static final long serialVersionUID = 1L;
    private StackNode top;

    // Push data onto stack
    void push(String data) {
        StackNode n = new StackNode(data);
        n.next = top;
        top    = n;
    }

    // Pop data from stack
    String pop() {
        if (top == null) {
            return null;
        }
        String d = top.data;
        top      = top.next;
        return d;
    }

    String  peek()    { return top == null ? null : top.data; }
    boolean isEmpty() { return top == null; }
    void    clear()   { top = null; }
}


// ================================================================
//  SECTION 4 — HASHMAP (Data Structure 5)
//  Used for: Working file tracking (filename → FileEntry)
//  Collision handling: chaining (linked list per bucket)
// ================================================================

class HashNode implements Serializable {
    private static final long serialVersionUID = 1L;
    String    key;
    FileEntry value;
    HashNode  next;

    HashNode(String key, FileEntry value) {
        this.key   = key;
        this.value = value;
    }
}

class MyHashMap implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int  BUCKETS = 16;
    private HashNode[] table;
    private int        size;

    MyHashMap() {
        table = new HashNode[BUCKETS];
    }

    // Hash function: sum of character values mod bucket count
    private int hash(String key) {
        int h = 0;
        for (char c : key.toCharArray()) {
            h += c;
        }
        return Math.abs(h % BUCKETS);
    }

    // PUT — insert or update key-value pair
    void put(String key, FileEntry value) {
        int      idx  = hash(key);
        HashNode curr = table[idx];

        // Check if key exists — update it
        while (curr != null) {
            if (curr.key.equals(key)) {
                curr.value = value;
                return;
            }
            curr = curr.next;
        }

        // Key not found — insert at front of chain
        HashNode n = new HashNode(key, value);
        n.next      = table[idx];
        table[idx]  = n;
        size++;
    }

    // GET — retrieve value by key
    FileEntry get(String key) {
        int      idx  = hash(key);
        HashNode curr = table[idx];

        while (curr != null) {
            if (curr.key.equals(key)) {
                return curr.value;
            }
            curr = curr.next;
        }
        return null;
    }

    // REMOVE — delete by key
    void remove(String key) {
        int      idx  = hash(key);
        HashNode curr = table[idx];
        HashNode prev = null;

        while (curr != null) {
            if (curr.key.equals(key)) {
                if (prev == null) {
                    table[idx] = curr.next;
                } else {
                    prev.next = curr.next;
                }
                size--;
                return;
            }
            prev = curr;
            curr = curr.next;
        }
    }

    boolean containsKey(String key) { return get(key) != null; }
    int     size()                  { return size; }

    // Return all keys as array
    String[] keys() {
        String[] result = new String[size];
        int      idx    = 0;

        for (HashNode bucket : table) {
            HashNode curr = bucket;
            while (curr != null) {
                result[idx++] = curr.key;
                curr          = curr.next;
            }
        }
        return result;
    }

    // Return all values as array
    FileEntry[] values() {
        FileEntry[] result = new FileEntry[size];
        int         idx    = 0;

        for (HashNode bucket : table) {
            HashNode curr = bucket;
            while (curr != null) {
                result[idx++] = curr.value;
                curr          = curr.next;
            }
        }
        return result;
    }
}


// ================================================================
//  SECTION 5 — CORE DATA CLASSES
// ================================================================

class FileEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private String filename;
    private String content;

    FileEntry(String filename, String content) {
        this.filename = filename;
        this.content  = content;
    }

    // Deep copy constructor — keeps commit snapshots independent
    FileEntry(FileEntry other) {
        this.filename = other.filename;
        this.content  = other.content;
    }

    String getFilename()              { return filename; }
    String getContent()               { return content; }
    void   setContent(String c)       { this.content = c; }

    public String toString() {
        return "[" + filename + "]\n" + content;
    }
}

class Commit implements Serializable {
    private static final long serialVersionUID = 1L;
    private String      id;
    private String      message;
    private String      author;
    private String      timestamp;
    private String      branch;
    private String      tag;
    private FileEntry[] files;
    private int         fileCount;
    Commit parent;               // LinkedList pointer to previous commit

    Commit(String id, String message, String author, String branch, FileEntry[] files, int fileCount) {
        this.id        = id;
        this.message   = message;
        this.author    = author;
        this.branch    = branch;
        this.tag       = "";
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        this.fileCount = fileCount;
        this.parent    = null;

        // Deep copy every file so future edits never corrupt this snapshot
        this.files = new FileEntry[fileCount];
        for (int i = 0; i < fileCount; i++) {
            this.files[i] = new FileEntry(files[i]);
        }
    }

    String    getId()        { return id; }
    String    getMessage()   { return message; }
    String    getAuthor()    { return author; }
    String    getTimestamp() { return timestamp; }
    String    getBranch()    { return branch; }
    String    getTag()       { return tag; }
    void      setTag(String t) { this.tag = t; }
    int       getFileCount() { return fileCount; }

    FileEntry getFile(int i) {
        return (i >= 0 && i < fileCount) ? files[i] : null;
    }

    FileEntry findFile(String filename) {
        for (int i = 0; i < fileCount; i++) {
            if (files[i].getFilename().equals(filename)) {
                return files[i];
            }
        }
        return null;
    }

    public String toString() {
        String t = (tag != null && !tag.isEmpty()) ? " <" + tag + ">" : "";
        return "* " + id + t + " - " + message + " (" + author + ") [" + branch + "] [" + timestamp + "]";
    }
}


// ================================================================
//  SECTION 6 — COMMIT LINKED LIST (Data Structure 2)
//  Used for: Commit history chain — newest commit at head
// ================================================================

class CommitList implements Serializable {
    private static final long serialVersionUID = 1L;
    Commit head;         // most recent commit
    private int size;

    // Add new commit at front (newest first)
    void add(Commit c) {
        c.parent = head;
        head     = c;
        size++;
    }

    // Linear search through chain to find commit by ID
    Commit findById(String id) {
        Commit curr = head;
        while (curr != null) {
            if (curr.getId().equals(id)) {
                return curr;
            }
            curr = curr.parent;
        }
        return null;
    }

    int size() { return size; }
}


// ================================================================
//  SECTION 7 — BST FOR COMMIT SEARCH (Data Structure 3)
//  Used for: Fast O(log n) commit lookup by ID
// ================================================================

class BSTNode implements Serializable {
    private static final long serialVersionUID = 1L;
    String  commitId;
    Commit  commit;
    BSTNode left;
    BSTNode right;

    BSTNode(Commit c) {
        this.commitId = c.getId();
        this.commit   = c;
    }
}

class CommitBST implements Serializable {
    private static final long serialVersionUID = 1L;
    private BSTNode root;

    // Recursive insert — smaller IDs go left, larger go right
    private BSTNode insert(BSTNode node, Commit c) {
        if (node == null) {
            return new BSTNode(c);
        }
        int cmp = c.getId().compareTo(node.commitId);
        if (cmp < 0) {
            node.left  = insert(node.left,  c);
        } else if (cmp > 0) {
            node.right = insert(node.right, c);
        }
        return node;
    }

    void insert(Commit c) {
        root = insert(root, c);
    }

    // Recursive search — O(log n) on balanced tree
    private BSTNode search(BSTNode node, String id) {
        if (node == null || node.commitId.equals(id)) {
            return node;
        }
        if (id.compareTo(node.commitId) < 0) {
            return search(node.left, id);
        } else {
            return search(node.right, id);
        }
    }

    Commit search(String id) {
        BSTNode n = search(root, id);
        return n == null ? null : n.commit;
    }

    // Inorder traversal: LEFT → ROOT → RIGHT = sorted order
    private void inorder(BSTNode node, java.util.List<Commit> list) {
        if (node == null) {
            return;
        }
        inorder(node.left, list);
        list.add(node.commit);
        inorder(node.right, list);
    }

    java.util.List<Commit> getSorted() {
        java.util.List<Commit> list = new java.util.ArrayList<>();
        inorder(root, list);
        return list;
    }
}


// ================================================================
//  SECTION 8 — BRANCH (LinkedList of branches)
// ================================================================

class Branch implements Serializable {
    private static final long serialVersionUID = 1L;
    String name;
    Commit head;    // tip of this branch's commit chain
    Branch next;    // LinkedList pointer to next branch

    Branch(String name) {
        this.name = name;
    }
}

class BranchList implements Serializable {
    private static final long serialVersionUID = 1L;
    Branch head;
    int    size;

    // Create new branch and add to list
    Branch create(String name) {
        if (find(name) != null) {
            return null;
        }
        Branch b = new Branch(name);
        b.next   = head;
        head     = b;
        size++;
        return b;
    }

    // Linear search through branch list
    Branch find(String name) {
        Branch curr = head;
        while (curr != null) {
            if (curr.name.equals(name)) {
                return curr;
            }
            curr = curr.next;
        }
        return null;
    }

    // Return all branch names as list
    java.util.List<String> getNames() {
        java.util.List<String> names = new java.util.ArrayList<>();
        Branch curr = head;
        while (curr != null) {
            names.add(curr.name);
            curr = curr.next;
        }
        return names;
    }
}


// ================================================================
//  SECTION 9 — REMOTE REPOSITORY (Push/Pull simulation)
// ================================================================

class RemoteRepo implements Serializable {
    private static final long serialVersionUID = 1L;
    String     name;
    CommitList commits;
    RemoteRepo next;

    // Feature 1: shared contributor tracker at REMOTE level
    // All push authors are recorded here — visible to all users
    ContributorTracker sharedContributors;

    RemoteRepo(String name) {
        this.name               = name;
        this.commits            = new CommitList();
        this.sharedContributors = new ContributorTracker();
    }
}

class RemoteStore implements Serializable {
    private static final long serialVersionUID = 1L;
    private RemoteRepo head;

    // Get existing remote repo or create new one
    RemoteRepo getOrCreate(String name) {
        RemoteRepo curr = head;
        while (curr != null) {
            if (curr.name.equals(name)) {
                return curr;
            }
            curr = curr.next;
        }
        RemoteRepo r = new RemoteRepo(name);
        r.next = head;
        head   = r;
        return r;
    }

    // Check if remote repo exists and has commits
    boolean exists(String name) {
        RemoteRepo curr = head;
        while (curr != null) {
            if (curr.name.equals(name)) {
                return curr.commits.head != null;
            }
            curr = curr.next;
        }
        return false;
    }

    // Feature 1: get shared contributors for a repo
    ContributorTracker getSharedContributors(String name) {
        RemoteRepo r = getOrCreate(name);
        return r.sharedContributors;
    }
}


// ================================================================
//  SECTION 10 — CONTRIBUTOR TRACKER (LinkedList + Bubble Sort)
// ================================================================

class ContribNode implements Serializable {
    private static final long serialVersionUID = 1L;
    String      author;
    int         commitCount;
    ContribNode next;

    ContribNode(String author) {
        this.author      = author;
        this.commitCount = 1;
    }
}

class ContributorTracker implements Serializable {
    private static final long serialVersionUID = 1L;
    private ContribNode head;

    // Record a commit by this author
    void record(String author) {
        ContribNode curr = head;

        // Search if author already exists
        while (curr != null) {
            if (curr.author.equals(author)) {
                curr.commitCount++;
                return;
            }
            curr = curr.next;
        }

        // Author not found — add new node at front
        ContribNode n = new ContribNode(author);
        n.next = head;
        head   = n;
    }

    // Return leaderboard sorted by commit count (descending)
    java.util.List<String[]> getLeaderboard() {
        java.util.List<String[]> list = new java.util.ArrayList<>();
        ContribNode curr = head;

        while (curr != null) {
            list.add(new String[]{curr.author, String.valueOf(curr.commitCount)});
            curr = curr.next;
        }

        // Sort by commit count descending
        list.sort((a, b) -> Integer.parseInt(b[1]) - Integer.parseInt(a[1]));
        return list;
    }
}


// ================================================================
//  SECTION 11 — REPOSITORY
// ================================================================

class Repository implements Serializable {
    private static final long serialVersionUID = 1L;

    private String    name;
    private String    ownerUsername;

    // Commit infrastructure
    CommitList        history;        // LinkedList: full commit chain
    CommitBST         commitIndex;    // BST: fast lookup by ID
    private int       commitCounter;

    // Branch infrastructure
    BranchList        branches;
    Branch            activeBranch;

    // Staging area (Array — Data Structure 4)
    FileEntry[]       stagedFiles;
    int               stagedCount;
    private static final int MAX_STAGED = 20;

    // Undo/Redo for staging (Stack)
    private MyStack   undoStack;
    private MyStack   redoStack;

    // Working files (HashMap — Data Structure 5)
    MyHashMap         workingFiles;

    // Push tracking
    private String[]  pushedIds;
    private int       pushedCount;
    private static final int MAX_PUSHED = 200;

    // Contributor tracker
    ContributorTracker contributors;

    Repository(String name, String ownerUsername) {
        this.name          = name;
        this.ownerUsername = ownerUsername;
        this.history       = new CommitList();
        this.commitIndex   = new CommitBST();
        this.commitCounter = 0;
        this.branches      = new BranchList();
        this.stagedFiles   = new FileEntry[MAX_STAGED];
        this.stagedCount   = 0;
        this.undoStack     = new MyStack();
        this.redoStack     = new MyStack();
        this.workingFiles  = new MyHashMap();
        this.pushedIds     = new String[MAX_PUSHED];
        this.pushedCount   = 0;
        this.contributors  = new ContributorTracker();
        this.activeBranch  = branches.create("main");
    }

    String getName()          { return name; }
    String getOwnerUsername() { return ownerUsername; }
    String getActiveBranch()  { return activeBranch == null ? "main" : activeBranch.name; }

    // Auto-generate commit IDs: c001, c002, c003...
    private String nextCommitId() {
        commitCounter++;
        return String.format("c%03d", commitCounter);
    }


    // ================================================================
    //  FILE HANDLING 1 — Create or update file in working area
    // ================================================================

    void writeFile(String filename, String content) {
        workingFiles.put(filename, new FileEntry(filename, content));
    }


    // ================================================================
    //  FILE HANDLING 2 — Import real file from computer
    //  Reads file line by line using Scanner
    //  Closes scanner in finally block (best practice)
    // ================================================================

    void importFile(String path) throws FileNotFoundException2, IOException {
        File f = new File(path);

        if (!f.exists()) {
            throw new FileNotFoundException2("File does not exist: " + path);
        }
        if (!f.isFile()) {
            throw new FileNotFoundException2("Path is not a file: " + path);
        }
        if (!f.canRead()) {
            throw new FileNotFoundException2("Cannot read file: " + path);
        }

        java.util.Scanner sc = null;

        try {
            sc = new java.util.Scanner(f);
            StringBuilder sb = new StringBuilder();

            while (sc.hasNextLine()) {
                sb.append(sc.nextLine()).append("\n");
            }

            String filename = f.getName();
            String content  = sb.toString().trim();
            workingFiles.put(filename, new FileEntry(filename, content));

        } finally {
            if (sc != null) {
                sc.close();   // always close even if exception occurs
            }
        }
    }


    // ================================================================
    //  FILE HANDLING 3 — Export file to computer
    //  Uses try-with-resources (auto-closes FileWriter)
    // ================================================================

    void exportFile(String filename, String destPath) throws FileNotFoundException2, IOException {
        FileEntry fe = workingFiles.get(filename);

        if (fe == null) {
            throw new FileNotFoundException2("File not found: " + filename);
        }

        File dest = new File(destPath);

        if (dest.isDirectory()) {
            dest = new File(destPath + File.separator + filename);
        }

        // try-with-resources — automatically closes FileWriter
        try (FileWriter fw = new FileWriter(dest)) {
            fw.write(fe.getContent());
        }
    }


    // ================================================================
    //  STAGING OPERATIONS
    // ================================================================

    void stageFile(String filename) throws FileNotFoundException2 {
        FileEntry wf = workingFiles.get(filename);

        if (wf == null) {
            throw new FileNotFoundException2("File not found in working area: " + filename);
        }

        // Check if already staged — update it
        for (int i = 0; i < stagedCount; i++) {
            if (stagedFiles[i].getFilename().equals(filename)) {
                stagedFiles[i] = new FileEntry(wf);
                undoStack.push("unstage:" + filename);
                redoStack.clear();
                return;
            }
        }

        // Not staged yet — add to staging area
        if (stagedCount < MAX_STAGED) {
            stagedFiles[stagedCount] = new FileEntry(wf);
            stagedCount++;
            undoStack.push("unstage:" + filename);
            redoStack.clear();
        }
    }

    void unstageFile(String filename) throws FileNotFoundException2 {
        for (int i = 0; i < stagedCount; i++) {
            if (stagedFiles[i].getFilename().equals(filename)) {

                // Shift array left to fill gap
                for (int j = i; j < stagedCount - 1; j++) {
                    stagedFiles[j] = stagedFiles[j + 1];
                }
                stagedCount--;
                stagedFiles[stagedCount] = null;
                return;
            }
        }
        throw new FileNotFoundException2("File not in staging area: " + filename);
    }

    // Undo last stage action using Stack
    void undoStage() {
        String action = undoStack.pop();

        if (action == null) {
            return;
        }
        if (action.startsWith("unstage:")) {
            try {
                unstageFile(action.substring(8));
            } catch (Exception e) {
                // already removed
            }
            redoStack.push("stage:" + action.substring(8));
        }
    }

    // Redo last undone stage action using Stack
    void redoStage() {
        String action = redoStack.pop();

        if (action == null) {
            return;
        }
        if (action.startsWith("stage:")) {
            try {
                stageFile(action.substring(6));
            } catch (Exception e) {
                // ignore
            }
        }
    }


    // ================================================================
    //  COMMIT
    // ================================================================

    String commit(String message, String author) throws EmptyCommitException, EmptyCommitMessageException {
        if (stagedCount == 0) {
            throw new EmptyCommitException("Nothing staged. Stage files before committing.");
        }

        Validator.validateCommitMessage(message);

        String id = nextCommitId();
        Commit c  = new Commit(id, message, author, activeBranch.name, stagedFiles, stagedCount);

        c.parent          = activeBranch.head;
        history.add(c);
        commitIndex.insert(c);
        activeBranch.head = c;
        contributors.record(author);

        // Clear staging after commit
        stagedFiles = new FileEntry[MAX_STAGED];
        stagedCount = 0;
        undoStack.clear();
        redoStack.clear();

        return "Committed: " + c;
    }

    void tagCommit(String commitId, String tag) throws CommitNotFoundException {
        if (commitId == null || commitId.isEmpty()) {
            throw new CommitNotFoundException("Commit ID cannot be empty.");
        }

        Commit c = commitIndex.search(commitId);

        if (c == null) {
            throw new CommitNotFoundException("Commit not found: " + commitId);
        }

        c.setTag(tag);
    }


    // ================================================================
    //  BRANCH OPERATIONS
    // ================================================================

    void createBranch(String name) throws BranchNotFoundException {
        Validator.validateBranchName(name);

        if (branches.find(name) != null) {
            throw new BranchNotFoundException("Branch '" + name + "' already exists.");
        }

        Branch b = branches.create(name);

        if (b != null) {
            b.head = activeBranch.head;   // branch starts from current HEAD
        }
    }

    String switchBranch(String name) throws BranchNotFoundException {
        Branch b = branches.find(name);

        if (b == null) {
            throw new BranchNotFoundException("Branch not found: " + name);
        }

        activeBranch = b;

        // Restore working files from branch's latest commit
        if (b.head != null) {
            workingFiles = new MyHashMap();
            for (int i = 0; i < b.head.getFileCount(); i++) {
                FileEntry fe = b.head.getFile(i);
                workingFiles.put(fe.getFilename(), new FileEntry(fe));
            }
        }

        return "Switched to branch: " + name;
    }

    String mergeBranch(String sourceName, String author) throws BranchNotFoundException, EmptyCommitException {
        if (sourceName.equals(activeBranch.name)) {
            throw new BranchNotFoundException("Cannot merge branch into itself.");
        }

        Branch source = branches.find(sourceName);

        if (source == null) {
            throw new BranchNotFoundException("Branch not found: " + sourceName);
        }
        if (source.head == null) {
            throw new EmptyCommitException("Source branch has no commits.");
        }
        if (activeBranch.head == null) {
            throw new EmptyCommitException("Current branch has no commits.");
        }

        // Start with files from current branch tip
        stagedFiles = new FileEntry[MAX_STAGED];
        stagedCount = 0;
        Commit currentTip = activeBranch.head;

        for (int i = 0; i < currentTip.getFileCount(); i++) {
            if (stagedCount < MAX_STAGED) {
                stagedFiles[stagedCount] = new FileEntry(currentTip.getFile(i));
                stagedCount++;
            }
        }

        // Overlay files from source branch (source wins on conflict)
        Commit sourceTip = source.head;

        for (int i = 0; i < sourceTip.getFileCount(); i++) {
            FileEntry incoming = sourceTip.getFile(i);
            boolean   found    = false;

            for (int j = 0; j < stagedCount; j++) {
                if (stagedFiles[j].getFilename().equals(incoming.getFilename())) {
                    stagedFiles[j] = new FileEntry(incoming);  // source wins
                    found = true;
                    break;
                }
            }

            if (!found && stagedCount < MAX_STAGED) {
                stagedFiles[stagedCount] = new FileEntry(incoming);
                stagedCount++;
            }
        }

        // Create merge commit
        String id  = nextCommitId();
        String msg = "Merge '" + sourceName + "' into '" + activeBranch.name + "'";
        Commit mc  = new Commit(id, msg, author, activeBranch.name, stagedFiles, stagedCount);

        mc.parent         = activeBranch.head;
        history.add(mc);
        commitIndex.insert(mc);
        activeBranch.head = mc;
        contributors.record(author);

        // Clear staging
        stagedFiles = new FileEntry[MAX_STAGED];
        stagedCount = 0;
        undoStack.clear();
        redoStack.clear();

        // Update working files to merged state
        workingFiles = new MyHashMap();
        for (int i = 0; i < mc.getFileCount(); i++) {
            FileEntry fe = mc.getFile(i);
            workingFiles.put(fe.getFilename(), new FileEntry(fe));
        }

        return "Merged successfully. Commit: " + mc.getId();
    }


    // ================================================================
    //  QUERIES
    // ================================================================

    java.util.List<Commit> getAllCommits() {
        java.util.List<Commit> list = new java.util.ArrayList<>();
        Commit curr = history.head;

        while (curr != null) {
            list.add(curr);
            curr = curr.parent;
        }
        return list;
    }

    java.util.List<Commit> getBranchCommits() {
        java.util.List<Commit> list = new java.util.ArrayList<>();
        Commit curr = activeBranch.head;

        while (curr != null) {
            list.add(curr);
            curr = curr.parent;
        }
        return list;
    }

    Commit searchCommit(String id) throws CommitNotFoundException {
        Validator.validateCommitId(id);
        Commit c = commitIndex.search(id);   // BST search O(log n)

        if (c == null) {
            throw new CommitNotFoundException("Commit not found: " + id);
        }
        return c;
    }

    // Restore working files to state at a specific commit
    String checkout(String commitId) throws CommitNotFoundException {
        Validator.validateCommitId(commitId);
        Commit c = commitIndex.search(commitId);

        if (c == null) {
            throw new CommitNotFoundException("Commit not found: " + commitId);
        }

        workingFiles = new MyHashMap();

        for (int i = 0; i < c.getFileCount(); i++) {
            FileEntry fe = c.getFile(i);
            workingFiles.put(fe.getFilename(), new FileEntry(fe));
        }

        return "Checked out: " + commitId;
    }

    // Compare same file across two commits
    String diff(String id1, String id2, String filename) throws CommitNotFoundException, FileNotFoundException2 {
        Validator.validateCommitId(id1);
        Validator.validateCommitId(id2);

        Commit c1 = commitIndex.search(id1);
        Commit c2 = commitIndex.search(id2);

        if (c1 == null) {
            throw new CommitNotFoundException("Commit not found: " + id1);
        }
        if (c2 == null) {
            throw new CommitNotFoundException("Commit not found: " + id2);
        }

        FileEntry f1 = c1.findFile(filename);
        FileEntry f2 = c2.findFile(filename);

        if (f1 == null) {
            throw new FileNotFoundException2("File '" + filename + "' not in commit " + id1);
        }
        if (f2 == null) {
            throw new FileNotFoundException2("File '" + filename + "' not in commit " + id2);
        }

        String result = "--- " + id1 + " : " + filename + "\n"
                      + f1.getContent()
                      + "\n\n+++ " + id2 + " : " + filename + "\n"
                      + f2.getContent()
                      + "\n\n"
                      + (f1.getContent().equals(f2.getContent()) ? "(No changes)" : "(File differs)");

        return result;
    }


    // ================================================================
    //  PUSH / PULL
    // ================================================================

    private boolean isPushed(String id) {
        for (int i = 0; i < pushedCount; i++) {
            if (pushedIds[i].equals(id)) {
                return true;
            }
        }
        return false;
    }

    private void markPushed(String id) {
        if (!isPushed(id) && pushedCount < MAX_PUSHED) {
            pushedIds[pushedCount] = id;
            pushedCount++;
        }
    }

    // Collect commits oldest first for push order
    private Commit[] collectOldestFirst() {
        int total = history.size();

        if (total == 0) {
            return new Commit[0];
        }

        Commit[] arr  = new Commit[total];
        Commit   curr = history.head;

        for (int i = total - 1; i >= 0; i--) {
            arr[i] = curr;
            curr   = curr.parent;
        }
        return arr;
    }

    String push(RemoteStore rs) throws EmptyCommitException {
        if (history.head == null) {
            throw new EmptyCommitException("No commits to push.");
        }

        RemoteRepo remote = rs.getOrCreate(name);
        int        count  = 0;

        for (Commit c : collectOldestFirst()) {
            if (!isPushed(c.getId())) {
                remote.commits.add(c);
                markPushed(c.getId());

                // Feature 1: record author in SHARED remote contributor tracker
                // This makes all push authors visible to every user who pulls
                remote.sharedContributors.record(c.getAuthor());

                count++;
            }
        }

        if (count == 0) {
            return "Already up to date.";
        }
        return "Pushed " + count + " commit(s).";
    }

    // ================================================================
    //  FEATURE 2 + 3 + 4 — Improved Pull
    //  - appendMode=true  → keep local content and append remote content
    //  - appendMode=false → overwrite local content with remote content
    //  - Detects conflicts when same file modified both locally and remotely
    //  - Returns conflict list so GUI can show resolution dialog
    // ================================================================

    // Inner class to hold conflict info
    static class ConflictInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        String    filename;
        String    localContent;
        String    remoteContent;

        ConflictInfo(String filename, String localContent, String remoteContent) {
            this.filename      = filename;
            this.localContent  = localContent;
            this.remoteContent = remoteContent;
        }
    }

    // Pull result wrapper — holds message + any conflicts found
    static class PullResult implements Serializable {
        private static final long serialVersionUID = 1L;
        String                      message;
        java.util.List<ConflictInfo> conflicts;

        PullResult(String message, java.util.List<ConflictInfo> conflicts) {
            this.message   = message;
            this.conflicts = conflicts;
        }

        boolean hasConflicts() {
            return conflicts != null && !conflicts.isEmpty();
        }
    }

    PullResult pull(RemoteStore rs, boolean appendMode) throws EmptyCommitException {
        if (!rs.exists(name)) {
            throw new EmptyCommitException("Remote is empty. Push first.");
        }

        RemoteRepo remote   = rs.getOrCreate(name);
        int        total    = remote.commits.size();
        Commit[]   remoteCm = new Commit[total];
        Commit     curr     = remote.commits.head;

        // Collect remote commits oldest first
        for (int i = total - 1; i >= 0; i--) {
            remoteCm[i] = curr;
            curr        = curr.parent;
        }

        int                          count     = 0;
        java.util.List<ConflictInfo> conflicts = new java.util.ArrayList<>();

        for (Commit rc : remoteCm) {
            if (history.findById(rc.getId()) == null) {
                history.add(rc);
                commitIndex.insert(rc);
                markPushed(rc.getId());
                count++;

                // Feature 3: sync files from pulled commits into working area
                for (int i = 0; i < rc.getFileCount(); i++) {
                    FileEntry remoteFile = rc.getFile(i);
                    String    fname      = remoteFile.getFilename();
                    FileEntry localFile  = workingFiles.get(fname);

                    if (localFile == null) {
                        // New file from remote — just add it
                        workingFiles.put(fname, new FileEntry(remoteFile));

                    } else if (!localFile.getContent().equals(remoteFile.getContent())) {
                        // Feature 4: same file exists locally with different content = CONFLICT
                        ConflictInfo conflict = new ConflictInfo(
                            fname,
                            localFile.getContent(),
                            remoteFile.getContent()
                        );
                        conflicts.add(conflict);

                        // Default resolution: keep remote (overwrite) unless appendMode
                        if (appendMode) {
                            // Feature 2: Append mode — keep local + add remote below
                            String merged = localFile.getContent() + "\n" + remoteFile.getContent();
                            workingFiles.put(fname, new FileEntry(fname, merged));
                        } else {
                            // Overwrite mode — remote wins
                            workingFiles.put(fname, new FileEntry(remoteFile));
                        }
                    }
                    // If content is same — no action needed
                }
            }
        }

        String msg;
        if (count == 0) {
            msg = "Already up to date.";
        } else {
            msg = "Pulled " + count + " commit(s). " + conflicts.size() + " conflict(s) detected.";
        }

        return new PullResult(msg, conflicts);
    }

    // Backward-compatible pull with overwrite mode (no conflicts returned)
    String pull(RemoteStore rs) throws EmptyCommitException {
        PullResult result = pull(rs, false);
        return result.message;
    }

    // Feature 1: resolve conflict — user chooses keep local or keep remote
    void resolveConflict(String filename, boolean keepLocal, String manualContent) {
        if (manualContent != null && !manualContent.isEmpty()) {
            workingFiles.put(filename, new FileEntry(filename, manualContent));
        } else if (!keepLocal) {
            // keepLocal=false means keep remote — already set during pull
            // nothing extra needed
        }
        // keepLocal=true — restore from last local commit
        if (keepLocal && activeBranch.head != null) {
            FileEntry localVersion = activeBranch.head.findFile(filename);
            if (localVersion != null) {
                workingFiles.put(filename, new FileEntry(localVersion));
            }
        }
    }

    java.util.List<String[]> getPushStatus(RemoteStore rs) {
        java.util.List<String[]> list = new java.util.ArrayList<>();

        for (Commit c : collectOldestFirst()) {
            String status = isPushed(c.getId()) ? "PUSHED" : "LOCAL";
            list.add(new String[]{status, c.getId(), c.getMessage(), c.getAuthor()});
        }
        return list;
    }

    public String toString() {
        return name + " [" + activeBranch.name + "] (" + history.size() + " commits)";
    }
}


// ================================================================
//  SECTION 12 — USER + USER LINKED LIST (Data Structure 2)
// ================================================================

class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String       username;
    private String       password;
    private String       email;
    private Repository[] repos;
    private int          repoCount;
    private static final int MAX_REPOS = 10;
    User next;    // LinkedList pointer for UserList

    User(String username, String password, String email) {
        this.username  = username;
        this.password  = password;
        this.email     = email;
        this.repos     = new Repository[MAX_REPOS];
        this.repoCount = 0;
    }

    String getUsername() { return username; }
    String getPassword() { return password; }
    String getEmail()    { return email; }
    void   setPassword(String p) { this.password = p; }

    void createRepo(String name) throws InvalidFileNameException {
        Validator.validateRepoName(name);

        if (findRepo(name) != null) {
            throw new InvalidFileNameException("Repository '" + name + "' already exists.");
        }
        if (repoCount >= MAX_REPOS) {
            throw new InvalidFileNameException("Maximum repositories (10) reached.");
        }

        repos[repoCount] = new Repository(name, username);
        repoCount++;
    }

    Repository findRepo(String name) {
        for (int i = 0; i < repoCount; i++) {
            if (repos[i].getName().equals(name)) {
                return repos[i];
            }
        }
        return null;
    }

    Repository[] getRepos() {
        Repository[] r = new Repository[repoCount];
        for (int i = 0; i < repoCount; i++) {
            r[i] = repos[i];
        }
        return r;
    }

    public String toString() {
        return username + " (" + email + ")";
    }
}

class UserList implements Serializable {
    private static final long serialVersionUID = 1L;
    private User head;

    // Add user at front of linked list
    void add(User u) {
        u.next = head;
        head   = u;
    }

    boolean exists(String un) {
        return find(un) != null;
    }

    // Linear search through user list
    User find(String username) {
        User curr = head;
        while (curr != null) {
            if (curr.getUsername().equals(username)) {
                return curr;
            }
            curr = curr.next;
        }
        return null;
    }

    // Login with proper exception handling
    User login(String username, String password) throws UserNotFoundException, WrongPasswordException {
        User u = find(username);

        if (u == null) {
            throw new UserNotFoundException("No account found with username '" + username + "'.");
        }
        if (!u.getPassword().equals(password)) {
            throw new WrongPasswordException("Incorrect password. Please try again.");
        }
        return u;
    }

    java.util.List<User> getAll() {
        java.util.List<User> list = new java.util.ArrayList<>();
        User curr = head;
        while (curr != null) {
            list.add(curr);
            curr = curr.next;
        }
        return list;
    }
}


// ================================================================
//  SECTION 13 — MAIN APPLICATION (JavaFX GUI)
// ================================================================

public class MainApplication extends Application {

    // App state — static so sidebar always reads latest value
    static UserList    users       = new UserList();
    static User        currentUser = null;
    static Repository  currentRepo = null;
    static RemoteStore remoteStore = new RemoteStore();
    static final String SAVE_FILE  = "gitlite_data.ser";

    // GitHub-inspired dark theme colors
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


    // ================================================================
    //  SAVE / LOAD (Java Serialization)
    // ================================================================

    static void saveData() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(SAVE_FILE))) {
            out.writeObject(users);
            out.writeObject(remoteStore);
        } catch (IOException e) {
            System.out.println("Save failed: " + e.getMessage());
        }
    }

    static void loadData() {
        File f = new File(SAVE_FILE);

        if (!f.exists()) {
            return;
        }

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(SAVE_FILE))) {
            users       = (UserList)    in.readObject();
            remoteStore = (RemoteStore) in.readObject();
        } catch (Exception e) {
            users       = new UserList();
            remoteStore = new RemoteStore();
        }
    }


    // ================================================================
    //  ENTRY POINT
    // ================================================================

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        loadData();
        primaryStage = stage;
        primaryStage.setTitle("GitLite — DSA Version Control");
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
        showLoginScreen();
    }


    // ================================================================
    //  STYLING HELPERS
    // ================================================================

    Button styledBtn(String text, String bg) {
        Button b    = new Button(text);
        String base = "-fx-background-color:" + bg
                    + ";-fx-text-fill:" + TEXT_PRI
                    + ";-fx-font-size:13px"
                    + ";-fx-padding:8 18"
                    + ";-fx-background-radius:6"
                    + ";-fx-cursor:hand"
                    + ";-fx-font-family:'Consolas';";

        b.setStyle(base);

        b.setOnMouseEntered(e -> b.setStyle(
            "-fx-background-color:" + (bg.equals(ACCENT) ? ACCENT_HVR : "#3a4048")
            + ";-fx-text-fill:" + TEXT_PRI
            + ";-fx-font-size:13px;-fx-padding:8 18;-fx-background-radius:6;-fx-cursor:hand;-fx-font-family:'Consolas';"));

        b.setOnMouseExited(e -> b.setStyle(base));
        return b;
    }

    TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color:#21262d"
                  + ";-fx-text-fill:" + TEXT_PRI
                  + ";-fx-prompt-text-fill:" + TEXT_SEC
                  + ";-fx-border-color:" + BORDER
                  + ";-fx-border-radius:6"
                  + ";-fx-background-radius:6"
                  + ";-fx-padding:8"
                  + ";-fx-font-size:13px;");
        return tf;
    }

    PasswordField styledPass(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setStyle("-fx-background-color:#21262d"
                  + ";-fx-text-fill:" + TEXT_PRI
                  + ";-fx-prompt-text-fill:" + TEXT_SEC
                  + ";-fx-border-color:" + BORDER
                  + ";-fx-border-radius:6"
                  + ";-fx-background-radius:6"
                  + ";-fx-padding:8"
                  + ";-fx-font-size:13px;");
        return pf;
    }

    Label heading(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:" + TEXT_PRI
                 + ";-fx-font-size:22px"
                 + ";-fx-font-weight:bold"
                 + ";-fx-font-family:'Consolas';");
        return l;
    }

    Label subLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:" + TEXT_SEC
                 + ";-fx-font-size:12px"
                 + ";-fx-font-family:'Consolas';");
        return l;
    }

    Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:" + TEXT_PRI
                 + ";-fx-font-size:13px"
                 + ";-fx-font-family:'Consolas';");
        return l;
    }

    Label hintLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:" + TEXT_SEC
                 + ";-fx-font-size:11px"
                 + ";-fx-font-family:'Consolas';");
        l.setWrapText(true);
        return l;
    }

    Label errorLabel() {
        Label l = new Label("");
        l.setStyle("-fx-text-fill:" + RED
                 + ";-fx-font-size:12px"
                 + ";-fx-font-family:'Consolas';");
        l.setWrapText(true);
        return l;
    }

    void showError(Label l, String msg) {
        l.setStyle("-fx-text-fill:" + RED + ";-fx-font-size:12px;-fx-font-family:'Consolas';");
        l.setText("X  " + msg);
    }

    void showSuccess(Label l, String msg) {
        l.setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-size:12px;-fx-font-family:'Consolas';");
        l.setText("OK  " + msg);
    }

    void showAlert(String title, String msg, boolean error) {
        Alert a = new Alert(error ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.getDialogPane().setStyle("-fx-background-color:" + BG_CARD + ";");
        a.showAndWait();
    }

    void setStatus(String msg) {
        if (statusBar != null) {
            statusBar.setText("  " + msg);
        }
    }

    void setContent(javafx.scene.Node node) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(node);
    }


    // ================================================================
    //  LOGIN SCREEN
    // ================================================================

    void showLoginScreen() {
        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color:" + BG_DARK + ";");
        box.setPadding(new Insets(60));

        // Logo area
        VBox logo = new VBox(5);
        logo.setAlignment(Pos.CENTER);

        Label icon = new Label("G");
        icon.setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-size:48px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

        Label title = new Label("GitLite");
        title.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:32px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

        Label sub = new Label("DSA Version Control System");
        sub.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:14px;-fx-font-family:'Consolas';");

        logo.getChildren().addAll(icon, title, sub);

        // Login card
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(30));
        card.setMaxWidth(400);
        card.setStyle("-fx-background-color:" + BG_CARD
                    + ";-fx-border-color:" + BORDER
                    + ";-fx-border-radius:10"
                    + ";-fx-background-radius:10;");

        Label loginTitle = new Label("Sign in");
        loginTitle.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:20px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

        TextField     userField = styledField("Username");
        userField.setPrefWidth(340);

        PasswordField passField = styledPass("Password");
        passField.setPrefWidth(340);

        Label errLabel = errorLabel();

        Button loginBtn = styledBtn("Sign in", ACCENT);
        loginBtn.setPrefWidth(340);

        Label orLabel = new Label("or");
        orLabel.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-family:'Consolas';");
        orLabel.setMaxWidth(340);
        orLabel.setAlignment(Pos.CENTER);

        Button registerBtn = styledBtn("Create new account", "#21262d");
        registerBtn.setPrefWidth(340);

        loginBtn.setOnAction(e -> {
            errLabel.setText("");
            try {
                if (userField.getText().trim().isEmpty()) {
                    throw new InvalidUsernameException("Please enter your username.");
                }
                if (passField.getText().isEmpty()) {
                    throw new InvalidPasswordException("Please enter your password.");
                }

                User u    = users.login(userField.getText().trim(), passField.getText());
                currentUser = u;
                currentRepo = null;
                showMainDashboard();

            } catch (UserNotFoundException ex) {
                showError(errLabel, ex.getMessage());
            } catch (WrongPasswordException ex) {
                showError(errLabel, ex.getMessage());
            } catch (InvalidUsernameException ex) {
                showError(errLabel, ex.getMessage());
            } catch (InvalidPasswordException ex) {
                showError(errLabel, ex.getMessage());
            }
        });

        registerBtn.setOnAction(e -> showRegisterScreen());

        card.getChildren().addAll(
            loginTitle,
            fieldLabel("Username"), userField,
            fieldLabel("Password"), passField,
            loginBtn,
            orLabel,
            registerBtn,
            errLabel
        );

        box.getChildren().addAll(logo, card);
        primaryStage.setScene(new Scene(box, 1100, 700));
        primaryStage.show();
    }


    // ================================================================
    //  REGISTER SCREEN
    // ================================================================

    void showRegisterScreen() {
        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color:" + BG_DARK + ";");
        box.setPadding(new Insets(40));

        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(30));
        card.setMaxWidth(440);
        card.setStyle("-fx-background-color:" + BG_CARD
                    + ";-fx-border-color:" + BORDER
                    + ";-fx-border-radius:10"
                    + ";-fx-background-radius:10;");

        Label title = new Label("Create Account");
        title.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:20px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

        TextField     unField  = styledField("e.g. ali_coder");
        unField.setPrefWidth(380);

        PasswordField pwField  = styledPass("Min 6 chars, include a number");
        pwField.setPrefWidth(380);

        PasswordField pw2Field = styledPass("Re-enter password");
        pw2Field.setPrefWidth(380);

        TextField emField = styledField("e.g. ali@gmail.com");
        emField.setPrefWidth(380);

        Label unErr  = errorLabel();
        Label pwErr  = errorLabel();
        Label pw2Err = errorLabel();
        Label emErr  = errorLabel();
        Label genMsg = errorLabel();

        Button regBtn  = styledBtn("Create account", ACCENT);
        regBtn.setPrefWidth(380);

        Button backBtn = styledBtn("Back to sign in", "#21262d");
        backBtn.setPrefWidth(380);

        // Live validation when user leaves each field
        unField.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                try {
                    Validator.validateUsername(unField.getText().trim());
                    unErr.setText("");
                } catch (InvalidUsernameException ex) {
                    showError(unErr, ex.getMessage());
                }
            }
        });

        pwField.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                try {
                    Validator.validatePassword(pwField.getText());
                    pwErr.setText("");
                } catch (InvalidPasswordException ex) {
                    showError(pwErr, ex.getMessage());
                }
            }
        });

        pw2Field.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                if (!pwField.getText().equals(pw2Field.getText())) {
                    showError(pw2Err, "Passwords do not match.");
                } else {
                    pw2Err.setText("");
                }
            }
        });

        emField.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                try {
                    Validator.validateEmail(emField.getText().trim());
                    emErr.setText("");
                } catch (InvalidEmailException ex) {
                    showError(emErr, ex.getMessage());
                }
            }
        });

        regBtn.setOnAction(e -> {
            genMsg.setText("");

            String un  = unField.getText().trim();
            String pw  = pwField.getText();
            String pw2 = pw2Field.getText();
            String em  = emField.getText().trim();

            try {
                Validator.validateUsername(un);

                if (users.exists(un)) {
                    throw new InvalidUsernameException("Username '" + un + "' is already taken.");
                }

                Validator.validatePassword(pw);

                if (!pw.equals(pw2)) {
                    throw new InvalidPasswordException("Passwords do not match.");
                }

                Validator.validateEmail(em);

                users.add(new User(un, pw, em));
                saveData();
                showSuccess(genMsg, "Account created! Redirecting...");

                // Auto redirect to login after 1.2 seconds
                javafx.animation.PauseTransition pause =
                    new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.2));
                pause.setOnFinished(ev -> showLoginScreen());
                pause.play();

            } catch (InvalidUsernameException ex) {
                showError(unErr,  ex.getMessage());
                showError(genMsg, "Fix errors above.");
            } catch (InvalidPasswordException ex) {
                showError(pwErr,  ex.getMessage());
                showError(genMsg, "Fix errors above.");
            } catch (InvalidEmailException ex) {
                showError(emErr,  ex.getMessage());
                showError(genMsg, "Fix errors above.");
            }
        });

        backBtn.setOnAction(e -> showLoginScreen());

        card.getChildren().addAll(
            title,
            fieldLabel("Username"),         unField,
            hintLabel("3-20 chars. Letters, numbers, underscore only."), unErr,
            fieldLabel("Password"),         pwField,
            hintLabel("Min 6 chars. Must include 1 letter and 1 number."), pwErr,
            fieldLabel("Confirm Password"), pw2Field, pw2Err,
            fieldLabel("Email"),            emField,
            hintLabel("Must contain @ and a valid domain e.g. gmail.com"), emErr,
            regBtn,
            backBtn,
            genMsg
        );

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

        // Build sidebar
        VBox sb = new VBox(4);
        sb.setPrefWidth(200);
        sb.setPadding(new Insets(16, 8, 16, 8));
        sb.setStyle("-fx-background-color:" + BG_SIDEBAR
                  + ";-fx-border-color:" + BORDER
                  + ";-fx-border-width:0 1 0 0;");

        String[][] items = {
            {"H", "Home"},
            {"R", "Repositories"},
            {"F", "Files"},
            {"S", "Staging"},
            {"B", "Branches"},
            {"C", "Commits"},
            {"L", "Contributors"},
            {"T", "Remote"}
        };

        for (String[] item : items) {
            Button btn = new Button(item[0] + "  " + item[1]);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setAlignment(Pos.CENTER_LEFT);

            String normal = "-fx-background-color:transparent"
                          + ";-fx-text-fill:" + TEXT_SEC
                          + ";-fx-font-size:13px"
                          + ";-fx-padding:8 12"
                          + ";-fx-background-radius:6"
                          + ";-fx-cursor:hand"
                          + ";-fx-font-family:'Consolas';";

            String hover = "-fx-background-color:#21262d"
                         + ";-fx-text-fill:" + TEXT_PRI
                         + ";-fx-font-size:13px"
                         + ";-fx-padding:8 12"
                         + ";-fx-background-radius:6"
                         + ";-fx-cursor:hand"
                         + ";-fx-font-family:'Consolas';";

            btn.setStyle(normal);
            btn.setOnMouseEntered(e -> btn.setStyle(hover));
            btn.setOnMouseExited(e  -> btn.setStyle(normal));

            String section = item[1];

            btn.setOnAction(e -> {
                // Check if repo is selected before allowing access
                boolean needsRepo = section.equals("Files")
                                 || section.equals("Staging")
                                 || section.equals("Branches")
                                 || section.equals("Commits")
                                 || section.equals("Remote")
                                 || section.equals("Contributors");

                if (needsRepo && currentRepo == null) {
                    showAlert("No Repository Selected",
                              "Please select a repository first.\nGo to Repositories and click Select.",
                              true);
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
        statusBar.setStyle("-fx-background-color:#010409"
                         + ";-fx-text-fill:" + TEXT_SEC
                         + ";-fx-font-size:11px"
                         + ";-fx-font-family:'Consolas'"
                         + ";-fx-padding:4 0;");
        statusBar.setMaxWidth(Double.MAX_VALUE);
        root.setBottom(statusBar);

        showRepoPanel();    // open Repositories first so user selects repo
        primaryStage.setScene(new Scene(root, 1100, 700));
    }

    HBox buildTopBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 20, 10, 20));
        bar.setStyle("-fx-background-color:" + BG_SIDEBAR
                   + ";-fx-border-color:" + BORDER
                   + ";-fx-border-width:0 0 1 0;");

        Label logo = new Label("GitLite");
        logo.setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-size:16px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userInfo = new Label("User: " + currentUser.getUsername());
        userInfo.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:13px;-fx-font-family:'Consolas';");

        Button logoutBtn = styledBtn("Sign out", "#21262d");
        logoutBtn.setOnAction(e -> {
            saveData();
            currentUser = null;
            currentRepo = null;
            showLoginScreen();
        });

        bar.getChildren().addAll(logo, spacer, userInfo, logoutBtn);
        return bar;
    }


    // ================================================================
    //  HOME PANEL
    // ================================================================

    void showHomePanel() {
        ScrollPane scroll = new ScrollPane();
        scroll.setStyle("-fx-background-color:" + BG_DARK + ";-fx-background:" + BG_DARK + ";");
        scroll.setFitToWidth(true);

        VBox panel = new VBox(20);
        panel.setPadding(new Insets(30));
        panel.setStyle("-fx-background-color:" + BG_DARK + ";");

        Label welcome = heading("Welcome, " + currentUser.getUsername());
        Label hint    = subLabel("Select a repository from the sidebar to get started.");

        panel.getChildren().add(welcome);
        panel.getChildren().add(hint);

        // Stats row
        HBox stats = new HBox(16);
        Repository[] repos       = currentUser.getRepos();
        int          totalCommits = 0;

        for (Repository r : repos) {
            totalCommits += r.history.size();
        }

        stats.getChildren().add(statCard("Repos",    String.valueOf(repos.length),   "Repositories"));
        stats.getChildren().add(statCard("Commits",  String.valueOf(totalCommits),    "Total Commits"));
        stats.getChildren().add(statCard("Branch",   currentRepo == null ? "none"  : currentRepo.getActiveBranch(), "Active Branch"));
        stats.getChildren().add(statCard("Staged",   currentRepo == null ? "0"     : String.valueOf(currentRepo.stagedCount), "Staged Files"));

        panel.getChildren().add(stats);

        // Show active repo indicator
        if (currentRepo != null) {
            Label activeLabel = new Label("Active repo: " + currentRepo.getName() + "  [" + currentRepo.getActiveBranch() + "]");
            activeLabel.setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-size:14px;-fx-font-family:'Consolas';");
            panel.getChildren().add(activeLabel);
        }

        scroll.setContent(panel);
        setContent(scroll);
        setStatus("Home");
    }

    VBox statCard(String icon, String value, String label) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setPrefWidth(180);
        card.setStyle("-fx-background-color:" + BG_CARD
                    + ";-fx-border-color:" + BORDER
                    + ";-fx-border-radius:8"
                    + ";-fx-background-radius:8;");

        Label ic  = new Label(icon);
        ic.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:13px;-fx-font-family:'Consolas';");

        Label val = new Label(value);
        val.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:22px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:12px;-fx-font-family:'Consolas';");

        card.getChildren().addAll(ic, val, lbl);
        return card;
    }


    // ================================================================
    //  REPOSITORIES PANEL
    // ================================================================

    void showRepoPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(30));
        panel.setStyle("-fx-background-color:" + BG_DARK + ";");

        // Header row
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = heading("Repositories");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        TextField nameField = styledField("my-project");
        nameField.setPrefWidth(220);

        Label repoErr = errorLabel();

        Button createBtn = styledBtn("New Repository", ACCENT);
        createBtn.setOnAction(e -> {
            repoErr.setText("");
            try {
                String name = nameField.getText().trim();
                currentUser.createRepo(name);
                saveData();
                nameField.clear();
                showSuccess(repoErr, "Repository created!");
                showRepoPanel();
            } catch (InvalidFileNameException ex) {
                showError(repoErr, ex.getMessage());
            }
        });

        header.getChildren().addAll(title, sp, nameField, createBtn);

        // Show active repo status
        if (currentRepo != null) {
            Label activeInfo = new Label("Active: " + currentRepo.getName() + " [" + currentRepo.getActiveBranch() + "]");
            activeInfo.setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-size:13px;-fx-font-family:'Consolas';");
            panel.getChildren().addAll(header, activeInfo, repoErr);
        } else {
            Label hint = new Label("Select a repository below to start working");
            hint.setStyle("-fx-text-fill:" + ORANGE + ";-fx-font-size:13px;-fx-font-family:'Consolas';");
            panel.getChildren().addAll(header, hint, repoErr);
        }

        // Repository list
        VBox list = new VBox(10);
        Repository[] repos = currentUser.getRepos();

        if (repos.length == 0) {
            list.getChildren().add(subLabel("No repositories yet. Create one above."));
        } else {
            for (Repository r : repos) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(14, 16, 14, 16));

                boolean isActive = currentRepo != null && currentRepo.getName().equals(r.getName());

                row.setStyle("-fx-background-color:" + BG_CARD
                           + ";-fx-border-color:" + (isActive ? ACCENT : BORDER)
                           + ";-fx-border-radius:8"
                           + ";-fx-background-radius:8"
                           + ";-fx-border-width:2;");

                VBox info = new VBox(3);

                Label name = new Label(r.getName());
                name.setStyle("-fx-text-fill:" + BLUE + ";-fx-font-size:15px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

                Label meta = new Label("Owner: " + r.getOwnerUsername()
                                     + "    Commits: " + r.history.size()
                                     + "    Branch: " + r.getActiveBranch());
                meta.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:12px;-fx-font-family:'Consolas';");

                info.getChildren().addAll(name, meta);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button selBtn = styledBtn(isActive ? "Selected" : "Select", isActive ? ACCENT : "#21262d");
                selBtn.setOnAction(ev -> {
                    currentRepo = r;
                    setStatus("Active repo: " + r.getName() + " [" + r.getActiveBranch() + "]");
                    showRepoPanel();
                });

                row.getChildren().addAll(info, spacer, selBtn);
                list.getChildren().add(row);
            }
        }

        panel.getChildren().add(list);

        ScrollPane scroll = new ScrollPane(panel);
        scroll.setStyle("-fx-background-color:" + BG_DARK + ";-fx-background:" + BG_DARK + ";");
        scroll.setFitToWidth(true);

        setContent(scroll);
        setStatus(currentRepo == null ? "Select a repository to begin" : "Repo: " + currentRepo.getName());
    }


    // ================================================================
    //  FILES PANEL
    // ================================================================

    void showFilesPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(30));
        panel.setStyle("-fx-background-color:" + BG_DARK + ";");

        panel.getChildren().add(heading("Files  —  " + currentRepo.getName() + " [" + currentRepo.getActiveBranch() + "]"));

        // Create/Edit file card
        VBox createCard = new VBox(8);
        createCard.setPadding(new Insets(16));
        createCard.setStyle("-fx-background-color:" + BG_CARD
                          + ";-fx-border-color:" + BORDER
                          + ";-fx-border-radius:8"
                          + ";-fx-background-radius:8;");

        Label createTitle = new Label("Create / Edit File");
        createTitle.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:14px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

        TextField fnField = styledField("notes.txt");
        Label     fnErr   = errorLabel();

        TextArea contentArea2 = new TextArea();
        contentArea2.setPromptText("File content...");
        contentArea2.setPrefRowCount(5);
        contentArea2.setStyle("-fx-background-color:#21262d"
                            + ";-fx-text-fill:" + TEXT_PRI
                            + ";-fx-font-family:'Consolas'"
                            + ";-fx-font-size:13px"
                            + ";-fx-border-color:" + BORDER
                            + ";-fx-border-radius:6"
                            + ";-fx-background-radius:6;");

        Button saveBtn = styledBtn("Save File", ACCENT);
        saveBtn.setOnAction(e -> {
            fnErr.setText("");
            try {
                String fn = fnField.getText().trim();
                Validator.validateFilename(fn);
                currentRepo.writeFile(fn, contentArea2.getText());
                saveData();
                setStatus("Saved: " + fn);
                showSuccess(fnErr, "File saved!");
                showFilesPanel();
            } catch (InvalidFileNameException ex) {
                showError(fnErr, ex.getMessage());
            }
        });

        createCard.getChildren().addAll(
            createTitle,
            fieldLabel("Filename"), fnField,
            hintLabel("Must have extension e.g. .txt .java .py — no spaces"),
            fnErr,
            fieldLabel("Content"), contentArea2,
            saveBtn
        );

        // Import and Export row
        HBox ioRow = new HBox(12);

        // Import card
        VBox importCard = new VBox(8);
        importCard.setPadding(new Insets(16));
        importCard.setPrefWidth(400);
        importCard.setStyle("-fx-background-color:" + BG_CARD
                          + ";-fx-border-color:" + BORDER
                          + ";-fx-border-radius:8"
                          + ";-fx-background-radius:8;");

        Label importTitle = new Label("Import from Computer");
        importTitle.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:14px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

        TextField importPath = styledField("C:\\Users\\Ali\\Desktop\\notes.txt");
        Label     importErr  = errorLabel();

        Button importBtn = styledBtn("Import File", "#21262d");
        importBtn.setOnAction(e -> {
            importErr.setText("");
            String path = importPath.getText().trim();
            if (path.isEmpty()) {
                showError(importErr, "File path cannot be empty.");
                return;
            }
            try {
                currentRepo.importFile(path);
                saveData();
                showSuccess(importErr, "Imported!");
                showFilesPanel();
            } catch (FileNotFoundException2 ex) {
                showError(importErr, ex.getMessage());
            } catch (IOException ex) {
                showError(importErr, ex.getMessage());
            }
        });

        importCard.getChildren().addAll(
            importTitle,
            fieldLabel("File path"),
            importPath,
            importBtn,
            importErr
        );

        // Export card
        VBox exportCard = new VBox(8);
        exportCard.setPadding(new Insets(16));
        exportCard.setPrefWidth(400);
        exportCard.setStyle("-fx-background-color:" + BG_CARD
                          + ";-fx-border-color:" + BORDER
                          + ";-fx-border-radius:8"
                          + ";-fx-background-radius:8;");

        Label exportTitle = new Label("Export File");
        exportTitle.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:14px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

        TextField exportFn   = styledField("notes.txt");
        TextField exportDest = styledField("C:\\Users\\Ali\\Desktop");
        Label     exportErr  = errorLabel();

        Button exportBtn = styledBtn("Export File", "#21262d");
        exportBtn.setOnAction(e -> {
            exportErr.setText("");
            try {
                if (exportFn.getText().trim().isEmpty()) {
                    throw new FileNotFoundException2("Filename cannot be empty.");
                }
                currentRepo.exportFile(exportFn.getText().trim(), exportDest.getText().trim());
                showSuccess(exportErr, "Exported!");
            } catch (FileNotFoundException2 ex) {
                showError(exportErr, ex.getMessage());
            } catch (IOException ex) {
                showError(exportErr, ex.getMessage());
            }
        });

        exportCard.getChildren().addAll(
            exportTitle,
            fieldLabel("Filename"),    exportFn,
            fieldLabel("Destination"), exportDest,
            exportBtn,
            exportErr
        );

        ioRow.getChildren().addAll(importCard, exportCard);

        // Working files list
        Label wfTitle = new Label("Working Files (HashMap)");
        wfTitle.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:15px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

        VBox fileList = new VBox(6);
        String[] keys = currentRepo.workingFiles.keys();

        if (keys == null || currentRepo.workingFiles.size() == 0) {
            fileList.getChildren().add(subLabel("No files yet. Create or import a file above."));
        } else {
            for (String key : keys) {
                if (key == null) {
                    continue;
                }

                FileEntry fe = currentRepo.workingFiles.get(key);

                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 14, 10, 14));
                row.setStyle("-fx-background-color:" + BG_CARD
                           + ";-fx-border-color:" + BORDER
                           + ";-fx-border-radius:6"
                           + ";-fx-background-radius:6;");

                Label fname = new Label(key);
                fname.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:13px;-fx-font-family:'Consolas';");

                Label fsize = new Label(fe.getContent().length() + " chars");
                fsize.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:12px;-fx-font-family:'Consolas';");

                Region spr = new Region();
                HBox.setHgrow(spr, Priority.ALWAYS);

                Button viewBtn = styledBtn("View", "#21262d");
                viewBtn.setOnAction(ev -> {
                    Alert a = new Alert(Alert.AlertType.INFORMATION);
                    a.setTitle(key);
                    a.setHeaderText(null);
                    TextArea ta = new TextArea(fe.getContent());
                    ta.setEditable(false);
                    ta.setStyle("-fx-font-family:'Consolas';");
                    a.getDialogPane().setContent(ta);
                    a.getDialogPane().setPrefSize(500, 300);
                    a.showAndWait();
                });

                row.getChildren().addAll(fname, spr, fsize, viewBtn);
                fileList.getChildren().add(row);
            }
        }

        panel.getChildren().addAll(createCard, ioRow, wfTitle, fileList);

        ScrollPane scroll = new ScrollPane(panel);
        scroll.setStyle("-fx-background-color:" + BG_DARK + ";-fx-background:" + BG_DARK + ";");
        scroll.setFitToWidth(true);

        setContent(scroll);
        setStatus("Files — " + currentRepo.workingFiles.size() + " file(s) in working area");
    }


    // ================================================================
    //  STAGING PANEL
    // ================================================================

    void showStagingPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(30));
        panel.setStyle("-fx-background-color:" + BG_DARK + ";");

        panel.getChildren().add(heading("Staging  —  " + currentRepo.getName()));

        // File selection and action buttons
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> fileCombo = new ComboBox<>();
        fileCombo.setStyle("-fx-background-color:#21262d;-fx-text-fill:" + TEXT_PRI + ";-fx-font-family:'Consolas';");
        fileCombo.setPromptText("Select file...");

        String[] keys = currentRepo.workingFiles.keys();
        if (keys != null) {
            for (String k : keys) {
                if (k != null) {
                    fileCombo.getItems().add(k);
                }
            }
        }

        Label  stageErr   = errorLabel();
        Button stageBtn   = styledBtn("Stage",   ACCENT);
        Button unstageBtn = styledBtn("Unstage", "#21262d");
        Button undoBtn    = styledBtn("Undo",    "#21262d");
        Button redoBtn    = styledBtn("Redo",    "#21262d");

        stageBtn.setOnAction(e -> {
            stageErr.setText("");
            String f = fileCombo.getValue();
            if (f == null) {
                showError(stageErr, "Select a file first.");
                return;
            }
            try {
                currentRepo.stageFile(f);
                saveData();
                setStatus("Staged: " + f);
                showStagingPanel();
            } catch (FileNotFoundException2 ex) {
                showError(stageErr, ex.getMessage());
            }
        });

        unstageBtn.setOnAction(e -> {
            stageErr.setText("");
            String f = fileCombo.getValue();
            if (f == null) {
                showError(stageErr, "Select a file first.");
                return;
            }
            try {
                currentRepo.unstageFile(f);
                saveData();
                showStagingPanel();
            } catch (FileNotFoundException2 ex) {
                showError(stageErr, ex.getMessage());
            }
        });

        undoBtn.setOnAction(e -> {
            currentRepo.undoStage();
            saveData();
            showStagingPanel();
        });

        redoBtn.setOnAction(e -> {
            currentRepo.redoStage();
            saveData();
            showStagingPanel();
        });

        controls.getChildren().addAll(fileCombo, stageBtn, unstageBtn, undoBtn, redoBtn);

        // Staged files display
        Label stagedTitle = new Label("Staged Files (" + currentRepo.stagedCount + ")");
        stagedTitle.setStyle("-fx-text-fill:" + ORANGE + ";-fx-font-size:15px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

        VBox stagedList = new VBox(6);

        if (currentRepo.stagedCount == 0) {
            stagedList.getChildren().add(subLabel("Nothing staged. Select a file above and click Stage."));
        } else {
            for (int i = 0; i < currentRepo.stagedCount; i++) {
                FileEntry fe = currentRepo.stagedFiles[i];

                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 14, 10, 14));
                row.setStyle("-fx-background-color:" + BG_CARD
                           + ";-fx-border-color:" + ORANGE
                           + ";-fx-border-radius:6"
                           + ";-fx-background-radius:6;");

                Label dot   = new Label("*");
                dot.setStyle("-fx-text-fill:" + ORANGE + ";");

                Label fname = new Label(fe.getFilename());
                fname.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:13px;-fx-font-family:'Consolas';");

                row.getChildren().addAll(dot, fname);
                stagedList.getChildren().add(row);
            }
        }

        Label stackNote = subLabel("Stack used for Undo/Redo — push on stage, pop on undo");

        panel.getChildren().addAll(controls, stageErr, stagedTitle, stagedList, stackNote);

        ScrollPane scroll = new ScrollPane(panel);
        scroll.setStyle("-fx-background-color:" + BG_DARK + ";-fx-background:" + BG_DARK + ";");
        scroll.setFitToWidth(true);

        setContent(scroll);
        setStatus("Staging — " + currentRepo.stagedCount + " file(s) staged");
    }


    // ================================================================
    //  BRANCHES PANEL
    // ================================================================

    void showBranchPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(30));
        panel.setStyle("-fx-background-color:" + BG_DARK + ";");

        panel.getChildren().add(heading("Branches  —  " + currentRepo.getName()));

        Label activeLabel = new Label("Active branch: " + currentRepo.getActiveBranch());
        activeLabel.setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-family:'Consolas';");
        panel.getChildren().add(activeLabel);

        // Create branch row
        HBox createRow = new HBox(10);
        createRow.setAlignment(Pos.CENTER_LEFT);

        TextField branchName = styledField("feature-login");
        branchName.setPrefWidth(220);

        Label  branchErr = errorLabel();
        Button createBtn = styledBtn("Create Branch", ACCENT);

        createBtn.setOnAction(e -> {
            branchErr.setText("");
            try {
                currentRepo.createBranch(branchName.getText().trim());
                saveData();
                showSuccess(branchErr, "Branch created!");
                showBranchPanel();
            } catch (BranchNotFoundException ex) {
                showError(branchErr, ex.getMessage());
            }
        });

        createRow.getChildren().addAll(fieldLabel("Branch name:"), branchName, createBtn);

        // Switch and merge row
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> branchCombo = new ComboBox<>();
        branchCombo.setStyle("-fx-background-color:#21262d;-fx-text-fill:" + TEXT_PRI + ";-fx-font-family:'Consolas';");
        branchCombo.setPromptText("Select branch...");

        for (String bn : currentRepo.branches.getNames()) {
            branchCombo.getItems().add(bn);
        }

        Label  actionErr = errorLabel();
        Button switchBtn = styledBtn("Switch", "#21262d");
        Button mergeBtn  = styledBtn("Merge into " + currentRepo.getActiveBranch(), "#21262d");

        switchBtn.setOnAction(e -> {
            actionErr.setText("");
            String b = branchCombo.getValue();
            if (b == null) {
                showError(actionErr, "Select a branch.");
                return;
            }
            try {
                String result = currentRepo.switchBranch(b);
                saveData();
                setStatus(result);
                showBranchPanel();
            } catch (BranchNotFoundException ex) {
                showError(actionErr, ex.getMessage());
            }
        });

        mergeBtn.setOnAction(e -> {
            actionErr.setText("");
            String b = branchCombo.getValue();
            if (b == null) {
                showError(actionErr, "Select a branch.");
                return;
            }
            try {
                String result = currentRepo.mergeBranch(b, currentUser.getUsername());
                saveData();
                showAlert("Merge", result, false);
                showBranchPanel();
            } catch (BranchNotFoundException ex) {
                showError(actionErr, ex.getMessage());
            } catch (EmptyCommitException ex) {
                showError(actionErr, ex.getMessage());
            }
        });

        actionRow.getChildren().addAll(fieldLabel("Branch:"), branchCombo, switchBtn, mergeBtn);

        // Branch list
        Label listTitle = new Label("All Branches");
        listTitle.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:15px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

        VBox branchList = new VBox(8);

        for (String bn : currentRepo.branches.getNames()) {
            Branch  b        = currentRepo.branches.find(bn);
            boolean isActive = bn.equals(currentRepo.getActiveBranch());

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(12, 16, 12, 16));
            row.setStyle("-fx-background-color:" + BG_CARD
                       + ";-fx-border-color:" + (isActive ? ACCENT : BORDER)
                       + ";-fx-border-radius:8"
                       + ";-fx-background-radius:8;");

            Label dot = new Label(isActive ? "*" : "o");
            dot.setStyle("-fx-text-fill:" + (isActive ? ACCENT : TEXT_SEC) + ";-fx-font-size:16px;");

            Label bname = new Label(bn + (isActive ? "  <- HEAD" : ""));
            bname.setStyle("-fx-text-fill:" + (isActive ? ACCENT : TEXT_PRI) + ";-fx-font-size:14px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

            Label tip = new Label(b.head == null ? "no commits" : b.head.getId() + " - " + b.head.getMessage());
            tip.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:12px;-fx-font-family:'Consolas';");

            Region spr = new Region();
            HBox.setHgrow(spr, Priority.ALWAYS);

            row.getChildren().addAll(dot, bname, spr, tip);
            branchList.getChildren().add(row);
        }

        panel.getChildren().addAll(createRow, branchErr, actionRow, actionErr, listTitle, branchList);

        ScrollPane scroll = new ScrollPane(panel);
        scroll.setStyle("-fx-background-color:" + BG_DARK + ";-fx-background:" + BG_DARK + ";");
        scroll.setFitToWidth(true);

        setContent(scroll);
        setStatus("Branches — " + currentRepo.branches.size + " branch(es)");
    }


    // ================================================================
    //  COMMITS PANEL
    // ================================================================

    void showCommitsPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(30));
        panel.setStyle("-fx-background-color:" + BG_DARK + ";");

        panel.getChildren().add(heading("Commits  —  " + currentRepo.getName() + " [" + currentRepo.getActiveBranch() + "]"));

        // Make commit card
        VBox commitCard = new VBox(8);
        commitCard.setPadding(new Insets(16));
        commitCard.setStyle("-fx-background-color:" + BG_CARD
                          + ";-fx-border-color:" + (currentRepo.stagedCount > 0 ? ACCENT : BORDER)
                          + ";-fx-border-radius:8"
                          + ";-fx-background-radius:8;");

        Label commitTitle = new Label("Make a Commit  [" + currentRepo.stagedCount + " file(s) staged]");
        commitTitle.setStyle("-fx-text-fill:" + (currentRepo.stagedCount > 0 ? ACCENT : TEXT_SEC) + ";-fx-font-size:14px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

        TextField msgField  = styledField("e.g. Add login feature");
        Label     commitErr = errorLabel();

        String btnLabel = currentRepo.stagedCount > 0 ? "Commit" : "Stage files first";
        String btnColor = currentRepo.stagedCount > 0 ? ACCENT   : "#3a4048";
        Button commitBtn = styledBtn(btnLabel, btnColor);

        commitBtn.setOnAction(e -> {
            commitErr.setText("");

            if (currentRepo.stagedCount == 0) {
                showError(commitErr, "Nothing staged. Go to Staging and stage files first.");
                return;
            }

            try {
                String result = currentRepo.commit(msgField.getText().trim(), currentUser.getUsername());
                saveData();
                msgField.clear();
                setStatus(result);
                showSuccess(commitErr, result);
                showCommitsPanel();
            } catch (EmptyCommitException ex) {
                showError(commitErr, ex.getMessage());
            } catch (EmptyCommitMessageException ex) {
                showError(commitErr, ex.getMessage());
            }
        });

        commitCard.getChildren().addAll(
            commitTitle,
            fieldLabel("Commit message"), msgField,
            hintLabel("3-100 characters. Describe what changed."),
            commitErr,
            commitBtn
        );

        if (currentRepo.stagedCount == 0) {
            commitCard.getChildren().add(subLabel("Go to Staging first to stage files before committing."));
        }

        // Tools row — Search, Tag, Diff, Checkout
        HBox toolsRow = new HBox(10);
        toolsRow.setAlignment(Pos.CENTER_LEFT);

        Label toolsErr = errorLabel();
        toolsErr.setPrefWidth(Double.MAX_VALUE);

        // BST Search
        TextField searchField = styledField("c001");
        searchField.setPrefWidth(90);

        Button searchBtn = styledBtn("Search (BST)", "#21262d");
        searchBtn.setOnAction(e -> {
            toolsErr.setText("");
            try {
                Commit c = currentRepo.searchCommit(searchField.getText().trim());
                showCommitDetail(c);
            } catch (CommitNotFoundException ex) {
                showError(toolsErr, ex.getMessage());
            }
        });

        // Tag
        TextField tagId  = styledField("c001");
        tagId.setPrefWidth(65);

        TextField tagVal = styledField("v1.0");
        tagVal.setPrefWidth(65);

        Button tagBtn = styledBtn("Tag", "#21262d");
        tagBtn.setOnAction(e -> {
            toolsErr.setText("");
            try {
                if (tagVal.getText().trim().isEmpty()) {
                    throw new CommitNotFoundException("Tag label cannot be empty.");
                }
                currentRepo.tagCommit(tagId.getText().trim(), tagVal.getText().trim());
                saveData();
                showSuccess(toolsErr, "Tagged!");
                showCommitsPanel();
            } catch (CommitNotFoundException ex) {
                showError(toolsErr, ex.getMessage());
            }
        });

        // Diff
        TextField diffId1 = styledField("c001");
        diffId1.setPrefWidth(60);

        TextField diffId2 = styledField("c002");
        diffId2.setPrefWidth(60);

        TextField diffFn  = styledField("file.txt");
        diffFn.setPrefWidth(90);

        Button diffBtn = styledBtn("Diff", "#21262d");
        diffBtn.setOnAction(e -> {
            toolsErr.setText("");
            try {
                if (diffFn.getText().trim().isEmpty()) {
                    throw new FileNotFoundException2("Filename required.");
                }
                String result = currentRepo.diff(
                    diffId1.getText().trim(),
                    diffId2.getText().trim(),
                    diffFn.getText().trim()
                );
                Alert    a  = new Alert(Alert.AlertType.INFORMATION);
                TextArea ta = new TextArea(result);
                ta.setEditable(false);
                ta.setStyle("-fx-font-family:'Consolas';");
                a.setTitle("Diff Result");
                a.setHeaderText(null);
                a.getDialogPane().setContent(ta);
                a.getDialogPane().setPrefSize(560, 360);
                a.showAndWait();
            } catch (CommitNotFoundException ex) {
                showError(toolsErr, ex.getMessage());
            } catch (FileNotFoundException2 ex) {
                showError(toolsErr, ex.getMessage());
            }
        });

        // Checkout
        TextField checkoutId = styledField("c001");
        checkoutId.setPrefWidth(70);

        Button checkoutBtn = styledBtn("Checkout", "#21262d");
        checkoutBtn.setOnAction(e -> {
            toolsErr.setText("");
            try {
                String result = currentRepo.checkout(checkoutId.getText().trim());
                saveData();
                setStatus(result);
                showSuccess(toolsErr, result);
                showCommitsPanel();
            } catch (CommitNotFoundException ex) {
                showError(toolsErr, ex.getMessage());
            }
        });

        toolsRow.getChildren().addAll(
            searchField, searchBtn,
            tagId, tagVal, tagBtn,
            diffId1, diffId2, diffFn, diffBtn,
            checkoutId, checkoutBtn
        );

        // Commit history
        Label histTitle = new Label("Commit History (LinkedList — newest first)");
        histTitle.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:15px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

        VBox commitList = new VBox(6);
        java.util.List<Commit> commits = currentRepo.getAllCommits();

        if (commits.isEmpty()) {
            commitList.getChildren().add(subLabel("No commits yet. Stage a file then commit."));
        } else {
            for (Commit c : commits) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 14, 10, 14));
                row.setStyle("-fx-background-color:" + BG_CARD
                           + ";-fx-border-color:" + BORDER
                           + ";-fx-border-radius:6"
                           + ";-fx-background-radius:6"
                           + ";-fx-cursor:hand;");

                Label cid = new Label(c.getId());
                cid.setStyle("-fx-text-fill:" + BLUE + ";-fx-font-size:13px;-fx-font-weight:bold;-fx-font-family:'Consolas';-fx-min-width:50;");

                String tagStr = (c.getTag() != null && !c.getTag().isEmpty()) ? "  [" + c.getTag() + "]" : "";

                Label cmsg = new Label(c.getMessage() + tagStr);
                cmsg.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:13px;-fx-font-family:'Consolas';");

                Region spr = new Region();
                HBox.setHgrow(spr, Priority.ALWAYS);

                Label cauthor = new Label(c.getAuthor());
                cauthor.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:12px;-fx-font-family:'Consolas';");

                Label cbranch = new Label("[" + c.getBranch() + "]");
                cbranch.setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-size:12px;-fx-font-family:'Consolas';");

                Label ctime = new Label(c.getTimestamp());
                ctime.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-size:11px;-fx-font-family:'Consolas';");

                row.getChildren().addAll(cid, cmsg, spr, cauthor, cbranch, ctime);
                row.setOnMouseClicked(e -> showCommitDetail(c));
                commitList.getChildren().add(row);
            }
        }

        panel.getChildren().addAll(commitCard, toolsRow, toolsErr, histTitle, commitList);

        ScrollPane scroll = new ScrollPane(panel);
        scroll.setStyle("-fx-background-color:" + BG_DARK + ";-fx-background:" + BG_DARK + ";");
        scroll.setFitToWidth(true);

        setContent(scroll);
        setStatus("Commits — " + commits.size() + " total | " + currentRepo.stagedCount + " staged");
    }

    void showCommitDetail(Commit c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Commit: " + c.getId());
        a.setHeaderText(c.toString());

        StringBuilder sb = new StringBuilder("Files in this commit:\n");
        for (int i = 0; i < c.getFileCount(); i++) {
            sb.append("  - ").append(c.getFile(i).getFilename()).append("\n");
        }

        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false);
        ta.setStyle("-fx-font-family:'Consolas';");

        a.getDialogPane().setContent(ta);
        a.getDialogPane().setPrefSize(500, 260);
        a.showAndWait();
    }


    // ================================================================
    //  CONTRIBUTORS PANEL
    // ================================================================

    void showContributorsPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(30));
        panel.setStyle("-fx-background-color:" + BG_DARK + ";");

        panel.getChildren().add(heading("Contributors  —  " + currentRepo.getName()));

        // Feature 1: use SHARED remote contributors if pushed
        // so ALL users who contributed are visible to everyone
        boolean usingRemote = remoteStore.exists(currentRepo.getName());

        java.util.List<String[]> lb;

        if (usingRemote) {
            lb = remoteStore.getSharedContributors(currentRepo.getName()).getLeaderboard();
            panel.getChildren().add(subLabel("Shared contributors from remote — visible to all users"));
        } else {
            lb = currentRepo.contributors.getLeaderboard();
            panel.getChildren().add(subLabel("Local contributors only — push to share with others"));
        }

        String[] medals = {"1st", "2nd", "3rd"};
        VBox     list   = new VBox(10);

        if (lb.isEmpty()) {
            list.getChildren().add(subLabel("No contributors yet. Make commits and push to remote."));
        } else {
            for (int i = 0; i < lb.size(); i++) {
                String[] entry = lb.get(i);

                HBox row = new HBox(16);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(16, 20, 16, 20));
                row.setStyle("-fx-background-color:" + BG_CARD
                           + ";-fx-border-color:" + BORDER
                           + ";-fx-border-radius:10"
                           + ";-fx-background-radius:10;");

                Label rank  = new Label(i < 3 ? medals[i] : String.valueOf(i + 1));
                rank.setStyle("-fx-text-fill:" + ORANGE + ";-fx-font-size:14px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

                Label name  = new Label(entry[0]);
                name.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:16px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);

                Label count = new Label(entry[1] + " commits");
                count.setStyle("-fx-text-fill:" + BLUE + ";-fx-font-size:15px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

                row.getChildren().addAll(rank, name, sp, count);
                list.getChildren().add(row);
            }
        }

        panel.getChildren().add(list);

        ScrollPane scroll = new ScrollPane(panel);
        scroll.setStyle("-fx-background-color:" + BG_DARK + ";-fx-background:" + BG_DARK + ";");
        scroll.setFitToWidth(true);

        setContent(scroll);
        setStatus("Contributors — " + (usingRemote ? "shared remote" : "local only"));
    }


    // ================================================================
    //  REMOTE PANEL
    // ================================================================

    void showRemotePanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(30));
        panel.setStyle("-fx-background-color:" + BG_DARK + ";");

        panel.getChildren().add(heading("Remote  —  " + currentRepo.getName()));

        // ── Push button ──────────────────────────────────────────
        Label remoteErr = errorLabel();

        Button pushBtn = styledBtn("Push to Remote", ACCENT);
        pushBtn.setOnAction(e -> {
            remoteErr.setText("");
            try {
                String result = currentRepo.push(remoteStore);
                saveData();
                showAlert("Push", result, false);
                showRemotePanel();
            } catch (EmptyCommitException ex) {
                showError(remoteErr, ex.getMessage());
            }
        });

        // ── Pull section with mode selector ──────────────────────
        Label pullTitle = new Label("Pull Options");
        pullTitle.setStyle("-fx-text-fill:" + TEXT_PRI
                         + ";-fx-font-size:14px"
                         + ";-fx-font-weight:bold"
                         + ";-fx-font-family:'Consolas';");

        // Feature 2: let user choose pull mode
        ToggleGroup modeGroup     = new ToggleGroup();
        RadioButton overwriteMode = new RadioButton("Overwrite Mode — replace local file content with remote");
        RadioButton appendMode    = new RadioButton("Append Mode — keep local content and add remote content below");

        overwriteMode.setToggleGroup(modeGroup);
        appendMode.setToggleGroup(modeGroup);
        overwriteMode.setSelected(true);

        String radioStyle = "-fx-text-fill:" + TEXT_PRI + ";-fx-font-family:'Consolas';-fx-font-size:12px;";
        overwriteMode.setStyle(radioStyle);
        appendMode.setStyle(radioStyle);

        // Mode description card
        VBox modeCard = new VBox(8);
        modeCard.setPadding(new Insets(14));
        modeCard.setStyle("-fx-background-color:" + BG_CARD
                        + ";-fx-border-color:" + BORDER
                        + ";-fx-border-radius:8"
                        + ";-fx-background-radius:8;");

        Label modeTitle = new Label("Select Pull Mode:");
        modeTitle.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:13px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

        Label modeHint = subLabel("Overwrite: remote file replaces local file completely\n"
                                + "Append:   local file content is kept, remote content added at the end");

        modeCard.getChildren().addAll(modeTitle, overwriteMode, appendMode, modeHint);

        Button pullBtn = styledBtn("Pull from Remote", "#21262d");
        pullBtn.setOnAction(e -> {
            remoteErr.setText("");
            boolean useAppend = appendMode.isSelected();

            try {
                // Feature 2 + 3 + 4: use improved pull
                Repository.PullResult result = currentRepo.pull(remoteStore, useAppend);
                saveData();

                // Feature 4: if conflicts found — show resolution dialog
                if (result.hasConflicts()) {
                    showConflictDialog(result.conflicts);
                } else {
                    showAlert("Pull", result.message, false);
                }

                showRemotePanel();

            } catch (EmptyCommitException ex) {
                showError(remoteErr, ex.getMessage());
            }
        });

        HBox btnRow = new HBox(12);
        btnRow.getChildren().addAll(pushBtn, pullBtn);

        // ── Push status list ─────────────────────────────────────
        Label statusTitle = new Label("Push Status");
        statusTitle.setStyle("-fx-text-fill:" + TEXT_PRI
                           + ";-fx-font-size:15px"
                           + ";-fx-font-weight:bold"
                           + ";-fx-font-family:'Consolas';");

        VBox statusList = new VBox(6);
        java.util.List<String[]> ps = currentRepo.getPushStatus(remoteStore);

        if (ps.isEmpty()) {
            statusList.getChildren().add(subLabel("No commits yet. Make a commit then push."));
        } else {
            for (String[] entry : ps) {
                boolean pushed = entry[0].equals("PUSHED");

                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 14, 10, 14));
                row.setStyle("-fx-background-color:" + BG_CARD
                           + ";-fx-border-color:" + (pushed ? ACCENT : ORANGE)
                           + ";-fx-border-radius:6"
                           + ";-fx-background-radius:6;");

                Label status = new Label(pushed ? "PUSHED" : "LOCAL");
                status.setStyle("-fx-text-fill:" + (pushed ? ACCENT : ORANGE)
                              + ";-fx-font-size:12px"
                              + ";-fx-font-weight:bold"
                              + ";-fx-font-family:'Consolas'"
                              + ";-fx-min-width:70;");

                Label cid = new Label(entry[1]);
                cid.setStyle("-fx-text-fill:" + BLUE + ";-fx-font-family:'Consolas';-fx-font-size:13px;");

                Label cmsg = new Label(entry[2]);
                cmsg.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-family:'Consolas';-fx-font-size:13px;");

                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);

                Label cauth = new Label(entry[3]);
                cauth.setStyle("-fx-text-fill:" + TEXT_SEC + ";-fx-font-family:'Consolas';-fx-font-size:12px;");

                row.getChildren().addAll(status, cid, cmsg, sp, cauth);
                statusList.getChildren().add(row);
            }
        }

        panel.getChildren().addAll(btnRow, modeCard, remoteErr, statusTitle, statusList);

        ScrollPane scroll = new ScrollPane(panel);
        scroll.setStyle("-fx-background-color:" + BG_DARK + ";-fx-background:" + BG_DARK + ";");
        scroll.setFitToWidth(true);

        setContent(scroll);
        setStatus("Remote — " + (remoteStore.exists(currentRepo.getName()) ? "Connected" : "Empty"));
    }


    // ================================================================
    //  FEATURE 4 — CONFLICT RESOLUTION DIALOG
    //  Shows each conflict with 3 options:
    //  [Keep Local]  [Keep Remote]  [Manual Merge]
    // ================================================================

    void showConflictDialog(java.util.List<Repository.ConflictInfo> conflicts) {
        // Create a stage for conflict resolution
        Stage conflictStage = new Stage();
        conflictStage.setTitle("Merge Conflicts Detected");
        conflictStage.initModality(Modality.APPLICATION_MODAL);

        VBox mainPanel = new VBox(16);
        mainPanel.setPadding(new Insets(24));
        mainPanel.setStyle("-fx-background-color:" + BG_DARK + ";");

        Label title = new Label("Conflicts Detected (" + conflicts.size() + " file(s))");
        title.setStyle("-fx-text-fill:" + RED + ";-fx-font-size:18px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

        Label hint = subLabel("For each conflicting file, choose how to resolve it.");

        mainPanel.getChildren().addAll(title, hint);

        for (Repository.ConflictInfo conflict : conflicts) {

            VBox conflictCard = new VBox(10);
            conflictCard.setPadding(new Insets(16));
            conflictCard.setStyle("-fx-background-color:" + BG_CARD
                                + ";-fx-border-color:" + RED
                                + ";-fx-border-radius:8"
                                + ";-fx-background-radius:8;");

            Label fname = new Label("CONFLICT: " + conflict.filename);
            fname.setStyle("-fx-text-fill:" + RED + ";-fx-font-size:14px;-fx-font-weight:bold;-fx-font-family:'Consolas';");

            // Show both versions side by side
            HBox versionsRow = new HBox(12);

            // Local version
            VBox localBox = new VBox(6);
            localBox.setPrefWidth(280);
            Label localTitle = new Label("Your Local Version:");
            localTitle.setStyle("-fx-text-fill:" + BLUE + ";-fx-font-size:12px;-fx-font-weight:bold;-fx-font-family:'Consolas';");
            TextArea localArea = new TextArea(conflict.localContent);
            localArea.setEditable(false);
            localArea.setPrefRowCount(5);
            localArea.setStyle("-fx-background-color:#21262d;-fx-text-fill:" + TEXT_PRI + ";-fx-font-family:'Consolas';-fx-font-size:12px;");
            localBox.getChildren().addAll(localTitle, localArea);

            // Remote version
            VBox remoteBox = new VBox(6);
            remoteBox.setPrefWidth(280);
            Label remoteTitle = new Label("Remote Version:");
            remoteTitle.setStyle("-fx-text-fill:" + ORANGE + ";-fx-font-size:12px;-fx-font-weight:bold;-fx-font-family:'Consolas';");
            TextArea remoteArea = new TextArea(conflict.remoteContent);
            remoteArea.setEditable(false);
            remoteArea.setPrefRowCount(5);
            remoteArea.setStyle("-fx-background-color:#21262d;-fx-text-fill:" + TEXT_PRI + ";-fx-font-family:'Consolas';-fx-font-size:12px;");
            remoteBox.getChildren().addAll(remoteTitle, remoteArea);

            versionsRow.getChildren().addAll(localBox, remoteBox);

            // Manual merge area
            VBox manualBox = new VBox(6);
            Label manualTitle = new Label("Manual Merge (edit below, then click Apply):");
            manualTitle.setStyle("-fx-text-fill:" + TEXT_PRI + ";-fx-font-size:12px;-fx-font-family:'Consolas';");
            TextArea manualArea = new TextArea(conflict.localContent + "\n" + conflict.remoteContent);
            manualArea.setPrefRowCount(4);
            manualArea.setStyle("-fx-background-color:#21262d;-fx-text-fill:" + TEXT_PRI + ";-fx-font-family:'Consolas';-fx-font-size:12px;");
            manualBox.getChildren().addAll(manualTitle, manualArea);

            // Resolution buttons
            HBox btnRow = new HBox(10);

            Button keepLocalBtn  = styledBtn("Keep Local Version",  "#21262d");
            Button keepRemoteBtn = styledBtn("Keep Remote Version", "#21262d");
            Button applyManualBtn = styledBtn("Apply Manual Merge", ACCENT);

            keepLocalBtn.setOnAction(ev -> {
                currentRepo.resolveConflict(conflict.filename, true, null);
                saveData();
                conflictCard.setStyle("-fx-background-color:" + BG_CARD
                                    + ";-fx-border-color:" + ACCENT
                                    + ";-fx-border-radius:8"
                                    + ";-fx-background-radius:8;");
                fname.setText("RESOLVED (local kept): " + conflict.filename);
                fname.setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-size:14px;-fx-font-weight:bold;-fx-font-family:'Consolas';");
            });

            keepRemoteBtn.setOnAction(ev -> {
                currentRepo.resolveConflict(conflict.filename, false, null);
                saveData();
                conflictCard.setStyle("-fx-background-color:" + BG_CARD
                                    + ";-fx-border-color:" + ACCENT
                                    + ";-fx-border-radius:8"
                                    + ";-fx-background-radius:8;");
                fname.setText("RESOLVED (remote kept): " + conflict.filename);
                fname.setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-size:14px;-fx-font-weight:bold;-fx-font-family:'Consolas';");
            });

            applyManualBtn.setOnAction(ev -> {
                String manualContent = manualArea.getText();
                currentRepo.resolveConflict(conflict.filename, false, manualContent);
                saveData();
                conflictCard.setStyle("-fx-background-color:" + BG_CARD
                                    + ";-fx-border-color:" + ACCENT
                                    + ";-fx-border-radius:8"
                                    + ";-fx-background-radius:8;");
                fname.setText("RESOLVED (manual merge): " + conflict.filename);
                fname.setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-size:14px;-fx-font-weight:bold;-fx-font-family:'Consolas';");
            });

            btnRow.getChildren().addAll(keepLocalBtn, keepRemoteBtn, applyManualBtn);

            conflictCard.getChildren().addAll(fname, versionsRow, manualBox, btnRow);
            mainPanel.getChildren().add(conflictCard);
        }

        Button doneBtn = styledBtn("Done — Close", ACCENT);
        doneBtn.setOnAction(ev -> conflictStage.close());
        mainPanel.getChildren().add(doneBtn);

        ScrollPane scroll = new ScrollPane(mainPanel);
        scroll.setStyle("-fx-background-color:" + BG_DARK + ";-fx-background:" + BG_DARK + ";");
        scroll.setFitToWidth(true);

        Scene scene = new Scene(scroll, 700, 600);
        conflictStage.setScene(scene);
        conflictStage.showAndWait();
    }
}