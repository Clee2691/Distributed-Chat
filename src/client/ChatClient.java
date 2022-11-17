package client;

// Logging Imports
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

// RMI Imports
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// Custom Imports
import gui.ClientGUI;
import server.ChatServerInterface;
import server.Response;

public class ChatClient implements ClientInterface {

    /**
     * Logging support that loads a custom logging properties file.
     */
    static Logger LOGGER = Logger.getLogger(ChatClient.class.getName());
    // Load a custom properities file
    static {
        String filePath = "../config/clientlogging.properties";
        try {
            LogManager.getLogManager().readConfiguration(new FileInputStream(filePath));
        } catch (IOException io)  {
            LOGGER.severe("Logging config file not found.");
        }
    }

    /**
     * Client private variables
     */
    private ClientGUI theGUI;
    private ChatServerInterface chatStub;
    private Registry remoteReg;

    /**
     * Empty constructor
     */
    public ChatClient() {};

    /**
     * Set the GUI for the client.
     * @param gui The clientGUI Class
     */
    public void setGUI(ClientGUI gui) {
        this.theGUI = gui;
    }

    public void setChatStub(Registry theReg) throws NotBoundException, RemoteException {
        this.chatStub = (ChatServerInterface)theReg.lookup("chat");
    }

    public ChatServerInterface getChatStub() {
        return this.chatStub;
    }

    public void setRemoteReg(Registry reg) {
        this.remoteReg = reg;
    }

    public Registry getRemoteReg() {
        return this.remoteReg;
    }

    // ======================================

    //         Register/Login the user

    // ======================================

    public Response registerUser(String user, String pw) throws RemoteException {
        Response serverResp = this.chatStub.registerUser(user, pw);

        if (serverResp.getServerReply().equals("success")) {
            ClientInterface clientStub = (ClientInterface)UnicastRemoteObject.exportObject(this, 0);
            remoteReg.rebind(String.format("client:%s", user), clientStub);
        }
        
        return serverResp;
    }

    public Response loginUser(String user, String pw) throws RemoteException {
        Response serverResp = this.chatStub.loginUser(user, pw);

        if (serverResp.getServerReply().equals("success")) {
            ClientInterface clientStub = (ClientInterface)UnicastRemoteObject.exportObject(this, 0);
            remoteReg.rebind(String.format("client:%s", user), clientStub);
        }
        return serverResp;
    }

    // ======================================

    //        Get Server Information

    // ======================================

    /**
     * Call server's remote method to get the currently active rooms and the number of users in them
     * @return
     */
    public Map<String, Integer> getChatRoomInformation() throws RemoteException {
        return this.chatStub.getChatRoomInformation();
    }

    // ======================================

    //         Create/Join chatroom

    // ======================================

    /**
     * Allow a user to create a chatroom
     * @param chatname The chatroom name
     * @param user The user attempting to create the chatroom
     * @return String signifying the success or failure
     * @throws RemoteException
     */
    public String createChatRoom(String chatname, String user) throws RemoteException {
        return this.chatStub.createChatRoom(chatname, user);
    }

    /**
     * Allow a user to join the specified chatroom
     * @param chatname The chatroom name
     * @param user The user
     * @return String signifying success or failure
     * @throws RemoteException
     */
    public String joinChatRoom(String chatname, String user) throws RemoteException {
        return this.chatStub.joinChatRoom(chatname, user);
    }

    /**
     * Call the server's broadcast message to send the message to all room participants
     * @param chatRoom The current chatroom the user is in
     * @param message The message to send
     * @throws RemoteException
     */
    public void sendMessage(Instant timeStamp, String user,String chatRoom, String message) throws RemoteException {
        this.chatStub.broadCastMessage(timeStamp, user, chatRoom, message);
    }


    // ======================================

    //         Remote Methods

    // ======================================

    public void displayMessage(Instant timeStamp, String sender, String message) {
        this.theGUI.displayNewMessage(timeStamp, sender, message);
        LOGGER.info(message);
    }

    public void displayUserJoinLeave(Instant timeStamp, String user, String action) {
        this.theGUI.displayNewMessage(timeStamp, user,
            // Either joined or left
            String.format("has %s the chat.", action));
        LOGGER.info(String.format("%s has %s the chat.", user, action));
    }



    /**
     * Driver for the client
     * @param args
     */
    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();

        // Locate the remote stub for the chat server (Using localhost:5555) for now
        try {
            //TODO: CHANGE LOCALHOST/PORT TO BE USER INPUTS
            // Get a reference to the remote registry object on the specified host
            chatClient.setRemoteReg(LocateRegistry.getRegistry("localhost", 5555));
            chatClient.setChatStub(chatClient.getRemoteReg());

        } catch (RemoteException re) {
            LOGGER.severe(re.toString());
            LOGGER.severe("Could not find remote on client instantiation. Restart the client!");
        } catch (NotBoundException nbe) {
            LOGGER.severe("Registry name not found!");
        }

        // Set the GUI for the client
        ClientGUI cGUI = new ClientGUI(chatClient);
        chatClient.setGUI(cGUI);
    }
}
