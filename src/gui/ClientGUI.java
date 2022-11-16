package gui;

// Logging Imports
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

// Java GUI Imports
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

// Java Utils
import java.util.ArrayList;

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
  public JFrame frame;
  public JPanel panel;
  public ArrayList<Component> componentsOnPanel;

  public String myUsername;
  private String currChatRoom;

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
    if (loginUsername.getText().length() == 0 || loginPassword.getText().length() == 0) {
      openToastLabel("Enter a username and password!");
    } else {
      Response res = null;

      try {
        res = client.loginUser(loginUsername.getText(), loginPassword.getText());
        // change GUI to open chat selection screen
        if (res.getServerReply().equalsIgnoreCase("success")) {
          myUsername = loginUsername.getText();
          LOGGER.severe("SUCCESS: Successfully logged in.");
          openChatSelectionScreen();
        } else if (res.getServerReply().equalsIgnoreCase("incorrect")) {
          openToastLabel("Incorrect username/password!");
          LOGGER.severe("FAIL: Incorrect username/password.");
        } else {
          openToastLabel("This user already logged in!");
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
      openToastLabel("Enter a username and password!");
    } else {
      Response res = null;
      try {
        res = client.registerUser(registerUsername.getText(), registerPassword.getText());
        
        if (res == null) {
          openToastLabel("Something went wrong registering. Try again.");
          LOGGER.severe("RegisterUser response is null.");
        } else if  (res.getServerReply().equalsIgnoreCase("success")) {
          myUsername = registerUsername.getText();
          LOGGER.info("User successfully logged in!");
          openChatSelectionScreen();
        } else {
          openToastLabel(res.getServerReply());
          LOGGER.severe(res.getServerReply());
        }
      } catch (RemoteException re) {
        LOGGER.severe("ERROR: Failed connecting to the server. Failed registering user!");
      }
    }
  }

  // =================================

  //         Utility Functions

  // =================================

  /**
   * Display the resulting error.
   * @param message
   */
  public void openToastLabel(String message) {
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
  public void addComponentToPanel(Component component) {
    this.componentsOnPanel.add(component);
    this.panel.add(component);
  }

  /**
   * Display a message and who it was sent by in the chatroom text area.
   * @param sender
   * @param message
   */
  public void displayNewMessage(String sender, String message) {
    this.chatroomTextArea.append(sender + ": " + message + "\n");
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
        openToastLabel("Provide a chatroom name!");
      } else if (chatroomName.contains("@#@") || chatroomName.contains("%&%")) {
        // These are special reserved sequences since all communication is through sockets and
        // delineators between content must be kept unique.
        openToastLabel("Do not use special reserved sequences '@#@' or '%&%'!");
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
        } else { // lookup server returns "nonexistent"
          openToastLabel("Chatroom name does not exist!");
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
        openToastLabel("Enter the name for a chatroom!");
      }else {
        String res = null;
        try {
          res = client.createChatRoom(chatroomName, myUsername);
        } catch (RemoteException re) {
          LOGGER.severe("Error creating a chatroom!");
          return;
        }
        
        if (res.equalsIgnoreCase("success")) {
          currChatRoom = chatroomName;
          openChatroomScreen();
        } else { // when lookup server returns "exists"
          openToastLabel("A chatroom already has that name!");
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

  public void sendNewMessage() {
    String newMessage = chatroomNewMessageField.getText();
    
    if (newMessage.length() == 0) {
      openToastLabel("Message cannot be empty!");
    } else {
      try {
        client.sendMessage(currChatRoom, chatroomNewMessageField.getText());
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
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    // Add messaging components to panel
    this.chatroomLabel = new JLabel("Your username: " + this.myUsername);
    this.chatroomTextArea = new JTextArea(10, 30);
    this.chatroomScrollPane = new JScrollPane(this.chatroomTextArea);
    this.chatroomTextArea.setEditable(false);
    // smart scroller ensures that display shows most recent messages by default, and it also ensures
    // that a new message will not force the screen to move to the bottom if the user has scrolled
    // up and is looking at older messages.
    new SmartScroller(this.chatroomScrollPane);
    this.chatroomNewMessageField = new JTextField(30);
    this.chatroomNewMessageButton = new JButton("Send");
    this.chatroomNewMessageButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        sendNewMessage();
      }
    });
    this.chatroomSeparator = new JSeparator();
    addComponentToPanel(this.chatroomLabel);
    addComponentToPanel(this.chatroomScrollPane);
    addComponentToPanel(this.chatroomNewMessageField);
    addComponentToPanel(this.chatroomNewMessageButton);
    addComponentToPanel(this.chatroomSeparator);

    // Add section for what room members are in chatroom
    this.roomMembersLabel = new JLabel("Users in this chatroom:");
    this.roomMembersTextArea = new JTextArea(5, 20);
    this.roomMembersScrollPane = new JScrollPane(this.roomMembersTextArea);
    this.roomMembersTextArea.setEditable(false);
    new SmartScroller(this.roomMembersScrollPane);
    this.getUsersInChatroomButton = new JButton("Update Members In Room");
    this.getUsersInChatroomButton.addActionListener(new GetUsersInChatroomButtonListener());
    addComponentToPanel(this.roomMembersLabel);
    addComponentToPanel(this.roomMembersScrollPane);
    addComponentToPanel(this.getUsersInChatroomButton);

    // Add button to go back to menu to join another chatroom
    this.backToChatSelectionButton = new JButton("Go Back To Chatroom Selection Screen");
    this.backToChatSelectionButton.addActionListener(new BackToChatSelectionButtonListener());
    this.logoutButton = new JButton("Log Out");
    this.logoutButton.addActionListener(new ChatroomLogOutButtonListener());
    addComponentToPanel(this.backToChatSelectionButton);
    addComponentToPanel(this.logoutButton);

    frame.setTitle(this.chatroomName + " Chatroom");
    frame.pack();
  }

  /**
   * Open the screen for joining or creating a chatroom. This screen has a textbox and button for
   * joining a chatroom, a textbox and button for creating a chatroom, a display for what chatrooms
   * are live and how many people are in them, and a button for logging out.
   */
  public void openChatSelectionScreen() {
    this.removeAllComponents();
    //panel.setLayout(new GridLayout(0, 2));
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
    panel.add(userLabel, gbc);

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
    panel.add(logoutButton, gbc);

    // Joining chat
    joinChatLabel = new JLabel("Join Chatroom (Enter Name):");
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = 1;
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.gridx = 0;
    gbc.gridy = 1;
    panel.add(joinChatLabel, gbc);

    joinChatField = new JTextField(10);
    gbc.gridx = 1;
    gbc.gridy = 1;
    panel.add(joinChatField, gbc);

    joinChatButton = new JButton("Join Chat");
    joinChatButton.addActionListener(new JoinChatButtonListener());
    gbc.insets = new Insets(0, 10, 0, 0);
    gbc.gridx = 2;
    gbc.gridy = 1;
    panel.add(joinChatButton, gbc);

    createChatLabel = new JLabel("Create Chatroom (Enter Name):");
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.gridwidth = 1;
    gbc.gridx = 0;
    gbc.gridy = 2;
    panel.add(createChatLabel, gbc);

    createChatField = new JTextField(10);
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.gridwidth = 1;
    gbc.gridx = 1;
    gbc.gridy = 2;
    panel.add(createChatField, gbc);
    
    createChatButton = new JButton("Create Chat");
    createChatButton.addActionListener(new CreateChatButtonListener());
    gbc.insets = new Insets(0, 10, 0, 0);
    gbc.gridwidth = 1;
    gbc.gridx = 2;
    gbc.gridy = 2;
    panel.add(createChatButton, gbc);

    
    

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
    // TODO: CHANGE THIS
    ArrayList<String[]> chatNameNumberPairs = new ArrayList<>();//this.client.attemptGetNumUsersInChatrooms();
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
    frame.pack();
  }

  /**
   * Open a frame for users to register.
   */
  public void openRegisterScreen() {
    LOGGER.info("Displaying register screen.");
    JFrame regFrame = new JFrame("Register");
    JPanel regPanel = new JPanel();

    regPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 10, 30));
    regPanel.setLayout(new GridLayout(0, 1));

    this.registerTitleLabel = new JLabel("Register");
    this.registerTitleLabel.setFont(new Font("Serif", Font.BOLD, 26));
    this.registerUsernameLabel = new JLabel("Username:");
    this.registerUsername = new JTextField(10);
    this.registerPasswordLabel = new JLabel("Password:");
    this.registerPassword = new JTextField(10);
    this.registerButton = new JButton("Register");
    this.registerButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        registerUser();
      }
    });

    regPanel.add(registerTitleLabel);
    regPanel.add(registerUsernameLabel);
    regPanel.add(registerUsername);
    regPanel.add(registerPasswordLabel);
    regPanel.add(registerPassword);
    regPanel.add(registerButton);

    regFrame.add(regPanel, BorderLayout.CENTER);
    regFrame.setSize(300,400);
    regFrame.setVisible(true); 
  }

  /**
   * Open a frame for users to login.
   */
  public void openLoginScreen() {
    LOGGER.info("Displaying login screen.");
    JFrame loginFrame = new JFrame("Login");
    JPanel loginPanel = new JPanel();

    loginPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 10, 30));
    loginPanel.setLayout(new GridLayout(0, 1));

    // components for registering
    this.loginTitleLabel = new JLabel("Login");
    this.loginTitleLabel.setFont(new Font("Serif", Font.BOLD, 26));
    this.loginUsernameLabel = new JLabel("Username:");
    this.loginUsername = new JTextField(10);
    this.loginPasswordLabel = new JLabel("Password:");
    this.loginPassword = new JTextField(10);
    this.loginButton = new JButton("Login");

    this.loginButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        loginUser();
      }
    });

    loginPanel.add(loginTitleLabel);
    loginPanel.add(loginUsernameLabel);
    loginPanel.add(loginUsername);
    loginPanel.add(loginPasswordLabel);
    loginPanel.add(loginPassword);
    loginPanel.add(loginButton);

    loginFrame.add(loginPanel, BorderLayout.CENTER);
    loginFrame.setSize(300,400);
    loginFrame.setVisible(true); 
  }

  /**
   * Displays the start screen. This shows current active chatrooms as well as users connected.
   * It also gives user the option to register/ login.
   */
  public void openStartScreen() {
    LOGGER.info("Displaying start screen.");
    // Clear the frame
    this.removeAllComponents();

    // Add the register button
    JButton regButton = new JButton("Register");
    regButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        openRegisterScreen();
      }
    });

    // A separator
    JSeparator startSeparator = new JSeparator();

    // Add the loging button
    JButton loginButton = new JButton("Login");
    loginButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        openLoginScreen();
      }
    });

    // Set a panel layout
    panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 10, 30));
    panel.setLayout(new GridLayout(0, 1));

    // Add the buttons/separator to the panel
    addComponentToPanel(loginButton);
    addComponentToPanel(startSeparator);
    addComponentToPanel(regButton);

    // Display the frame and panel
    frame.add(panel, BorderLayout.CENTER);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setTitle("Distributed Chat: Login/Register");
    frame.setSize(500,500);
    frame.setVisible(true);
  }
}
