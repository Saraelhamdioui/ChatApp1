package server;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class Server {

    public static ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Boolean> allUsers = new ConcurrentHashMap<>();

    public static void main(String[] args) {

        try (ServerSocket serverSocket = new ServerSocket(1234)) {

            System.out.println("Server started...");

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addClient(String username, ClientHandler client) {
        clients.put(username, client);
        allUsers.put(username, true);
        broadcastUsers();
    }

    public static void removeClient(String username) {
        clients.remove(username);
        broadcastUsers();
    }

    public static void sendPrivate(String user, String msg) {
        ClientHandler c = clients.get(user);
        if (c != null) c.send(msg);
    }

    public static void broadcastUsers() {

        String online = String.join(",", clients.keySet());
        String all = String.join(",", allUsers.keySet());

        String data = "USERS:" + online + "|ALL:" + all;

        for (ClientHandler c : clients.values()) {
            c.send(data);
        }
    }
}