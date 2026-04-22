package Controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.Message;
import java.util.*;

public class UIController {

    @FXML private VBox messagesBox;
    @FXML private TextField messageField;
    @FXML private ListView<String> contactsList;

    private Client client;
    private String username;
    private String selectedUser;

    private Map<String, List<Message>> conversations = new HashMap<>();
    private Map<String, Integer> unreadCount = new HashMap<>();

    private List<String> allUsersUI = new ArrayList<>();
    private Set<String> onlineUsers = new HashSet<>();

    public void setUsername(String username) {

        this.username = username;

        contactsList.setOnMouseClicked(e -> {

            int index = contactsList.getSelectionModel().getSelectedIndex();
            if (index == -1) return;

            selectedUser = allUsersUI.get(index);

            messagesBox.getChildren().clear();

            conversations.putIfAbsent(selectedUser, new ArrayList<>());

            for (Message m : conversations.get(selectedUser)) {
                drawMessage(m);
            }

            // reset unread
            unreadCount.put(selectedUser, 0);
            updateUsersListUI();

            client.sendSeen(username, selectedUser);
            markSeen();
        });

        client = new Client();
        client.login(username);

        client.listen(msg -> {
            Platform.runLater(() -> {

                if (msg.startsWith("USERS:")) {
                    updateUsers(msg);

                } else if (msg.startsWith("HISTORY:")) {
                    addMessage(msg.replace("HISTORY:", ""));

                } else if (msg.startsWith("SEEN:")) {
                    markSeen();

                } else {
                    addMessage(msg);
                }
            });
        });
    }

    @FXML
    public void sendMessage() {

        String msg = messageField.getText();

        if (msg.isEmpty() || selectedUser == null) {
            System.out.println("No user selected!");
            return;
        }

        client.sendMessage(username, selectedUser, msg);
        messageField.clear();
    }

    private void addMessage(String msg) {

        String[] parts = msg.split(":", 3);

        String sender = parts[0];
        String receiver = parts[1];
        String content = parts[2];

        String otherUser = sender.equals(username) ? receiver : sender;

        conversations.putIfAbsent(otherUser, new ArrayList<>());

        Message m = new Message(sender, receiver, content);
        conversations.get(otherUser).add(m);

        // unread messages
        if (!sender.equals(username) && !otherUser.equals(selectedUser)) {
            unreadCount.put(otherUser,
                    unreadCount.getOrDefault(otherUser, 0) + 1);
            updateUsersListUI();
        }

        // seen مباشرة
        if (!sender.equals(username) && otherUser.equals(selectedUser)) {
            client.sendSeen(username, sender);
            m.setSeen(true);
        }

        if (otherUser.equals(selectedUser)) {
            drawMessage(m);
        }
    }

    private void drawMessage(Message m) {

        boolean isMe = m.getSender().equals(username);

        HBox box = new HBox();
        Label label = new Label();

        if (isMe) {
            String ticks = m.isSeen() ? " ✔✔" : " ✔";
            label.setText(m.getSender() + ": " + m.getContent() + ticks);
        } else {
            label.setText(m.getSender() + ": " + m.getContent());
        }

        label.setWrapText(true);
        label.setMaxWidth(300);

        label.setStyle(
                "-fx-padding:10;" +
                        "-fx-background-radius:15;" +
                        (isMe
                                ? "-fx-background-color:#00a884; -fx-text-fill:white;"
                                : "-fx-background-color:#202c33; -fx-text-fill:white;")
        );

        box.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        box.getChildren().add(label);

        messagesBox.getChildren().add(box);
    }

    private void markSeen() {

        if (selectedUser == null) return;

        List<Message> msgs = conversations.get(selectedUser);
        if (msgs == null) return;

        for (Message m : msgs) {
            if (m.getSender().equals(username)) {
                m.setSeen(true);
            }
        }

        messagesBox.getChildren().clear();

        for (Message m : msgs) {
            drawMessage(m);
        }
    }

    private void updateUsers(String msg) {

        String[] parts = msg.split("\\|");

        String onlinePart = parts[0].replace("USERS:", "");
        String allPart = parts[1].replace("ALL:", "");

        onlineUsers.clear();
        allUsersUI.clear();

        if (!allPart.isEmpty()) {
            allUsersUI.addAll(Arrays.asList(allPart.split(",")));
        }

        if (!onlinePart.isEmpty()) {
            onlineUsers.addAll(Arrays.asList(onlinePart.split(",")));
        }

        allUsersUI.remove(username);

        updateUsersListUI();
    }

    private void updateUsersListUI() {

        contactsList.getItems().clear();

        for (String user : allUsersUI) {

            int count = unreadCount.getOrDefault(user, 0);

            String display = "● " + user;

            if (count > 0) {
                display += " (" + count + ")";
            }

            contactsList.getItems().add(display);
        }

        contactsList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(item);

                String usernameOnly = item.replaceAll("^● ", "")
                        .replaceAll("\\(\\d+\\)", "")
                        .trim();

                if (onlineUsers.contains(usernameOnly)) {
                    setStyle("-fx-text-fill: #27ae60;");
                } else {
                    setStyle("-fx-text-fill: #888888;");
                }
            }
        });
    }
}