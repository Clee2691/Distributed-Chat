package client;

// Logging Imports
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

// RMI Imports
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

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
        ClientInterface clientStub = (ClientInterface)UnicastRemoteObject.exportObject(this, 0);
        remoteReg.rebind(String.format("client:%s", user), clientStub);
        return this.chatStub.registerUser(user, pw);
    }

    public Response loginUser(String user, String pw) throws RemoteException {
        ClientInterface clientStub = (ClientInterface)UnicastRemoteObject.exportObject(this, 0);
        remoteReg.rebind(String.format("client:%s", user), clientStub);
        return this.chatStub.loginUser(user, pw);
    }

    // ======================================

    //         Create/Join chatroom

    // ======================================

    public String createChatRoom(String chatname, String user) throws RemoteException {
        return this.chatStub.createChatRoom(chatname, user);
    }

    public String joinChatRoom(String chatname, String user) throws RemoteException {
        return this.chatStub.joinChatRoom(chatname, user);
    }

    public void sendMessage(String chatRoom, String message) throws RemoteException {
        this.chatStub.broadCastMessage("test", message);
    }


    // ======================================

    //         Remote Methods

    // ======================================

    public void displayMessage(String message) {
        LOGGER.info(message);
    }



    /**
     * Driver for the client
     * @param args
     */
    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();

        // Set the GUI for the client
        ClientGUI cGUI = new ClientGUI(chatClient);
        chatClient.setGUI(cGUI);

        // Locate the remote stub for the chat server (Using localhost:5555) for now
        try {
            //TODO: CHANGE LOCALHOST/PORT TO BE USER INPUTS
            // Get a reference to the remote registry object on the specified host
            chatClient.setRemoteReg(LocateRegistry.getRegistry("localhost", 5555));
            chatClient.setChatStub(chatClient.getRemoteReg());

        } catch (RemoteException re) {
            LOGGER.severe("Could not find remote!");
        } catch (NotBoundException nbe) {
            LOGGER.severe("Registry name not found!");
        }
    }
}
