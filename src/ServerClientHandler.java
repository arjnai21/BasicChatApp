import java.io.*;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    public void broadcast(Message msg) {
        try {
            System.out.println("Broadcasting -- " + msg);
            synchronized (clientList) {
                for (ClientConnectionData c : clientList){
                    c.getOut().writeObject(msg);
                    // c.getOut().flush();
                }
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }

    }

    public void broadcastAndExcludeUser(Message msg) {
        try {
            System.out.println("Broadcasting -- " + msg);
            synchronized (clientList) {
                for (ClientConnectionData c : clientList){
                    if(c == client){
                        continue;
                    }
                    c.getOut().writeObject(msg);
                    // c.getOut().flush();
                }
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }

    }

    //recipient guaranteed to be in room
    public void sendPChat(Message msg, String recipient){
        try {
            System.out.println("Broadcasting -- " + msg);

            synchronized (clientMap) {
                clientMap.get(recipient).getOut().writeObject(msg);

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
            ObjectInputStream in = client.getInput();
            ObjectOutputStream out = client.getOut();

            //get userName, first message from user
            String userName;
            do {
                Message submitName = new Message("SUBMITNAME", "");
                out.writeObject(submitName);
                userName = ((Message) in.readObject()).getMsgBody();
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
//            broadcast(String.format("WELCOME %s", client.getUserName()));
            broadcast(new Message("WELCOME", client.getUserName()));


            Message incoming;

            readClientMessages:
            while( (incoming = (Message) in.readObject()) != null) {
                String msgHeader = incoming.getMsgHeader();
                String msgBody = incoming.getMsgBody();
                switch (msgHeader) {
                    case "CHAT": {
                        if (msgBody.length() > 0) {
//                        String msg = String.format("CHAT %s %s", client.getUserName(), chat);
                            Message msg = new Message("CHAT", client.getUserName()+ " " + msgBody);
                            broadcastAndExcludeUser(msg);
                        }
                        break;
                    }
                    case "PCHAT": {
                        String recipient = msgBody.substring(0, msgBody.indexOf(" "));
                        if (!userNameInClientList(recipient)) {
                            client.getOut().writeObject(new Message("NOUSER", recipient));
                            continue;
                        }
                        String chat = msgBody.substring(recipient.length()).trim();
                        if (chat.length() > 0) {
//                            String msg = String.format("PCHAT %s %s", client.getUserName(), chat);
                            Message msg = new Message("PCHAT", client.getUserName() + " " + chat);
                            sendPChat(msg, recipient);

                        }
                        break;
                    }
                    case "USERS":
                        StringBuilder users = new StringBuilder();
                        users.append("Users: ");
                        for (int i = 0; i < clientList.size() - 1; i++) {
                            users.append(clientList.get(i).getUserName());
                            users.append(", ");
                        }
                        users.append(clientList.get(clientList.size() - 1).getUserName());
                        String sendMsg = users.toString();
                        client.getOut().writeObject(new Message("USERS", sendMsg));
                        break;
                    case "QUIT":
                        break readClientMessages;
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
            try {
                client.getOut().writeObject(new Message("LEFT", ""));
            } catch (IOException e) {
                e.printStackTrace();
            }
            //Remove client from clientList, notify all
            synchronized (clientList) {
                clientList.remove(client);
            }
            synchronized (clientMap) {
                clientMap.remove(client.getUserName());
            }
            System.out.println(client.getName() + " has left.");
//            broadcast(String.format("EXIT %s", client.getUserName()));
            broadcast(new Message("EXIT", client.getUserName()));
            try {

                Thread.sleep(2000); // close client socket to make sure it is closed if client has not already done so

                client.getSocket().close();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
//            try {
//                //client.getSocket().close();
//            } catch (IOException ignored) {}

        }
    }
}
