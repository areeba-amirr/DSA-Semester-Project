package Gitlite;
import java.util.Scanner;


class Commit {

    String message;
    String author;
    String content;
    Commit next;

    Commit(String m, String a, String c) {
        message = m;
        author = a;
        content = c;
        next = null;
    }

    void display() {
        System.out.println("Author: " + author);
        System.out.println("Message: " + message);
        System.out.println("Content: " + content);
        System.out.println("---------------------------");
    }
}

class CommitList {

    Commit head;

    void addCommit(Commit c) {

        if (head == null) {
            head = c;
            return;
        }

        Commit temp = head;

        while (temp.next != null) {
            temp = temp.next;
        }

        temp.next = c;
    }

    void showHistory() {

        if (head == null) {
            System.out.println("No commits yet.");
            return;
        }

        Commit temp = head;

        while (temp != null) {
            temp.display();
            temp = temp.next;
        }
    }

    void removeLast() {

        if (head == null)
            return;

        if (head.next == null) {
            head = null;
            return;
        }

        Commit temp = head;

        while (temp.next.next != null) {
            temp = temp.next;
        }

        temp.next = null;
    }
}


class CommitStack {

    Commit top;

    void push(Commit c) {

        Commit node = new Commit(c.message, c.author, c.content);
        node.next = top;
        top = node;
    }

    Commit pop() {

        if (top == null)
            return null;

        Commit temp = top;
        top = top.next;
        return temp;
    }

    boolean isEmpty() {
        return top == null;
    }
}



class Repository {

    String name;
    String fileContent = "";

    CommitList history = new CommitList();
    CommitStack undoStack = new CommitStack();

    Repository(String n) {
        name = n;
    }

    void updateFile(String content) {

        fileContent = content;
        System.out.println("File updated.");
    }

    void commit(String msg, String author) {

        Commit c = new Commit(msg, author, fileContent);

        history.addCommit(c);
        undoStack.push(c);

        System.out.println("Commit added.");
    }

    void viewHistory() {

        history.showHistory();
    }

    void undoCommit() {

        if (undoStack.isEmpty()) {
            System.out.println("Nothing to undo.");
            return;
        }

        undoStack.pop();
        history.removeLast();

        System.out.println("Last commit undone.");
    }
}

class User {

    String username;
    String password;

    Repository repos[] = new Repository[10];
    int repoCount = 0;

    User(String u, String p) {
        username = u;
        password = p;
    }

    void createRepo(String name) {

        repos[repoCount] = new Repository(name);
        repoCount++;

        System.out.println("Repository created.");
    }

    Repository getRepo(String name) {

        for (int i = 0; i < repoCount; i++) {

            if (repos[i].name.equals(name))
                return repos[i];
        }

        return null;
    }

    void listRepos() {

        if (repoCount == 0) {
            System.out.println("No repositories.");
            return;
        }

        for (int i = 0; i < repoCount; i++)
            System.out.println((i + 1) + ". " + repos[i].name);
    }
}


class GitHubSystem {

    User users[] = new User[10];
    int count = 0;

    void register(String u, String p) {

        users[count] = new User(u, p);
        count++;

        System.out.println("Profile created successfully.");
    }

    User login(String u, String p) {

        for (int i = 0; i < count; i++) {

            if (users[i].username.equals(u) && users[i].password.equals(p))
                return users[i];
        }

        return null;
    }
}



public class gitLite {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        GitHubSystem system = new GitHubSystem();

        while (true) {

            System.out.println("\n===== Git Lite =====");
            System.out.println("1. Create GitHub Profile");
            System.out.println("2. Login");
            System.out.println("3. Exit");

            int choice = sc.nextInt();
            sc.nextLine();

            if (choice == 1) {

                System.out.print("Enter username: ");
                String u = sc.nextLine();

                System.out.print("Enter password: ");
                String p = sc.nextLine();

                system.register(u, p);
            }

            else if (choice == 2) {

                System.out.print("Username: ");
                String u = sc.nextLine();

                System.out.print("Password: ");
                String p = sc.nextLine();

                User current = system.login(u, p);

                if (current == null) {
                    System.out.println("Invalid login.");
                    continue;
                }

                while (true) {

                    System.out.println("\n--- Welcome " + current.username + " ---");
                    System.out.println("1. Create Repository");
                    System.out.println("2. Open Repository");
                    System.out.println("3. List Repositories");
                    System.out.println("4. Logout");

                    int op = sc.nextInt();
                    sc.nextLine();

                    if (op == 1) {

                        System.out.print("Repository name: ");
                        current.createRepo(sc.nextLine());
                    }

                    else if (op == 2) {

                        System.out.print("Enter repository name: ");
                        Repository r = current.getRepo(sc.nextLine());

                        if (r == null) {
                            System.out.println("Repository not found.");
                            continue;
                        }

                        while (true) {

                            System.out.println("\nRepository: " + r.name);
                            System.out.println("1. Update File");
                            System.out.println("2. Commit");
                            System.out.println("3. View Commit History");
                            System.out.println("4. Undo Commit");
                            System.out.println("5. Back");

                            int ch = sc.nextInt();
                            sc.nextLine();

                            if (ch == 1) {

                                System.out.print("Enter new file content: ");
                                r.updateFile(sc.nextLine());
                            }

                            else if (ch == 2) {

                                System.out.print("Commit message: ");
                                String msg = sc.nextLine();

                                System.out.print("Author name: ");
                                String a = sc.nextLine();

                                r.commit(msg, a);
                            }

                            else if (ch == 3) {

                                r.viewHistory();
                            }

                            else if (ch == 4) {

                                r.undoCommit();
                            }

                            else {

                                break;
                            }
                        }
                    }

                    else if (op == 3) {

                        current.listRepos();
                    }

                    else {

                        break;
                    }
                }
            }

            else {

                System.out.println("Exiting program...");
                break;
            }
        }
    }
}
