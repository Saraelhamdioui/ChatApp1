package Controller;

import network.Protocol;
import java.io.*;
import java.net.*;

public class Client {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public Client() {
        try {
            socket = new Socket("localhost", 1234);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void login(String username) {
        out.println(Protocol.LOGIN + ":" + username);
    }

    public void sendMessage(String sender, String receiver, String msg) {
        out.println(Protocol.MSG + ":" + sender + ":" + receiver + ":" + msg);
    }

    public void sendSeen(String sender, String receiver) {
        out.println(Protocol.SEEN + ":" + sender + ":" + receiver);
    }

    public void listen(MessageListener listener) {

        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    listener.onMessage(msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}