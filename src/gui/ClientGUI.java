package gui;

// Logging Imports
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.LogManager;
import java.util.logging.Logger;

// Java GUI Imports
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

// Java Utils
import java.util.ArrayList;
import java.util.Map;

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

  // Vars for construction / login / register
  public ChatClient client;
  public String myUsername;
  public String currChatRoom;

  public JFrame frame;
  public JPanel panel;
  public ArrayList<Component> componentsOnPanel;

  public JButton loginButton;
  public JTextField loginUsername;
  public JTextField loginPassword;
  public JLabel loginTitleLabel;
  public JLabel registerTitleLabel;
  public JLabel loginUsernameLabel;
  public JLabel loginPasswordLabel;
  public JSeparator separator;
  public JTextField registerUsername;
  public JTextField registerPassword;
  public JButton registerButton;
  public JLabel registerUsernameLabel;
  public JLabel registerPasswordLabel;
  public JTextArea activeChatArea;

  public JLabel toastLabel;

  // Vars for Chat selection screen
  public JLabel joinChatLabel;
  public JTextField joinChatField;
  public JButton joinChatButton;
  public JLabel createChatLabel;
  public JTextField createChatField;
  public JButton createChatButton;
  public JLabel allChatroomNamesLabel;
  public JLabel allChatroomMembersLabel;
  public JTextArea allChatroomNamesTextArea;
  public JScrollPane allChatroomNamesScrollPane;
  public JTextArea allChatroomMembersTextArea;
  public JScrollPane allChatroomMembersScrollPane;
  public JButton logoutButton;

  // Vars for Chatroom screen
  public String chatroomName;
  public JLabel chatroomLabel;
  public JTextArea chatroomTextArea;
  public JScrollPane chatroomScrollPane;
  public SmartScroller chatroomSmartScroller;
  public JTextField chatroomNewMessageField;
  public JButton chatroomNewMessageButton;
  public JSeparator chatroomSeparator;
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

  //   User Register/Login Functions

  // =================================

  /**
   * Log in the user if user is in the database.
   */
  public void loginUser() {
    if (registerUsername.getText().length() == 0 || registerPassword.getText().length() == 0) {
      openPopUp("Enter a username and password!");
    } else {
      Response res = null;

      try {
        res = client.loginUser(registerUsername.getText(), registerPassword.getText());
        // change GUI to open chat selection screen
        if (res.getServerReply().equalsIgnoreCase("success")) {
          myUsername = registerUsername.getText();
          LOGGER.severe("SUCCESS: Successfully logged in.");
          openChatSelectionScreen();
        } else if (res.getServerReply().equalsIgnoreCase("incorrect")) {
          openPopUp("Incorrect username/password!");
          LOGGER.severe("FAIL: Incorrect username/password.");
        } else {
          openPopUp("This user already logged in!");
          LOGGER.severe("FAIL: Already logged in.");
        }
      } catch (RemoteException re) {
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
      Response res = null;
      try {
        res = client.registerUser(registerUsername.getText(), registerPassword.getText());
        
        if (res == null) {
          openPopUp("Something went wrong registering. Try again.");
          LOGGER.severe("RegisterUser response is null.");
        } else if  (res.getServerReply().equalsIgnoreCase("success")) {
          myUsername = registerUsername.getText();
          LOGGER.info("User successfully logged in!");
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
   * Update the room list on the start page
   */
  public void setRoomList() {
    activeChatArea.setText("");
    // Get the rooms and the number of users within it
    try {
      Map<String, Integer> roomAndNumUsers = client.getChatRoomInformation();
      if (roomAndNumUsers == null) {
        LOGGER.severe("No rooms currently active.");
        activeChatArea.append("No active rooms currently\n");
      } else {
        for (Map.Entry<String, Integer> roomUsers : roomAndNumUsers.entrySet()) {
          activeChatArea.append(String.format(
                          "Name: %s | Currently Active Users: %d\n",
                                  roomUsers.getKey(), roomUsers.getValue()));
        }
      }
    } catch (RemoteException re) {
      LOGGER.severe("Couldn't get the rooms and the number of users. Server might be down!");
    }
  }

  // =================================

  //         GUI Utility Functions

  // =================================

  /**
   * Display the resulting error.
   * @param message
   */
  public void openPopUp(String message) {
    JFrame resFrame = new JFrame();
    JPanel resPanel = new JPanel();

    JLabel newToast = new JLabel(message);
    newToast.setForeground(Color.red);
    
    JButton okButton = new JButton("OK");
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
    resFrame.setTitle("");
    resFrame.pack();
    resFrame.setVisible(true);
  }

  /**
   * Remove all of the Swing components on panel so that a new screen can be put up.
   */
  public void removeAllComponents() {
    this.componentsOnPanel.forEach((component -> {
      this.panel.remove(component);
    }));
  }

  /**
   * Add component to the list of components so that they can be tracked and removed as necessary.
   * @param component
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
  public void displayNewMessage(Instant timeStamp, String sender, String message) {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    String formattedTime = formatter.format(timeStamp);
    this.chatroomTextArea.append(
      String.format("[%s] %s: %s\n", formattedTime, sender, message));
  }

  // ===============================================

  //            Chat Screen Methods

  // ===============================================

  /**
   * Listener for Join Chat button. Checks if contents of associated checkbox are valid and then
   * checks if a chatroom by the given name exists. If it exists, then the chatroom screen is opened
   * and the user has joined that chat. If it does not exist, then a toast message is opened indicating
   * that it does not exist.
   */
  public class JoinChatButtonListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      chatroomName = joinChatField.getText();
      if (chatroomName.length() == 0) {
        openPopUp("Provide a chatroom name!");
      } else {
        String response = null;
        try {
          response = client.joinChatRoom(chatroomName, myUsername);
        } catch (RemoteException re) {
          LOGGER.severe(String.format("Error joining chatroom: %s", chatroomName));
        }
        
        if (response.equalsIgnoreCase("success")) {
          currChatRoom = chatroomName;
          openChatroomScreen();
          client.displayUserJoinLeave(Instant.now(), myUsername, "joined");
        } else { // lookup server returns "nonexistent"
          openPopUp("Chatroom name does not exist!");
        }
      }
    }
  }

  /**
   * Listener for Join Chat button. Checks if contents of associated checkbox are valid and then
   * checks if a chatroom by the given name exists. If it exists, then the chatroom screen is opened
   * and the user has joined that chat. If it does not exist, then a toast message is opened indicating
   * that it does not exist.
   */
  public class CreateChatButtonListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      chatroomName = createChatField.getText();
      if (chatroomName.length() == 0) {
        openPopUp("Enter the name for a chatroom!");
      }else {
        String res = null;
        try {
          res = client.createChatRoom(chatroomName, myUsername);
        } catch (RemoteException re) {
          LOGGER.severe("Error creating a chatroom! Server might be down!");
          return;
        }
        
        if (res.equalsIgnoreCase("success")) {
          currChatRoom = chatroomName;
          openChatroomScreen();
          client.displayUserJoinLeave(Instant.now(), myUsername, "joined");
        } else if (res.equalsIgnoreCase("exists")) {
          openPopUp("A chatroom with that name already exists!");
        }
      }
    }
  }

  /**
   * Listener for Logout button in Chat selection screen. Notifies LookUp server that we have logged
   * out. User is not in a chatroom at this screen so the server does not need to account for a member
   * leaving a chatroom. Success causes the Login/Register screen to appear.
   */
  public class ChatSelectionLogOutButtonListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
        //TODO:CHANGE THIS
      String response = "success";//client.attemptChatSelectionLogout();
      if (response.equalsIgnoreCase("success")) {
        openStartScreen();
      } else {
      }
    }
  }

  /**
   * Listener for Logout button in chatroom. Notifies chatroom server that we have logged out. Chatroom
   * server handles case based on if user logging out is the host client or just a normal client.
   * Success causes the Login/Register screen to appear.
   */
  public class ChatroomLogOutButtonListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
        //TODO:CHANGE THIS
      String response = "success";//client.attemptChatroomLogout();
      if (response.equalsIgnoreCase("success")) {
        openStartScreen();
      } else {
      }
    }
  }

  /**
   * The action performed when the send button is pressed.
   * Must be a message of at least length 1.
   */
  public void sendNewMessage() {
    String newMessage = chatroomNewMessageField.getText();
    
    if (newMessage.length() == 0) {
      openPopUp("Message cannot be empty!");
    } else {
      try {
        client.sendMessage(Instant.now(), myUsername, currChatRoom, chatroomNewMessageField.getText());
      } catch (RemoteException re) {
        LOGGER.severe("Error sending message!");
      }
    }
  }

  /**
   * Listener for button that updates what users are in the chat.
   */
  public class GetUsersInChatroomButtonListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      ArrayList<String> members = new ArrayList<>();//client.attemptGetUsersInChatroom(chatroomName);
      roomMembersTextArea.setText("");
    //   for (String member : members) {
    //     roomMembersTextArea.append(member + "\n");
    //   }
    }
  }

  /**
   * Listener for button that sends user back to chat selection screen. Client tells Chatroom server
   * that this user is leaving. If successful then chat selection screen is opened.
   */
  public class BackToChatSelectionButtonListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
        //TODO: CHANGE THIS
      String response = "success";//client.attemptBackToChatSelection();
      if (response.equalsIgnoreCase("success")) {
        openChatSelectionScreen();
      } else {
      }
    }
  }

  

  // ==============================================

  //     Methods to Make/Open Different Screens

  // ==============================================

  /**
   * Open the chatroom screen which includes an area that displays texts, a textbox and button to
   * send messages, a section that displays the users currently in the chatroom, a button to update
   * this section, a button to go back to the chat selection screen, and a logout button.
   */
  public void openChatroomScreen() {
    this.removeAllComponents();

    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    // Add messaging components to panel
    this.chatroomLabel = new JLabel(
                              String.format("Chat: %s | Username: %s", 
                              this.currChatRoom, 
                              this.myUsername), SwingConstants.CENTER);
    this.chatroomLabel.setFont(new Font("Calibri", Font.BOLD, 20));
    gbc.gridwidth = 2;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.ipady = 50;
    gbc.weightx = 0.5;
    gbc.weighty = 0.1;
    addComponentToPanel(chatroomLabel, gbc);

    // The text area showing the messages
    this.chatroomTextArea = new JTextArea(10, 30);
    this.chatroomScrollPane = new JScrollPane(this.chatroomTextArea);
    this.chatroomTextArea.setEditable(false);
    // smart scroller ensures that display shows most recent messages by default, and it also ensures
    // that a new message will not force the screen to move to the bottom if the user has scrolled
    // up and is looking at older messages.
    new SmartScroller(this.chatroomScrollPane);
    gbc.gridwidth = 2;
    gbc.insets = new Insets(0, 0, 10, 0);
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.ipady = 0;
    addComponentToPanel(this.chatroomScrollPane, gbc);
    

    this.chatroomNewMessageField = new JTextField(30);
    gbc.insets = new Insets(0, 0, 0, 5);
    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.ipady = 10;
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

    // Add section for what room members are in chatroom
    this.roomMembersLabel = new JLabel("Users in this chatroom:");
    this.roomMembersTextArea = new JTextArea(5, 20);
    this.roomMembersScrollPane = new JScrollPane(this.roomMembersTextArea);
    this.roomMembersTextArea.setEditable(false);
    new SmartScroller(this.roomMembersScrollPane);
    this.getUsersInChatroomButton = new JButton("Update Members In Room");
    this.getUsersInChatroomButton.addActionListener(new GetUsersInChatroomButtonListener());
    // addComponentToPanel(this.roomMembersLabel);
    // addComponentToPanel(this.roomMembersScrollPane);
    // addComponentToPanel(this.getUsersInChatroomButton);

    // Add button to go back to menu to join another chatroom
    this.backToChatSelectionButton = new JButton("Go Back To Chatroom Selection Screen");
    this.backToChatSelectionButton.addActionListener(new BackToChatSelectionButtonListener());
    this.logoutButton = new JButton("Log Out");
    this.logoutButton.addActionListener(new ChatroomLogOutButtonListener());
    // addComponentToPanel(this.backToChatSelectionButton);
    // addComponentToPanel(this.logoutButton);

    frame.setTitle(String.format("Chatroom: %s", this.currChatRoom));
    frame.getContentPane().validate();
    frame.getContentPane().repaint();
  }

  /**
   * Open the screen for joining or creating a chatroom. This screen has a textbox and button for
   * joining a chatroom, a textbox and button for creating a chatroom, a display for what chatrooms
   * are live and how many people are in them, and a button for logging out.
   */
  public void openChatSelectionScreen() {
    this.removeAllComponents();
    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    JLabel userLabel = new JLabel(String.format("Welcome: %s", myUsername));
    userLabel.setFont(new Font("Serif", Font.BOLD, 30));
    gbc.gridwidth = 2;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.insets = new Insets(0, 50, 0, 0);
    gbc.weightx = 1;
    gbc.weighty = 1;
    addComponentToPanel(userLabel, gbc);

    logoutButton = new JButton("Log Out");
    logoutButton.addActionListener(new ChatSelectionLogOutButtonListener());
    gbc.anchor = GridBagConstraints.NORTHEAST;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = 1;
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.insets = new Insets(0, 10, 0, 0);
    gbc.weightx = 1;
    gbc.weighty = 1;
    addComponentToPanel(logoutButton, gbc);

    // Joining chat
    joinChatLabel = new JLabel("Join Chatroom (Enter Name):");
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = 1;
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.gridx = 0;
    gbc.gridy = 1;
    addComponentToPanel(joinChatLabel, gbc);

    joinChatField = new JTextField(10);
    gbc.gridx = 1;
    gbc.gridy = 1;
    addComponentToPanel(joinChatField, gbc);

    joinChatButton = new JButton("Join Chat");
    joinChatButton.addActionListener(new JoinChatButtonListener());
    gbc.insets = new Insets(0, 10, 0, 0);
    gbc.gridx = 2;
    gbc.gridy = 1;
    addComponentToPanel(joinChatButton, gbc);

    createChatLabel = new JLabel("Create Chatroom (Enter Name):");
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.gridwidth = 1;
    gbc.gridx = 0;
    gbc.gridy = 2;
    addComponentToPanel(createChatLabel, gbc);

    createChatField = new JTextField(10);
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.gridwidth = 1;
    gbc.gridx = 1;
    gbc.gridy = 2;
    addComponentToPanel(createChatField, gbc);
    
    createChatButton = new JButton("Create Chat");
    createChatButton.addActionListener(new CreateChatButtonListener());
    gbc.insets = new Insets(0, 10, 0, 0);
    gbc.gridwidth = 1;
    gbc.gridx = 2;
    gbc.gridy = 2;
    addComponentToPanel(createChatButton, gbc);

    // all components for displaying live chatrooms and how many people are in them
    allChatroomNamesLabel = new JLabel("Available Chatrooms:");
    allChatroomMembersLabel = new JLabel("Total members:");
    allChatroomNamesTextArea = new JTextArea(4, 4);
    allChatroomNamesScrollPane = new JScrollPane(allChatroomNamesTextArea);
    allChatroomNamesTextArea.setEditable(false);
    new SmartScroller(allChatroomNamesScrollPane);
    allChatroomMembersTextArea = new JTextArea(4, 4);
    allChatroomMembersScrollPane = new JScrollPane(allChatroomMembersTextArea);
    allChatroomMembersTextArea.setEditable(false);
    new SmartScroller(allChatroomMembersScrollPane);

    //ArrayList<String[]> chatNameNumberPairs = new ArrayList<>();//this.client.attemptGetNumUsersInChatrooms();
    // for (String[] chatNameNumberPair : chatNameNumberPairs) {
    //   String roomName = chatNameNumberPair[0];
    //   String numUsers = chatNameNumberPair[1];
    //   allChatroomNamesTextArea.append(roomName + "\n");
    //   allChatroomMembersTextArea.append(numUsers + "\n");
    // }



    
    
    // addComponentToPanel(joinChatLabel);
    // addComponentToPanel(createChatLabel);
    // addComponentToPanel(joinChatField);
    // addComponentToPanel(createChatField);
    // addComponentToPanel(joinChatButton);
    // addComponentToPanel(createChatButton);
    // addComponentToPanel(allChatroomNamesLabel);
    // addComponentToPanel(allChatroomMembersLabel);
    // addComponentToPanel(allChatroomNamesScrollPane);
    // addComponentToPanel(allChatroomMembersScrollPane);
    // addComponentToPanel(logoutButton);

    frame.setTitle(String.format("Join/Create Chatroom: %s", myUsername));
    frame.getContentPane().validate();
    frame.getContentPane().repaint();
  }

  /**
   * Open a new frame (window) for users to register.
   */
  public void openRegisterLoginScreen(String type) {
    LOGGER.info(String.format("Displaying %s screen.", type));

    // New frame that becomes the popup window
    JFrame regFrame = new JFrame(type);
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
    regFrame.add(regPanel, BorderLayout.CENTER);
    regFrame.setSize(300,400);
    regFrame.setVisible(true); 
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
    panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 10, 30));
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
    setRoomList();

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
