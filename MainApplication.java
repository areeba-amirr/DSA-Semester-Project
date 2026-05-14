import java.util.Scanner;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// ============================================================
//  SIMPLIFIED GIT — DSA Semester Project
//  Data Structures used:
//    - Stack       : Undo/Redo staging actions
//    - LinkedList  : Commit history chain, User list
//    - BST         : Search commits by ID
//    - Custom Array: Staging area, working files, repos
// ============================================================


// ============================================================
//  SECTION 1 — STACK (reused from original, with redo fix)
// ============================================================

class StackNode {
    String data;
    StackNode next;
    StackNode(String data) { this.data = data; }
}

class MyStack {
    private StackNode top;

    void push(String data) {
        StackNode n = new StackNode(data);
        n.next = top;
        top = n;
    }

    String pop() {
        if (top == null) return null;
        String data = top.data;
        top = top.next;
        return data;
    }

    String peek() {
        return top == null ? null : top.data;
    }

    boolean isEmpty() { return top == null; }

    void clear() { top = null; }
}


// ============================================================
//  SECTION 2 — CORE DATA CLASSES
// ============================================================

// Represents a single file (name + content)
class FileEntry {
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
    void   setContent(String content) { this.content = content; }

    public String toString() {
        return "[" + filename + "]\n" + content;
    }
}

// One commit = one node in the commit LinkedList
class Commit {
    private String    id;
    private String    message;
    private String    author;
    private String    timestamp;
    private FileEntry[] files;
    private int       fileCount;
    Commit parent;                    // LinkedList pointer to previous commit

    Commit(String id, String message, String author,
           FileEntry[] files, int fileCount) {
        this.id        = id;
        this.message   = message;
        this.author    = author;
        this.timestamp = LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        this.fileCount = fileCount;
        this.parent    = null;

        // Deep copy every file so future edits never corrupt this snapshot
        this.files = new FileEntry[fileCount];
        for (int i = 0; i < fileCount; i++)
            this.files[i] = new FileEntry(files[i]);
    }

    String    getId()        { return id; }
    String    getMessage()   { return message; }
    String    getAuthor()    { return author; }
    String    getTimestamp() { return timestamp; }
    int       getFileCount() { return fileCount; }
    FileEntry getFile(int i) { return (i >= 0 && i < fileCount) ? files[i] : null; }

    FileEntry findFile(String filename) {
        for (int i = 0; i < fileCount; i++)
            if (files[i].getFilename().equals(filename))
                return files[i];
        return null;
    }

    public String toString() {
        return "* " + id + " - " + message +
               " (" + author + ") [" + timestamp + "]";
    }
}


// ============================================================
//  SECTION 3 — COMMIT LINKED LIST
// ============================================================

class CommitList {
    Commit head;         // most recent commit (front of list)
    private int size;

    void add(Commit c) {
        c.parent = head; // new commit points to old head
        head     = c;    // head moves to new commit
        size++;
    }

    // Print full commit history (newest -> oldest)
    void printLog() {
        if (head == null) {
            System.out.println("  No commits yet.");
            return;
        }
        Commit curr = head;
        while (curr != null) {
            System.out.println("  " + curr);
            curr = curr.parent;
        }
    }

    // Find commit by ID (linear search through chain)
    Commit findById(String id) {
        Commit curr = head;
        while (curr != null) {
            if (curr.getId().equals(id)) return curr;
            curr = curr.parent;
        }
        return null;
    }

    int size() { return size; }
}


// ============================================================
//  SECTION 4 — BST FOR COMMIT SEARCH
//  Allows fast lookup of commits by ID
// ============================================================

class BSTNode {
    String commitId;
    Commit commit;
    BSTNode left, right;

    BSTNode(Commit c) {
        this.commitId = c.getId();
        this.commit   = c;
    }
}

class CommitBST {
    private BSTNode root;

    // Insert a commit into the BST by its ID
    private BSTNode insert(BSTNode node, Commit c) {
        if (node == null) return new BSTNode(c);
        int cmp = c.getId().compareTo(node.commitId);
        if      (cmp < 0) node.left  = insert(node.left,  c);
        else if (cmp > 0) node.right = insert(node.right, c);
        return node;
    }

    void insert(Commit c) { root = insert(root, c); }

    // Search for a commit by ID
    private BSTNode search(BSTNode node, String id) {
        if (node == null || node.commitId.equals(id)) return node;
        return id.compareTo(node.commitId) < 0
            ? search(node.left,  id)
            : search(node.right, id);
    }

    Commit search(String id) {
        BSTNode n = search(root, id);
        return n == null ? null : n.commit;
    }

    // Print all commits in sorted order (in-order traversal)
    private void inorder(BSTNode node) {
        if (node == null) return;
        inorder(node.left);
        System.out.println("  " + node.commit);
        inorder(node.right);
    }

    void printAll() {
        if (root == null) { System.out.println("  No commits indexed."); return; }
        inorder(root);
    }
}


// ============================================================
//  SECTION 4B — REMOTE REPOSITORY (NEW — Push/Pull support)
//
//  RemoteRepo  : Simulates a remote server repo (like GitHub).
//                Stores its own CommitList (the "cloud" copy).
//  RemoteStore : Global registry of all remote repos.
//                Uses a linked list of RemoteRepo nodes.
// ============================================================

// One remote repository — shared across users who push/pull
class RemoteRepo {
    String     name;          // same name as the local repo
    CommitList commits;       // commits that have been pushed here
    RemoteRepo next;          // pointer for RemoteStore linked list

    RemoteRepo(String name) {
        this.name    = name;
        this.commits = new CommitList();
        this.next    = null;
    }
}

// Global store — one RemoteRepo per unique repo name
class RemoteStore {
    private RemoteRepo head;   // linked list of RemoteRepo nodes

    // Get existing remote repo, or create it if it doesn't exist yet
    RemoteRepo getOrCreate(String name) {
        RemoteRepo curr = head;
        while (curr != null) {
            if (curr.name.equals(name)) return curr;
            curr = curr.next;
        }
        // Not found — create new remote repo
        RemoteRepo r = new RemoteRepo(name);
        r.next = head;
        head   = r;
        return r;
    }

    // Check if a remote repo exists (for pull — warn if nothing pushed yet)
    boolean exists(String name) {
        RemoteRepo curr = head;
        while (curr != null) {
            if (curr.name.equals(name)) return curr.commits.head != null;
            curr = curr.next;
        }
        return false;
    }
}


// ============================================================
//  SECTION 5 — REPOSITORY
//  (existing code unchanged — new push/pull fields & methods added at bottom)
// ============================================================

class Repository {
    private String     name;
    private String     ownerUsername;
    CommitList         history;       // LinkedList of commits
    CommitBST          commitIndex;   // BST for fast commit search
    private int        commitCounter;

    // Staging area
    // package-accessible so commitMenu() can check stagedCount for the guard
    FileEntry[] stagedFiles;
    int         stagedCount;
    private static final int MAX_STAGED = 20;

    // Undo/Redo for staging actions (Stack)
    private MyStack undoStack;
    private MyStack redoStack;

    // Working files (current editable state)
    private FileEntry[] workingFiles;
    private int         workingCount;
    private static final int MAX_FILES = 20;

    // ── NEW: Push/Pull tracking ──────────────────────────────
    // Stores IDs of commits already pushed to remote
    // so we don't push the same commit twice
    private String[] pushedIds;
    private int      pushedCount;
    private static final int MAX_PUSHED = 100;
    // ────────────────────────────────────────────────────────

    Repository(String name, String ownerUsername) {
        this.name          = name;
        this.ownerUsername = ownerUsername;
        this.history       = new CommitList();
        this.commitIndex   = new CommitBST();
        this.commitCounter = 0;
        this.stagedFiles   = new FileEntry[MAX_STAGED];
        this.stagedCount   = 0;
        this.undoStack     = new MyStack();
        this.redoStack     = new MyStack();
        this.workingFiles  = new FileEntry[MAX_FILES];
        this.workingCount  = 0;

        // NEW — initialise push tracking array
        this.pushedIds  = new String[MAX_PUSHED];
        this.pushedCount = 0;
    }

    String getName()          { return name; }
    String getOwnerUsername() { return ownerUsername; }

    // Auto-generate commit IDs: c001, c002, ...
    private String nextCommitId() {
        commitCounter++;
        return String.format("c%03d", commitCounter);
    }

    // ---- Working File Operations ----

    void writeFile(String filename, String content) {
        for (int i = 0; i < workingCount; i++) {
            if (workingFiles[i].getFilename().equals(filename)) {
                workingFiles[i].setContent(content);
                System.out.println("  Updated: " + filename);
                return;
            }
        }
        if (workingCount < MAX_FILES) {
            workingFiles[workingCount++] = new FileEntry(filename, content);
            System.out.println("  Created: " + filename);
        } else {
            System.out.println("  Working area full.");
        }
    }

    void showWorkingFiles() {
        if (workingCount == 0) { System.out.println("  No files in working area."); return; }
        System.out.println("  Working files:");
        for (int i = 0; i < workingCount; i++)
            System.out.println("    - " + workingFiles[i].getFilename());
    }

    void viewFile(String filename) {
        for (int i = 0; i < workingCount; i++) {
            if (workingFiles[i].getFilename().equals(filename)) {
                System.out.println(workingFiles[i]);
                return;
            }
        }
        System.out.println("  File not found: " + filename);
    }

    FileEntry getWorkingFile(String filename) {
        for (int i = 0; i < workingCount; i++)
            if (workingFiles[i].getFilename().equals(filename))
                return workingFiles[i];
        return null;
    }

    // ---- Staging Operations ----

    void stageFile(String filename) {
        FileEntry wf = getWorkingFile(filename);
        if (wf == null) {
            System.out.println("  File not found in working area: " + filename);
            return;
        }
        for (int i = 0; i < stagedCount; i++) {
            if (stagedFiles[i].getFilename().equals(filename)) {
                stagedFiles[i] = new FileEntry(wf);
                undoStack.push("unstage:" + filename);
                redoStack.clear();
                System.out.println("  Re-staged: " + filename);
                return;
            }
        }
        if (stagedCount < MAX_STAGED) {
            stagedFiles[stagedCount++] = new FileEntry(wf);
            undoStack.push("unstage:" + filename);
            redoStack.clear();
            System.out.println("  Staged: " + filename);
        } else {
            System.out.println("  Staging area full.");
        }
    }

    void unstageFile(String filename) {
        for (int i = 0; i < stagedCount; i++) {
            if (stagedFiles[i].getFilename().equals(filename)) {
                // Shift array left
                for (int j = i; j < stagedCount - 1; j++)
                    stagedFiles[j] = stagedFiles[j + 1];
                stagedFiles[--stagedCount] = null;
                System.out.println("  Unstaged: " + filename);
                return;
            }
        }
        System.out.println("  File not in staging area: " + filename);
    }

    void showStagedFiles() {
        if (stagedCount == 0) { System.out.println("  Nothing staged."); return; }
        System.out.println("  Staged files:");
        for (int i = 0; i < stagedCount; i++)
            System.out.println("    - " + stagedFiles[i].getFilename());
    }

    // Undo last stage action
    void undoStage() {
        String action = undoStack.pop();
        if (action == null) { System.out.println("  Nothing to undo."); return; }
        if (action.startsWith("unstage:")) {
            String filename = action.substring(8);
            unstageFile(filename);
            redoStack.push("stage:" + filename);
            System.out.println("  Undo: unstaged " + filename);
        }
    }

    // Redo last undone stage action
    void redoStage() {
        String action = redoStack.pop();
        if (action == null) { System.out.println("  Nothing to redo."); return; }
        if (action.startsWith("stage:")) {
            String filename = action.substring(6);
            stageFile(filename);
            System.out.println("  Redo: staged " + filename);
        }
    }

    // ---- Commit ----

    void commit(String message, String author) {
        if (stagedCount == 0) {
            System.out.println("  Nothing staged to commit.");
            return;
        }
        String id = nextCommitId();
        Commit c  = new Commit(id, message, author, stagedFiles, stagedCount);
        history.add(c);
        commitIndex.insert(c);

        // Clear staging after commit
        stagedFiles = new FileEntry[MAX_STAGED];
        stagedCount = 0;
        undoStack.clear();
        redoStack.clear();

        System.out.println("  Committed: " + c);
    }

    // ---- Log & Search ----

    void printLog() { history.printLog(); }

    void searchCommit(String id) {
        Commit c = commitIndex.search(id);
        if (c == null) System.out.println("  Commit not found: " + id);
        else {
            System.out.println("  Found: " + c);
            System.out.println("  Files in this commit:");
            for (int i = 0; i < c.getFileCount(); i++)
                System.out.println("    - " + c.getFile(i).getFilename());
        }
    }

    // Print all commits sorted by ID (BST in-order)
    void printAllSorted() { commitIndex.printAll(); }

    // ---- Checkout (commented out as in original) ----

//    void checkout(String commitId) {
//        Commit c = commitIndex.search(commitId);
//        if (c == null) { System.out.println("  Commit not found: " + commitId); return; }
//
//        workingFiles = new FileEntry[MAX_FILES];
//        workingCount = 0;
//        for (int i = 0; i < c.getFileCount(); i++)
//            workingFiles[workingCount++] = new FileEntry(c.getFile(i));
//
//        System.out.println("  Checked out commit: " + commitId);
//        System.out.println("  Working area restored to that snapshot.");
//    }

    // ---- Diff ----

    // Compare a file between two commits
    void diff(String commitId1, String commitId2, String filename) {
        Commit c1 = commitIndex.search(commitId1);
        Commit c2 = commitIndex.search(commitId2);

        if (c1 == null) { System.out.println("  Commit not found: " + commitId1); return; }
        if (c2 == null) { System.out.println("  Commit not found: " + commitId2); return; }

        FileEntry f1 = c1.findFile(filename);
        FileEntry f2 = c2.findFile(filename);

        if (f1 == null) { System.out.println("  File not in commit " + commitId1); return; }
        if (f2 == null) { System.out.println("  File not in commit " + commitId2); return; }

        System.out.println("  --- " + commitId1 + " : " + filename);
        System.out.println(f1.getContent());
        System.out.println("  +++ " + commitId2 + " : " + filename);
        System.out.println(f2.getContent());

        if (f1.getContent().equals(f2.getContent()))
            System.out.println("  (No changes)");
        else
            System.out.println("  (File differs between commits)");
    }

    // ---- ASCII Commit Graph ----

    void printGraph() {
        if (history.head == null) { System.out.println("  No commits yet."); return; }
        System.out.println("  Commit Graph (newest -> oldest):");
        Commit curr = history.head;
        boolean first = true;
        while (curr != null) {
            if (first) {
                System.out.println("  HEAD");
                System.out.println("   |");
                first = false;
            }
            System.out.println("  [" + curr.getId() + "] " + curr.getMessage() +
                               " - " + curr.getAuthor());
            if (curr.parent != null) System.out.println("   |");
            curr = curr.parent;
        }
    }

    public String toString() {
        return "  " + name + " (owner: " + ownerUsername +
               ", commits: " + history.size() + ")";
    }

    // ============================================================
    //  NEW — PUSH & PULL METHODS
    // ============================================================

    // Helper: check if a commit ID is already marked as pushed
    private boolean isPushed(String id) {
        for (int i = 0; i < pushedCount; i++)
            if (pushedIds[i].equals(id)) return true;
        return false;
    }

    // Helper: mark a commit ID as pushed
    private void markPushed(String id) {
        if (!isPushed(id) && pushedCount < MAX_PUSHED)
            pushedIds[pushedCount++] = id;
    }

    // Helper: collect all local commits into an array (oldest first)
    //         needed so we push in chronological order
    private Commit[] collectCommitsOldestFirst() {
        int total = history.size();
        if (total == 0) return new Commit[0];

        Commit[] arr = new Commit[total];
        Commit curr = history.head;
        for (int i = total - 1; i >= 0; i--) {
            arr[i] = curr;
            curr   = curr.parent;
        }
        return arr;
    }

    // ── PUSH ────────────────────────────────────────────────
    // Sends all local commits that have NOT been pushed yet
    // to the shared RemoteRepo. Like: git push origin main
    void push(RemoteStore remoteStore) {
        if (history.head == null) {
            System.out.println("  Nothing to push. No commits in this repository.");
            return;
        }

        // Get (or create) the remote repo for this repo name
        RemoteRepo remote = remoteStore.getOrCreate(name);

        // Collect local commits oldest-first
        Commit[] localCommits = collectCommitsOldestFirst();

        int pushedNow = 0;
        for (Commit c : localCommits) {
            if (!isPushed(c.getId())) {
                // Send this commit to remote
                remote.commits.add(c);
                markPushed(c.getId());
                System.out.println("  Pushed: " + c);
                pushedNow++;
            }
        }

        if (pushedNow == 0) {
            System.out.println("  Everything up to date. Nothing new to push.");
        } else {
            System.out.println("  Push complete. " + pushedNow + " commit(s) sent to remote.");
            System.out.println("  Remote: " + name + " now has " + remote.commits.size() + " commit(s).");
        }
    }

    // ── PULL ────────────────────────────────────────────────
    // Fetches commits from the RemoteRepo that are NOT in
    // local history, and adds them to local. Like: git pull
    void pull(RemoteStore remoteStore) {
        if (!remoteStore.exists(name)) {
            System.out.println("  Nothing to pull. Remote repository '" + name + "' is empty.");
            System.out.println("  Push some commits first before pulling.");
            return;
        }

        RemoteRepo remote = remoteStore.getOrCreate(name);

        // Collect remote commits oldest-first
        int total = remote.commits.size();
        Commit[] remoteCommits = new Commit[total];
        Commit curr = remote.commits.head;
        for (int i = total - 1; i >= 0; i--) {
            remoteCommits[i] = curr;
            curr             = curr.parent;
        }

        int pulledNow = 0;
        for (Commit rc : remoteCommits) {
            // Check if this commit already exists locally
            if (history.findById(rc.getId()) == null) {
                // Add to local history and BST index
                history.add(rc);
                commitIndex.insert(rc);
                markPushed(rc.getId()); // mark as pushed so we don't re-push it
                System.out.println("  Pulled: " + rc);
                pulledNow++;
            }
        }

        if (pulledNow == 0) {
            System.out.println("  Already up to date. No new commits on remote.");
        } else {
            System.out.println("  Pull complete. " + pulledNow + " commit(s) added to local history.");
        }
    }

    // ── PUSH STATUS ─────────────────────────────────────────
    // Shows which local commits are pushed and which are not
    void pushStatus(RemoteStore remoteStore) {
        if (history.head == null) {
            System.out.println("  No local commits.");
            return;
        }
        System.out.println("  Push status for: " + name);
        System.out.println("  Remote: " + (remoteStore.exists(name) ? "connected" : "empty — nothing pushed yet"));
        System.out.println();

        Commit[] localCommits = collectCommitsOldestFirst();
        for (Commit c : localCommits) {
            String status = isPushed(c.getId()) ? "[pushed]  " : "[local]   ";
            System.out.println("  " + status + c);
        }
    }
}


// ============================================================
//  SECTION 6 — USER + USER LINKED LIST
// ============================================================

class User {
    private String username;
    private String password;
    private String email;

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
        this.next      = null;
    }

    String getUsername() { return username; }
    String getPassword() { return password; }
    String getEmail()    { return email; }
    void   setPassword(String p) { this.password = p; }

    void createRepo(String repoName) {
        if (findRepo(repoName) != null) {
            System.out.println("  Repository '" + repoName + "' already exists.");
            return;
        }
        if (repoCount < MAX_REPOS) {
            repos[repoCount++] = new Repository(repoName, username);
            System.out.println("  Repository '" + repoName + "' created.");
        } else {
            System.out.println("  Max repositories reached (10).");
        }
    }

    Repository findRepo(String repoName) {
        for (int i = 0; i < repoCount; i++)
            if (repos[i].getName().equals(repoName))
                return repos[i];
        return null;
    }

    void listRepos() {
        if (repoCount == 0) { System.out.println("  No repositories."); return; }
        for (int i = 0; i < repoCount; i++)
            System.out.println(repos[i]);
    }

    public String toString() {
        return username + " (" + email + ")";
    }
}

// LinkedList of all registered users
class UserList {
    private User head;

    void add(User u) {
        u.next = head;
        head   = u;
    }

    User find(String username) {
        User curr = head;
        while (curr != null) {
            if (curr.getUsername().equals(username)) return curr;
            curr = curr.next;
        }
        return null;
    }

    boolean exists(String username) { return find(username) != null; }

    void listAll() {
        if (head == null) { System.out.println("  No users registered."); return; }
        User curr = head;
        while (curr != null) {
            System.out.println("  - " + curr);
            curr = curr.next;
        }
    }
}


// ============================================================
//  SECTION 7 — MAIN APPLICATION
// ============================================================

public class MainApplication {

    static Scanner     sc          = new Scanner(System.in);
    static UserList    users       = new UserList();
    static User        currentUser = null;   // logged-in user
    static Repository  currentRepo = null;   // active repository

    // NEW — single global RemoteStore shared by all users (simulates GitHub)
    static RemoteStore remoteStore = new RemoteStore();

    // ---- Input Helpers ----

    static String prompt(String msg) {
        System.out.print(msg);
        return sc.nextLine().trim();
    }

    static int menu(String title, String[] options) {
        System.out.println("\n╔══════════════════════════════╗");
        System.out.println("║  " + title);
        System.out.println("╠══════════════════════════════╣");
        for (int i = 0; i < options.length; i++)
            System.out.println("║  " + (i + 1) + ". " + options[i]);
        System.out.println("╚══════════════════════════════╝");
        System.out.print("  Choice: ");
        try {
            int ch = Integer.parseInt(sc.nextLine().trim());
            return ch;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ---- Auth Menus ----

    static void register() {
        System.out.println("\n-- Register --");
        String username = prompt("  Username : ");
        if (username.isEmpty()) { System.out.println("  Username cannot be empty."); return; }
        if (users.exists(username)) { System.out.println("  Username already taken."); return; }

        String password = prompt("  Password : ");
        if (password.isEmpty()) { System.out.println("  Password cannot be empty."); return; }

        String email = prompt("  Email    : ");

        users.add(new User(username, password, email));
        System.out.println("  Registered successfully! Please log in.");
    }

    static void login() {
        System.out.println("\n-- Login --");
        String username = prompt("  Username: ");
        String password = prompt("  Password: ");

        User u = users.find(username);
        if (u == null || !u.getPassword().equals(password)) {
            System.out.println("  Invalid username or password.");
            return;
        }
        currentUser = u;
        currentRepo = null;
        System.out.println("  Welcome, " + currentUser.getUsername() + "!");
    }

    static void logout() {
        System.out.println("  Goodbye, " + currentUser.getUsername() + "!");
        currentUser = null;
        currentRepo = null;
    }

    // ---- Repository Menu ----

    static void repoMenu() {
        while (true) {
            int ch = menu("REPOSITORIES", new String[]{
                "Create repository",
                "Select repository",
                "List my repositories",
                "Back"
            });
            if      (ch == 1) createRepo();
            else if (ch == 2) selectRepo();
            else if (ch == 3) currentUser.listRepos();
            else if (ch == 4) break;
            else System.out.println("  Invalid choice.");
        }
    }

    static void createRepo() {
        String name = prompt("  Repository name: ");
        if (name.isEmpty()) { System.out.println("  Name cannot be empty."); return; }
        currentUser.createRepo(name);
    }

    static void selectRepo() {
        String name = prompt("  Repository name: ");
        Repository r = currentUser.findRepo(name);
        if (r == null) { System.out.println("  Repository not found."); return; }
        currentRepo = r;
        System.out.println("  Switched to repository: " + currentRepo.getName());
    }

    // ---- File Menu ----

    static void fileMenu() {
        while (true) {
            int ch = menu("FILES  [repo: " + currentRepo.getName() + "]", new String[]{
                "Create / edit file",
                "View file",
                "List working files",
                "Back"
            });
            if      (ch == 1) createFile();
            else if (ch == 2) viewFile();
            else if (ch == 3) currentRepo.showWorkingFiles();
            else if (ch == 4) break;
            else System.out.println("  Invalid choice.");
        }
    }

    static void createFile() {
        String filename = prompt("  Filename       : ");
        if (filename.isEmpty()) { System.out.println("  Filename cannot be empty."); return; }
        System.out.println("  Content (type END on a new line to finish):");
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = sc.nextLine();
            if (line.equals("END")) break;
            sb.append(line).append("\n");
        }
        currentRepo.writeFile(filename, sb.toString().trim());
    }

    static void viewFile() {
        String filename = prompt("  Filename: ");
        currentRepo.viewFile(filename);
    }

    // ---- Staging Menu ----

    static void stagingMenu() {
        while (true) {
            int ch = menu("STAGING  [repo: " + currentRepo.getName() + "]", new String[]{
                "Stage a file",
                "Unstage a file",
                "Show staged files",
                "Undo last stage",
                "Redo last undo",
                "Back"
            });
            if      (ch == 1) { String f = prompt("  Filename: "); currentRepo.stageFile(f); }
            else if (ch == 2) { String f = prompt("  Filename: "); currentRepo.unstageFile(f); }
            else if (ch == 3) currentRepo.showStagedFiles();
            else if (ch == 4) currentRepo.undoStage();
            else if (ch == 5) currentRepo.redoStage();
            else if (ch == 6) break;
            else System.out.println("  Invalid choice.");
        }
    }

    // ---- Commit Menu ----
    // Option 1 dynamically shows/hides based on staging status.
    // NEW: Options 8 (Push), 9 (Pull), 10 (Push Status) added.

    static void commitMenu() {
        while (true) {
            boolean hasStaged = currentRepo.stagedCount > 0;

            // Print menu manually so option 1 text can change dynamically
            System.out.println("\n╔══════════════════════════════╗");
            System.out.println("║  COMMITS  [repo: " + currentRepo.getName() + "]");
            System.out.println("╠══════════════════════════════╣");

            // Option 1 changes based on whether files are staged
            if (hasStaged) {
                System.out.println("║  1. Make a commit  [" + currentRepo.stagedCount + " file(s) staged]");
            } else {
                System.out.println("║  1. (Stage a file first to unlock commit)");
            }

            System.out.println("║  2. View commit log");
            System.out.println("║  3. Search commit by ID (BST)");
            System.out.println("║  4. View all commits sorted (BST)");
//            System.out.println("║  5. Checkout a commit");
            System.out.println("║  5. Diff two commits");
            System.out.println("║  6. View commit graph");
            // ── NEW options ──────────────────────────────
            System.out.println("║  7. Push to remote");
            System.out.println("║  8. Pull from remote");
            System.out.println("║  9. Push status");
            // ────────────────────────────────────────────
            System.out.println("║  0. Back");
            System.out.println("╚══════════════════════════════╝");
            System.out.print("  Choice: ");

            int ch;
            try { ch = Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException e) { ch = -1; }

            if (ch == 1) {
                // Double guard — blocks even if user types 1 when nothing is staged
                if (!hasStaged) {
                    System.out.println("  Nothing staged. Go to Staging menu and stage a file first.");
                } else {
                    makeCommit();
                }
            }
            else if (ch == 2) currentRepo.printLog();
            else if (ch == 3) searchCommit();
            else if (ch == 4) currentRepo.printAllSorted();
//            else if (ch == 5) checkoutCommit();
            else if (ch == 5) diffCommits();
            else if (ch == 6) currentRepo.printGraph();
            // ── NEW handlers ─────────────────────────────
            else if (ch == 7) currentRepo.push(remoteStore);
            else if (ch == 8) currentRepo.pull(remoteStore);
            else if (ch == 9) currentRepo.pushStatus(remoteStore);
            // ────────────────────────────────────────────
            else if (ch == 0) break;
            else System.out.println("  Invalid choice.");
        }
    }

    static void makeCommit() {
        currentRepo.showStagedFiles();
        String msg = prompt("  Commit message: ");
        if (msg.isEmpty()) { System.out.println("  Message cannot be empty."); return; }
        currentRepo.commit(msg, currentUser.getUsername());
    }

    static void searchCommit() {
        String id = prompt("  Commit ID (e.g. c001): ");
        currentRepo.searchCommit(id);
    }

//    static void checkoutCommit() {
//        String id = prompt("  Commit ID to checkout: ");
//        currentRepo.checkout(id);
//    }

    static void diffCommits() {
        String id1  = prompt("  First commit ID  : ");
        String id2  = prompt("  Second commit ID : ");
        String file = prompt("  Filename to diff : ");
        currentRepo.diff(id1, id2, file);
    }

    // ---- Main Menus ----

    static void loggedInMenu() {
        while (true) {
            String repoLabel = currentRepo == null
                ? "none"
                : currentRepo.getName();

            int ch = menu("MAIN  [user: " + currentUser.getUsername() +
                          " | repo: " + repoLabel + "]", new String[]{
                "Repositories",
                "Files",
                "Staging",
                "Commits",
                "Logout"
            });

            if (ch == 1) repoMenu();
            else if (ch == 2) {
                if (currentRepo == null) System.out.println("  Please select a repository first.");
                else fileMenu();
            }
            else if (ch == 3) {
                if (currentRepo == null) System.out.println("  Please select a repository first.");
                else stagingMenu();
            }
            else if (ch == 4) {
                if (currentRepo == null) System.out.println("  Please select a repository first.");
                else commitMenu();
            }
            else if (ch == 5) { logout(); break; }
            else System.out.println("  Invalid choice.");
        }
    }

    // ---- Entry Point ----

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║     SimpleGit — DSA Version Control  ║");
        System.out.println("╚══════════════════════════════════════╝");

        while (true) {
            int ch = menu("WELCOME", new String[]{
                "Register",
                "Login",
                "List all users",
                "Exit"
            });

            if      (ch == 1) register();
            else if (ch == 2) {
                login();
                if (currentUser != null) loggedInMenu();
            }
            else if (ch == 3) users.listAll();
            else if (ch == 4) { System.out.println("  Goodbye!"); break; }
            else System.out.println("  Invalid choice.");
        }

        sc.close();
    }
}