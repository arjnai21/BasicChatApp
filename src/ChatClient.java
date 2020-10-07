import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private static Socket socket;
    private static BufferedReader socketIn;
    private static PrintWriter out;
    
    public static void main(String[] args) throws Exception {
        Scanner userInput = new Scanner(System.in);

        System.out.println("What's the server IP? ");
        String serverip = userInput.nextLine();
        System.out.println("What's the server port? ");
        int port = userInput.nextInt();
        userInput.nextLine();

        socket = new Socket(serverip, port);
        socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Submit name to server
        while (!socketIn.readLine().trim().equals("SUBMITNAME"));

        System.out.print("Chat sessions has started - enter a user name: ");
        String name = userInput.nextLine().trim();
        out.println(name); //out.flush();
        while (socketIn.readLine().trim().equals("SUBMITNAME")) {
            System.out.print("Name already taken. Enter a different user name: ");
            name = userInput.nextLine().trim();
            out.println(name); //out.flush();
        }

        // start a thread to listen for server messages
        ClientServerHandler listener = new ClientServerHandler(socketIn);
        Thread t = new Thread(listener);
        t.start();

        String line = userInput.nextLine().trim();
        while(!line.toLowerCase().startsWith("/quit")) {
            if(line.charAt(0) == '@'){
                //pchat
                String[] message = line.split(" ");
                String recipient = message[0].substring(1);
                String chat = line.substring(message[0].length()).trim();
                String msg = String.format("PCHAT %s %s", recipient, chat);
                out.println(msg);
            }
            else if(line.equals("/users")){
                out.println("USERS");
            }
            else{
                String msg = String.format("CHAT %s", line);
                out.println(msg);

            }
            line = userInput.nextLine().trim();

        }
        out.println("QUIT");
        out.close();
        userInput.close();
        socketIn.close();
        socket.close();
        
    }
}
