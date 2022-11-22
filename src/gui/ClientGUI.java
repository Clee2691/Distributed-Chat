package gui;

// Logging Imports
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;
// Java GUI Imports
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

// Java Utils
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

// Java Time
import java.time.Instant;

// Custom Imports
import client.ChatClient;
import server.Response;

/**
 * The GUI of the client.
 */
public class ClientGUI {
  /**
   * Logging support that loads a custom logging properties file.
   */
  static Logger LOGGER = Logger.getLogger(ClientGUI.class.getName());
  // Load a custom properities file
  static {
      String filePath = "../config/clientlogging.properties";
      try {
          LogManager.getLogManager().readConfiguration(new FileInputStream(filePath));
      } catch (IOException io)  {
          LOGGER.severe("Logging config file not found.");
      }
  }

  // Chat client GUI Variables
  public ChatClient client;
  public String currChatRoom;

  // GUI Components
  public JFrame frame;
  public JFrame regLogFrame;
  public JPanel panel;
  public ArrayList<Component> componentsOnPanel;

  // Login/Register components
  public JButton loginButton;
  public JLabel registerTitleLabel;
  public JTextField registerUsername;
  public JTextField registerPassword;
  public JButton registerButton;
  public JLabel registerUsernameLabel;
  public JLabel registerPasswordLabel;
  public JTextArea activeChatArea;

  // Components on the Chat Selection Screen
  public JLabel joinChatLabel;
  public JTextField joinChatField;
  public JButton joinChatButton;
  public JLabel createChatLabel;
  public JTextField createChatField;
  public JButton createChatButton;
  public JButton logoutButton;
 
  // Components in the Chat room screen
  public String chatroomName;
  public JLabel chatroomLabel;
  public JTextArea chatroomTextArea;
  public JScrollPane chatroomScrollPane;
  public JTextField chatroomNewMessageField;
  public JButton chatroomNewMessageButton;
  public JLabel roomMembersLabel;
  public JTextArea roomMembersTextArea;
  public JScrollPane roomMembersScrollPane;
  public JButton getUsersInChatroomButton;
  public JButton backToChatSelectionButton;

  /**
   * Constructor that takes in a client. Initializes the GUI
   * @param client The chat client object
   */
  public ClientGUI(ChatClient client) {
    this.client = client;
    this.frame = new JFrame();
    this.panel = new JPanel();
    this.componentsOnPanel = new ArrayList<>();
    // Display the home page GUI
    openStartScreen();
  }

  // =================================

  //        User Functions

  // =================================

  /**
   * Log in the user if user is in the database.
   */
  public void loginUser() {
    if (registerUsername.getText().length() == 0 || registerPassword.getText().length() == 0) {
      openPopUp("Enter a username and password!");
    } else {
      final String currUserName = registerUsername.getText();
      final String currPassword = registerPassword.getText();
      Response res = null;

      try {
        res = client.loginUser(currUserName, currPassword);

        if (res == null) {
          openPopUp("Something went wrong logging in. Try again.");
          LOGGER.severe("Logging in user response is null.");

        } else if (res.getServerReply().equalsIgnoreCase("success")) {
          client.setUserName(currUserName);
          this.client.setIsLoggedIn(true);
          LOGGER.info("SUCCESS: Successfully logged in.");
          // Close the login frame
          regLogFrame.dispose();
          openChatSelectionScreen();

        } else if (res.getServerReply().equalsIgnoreCase("incorrect")) {
          openPopUp("Incorrect username/password!");
          LOGGER.severe("FAIL: Incorrect username/password.");

        } else if (res.getServerReply().equals("loggedIn")) {
          openPopUp("This user already logged in!");
          LOGGER.severe("FAIL: Already logged in.");

        } else {
          openPopUp("Failed logging in. Try again!");
          LOGGER.severe("FAIL: Unknown error.");
        }
      } catch (RemoteException re) {
        LOGGER.severe(re.toString());
        LOGGER.severe("ERROR: Failed connecting to remote. Failed log in.");
      }
    }
  }

  /**
   * Register the user based on username and password entered
   */
  public void registerUser() {
    // send register username and password
    if (registerUsername.getText().length() == 0 || registerPassword.getText().length() == 0) {
      openPopUp("Enter a username and password!");
    } else {
      final String currUserName = registerUsername.getText();
      final String currPassword = registerPassword.getText();
      Response res = null;
      try {
        res = client.registerUser(currUserName, currPassword);
        
        if (res == null) {
          openPopUp("Something went wrong registering. Try again.");
          LOGGER.severe("Register user response is null.");

        } else if  (res.getServerReply().equalsIgnoreCase("success")) {
          client.setUserName(currUserName);
          this.client.setIsLoggedIn(true);
          LOGGER.info("User successfully logged in!");
          regLogFrame.dispose();
          openChatSelectionScreen();
        } else {
          openPopUp(res.getServerReply());
          LOGGER.severe(res.getServerReply());
        }
      } catch (RemoteException re) {
        LOGGER.severe(re.toString());
        LOGGER.severe("ERROR: Failed connecting to the server. Failed registering user!");
      }
    }
  }

  /**
   * Log out of the application and go back to the register/login screen
   */
  public void logOutApplication() {
    String response = null;
    try {
      response = client.logOutApp(client.getUsername());
    } catch (RemoteException re) {
      LOGGER.severe(
        "Server error logging out of application. Could not connect to the remote.");
    }

    if (response == null) {
      openPopUp("Something went wrong logging out. Try again.");
      LOGGER.severe("Log out user response is null.");

    } else if (response.equalsIgnoreCase("success")) {
      client.setUserName(null);
      this.client.setIsLoggedIn(false);
      LOGGER.info(
        "Successfully logged out of application.");
      openStartScreen();
    } else {
      LOGGER.severe(
        "Error logging out of application");
      openPopUp("Error logging out! Try again.");
    }
  }

  // =================================

  //   Room Functions

  // =================================

  /**
   * Update the room list on the start page
   */
  public void setRoomList() {
    activeChatArea.setText("");
    // Get the rooms and the number of users within it
    try {
      Map<String, List<String>> roomAndNumUsers = client.getChatRoomInformation();
      if (roomAndNumUsers == null) {
        LOGGER.severe("No rooms currently active.");
        activeChatArea.append("No active rooms currently\n");
      } else {
        for (Map.Entry<String, List<String>> roomUsers : roomAndNumUsers.entrySet()) {
          activeChatArea.append(String.format(
                        "Name: %s | Currently Active Users: %d\n",
                                roomUsers.getKey(), roomUsers.getValue().size()));
          
        }
      }
    } catch (RemoteException re) {
      LOGGER.severe("Couldn't get the rooms and the number of users. Server might be down!");
      activeChatArea.append("Error retrieving room data!\n");
    }
  }

  /**
   * Updates the current rooms member list
   */
  public void updateRoomMemberList() {
    roomMembersTextArea.setText("");
    try {
      final Map<String, List<String>> roomAndNumUsers = client.getChatRoomInformation();
      if (roomAndNumUsers == null) {
        LOGGER.severe("Room name not active.");
        roomMembersTextArea.append(String.format("Room name: %s is not active!\n", currChatRoom));
      } else {
        List<String> roomUsers = roomAndNumUsers.get(currChatRoom);
        List<String> listWithoutDuplicates = roomUsers.stream()
            .distinct().collect(Collectors.toList());
        
        for (String user: listWithoutDuplicates) {
          roomMembersTextArea.append(String.format("%s\n", user));
        }
      }
    } catch (RemoteException re) {
      LOGGER.severe("Couldn't get the rooms and the number of users. Server might be down!");
      roomMembersTextArea.append("Error retrieving participants!\n");
    }
  }

  // =================================

  //      GUI Utility Functions

  // =================================

  /**
   * Display a popup with the specified message
   * @param message The message to display
   */
  public void openPopUp(String message) {
    JFrame resFrame = new JFrame();
    JPanel resPanel = new JPanel();

    JLabel newToast = new JLabel(message);
    newToast.setForeground(Color.red);
    
    JButton okButton = new JButton("DISMISS");
    okButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        resFrame.dispose();
      }
    });

    // Add the message and the button
    resPanel.add(newToast);
    resPanel.add(okButton);
    
    resFrame.add(resPanel);
    resFrame.setTitle("ALERT");
    resFrame.pack();
    resFrame.setVisible(true);
  }

  /**
   * Remove all of the Swing components on panel so that the panel can be reused.
   */
  public void removeAllComponents() {
    this.componentsOnPanel.forEach((component -> {
      this.panel.remove(component);
    }));
  }

  /**
   * Add component to the list of components so that they can be tracked and removed as necessary.
   * @param gbc A GridBagConstraints object 
   * @param component The component to add.
   */
  public void addComponentToPanel(Component component, GridBagConstraints gbc) {
    this.componentsOnPanel.add(component);
    this.panel.add(component, gbc);
  }

  /**
   * Display a message and who it was sent by in the chatroom text area.
   * @param timeStamp The time stamp for the sent message
   * @param sender The sender of the message
   * @param message The message that was sent
   */
  public void displayNewMessage(String sender, String message) {

    this.chatroomTextArea.append(message + "\n");
  }

  /**
   * The action performed when the send button is pressed.
   * Must be a message of at least length 1.
   */
  public void sendNewMessage() {
    String newMessage = chatroomNewMessageField.getText();
    // Reset the text in the field
    chatroomNewMessageField.setText("");
    
    if (newMessage.length() == 0) {
      openPopUp("Message cannot be empty!");
    } else {
      try {
        client.sendMessage(Instant.now(), client.getUsername(), currChatRoom, newMessage);
      } catch (RemoteException re) {
        LOGGER.severe("Error sending message!");
        openPopUp("Error sending message!");
      }
    }
  }

  // ===============================================

  //            Chat Screen Methods

  // ===============================================

  /**
   * The join action when user clicks to join a chatroom.
   */
  public void joinChatAction() {
    // Get the typed in chat name
    chatroomName = joinChatField.getText();
    if (chatroomName.length() < 3) {
      openPopUp("Provide a chatroom name! Must be at least 3 characters!");
    } else {
      String response = null;
      // Attempt to join the chatroom
      try {
        response = client.joinChatRoom(chatroomName, client.getUsername());
      } catch (RemoteException re) {
        LOGGER.severe(
          String.format("Error joining chatroom: %s", chatroomName));
          openPopUp("Server error on joining the chatroom. Try again.");
          return;
      }
      
      // Success or fail
      if (response.equalsIgnoreCase("success")) {
        currChatRoom = chatroomName;
        Instant currTime = Instant.now();
        LOGGER.info(
          String.format(
            "User: %s successfully joined chat: %s", client.getUsername(), currChatRoom));
        // Broadcast that the client joined the chat room
        try {
          client.sendMessage(currTime, "SYSTEM", currChatRoom,
            String.format("%s has joined the chat.", client.getUsername()));

            // Notify all members of the room
            client.notifyOthersJoinLeave(currChatRoom, client.getUsername());
        } catch (RemoteException re) {
          LOGGER.severe(
            String.format(
              "Error alerting others I (%s) joined the chatroom: %s.", 
              client.getUsername(), 
              currChatRoom));
        }
        openChatroomScreen();
      } else {
        openPopUp("Chatroom name does not exist!");
        LOGGER.severe("Error joining chatroom. Chatroom does not exist.");
      }
    }
  }

  /**
   * The create chat action when user clicks create chat
   */
  public void createChatAction() {
    // Get the chatroom name from the text field
    chatroomName = createChatField.getText();
    // Must be > len 0;
    if (chatroomName.length() < 3) {
      openPopUp("Enter the name for a chatroom! Must be at least 3 characters!");
    }else {
      String res = null;
      // Attempt to create the chat room
      try {
        res = client.createChatRoom(chatroomName, client.getUsername());
      } catch (RemoteException re) {
        LOGGER.severe("Error creating a chatroom! Server might be down!");
        return;
      }
      
      // Success or fail
      if (res.equalsIgnoreCase("success")) {
        Instant currTime = Instant.now();
        currChatRoom = chatroomName;
        LOGGER.info(String.format("User: %s successfully created chat: %s", client.getUsername(), currChatRoom));
        openChatroomScreen();

        // Broadcast that the client created the chat room
        try {
          client.sendMessage(currTime, "SYSTEM", currChatRoom,
            String.format("%s has created the chat.", client.getUsername()));
        } catch (RemoteException re) {
          LOGGER.severe(
            String.format(
              "Error alerting others I (%s) created the chatroom: %s.", 
              client.getUsername(),
              currChatRoom));
        }
      } else if (res.equalsIgnoreCase("exists")) {
        openPopUp("A chatroom with that name already exists!");
      } else {
        openPopUp("Error creating chatroom. Try again!");
        LOGGER.severe("Error from server creating chatroom.");
      }
    }
  }

  /**
   * Leave the current chatroom
   */
  public void leaveCurrentChat() {
    String response = "";

    try {
      response = client.leaveCurrChat(currChatRoom, client.getUsername());
    } catch (RemoteException re) {
      LOGGER.severe(
        String.format(
          "Server error leaving chatroom: %s. Could not connect to the remote!", 
          chatroomName));
    }
    
    if (response.equalsIgnoreCase("success")) {
      Instant currTime = Instant.now();
      LOGGER.info(String.format("User: %s successfully left chat: %s", client.getUsername(), currChatRoom));
      openChatSelectionScreen();
      // Broadcast that the client joined the chat room
      try {
        client.sendMessage(currTime, "SYSTEM", currChatRoom,
          String.format("%s has left the chat.", client.getUsername()));
          client.notifyOthersJoinLeave(currChatRoom, client.getUsername());
          currChatRoom = null;
      } catch (RemoteException re) {
        LOGGER.severe(
          String.format(
            "Error alerting others I (%s) joined the chatroom: %s.", 
            client.getUsername(), 
            currChatRoom));
      }
    } else {
      LOGGER.severe(String.format("Error leaving chatroom: %s", chatroomName));
      openPopUp("Error leaving the chatroom. Try again.");
    }
  }

  // ==============================================

  //     Methods to Make/Open Different Screens

  // ==============================================

  /**
   * Displays the selected or created chatroom.
   */
  public void openChatroomScreen() {
    // Clear the current panel
    this.removeAllComponents();

    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    this.chatroomLabel = new JLabel(
                              String.format("Chat: %s", 
                              this.currChatRoom, 
                              this.client.getUsername()), SwingConstants.CENTER);
    this.chatroomLabel.setFont(new Font("Calibri", Font.BOLD, 30));
    gbc.gridwidth = 3;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.ipady = 30;
    gbc.weightx = 0.5;
    gbc.weighty = 0.1;
    addComponentToPanel(chatroomLabel, gbc);

    // The text area showing the messages
    this.chatroomTextArea = new JTextArea(10, 30);
    this.chatroomScrollPane = new JScrollPane(this.chatroomTextArea);
    this.chatroomTextArea.setEditable(false);
    new SmartScroller(this.chatroomScrollPane);
    gbc.gridwidth = 2;
    gbc.insets = new Insets(0, 0, 10, 0);
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.ipady = 0;
    addComponentToPanel(this.chatroomScrollPane, gbc);
    
    // Get the chatroom history when joining a chatroom
    try {
      List<String> messageHistory = client.getChatRoomHistory(currChatRoom);
      if (messageHistory != null) {
        for (String mess : messageHistory) {
          this.chatroomTextArea.append(mess + "\n");
        }
      }
    } catch (RemoteException re) {
      LOGGER.severe(String.format("Couldn't get history for chatroom: %s", currChatRoom));
    }
    

    this.chatroomNewMessageField = new JTextField(30);
    gbc.insets = new Insets(0, 0, 0, 5);
    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.ipady = 5;
    addComponentToPanel(this.chatroomNewMessageField, gbc);

    this.chatroomNewMessageButton = new JButton("Send");
    this.chatroomNewMessageButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        sendNewMessage();
      }
    });
    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.ipady = 0;
    addComponentToPanel(this.chatroomNewMessageButton, gbc);

    this.roomMembersLabel = new JLabel("Participants:", SwingConstants.CENTER);
    gbc.insets = new Insets(0, 5, 5, 0);
    gbc.anchor = GridBagConstraints.SOUTH;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.weightx = 0;
    gbc.weighty = 0;
    addComponentToPanel(roomMembersLabel, gbc);

    this.roomMembersTextArea = new JTextArea(5, 10);
    this.roomMembersScrollPane = new JScrollPane(this.roomMembersTextArea);
    this.roomMembersTextArea.setEditable(false);
    new SmartScroller(this.roomMembersScrollPane);
    gbc.insets = new Insets(0, 5, 10, 0);
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 2;
    gbc.gridy = 1;
    addComponentToPanel(this.roomMembersScrollPane, gbc);

    updateRoomMemberList();

    this.backToChatSelectionButton = new JButton("Leave");
    this.backToChatSelectionButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        leaveCurrentChat();
      }
    });
    gbc.insets = new Insets(0, 5, 0, 0);
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx = 2;
    gbc.gridy = 2;
    gbc.weightx = 0.5;
    addComponentToPanel(backToChatSelectionButton, gbc);

    this.getUsersInChatroomButton = new JButton("Update");
    this.getUsersInChatroomButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateRoomMemberList();
      }
    });
    gbc.insets = new Insets(0, 5, 0, 0);
    gbc.anchor = GridBagConstraints.NORTHEAST;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx = 2;
    gbc.gridy = 2;
    addComponentToPanel(this.getUsersInChatroomButton, gbc);


    frame.setTitle(String.format("Chatroom: %s", this.currChatRoom));
    frame.setSize(750, 500);
    frame.getContentPane().validate();
    frame.getContentPane().repaint();
  }

  /**
   * Opens the main screen where one can see the active chatrooms as well as 
   * options to join and create new chatrooms
   */
  public void openChatSelectionScreen() {
    this.removeAllComponents();
    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    JLabel userLabel = new JLabel(String.format("Welcome: %s", client.getUsername()), SwingConstants.CENTER);
    userLabel.setFont(new Font("Calibri", Font.BOLD, 30));
    gbc.gridwidth = 2;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.weightx = 0.1;
    gbc.weighty = 0.1;
    addComponentToPanel(userLabel, gbc);

    logoutButton = new JButton("Log Out");
    logoutButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        logOutApplication();
      }
    });
    gbc.anchor = GridBagConstraints.NORTHEAST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = 1;
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.insets = new Insets(0, 10, 0, 0);
    addComponentToPanel(logoutButton, gbc);

    JLabel activeChatLabel = new JLabel("Active Chatrooms:", SwingConstants.CENTER);
    gbc.gridwidth = 2;
    activeChatLabel.setFont(new Font("Calibri", Font.BOLD, 18));
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.ipady = 0;
    gbc.gridx = 0;
    gbc.gridy = 1;
    addComponentToPanel(activeChatLabel, gbc);

    JButton refreshRooms = new JButton("Refresh List");
    refreshRooms.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        setRoomList();
      }
    });

    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.ipady = 10;
    gbc.gridx = 2;
    gbc.gridy = 1;
    addComponentToPanel(refreshRooms, gbc);

    this.activeChatArea = new JTextArea(6, 6);
    this.activeChatArea.setEditable(false);
    gbc.gridwidth = 3;
    gbc.insets = new Insets(0, 0, 10, 0);
    gbc.fill = GridBagConstraints.BOTH;
    gbc.ipady = 0;
    gbc.gridx = 0;
    gbc.gridy = 2;
    addComponentToPanel(activeChatArea, gbc);

    // Update list of active chat rooms
    if (client.getChatStub() != null) {
      setRoomList();
    } else {
      LOGGER.severe("Remote is not connected!");
    }

    // Joining chat
    joinChatLabel = new JLabel("Join Room (Enter Name):");
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = 1;
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.gridx = 0;
    gbc.gridy = 3;
    addComponentToPanel(joinChatLabel, gbc);

    joinChatField = new JTextField(10);
    gbc.ipady = 10;
    gbc.gridx = 1;
    gbc.gridy = 3;
    addComponentToPanel(joinChatField, gbc);

    joinChatButton = new JButton("Join");
    joinChatButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        joinChatAction();
      }
    });
    gbc.insets = new Insets(0, 10, 0, 0);
    gbc.ipady = 0;
    gbc.gridx = 2;
    gbc.gridy = 3;
    addComponentToPanel(joinChatButton, gbc);

    createChatLabel = new JLabel("Create Room (Enter Name):");
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.gridwidth = 1;
    gbc.gridx = 0;
    gbc.gridy = 4;
    addComponentToPanel(createChatLabel, gbc);

    createChatField = new JTextField(10);
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.gridx = 1;
    gbc.gridy = 4;
    gbc.ipady = 10;
    addComponentToPanel(createChatField, gbc);
    
    createChatButton = new JButton("Create");
    createChatButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        createChatAction();
      }
    });
    gbc.insets = new Insets(0, 10, 0, 0);
    gbc.gridwidth = 1;
    gbc.gridx = 2;
    gbc.gridy = 4;
    gbc.ipady = 0;
    addComponentToPanel(createChatButton, gbc);

    frame.setTitle(String.format("Join/Create Chatroom: %s", client.getUsername()));
    frame.getContentPane().validate();
    frame.getContentPane().repaint();
  }

  /**
   * Open a new frame (window) for users to register or login
   */
  public void openRegisterLoginScreen(String type) {
    LOGGER.info(String.format("Displaying %s screen.", type));

    // New frame that becomes the popup window
    this.regLogFrame = new JFrame(type);
    JPanel regPanel = new JPanel();
    // GridBagLayout is used
    regPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 10, 30));
    regPanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    this.registerTitleLabel = new JLabel(type);
    this.registerTitleLabel.setFont(new Font("Serif", Font.BOLD, 30));
    gbc.gridwidth = 2;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.insets = new Insets(0, 50, 0, 0);
    gbc.weightx = 1;
    gbc.weighty = 1;
    regPanel.add(registerTitleLabel, gbc);

    this.registerUsernameLabel = new JLabel("Username:");
    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.insets = new Insets(0, 0, 0, 0);
    regPanel.add(registerUsernameLabel, gbc);

    this.registerUsername = new JTextField(10);
    gbc.gridx = 1;
    gbc.gridy = 1;
    regPanel.add(registerUsername, gbc);
    
    this.registerPasswordLabel = new JLabel("Password:");
    gbc.gridx = 0;
    gbc.gridy = 2;
    regPanel.add(registerPasswordLabel, gbc);
    
    this.registerPassword = new JTextField(10);
    gbc.gridx = 1;
    gbc.gridy = 2;
    regPanel.add(registerPassword, gbc);

    this.registerButton = new JButton(type);
    gbc.gridwidth = 2;
    gbc.gridx = 0;
    gbc.gridy = 3;

    this.registerButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (type.equals("Register")) {
          registerUser();
        } else if (type.equals("Login")){
          loginUser();
        }
      }
    });
    regPanel.add(registerButton, gbc);
    
    // Add the panel to the frame
    regLogFrame.add(regPanel, BorderLayout.CENTER);
    regLogFrame.setSize(300,400);
    regLogFrame.setVisible(true); 
  }

  /**
   * Displays the start screen. This shows current active chatrooms as well as users connected.
   * It also gives user the option to register/ login.
   */
  public void openStartScreen() {
    LOGGER.info("Displaying start screen.");
    // Clear the frame
    this.removeAllComponents();

    // Set a panel layout
    panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 5, 30));
    panel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
    panel.setLayout(new GridBagLayout());

    GridBagConstraints gbc = new GridBagConstraints();

    JLabel appLabel = new JLabel("Distributed Chat Application", SwingConstants.CENTER);
    appLabel.setFont(new Font("Calibri", Font.BOLD, 30));
    gbc.gridwidth = 2;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.ipady = 50;
    gbc.weightx = 0.5;
    gbc.weighty = 0.1;
    addComponentToPanel(appLabel, gbc);

    JLabel activeChatLabel = new JLabel("Currently Active Chatrooms:");
    gbc.gridwidth = 1;
    activeChatLabel.setFont(new Font("Calibri", Font.BOLD, 18));
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.ipady = 0;
    gbc.gridx = 0;
    gbc.gridy = 1;
    addComponentToPanel(activeChatLabel, gbc);

    JButton refreshRooms = new JButton("Refresh List");
    refreshRooms.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        setRoomList();
      }
    });

    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.ipady = 10;
    gbc.gridx = 1;
    gbc.gridy = 1;
    addComponentToPanel(refreshRooms, gbc);

    this.activeChatArea = new JTextArea(6, 6);
    this.activeChatArea.setEditable(false);
    gbc.gridwidth = 2;
    gbc.insets = new Insets(0, 0, 10, 0);
    gbc.fill = GridBagConstraints.BOTH;
    gbc.ipady = 0;
    gbc.gridx = 0;
    gbc.gridy = 2;
    addComponentToPanel(activeChatArea, gbc);

    // Update list of active chat rooms
    if (client.getChatStub() != null) {
      setRoomList();
    } else {
      LOGGER.severe("Not connected to a remote yet!");
    }
    
    // Add the register button
    JButton regButton = new JButton("Register");
    regButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        openRegisterLoginScreen("Register");
      }
    });
    gbc.gridwidth = 1;
    gbc.ipady = 10;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 3;
    addComponentToPanel(regButton, gbc);

    // Add the loging button
    JButton loginButton = new JButton("Login");
    loginButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        openRegisterLoginScreen("Login");
      }
    });
    gbc.gridx = 1;
    gbc.gridy = 3;

    addComponentToPanel(loginButton, gbc);

    // Display the frame and panel
    frame.add(panel, BorderLayout.CENTER);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setTitle("Distributed Chat: Login/Register");
    frame.setSize(600,400);
    frame.setVisible(true);
  }
}
