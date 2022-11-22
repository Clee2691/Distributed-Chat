package server;

import java.io.Serializable;

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

    /**
     * Empty constructor
     */
    public DBOperation() {}

    /**
     * Full paramterized constructor
     * @param op The operation name
     * @param username The username
     * @param password The password
     * @param message The message
     * @param chatroom The chatroom
     */
    public DBOperation(String op, String username, String password, String message, String chatroom) {
        this.op = op;
        this.username = username;
        this.password = password;
        this.message = message;
        this.chatroom = chatroom;
    }

    /**
     * Get the operation
     * @return String operation
     */
    public String getOp() {
        return this.op;
    }

    /**
     * Set the operation
     * @param op The operation
     */
    public void setOp(String op) {
        this.op = op;
    }
    
    /** 
     * Get the username
     * @return String Username
     */
    public String getUsername() {
        return this.username;
    }
    
    /** 
     * Set the username
     * @param username The username
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
    /** 
     * Get the password
     * @return String Password
     */
    public String getPassword() {
        return this.password;
    }
    
    /** 
     * Set the password
     * @param password The password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /** 
     * Get the message
     * @return String Message
     */
    public String getMessage() {
        return this.message;
    }
    
    /** 
     * Set the message
     * @param message The message
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /** 
     * Get the chatroom name
     * @return String The chatroom's name
     */
    public String getChatroom() {
        return this.chatroom;
    }

    /** 
     * Set the chatroom name
     * @param chatroom The name
     */
    public void setChatroom(String chatroom) {
        this.chatroom = chatroom;
    }
}
