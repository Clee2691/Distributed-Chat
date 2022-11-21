package client;

import java.io.BufferedReader;
// Logging Imports
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

// RMI Imports
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
// Java Imports
import java.util.List;
import java.util.Map;
import java.time.Instant;

// Custom Imports
import gui.ClientGUI;
import server.ChatServerInterface;
import server.Response;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private List<Integer> serverPorts;

    private ExecutorService executorService;

    private String host;
    private int connectedPort;

    //TODO: GET RID OF AFTER DONE
    public Random rand = new Random();

    /**
     * Empty constructor
     */
    public ChatClient() {
        this.serverPorts = new ArrayList<Integer>();
        this.executorService = Executors.newFixedThreadPool(10);
    };

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
    public void setRemoteReg(String host) {
        LOGGER.info("Setting the remote registry...");
        Registry currReg;
        ChatServerInterface chatStub;

        // Locate the leader server
        for (int port : serverPorts) {
            try {
                currReg = LocateRegistry.getRegistry(host, port);
                chatStub = (ChatServerInterface)currReg.lookup("chat");
                if (chatStub.sendIsLeader()) {
                    this.remoteReg = currReg;
                    this.connectedPort = port;
                    setChatStub();
                    break;
                }
            }  catch (RemoteException re) {
                LOGGER.severe(String.format("Server on port: %d could not be reached!", port));
                continue;
            } catch (NotBoundException nbe) {
                LOGGER.severe("Registry name not found!");
                continue;
            }
        }
    }

    /**
     * Get the remote registry
     * @return
     */
    public Registry getRemoteReg() {
        return this.remoteReg;
    }

    /**
     * Get the list of server ports that are available to choose from
     * @return
     */
    public List<Integer> getServerPorts() {
        return this.serverPorts;
    }

    /**
     * Set the host
     * @param h The host name
     */
    public void setHost(String h) {
        this.host = h;
    }

    /**
     * Get the host
     * @return The host
     */
    public String getHost() {
        return this.host;
    }

    public void setConnectedPort(int p) {
        this.connectedPort = p;
    }

    public int getConnectedPort() {
        return this.connectedPort;
    }
    // ======================================

    //         Register/Login the user

    // ======================================

    /**
     * Register the user with the specified user and password.
     * Connects this client object to the registry by registering it.
     * @param user The username
     * @param pw The password
     * @return Response object whether it was successful or not
     * @throws RemoteException
     */
    public Response registerUser(String user, String pw) throws RemoteException {
        Response serverResp = this.chatStub.registerUser(user, pw);

        if (serverResp.getServerReply().equals("success")) {
            ClientInterface clientStub = (ClientInterface)UnicastRemoteObject.exportObject(this, 0);
            remoteReg.rebind(String.format("client:%s", user), clientStub);
            LOGGER.info("Successfully registered.");
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
     * @param chatName The chatroom's name
     * @return Map of rooms and the list of users in each room
     */
    public List<String> getChatRoomHistory(String chatName) throws RemoteException {
        return this.chatStub.getChatRoomMessageHistory(chatName);
    }

    // ======================================

    //      Chat Sever Remote Method Calls

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

    /**
     * Notify other clients of joining or leaving. Call the server remote method.
     * @param chatname The chat room user joined or left
     * @param user The user joining or leaving
     * @throws RemoteException
     */
    public void notifyOthersJoinLeave(String chatname, String user) throws RemoteException {
        this.chatStub.notifyJoinLeave(chatname, user);
    }


    // ======================================

    //         Client Remote Methods

    // ======================================

    @Override
    public void displayMessage(String sender, String message) {
        this.theGUI.displayNewMessage(sender, message);
        LOGGER.info(message);
    }

    @Override
    public void notifyJoinLeave() {
        // Update room list
        this.theGUI.updateRoomMemberList();
    }

    @Override
    public boolean sendHeartBeat() {
        return true;
    }

    // ======================================

    //         Threaded Methods

    // ======================================

    public void getServerHeartBeat() {
        while(true) {
            try {
                if (this.chatStub == null) {
                    setRemoteReg(host);
                    continue;
                }

                // Check if it is alive
                if (this.chatStub.sendHeartBeat()) {
                    LOGGER.info(
                        String.format(
                            "Server on port: %d is still alive!", 
                            this.connectedPort));
                    // Check to see if it is the leader
                    if (!this.chatStub.sendIsLeader()) {
                        setRemoteReg(host);
                    }
                }
            } catch (RemoteException re) {
                LOGGER.severe(
                    String.format(
                        "Error connecting to server on port: %d. Finding new leader...", 
                        this.connectedPort));
                setRemoteReg(this.host);
            }

            try {
                LOGGER.info("Sleeping server heartbeat sensor for 3 seconds");
                Thread.sleep(3000);
            } catch (InterruptedException ie) {
                LOGGER.severe("Interrupted sleeping heartbeat sensor");
            }
        }
    }


    // ======================================

    //         DRIVER OF CLIENT

    // ======================================

    /**
     * Parse port arguments for the server replicas
     * I want 5 replicas at least
     * @param args
     */
    private void parsePorts(String[] args) {
        String portListFile = args[1];

        BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(portListFile));
			String portString = reader.readLine();
			while (portString != null) {
                int currPort = Integer.parseInt(portString);
                if (currPort <= 1000 && currPort >= 65535) {
                    LOGGER.severe("Port must be in Range: 1000-65535!");
                    System.exit(1);
                }
                serverPorts.add(currPort);
				// read next line
				portString = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			LOGGER.severe("Port list file was not found");
            System.exit(1);
		} catch (NumberFormatException ne) {
            LOGGER.severe("Error parsing the port list file ports. Make sure they are formatted right!");
            System.exit(1);
        }

        if (serverPorts.size() < 5) {
            LOGGER.severe("Invalid usage, you must have at least 5 ports!");
            System.exit(1);
        }
    }

    /**
     * Driver for the client
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            LOGGER.severe("Must include the host and the file with port numbers!");
            System.exit(1);
        }

        String host = args[0];

        ChatClient chatClient = new ChatClient();

        chatClient.setHost(host);
        chatClient.parsePorts(args);
        
        // Get a reference to the remote registry object on the specified host
        chatClient.setRemoteReg(host);

        // Set GUI
        ClientGUI cGUI = new ClientGUI(chatClient);
        chatClient.setGUI(cGUI);

        chatClient.executorService.submit(() -> {
            chatClient.getServerHeartBeat();
        });

    }
}
