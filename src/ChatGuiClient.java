import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
/**
 * For Java 8, javafx is installed with the JRE. You can run this program normally.
 * For Java 9+, you must install JavaFX separately: https://openjfx.io/openjfx-docs/
 * If you set up an environment variable called PATH_TO_FX where JavaFX is installed
 * you can compile this program with:
 *  Mac/Linux:
 *      > javac --module-path $PATH_TO_FX --add-modules javafx.controls day10_chatgui/ChatGuiClient.java
 *  Windows CMD:
 *      > javac --module-path %PATH_TO_FX% --add-modules javafx.controls day10_chatgui/ChatGuiClient.java
 *  Windows Powershell:
 *      > javac --module-path $env:PATH_TO_FX --add-modules javafx.controls day10_chatgui/ChatGuiClient.java
 * 
 * Then, run with:
 * 
 *  Mac/Linux:
 *      > java --module-path $PATH_TO_FX --add-modules javafx.controls day10_chatgui.ChatGuiClient 
 *  Windows CMD:
 *      > java --module-path %PATH_TO_FX% --add-modules javafx.controls day10_chatgui.ChatGuiClient
 *  Windows Powershell:
 *      > java --module-path $env:PATH_TO_FX --add-modules javafx.controls day10_chatgui.ChatGuiClient
 * 
 * There are ways to add JavaFX to your to your IDE so the compile and run process is streamlined.
 * That process is a little messy for VSCode; it is easiest to do it via the command line there.
 * However, you should open  Explorer -> Java Projects and add to Referenced Libraries the javafx .jar files 
 * to have the syntax coloring and autocomplete work for JavaFX 
 */

class ServerInfo {
    public final String serverAddress;
    public final int serverPort;

    public ServerInfo(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }
}

public class ChatGuiClient extends Application {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    
    private Stage stage;
    private TextArea messageArea;
    private TextField textInput;
    private Button sendButton;
    private Button specialButton;

    private Stage userListStage;
    private TextArea nameArea = new TextArea();

    private ServerInfo serverInfo;
    //volatile keyword makes individual reads/writes of the variable atomic
    // Since username is accessed from multiple threads, atomicity is important 
    private volatile String username = "";
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //If ip and port provided as command line arguments, use them
        List<String> args = getParameters().getUnnamed();
        if (args.size() == 2){
            this.serverInfo = new ServerInfo(args.get(0), Integer.parseInt(args.get(1)));
        }
        else {
            //otherwise, use a Dialog.
            Optional<ServerInfo> info = getServerIpAndPort();
            if (info.isPresent()) {
                this.serverInfo = info.get();
            } 
            else{
                Platform.exit();
                return;
            }
        }

        this.stage = primaryStage;
        BorderPane borderPane = new BorderPane();

        messageArea = new TextArea();
        messageArea.setWrapText(true);
        messageArea.setEditable(false);
        borderPane.setCenter(messageArea);

        //At first, can't send messages - wait for WELCOME!
        textInput = new TextField();
        textInput.setEditable(false);
        textInput.setOnAction(e -> sendMessage());
        sendButton = new Button("Send");
        sendButton.setDisable(true);
        sendButton.setOnAction(e -> sendMessage());
        specialButton = new Button("Users");
        specialButton.setDisable(true);
        specialButton.setOnAction(e -> messageArea.appendText("Users: " + nameArea.getText().substring(0, nameArea.getText().length() - 1).replaceAll("\n", ", ") + "\n"));

        HBox hbox = new HBox();
        hbox.getChildren().addAll(new Label("Message: "), textInput, sendButton, specialButton);
        HBox.setHgrow(textInput, Priority.ALWAYS);
        borderPane.setBottom(hbox);

        Scene scene = new Scene(borderPane, 400, 500);
        stage.setTitle("Chat Client");
        stage.setScene(scene);
        stage.show();

        ServerListener socketListener = new ServerListener();
        
        //Handle GUI closed event
        stage.setOnCloseRequest(e -> {
            try {
                out.writeObject(new Message("QUIT", ""));
            } catch (Exception ex) {}
            socketListener.appRunning = false;
            try {
                socket.close(); 
            } catch (IOException ex) {}
        });

        new Thread(socketListener).start();
    }

    private void sendMessage() {
        String message = textInput.getText().trim();
        if (message.length() == 0)
            return;
        textInput.clear();
        try {
            if(message.charAt(0) == '@') {
                //pchat
                String[] tokens = message.split(" ");
                int subStrNum = 0;
                int index = 0;
                while (tokens[index].charAt(0) == '@'){
                    subStrNum += tokens[index].length() + 1;
                    index++;
                }
                String recipients = message.substring(0, subStrNum).trim();
                String chat = message.substring(subStrNum).trim();
                if(chat.length() == 0){
                    System.out.println("You cannot send empty messages");
                }
                Message chatMessage = new Message("PCHAT", (subStrNum + Integer.toString(subStrNum).length() + 1) + " " + recipients + " " + chat);//String.format("PCHAT %s %s", recipient, chat);
                out.writeObject(chatMessage);
            } else {
                out.writeObject(new Message("CHAT", message));
            }
        } catch (Exception ex) {}
        
    }

    private Optional<ServerInfo> getServerIpAndPort() {
        // In a more polished product, we probably would have the ip /port hardcoded
        // But this a great way to demonstrate making a custom dialog
        // Based on Custom Login Dialog from https://code.makery.ch/blog/javafx-dialogs-official/

        // Create a custom dialog for server ip / port
        Dialog<ServerInfo> getServerDialog = new Dialog<>();
        getServerDialog.setTitle("Enter Server Info");
        getServerDialog.setHeaderText("Enter your server's IP address and port: ");

        // Set the button types.
        ButtonType connectButtonType = new ButtonType("Connect", ButtonData.OK_DONE);
        getServerDialog.getDialogPane().getButtonTypes().addAll(connectButtonType, ButtonType.CANCEL);

        // Create the ip and port labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField ipAddress = new TextField();
        ipAddress.setPromptText("e.g. localhost, 127.0.0.1");
        grid.add(new Label("IP Address:"), 0, 0);
        grid.add(ipAddress, 1, 0);

        TextField port = new TextField();
        port.setPromptText("e.g. 54321");
        grid.add(new Label("Port number:"), 0, 1);
        grid.add(port, 1, 1);


        // Enable/Disable connect button depending on whether a address/port was entered.
        Node connectButton = getServerDialog.getDialogPane().lookupButton(connectButtonType);
        connectButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax).
        ipAddress.textProperty().addListener((observable, oldValue, newValue) -> {
            connectButton.setDisable(newValue.trim().isEmpty());
        });

        port.textProperty().addListener((observable, oldValue, newValue) -> {
            // Only allow numeric values
            if (! newValue.matches("\\d*"))
                port.setText(newValue.replaceAll("[^\\d]", ""));

            connectButton.setDisable(newValue.trim().isEmpty());
        });

        getServerDialog.getDialogPane().setContent(grid);
        
        // Request focus on the username field by default.
        Platform.runLater(() -> ipAddress.requestFocus());


        // Convert the result to a ServerInfo object when the login button is clicked.
        getServerDialog.setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                return new ServerInfo(ipAddress.getText(), Integer.parseInt(port.getText()));
            }
            return null;
        });

        return getServerDialog.showAndWait();
    }

    private void getName(){
        try {
            while (!(((Message) in.readObject()).getMsgHeader().equals("SUBMITNAME")));

            TextInputDialog nameDialog = new TextInputDialog();
            nameDialog.setTitle("Enter Chat Name");
            nameDialog.setHeaderText("Please enter your username.");
            nameDialog.setContentText("Name: ");
            
        
            while(username.equals("")) {
                Optional<String> name = nameDialog.showAndWait();
                if (!name.isPresent() || name.get().trim().equals(""))
                    nameDialog.setHeaderText("You must enter a nonempty name: ");
                else if (name.get().trim().contains(" "))
                    nameDialog.setHeaderText("The name must have no spaces: ");
                else {
                    String possibleName = name.get().trim();
                    out.writeObject(new Message("SUBMITNAME", possibleName));
                    if (((Message) in.readObject()).getMsgHeader().equals("SUBMITNAME")) {
                        nameDialog.setHeaderText("Name already taken. Enter a different user name: ");
                    } else {
                        username = possibleName;
                    }
                }
            }
        } catch (Exception e) {}
    }

    class ServerListener implements Runnable {

        volatile boolean appRunning = false;

        public void run() {
            try {
                // Set up the socket for the Gui
                socket = new Socket(serverInfo.serverAddress, serverInfo.serverPort);
                in = new ObjectInputStream(socket.getInputStream());
                out = new ObjectOutputStream(socket.getOutputStream());
                
                appRunning = true;

                // String line;
                // while((line = in.readLine()) != null) {
                //     System.out.println(line.trim());
                // }

                //Ask the gui to show the username dialog and update username
                //Send to the server
                Platform.runLater(() -> {
                    getName();
                });

                while(username.equals(""));
                //handle all kinds of incoming messages
                Message incoming;

                // Welcome!
                Platform.runLater(() -> {
                    stage.setTitle("Chatter - " + username);
                    textInput.setEditable(true);
                    sendButton.setDisable(false);
                    specialButton.setDisable(false);
                    messageArea.appendText("Welcome to the chatroom, " + username + "!\n");
                });
                BorderPane borderPane = new BorderPane();

                // Create userListStage!
                Platform.runLater(() -> {
                    nameArea.setWrapText(true);
                    nameArea.setEditable(false);
                    borderPane.setCenter(nameArea);
    
                    userListStage = new Stage();
                    Scene scene = new Scene(borderPane, 400, 500);
                    userListStage.setTitle("Users");
                    userListStage.setScene(scene);
                    userListStage.setX(stage.getX() + stage.getWidth() + 5);
                    userListStage.setY(stage.getY());
                    userListStage.show();
                });

                while (appRunning && (incoming = (Message)in.readObject()) != null) {
                    if (incoming.getMsgHeader().equals("WELCOME")) {
                        String user = incoming.getMsgBody();
                        //got welcomed? Now you can send messages!
                        if (user.equals(username)) {
                            Platform.runLater(() -> {
                                stage.setTitle("Chatter - " + username);
                                textInput.setEditable(true);
                                sendButton.setDisable(false);
                                messageArea.appendText("Welcome to the chatroom, " + username + "!\n");
                            });
                        }
                        else {
                            Platform.runLater(() -> {
                                messageArea.appendText(user + " has joined the chatroom.\n");
                            });
                        }
                            
                    } else if (incoming.getMsgHeader().equals("CHAT")) {
                        int split = incoming.getMsgBody().indexOf(" ");
                        String user = incoming.getMsgBody().substring(0, split);
                        String msg = incoming.getMsgBody().substring(split + 1);

                        Platform.runLater(() -> {
                            messageArea.appendText(user + ": " + msg + "\n");
                        });
                    } else if(incoming.getMsgHeader().equals("PCHAT")) {
                        String[] message = incoming.getMsgBody().split(" ");
                        String username = message[0];
                        String chat = incoming.getMsgBody().substring(username.length()).trim();

                        Platform.runLater(() -> {
                            messageArea.appendText(username + " (private): " + chat + "\n");
                        });
                    } else if (incoming.getMsgHeader().equals("EXIT")) { 
                        String user = incoming.getMsgBody();
                        Platform.runLater(() -> {
                            messageArea.appendText(user + " has left the chatroom.\n");
                        });
                    } else if (incoming.getMsgHeader().equals("USERS")) {
                        // System.out.println("\"" + incoming.getMsgBody() + "\"");
                        String[] names = incoming.getMsgBody().substring(7).split(", ");
                        nameArea.clear();
                        for (String name: names) {
                            nameArea.appendText(name + "\n");
                        }
                    }
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (Exception e) {
                if (appRunning)
                    e.printStackTrace();
            } 
            finally {
                Platform.runLater(() -> {
                    stage.close();
                });
                try {
                    if (socket != null)
                        socket.close();
                }
                catch (IOException e){
                }
            }
        }
    }
}