package server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A Key Value Operation class that contains the key/value and the operation
 * to be done.
 */
public class DBOperation implements Serializable {
    private String op; // register, send, create, join
    private String username; // For registering
    private String password; // For registering
    private String message; // When user sends a message
    private String chatroom; // send, create, join operation

    private final List<String> usersOrMessages = new ArrayList<String>();

    /**
     * Empty constructor
     */
    public DBOperation() {}

    public DBOperation(String op, String username, String password, String message, String chatroom) {
        this.op = op;
        this.username = username;
        this.password = password;
        this.message = message;
        this.chatroom = chatroom;
    }

    public String getOp() {
        return this.op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getUsersOrMessages() {
        return this.usersOrMessages;
    }

    public String getChatroom() {
        return this.chatroom;
    }

    public void setChatroom(String chatroom) {
        this.chatroom = chatroom;
    }
}
