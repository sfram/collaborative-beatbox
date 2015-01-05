package beatbox;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

public class ChatServer {

    private static final int PORT = 9001;

    private static HashSet<PrintWriter> users = new HashSet<PrintWriter>();
    
    private static HashSet<String> usernames = new HashSet<String>();

    public static void main(String[] args) throws Exception {
        System.out.println("Chat server running.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                new ServerThread(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }

    public static class ServerThread extends Thread {
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        public ServerThread(Socket socket) {
            this.socket = socket;
        }
        
        public void run() {
            try {

                in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                while (true) {
                    out.println("USERNAME");
                    name = in.readLine();
                    if (name == null) {
                        return;
                    }
                    synchronized (usernames) {
                        if (!usernames.contains(name)) {
                            usernames.add(name);
                            break;
                        }
                    }
                }

                out.println("ACCEPTED");
                users.add(out);

                while (true) {
                    String input = in.readLine();
                    if (input == null) {
                        return;
                    }
                    for (PrintWriter user : users) {
                        user.println("MESSAGE " + name + ": " + input);
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {

                if (name != null) {
                    usernames.remove(name);
                }
                if (out != null) {
                    users.remove(out);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}