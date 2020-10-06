import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class ServerClientHandler implements Runnable {
    ClientConnectionData client;
    ArrayList<ClientConnectionData> clientList;
    HashMap<String, ClientConnectionData> clientMap;

    public ServerClientHandler(ClientConnectionData client, ArrayList<ClientConnectionData> clientList, HashMap<String, ClientConnectionData> clientMap) {
        this.client = client;
        this.clientList = clientList;
        this.clientMap = clientMap;
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

    private boolean userNameInClientList(final String userName){
        return clientMap.containsKey(userName);
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
            } while (userNameInClientList(userName));
            System.out.println(userName);

            synchronized (clientList) {
                client.setUserName(userName);
                clientList.add(client);
            }
            synchronized (clientMap) {
                clientMap.put(userName, client);
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
            synchronized (clientMap) {
                clientMap.remove(client.getUserName());
            }
            System.out.println(client.getName() + " has left.");
            broadcast(String.format("EXIT %s", client.getUserName()));
            try {
                client.getSocket().close();
            } catch (IOException ex) {}

        }
    }
}
