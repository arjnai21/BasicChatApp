import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientServerHandler implements Runnable {
    BufferedReader socketIn;

    public ClientServerHandler(BufferedReader socketIn) {
        this.socketIn = socketIn;
    }

    @Override
        public void run() {
            try {
                String incoming = "";

                while( (incoming = socketIn.readLine()) != null) {
                    //handle different headers
                    //WELCOME
                    if(incoming.startsWith("WELCOME")){
                        String username = incoming.substring(7).trim();
                        System.out.println(username + " has joined.");
                    }
                    //CHAT
                    else if(incoming.startsWith("CHAT")){
                        String[] message = incoming.split(" ");
                        String username = message[1];
                        String chat = incoming.substring(4 + 1 + username.length()).trim();
                        System.out.println(username + ": " + chat);

                    }
                    //PCHAT
                    else if(incoming.startsWith("PCHAT")){
                        String[] message = incoming.split(" ");
                        String username = message[1];
                        String chat = incoming.substring(5 + 1 + username.length()).trim();
                        System.out.println(username + "(private): " + chat);

                    }
                    //EXIT
                    else if(incoming.startsWith("EXIT")){
                        String name = incoming.substring(4).trim();
                        System.out.println(name + " has left.");
                    }
                    else if(incoming.startsWith("USERS")){
                        String users = incoming.substring(5);
                        System.out.println(users);
                    }
                    else if(incoming.equals("LEFT")){
                        socketIn.close(); //should close socket as well
                        break;                    }
                    else{
                        System.out.println("Unknown message from server");
                    }

                }
            } catch (Exception ex) {
                System.out.println("Exception caught in listener - " + ex);
            } finally{
                System.out.println("Client Listener exiting");
            }
        }
}
