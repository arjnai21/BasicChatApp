import java.io.*;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    public static final int PORT = 54321;
    private static final ArrayList<ClientConnectionData> clientList = new ArrayList<>();
    private static final HashMap<String, ClientConnectionData> clientMap = new HashMap<>();

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(100);

        try (ServerSocket serverSocket = new ServerSocket(PORT)){
            System.out.println("Chat Server started.");
            System.out.println("Local IP: "
                    + Inet4Address.getLocalHost().getHostAddress());
            System.out.println("Local Port: " + serverSocket.getLocalPort());
        
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.printf("Connected to %s:%d on local port %d\n",
                        socket.getInetAddress(), socket.getPort(), socket.getLocalPort());
                    
                    // This code should really be done in the separate thread
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream()); // must go first
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                    String name = socket.getInetAddress().getHostName();

                    ClientConnectionData client = new ClientConnectionData(socket, in, out, name);
                    // synchronized (clientList) {
                    //     clientList.add(client);
                    // }
                    
                    // System.out.println("added client " + name);

                    //handle client business in another thread
                    pool.execute(new ServerClientHandler(client, clientList, clientMap));
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }

            }
        } 
    }
}
