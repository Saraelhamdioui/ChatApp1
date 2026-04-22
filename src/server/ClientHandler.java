package server;

import network.Protocol;
import DAO.MessageDao;
import DAO.UserDao;
import model.User;
import model.Message;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    private MessageDao messageDao = new MessageDao();
    private UserDao userDao = new UserDao();

    public ClientHandler(Socket socket) {
        this.socket = socket;

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        try {
            String msg;

            while ((msg = in.readLine()) != null) {

                if (msg.startsWith(Protocol.LOGIN)) {

                    username = msg.split(":")[1];

                    userDao.save(new User(username));
                    Server.addClient(username, this);

                    sendHistory();
                }

                else if (msg.startsWith(Protocol.MSG)) {

                    String[] parts = msg.split(":", 4);

                    String sender = parts[1];
                    String receiver = parts[2];
                    String content = parts[3];

                    messageDao.save(new Message(sender, receiver, content));

                    String full = sender + ":" + receiver + ":" + content;

                    Server.sendPrivate(receiver, full);
                    Server.sendPrivate(sender, full);
                }

                else if (msg.startsWith(Protocol.SEEN)) {

                    String[] parts = msg.split(":", 3);

                    String sender = parts[1];
                    String receiver = parts[2];

                    messageDao.markAsSeen(receiver, sender);

                    Server.sendPrivate(receiver, Protocol.SEEN + ":" + sender);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (username != null) {
                Server.removeClient(username);
            }
        }
    }

    public void send(String msg) {
        out.println(msg);
    }

    private void sendHistory() {

        for (Message m : messageDao.getMessages(username)) {
            send(Protocol.HISTORY + ":" + m.getSender() + ":" + m.getReceiver() + ":" + m.getContent());
        }
    }
}