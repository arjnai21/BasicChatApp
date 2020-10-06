import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class ServerClientHandler implements Runnable {
    ClientConnectionData client;
    ArrayList<ClientConnectionData> clientList;

    public ServerClientHandler(ClientConnectionData client, ArrayList<ClientConnectionData> clientList) {
        this.client = client;
        this.clientList = clientList;
    }

    /**
     * Broadcasts a message to all clients connected to the server.
     */
    public void broadcast(String msg) {
        try {
            System.out.println("Broadcasting -- " + msg);
            synchronized (clientList) {
                for (ClientConnectionData c : clientList){
                    c.getOut().println(msg);
                    // c.getOut().flush();
                }
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }
        
    }

    public void broadcast(String msg, boolean excludeCurrentUser) {
        try {
            System.out.println("Broadcasting -- " + msg);
            synchronized (clientList) {
                for (ClientConnectionData c : clientList){
                    if(excludeCurrentUser && c == client) {
                        continue;
                    }
                    c.getOut().println(msg);
                    // c.getOut().flush();
                }
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }

    }

    private boolean nameInClientList(final String name){
        boolean containsName = false;
        for (ClientConnectionData data : clientList) {
            if (data.getUserName().equals(name)) {
                containsName = true;
                break;
            }
        }
        return containsName;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = client.getInput();
            PrintWriter out = client.getOut();

            //get userName, first message from user
            String userName;
            do {
                out.println("SUBMITNAME");
                userName = in.readLine().trim();
            } while (nameInClientList(userName));
            System.out.println(userName);

            synchronized (clientList) {
                client.setUserName(userName);
                clientList.add(client);
            }
            
            System.out.println("added client " + client.getName());
            //notify all that client has joined
            broadcast(String.format("WELCOME %s", client.getUserName()));

            
            String incoming = "";

            while( (incoming = in.readLine()) != null) {
                if (incoming.startsWith("CHAT")) {
                    String chat = incoming.substring(4).trim();
                    if (chat.length() > 0) {
                        String msg = String.format("CHAT %s %s", client.getUserName(), chat);
                        broadcast(msg, true);
                    }
                } else if (incoming.startsWith("QUIT")){
                    break;
                }
            }
        } catch (Exception ex) {
            if (ex instanceof SocketException) {
                System.out.println("Caught socket ex for " + 
                    client.getName());
            } else {
                System.out.println(ex);
                ex.printStackTrace();
            }
        } finally {
            //Remove client from clientList, notify all
            synchronized (clientList) {
                clientList.remove(client); 
            }
            System.out.println(client.getName() + " has left.");
            broadcast(String.format("EXIT %s", client.getUserName()));
            try {
                client.getSocket().close();
            } catch (IOException ex) {}

        }
    }
}
