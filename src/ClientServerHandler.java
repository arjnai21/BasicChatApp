import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

public class ClientServerHandler implements Runnable {
    ObjectInputStream socketIn;
    String users;

    public ClientServerHandler(ObjectInputStream socketIn) {
        this.socketIn = socketIn;
        users = "";
    }

    @Override
        public void run() {
            try {
                Message incoming;

                while( (incoming = (Message) socketIn.readObject()) != null) {
                    String msgHeader = incoming.getMsgHeader().trim();
                    String msgBody = incoming.getMsgBody().trim();
                    //handle different headers
                    //WELCOME
                    if(msgHeader.equals("WELCOME")){
//                        String username = incoming.getMsgBody();
                        System.out.println(msgBody + " has joined.");
                    }
                    //CHAT
                    else if(msgHeader.equals("CHAT")){
                        String username = msgBody.substring(0, msgBody.indexOf(' '));
                        String chat = msgBody.substring(username.length()).trim();
                        System.out.println(username + ": " + chat);

                    }
                    //PCHAT
                    else if(msgHeader.equals("PCHAT")){
                        String[] message = incoming.getMsgBody().split(" ");
                        String username = message[0];
                        String chat = incoming.getMsgBody().substring(username.length()).trim();
                        System.out.println(username + " (private): " + chat);

                    }
                    //EXIT
                    else if(msgHeader.equals("EXIT")){
                        String name = incoming.getMsgBody();
                        System.out.println(name + " has left.");
                    }
                    else if(msgHeader.equals("USERS")){
                        users = msgBody;
                    }
                    else if(incoming.getMsgHeader().equals("NOUSER")){
                        String recipient = incoming.getMsgBody();
                        System.out.println("Username \"" + recipient + "\" does not exist.");
                    }
                    else if(msgHeader.equals("LEFT")){
//                        socketIn.close(); //should close socket as well
                        break;
                    }

                    else{
                        System.out.println("Unknown message from server");
                    }

                }

            }
            catch (SocketException ex){
                //socket closed.
            }
            catch (Exception ex) {
                System.out.println("Exception caught in listener - " + ex);
            } finally{
                System.out.println("Client Listener exiting");
            }
        }
}
