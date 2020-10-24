import java.io.*;
import java.net.Socket;
import java.security.MessageDigestSpi;
import java.util.Scanner;

public class ChatClient {
    private static Socket socket;
    private static ObjectInputStream socketIn;
    private static ObjectOutputStream out;
    
    public static void main(String[] args) throws Exception {
        Scanner userInput = new Scanner(System.in);

        System.out.println("What's the server IP? ");
        String serverip = userInput.nextLine();
        System.out.println("What's the server port? ");
        int port = userInput.nextInt();
        userInput.nextLine();

        socket = new Socket(serverip, port);
        InputStream stream = socket.getInputStream();
        socketIn = new ObjectInputStream(socket.getInputStream());
        out = new ObjectOutputStream(socket.getOutputStream()); // muts create output stream first



        // Submit name to server
        Message inMessage = (Message) socketIn.readObject();
        while (!(inMessage.getMsgHeader().equals("SUBMITNAME"))){
            System.out.println(inMessage);
        }
        System.out.print("Chat session has started - enter a user name: ");
        String name = userInput.nextLine().trim();
        Message msg = new Message("SUBMITNAME", name);
        out.writeObject(msg); //out.flush();
        while (((Message) socketIn.readObject()).getMsgHeader().equals("SUBMITNAME")) {
            System.out.print("Name already taken. Enter a different user name: ");
            name = userInput.nextLine().trim();
//            msg.setMsgBody(name); this doesnt work for some unexplainable reason
            out.writeObject(new Message("SUBMITNAME", name)); //out.flush();
        }

        // start a thread to listen for server messages
        ClientServerHandler listener = new ClientServerHandler(socketIn);
        Thread t = new Thread(listener);
        t.start();


        String line = userInput.nextLine().trim();
        while(!line.toLowerCase().startsWith("/quit")) {
            if(line.charAt(0) == '@') {
                //pchat
                String[] message = line.split(" ");
                int subStrNum = 0;
                int numRecipients = 0;
                int index = 0;
                while (message[index].charAt(0) == '@'){
                    subStrNum += message[index].length() + 1;
                    index++;
                }
                String recipients = line.substring(0, subStrNum).trim();
                String chat = line.substring(subStrNum).trim();
                if(chat.length() == 0){
                    System.out.println("You cannot send empty messages");
                }
                Message chatMessage = new Message("PCHAT", (subStrNum) + 1 + " " + recipients + " " + chat);//String.format("PCHAT %s %s", recipient, chat);
                out.writeObject(chatMessage);
            }
            else if(line.equals("/whoishere")){
                System.out.println(listener.users);
            }
            else{
                out.writeObject(new Message("CHAT", line));

            }
            line = userInput.nextLine().trim();

        }
        out.writeObject(new Message("QUIT", ""));
        out.close();
        userInput.close();
        socketIn.close();
        socket.close();
        
    }
}
