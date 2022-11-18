package client;

// Logging Imports
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

// RMI Imports
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;

// Java Imports
import java.util.List;
import java.util.Map;

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

    // The GUI of the client
    private ClientGUI theGUI;

    // The chat server stub
    private ChatServerInterface chatStub;

    // The remote registry server is connected to
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

    /**
     * Set the chat stub for the server
     * @throws NotBoundException
     * @throws RemoteException
     */
    public void setChatStub() throws NotBoundException, RemoteException {
        this.chatStub = (ChatServerInterface)this.remoteReg.lookup("chat");
    }

    /**
     * Return the chat stub
     * @return The Server Interface
     */
    public ChatServerInterface getChatStub() {
        return this.chatStub;
    }

    /**
     * Set the remote registry
     * @param reg
     */
    public void setRemoteReg(Registry reg) throws NotBoundException, RemoteException {
        this.remoteReg = reg;
        setChatStub();
    }

    /**
     * Get the remote registry
     * @return
     */
    public Registry getRemoteReg() {
        return this.remoteReg;
    }

    // ======================================

    //         Register/Login the user

    // ======================================

    /**
     * 
     * @param user
     * @param pw
     * @return
     * @throws RemoteException
     */
    public Response registerUser(String user, String pw) throws RemoteException {
        Response serverResp = this.chatStub.registerUser(user, pw);

        if (serverResp.getServerReply().equals("success")) {
            ClientInterface clientStub = (ClientInterface)UnicastRemoteObject.exportObject(this, 0);
            remoteReg.rebind(String.format("client:%s", user), clientStub);
        }
        
        return serverResp;
    }

    /**
     * Log in a user by exporting the client object and registering it to the registry
     * @param user The user to login
     * @param pw User's password
     * @return Reponse object whether fail or success
     * @throws RemoteException
     */
    public Response loginUser(String user, String pw) throws RemoteException {
        Response serverResp = this.chatStub.loginUser(user, pw);

        if (serverResp.getServerReply().equals("success")) {
            ClientInterface clientStub = (ClientInterface)UnicastRemoteObject.exportObject(this, 0);
            remoteReg.rebind(String.format("client:%s", user), clientStub);
        }

        return serverResp;
    }

    /**
     * Log out of the application the current user
     * @param user The user to log out
     * @return String success or fail
     */
    public String logOutApp(String user) throws RemoteException{
        String serverResp = this.chatStub.logOutUser(user);
        if (serverResp.equals("success")) {

            try {
                UnicastRemoteObject.unexportObject(this, true);
                remoteReg.unbind(String.format("client:%s", user));
                LOGGER.info(String.format("Successfully unbound the client: %s", user));
            } catch (NotBoundException nbe) {
                LOGGER.severe("Client not bound already.");
            }

            return "success";
        }
        return "fail";
    }


    // ======================================

    //          Server Information

    // ======================================

    /**
     * Call server's remote method to get the currently active rooms and the number of users in them
     * @return Map of rooms and the list of users in each room
     */
    public Map<String, List<String>> getChatRoomInformation() throws RemoteException {
        return this.chatStub.getChatRoomInformation();
    }

    /**
     * Call server's remote method to get the currently active rooms and the number of users in them
     * @return Map of rooms and the list of users in each room
     */
    public List<String> getChatRoomHistory(String chatName) throws RemoteException {
        return this.chatStub.getChatRoomMessageHistory(chatName);
    }

    // ======================================

    //        Chat Remote Methods

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
     * Leave the current chatroom. 
     * @param chatname The chat room name
     * @param user The user leaving
     * @return String success or fail
     * @throws RemoteException
     */
    public String leaveCurrChat(String chatname, String user) throws RemoteException {
        return this.chatStub.leaveChatRoom(chatname, user);
    }

    /**
     * Call the server's broadcast message to send the message to all room participants
     * @param timeStamp The time the message was sent
     * @param user The user sending the message
     * @param chatRoom The current chatroom the user is in
     * @param message The message to send
     * @throws RemoteException
     */
    public void sendMessage(Instant timeStamp, String user,String chatRoom, String message) throws RemoteException {
        this.chatStub.broadCastMessage(timeStamp, user, chatRoom, message);
    }

    public void notifyOthersJoinLeave(String chatname, String user) throws RemoteException {
        this.chatStub.notifyJoinLeave(chatname, user);
    }


    // ======================================

    //         Client Remote Methods

    // ======================================

    public void displayMessage(Instant timeStamp, String sender, String message) {
        this.theGUI.displayNewMessage(timeStamp, sender, message);
        LOGGER.info(message);
    }

    public void notifyJoinLeave() {
        // Update room list
        this.theGUI.updateRoomMemberList();
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

        } catch (RemoteException re) {
            LOGGER.severe(re.toString());
            LOGGER.severe("Could not find remote on client instantiation. Server might be down!");
        } catch (NotBoundException nbe) {
            LOGGER.severe("Registry name not found!");
        }

        // Set the GUI for the client
        ClientGUI cGUI = new ClientGUI(chatClient);
        chatClient.setGUI(cGUI);
    }
}
