# BasicChatApp
BasicChatApp is a minimalist chat application that you can use with your friends, family, and/or enemies. We scorn decadence and avoid the use of such applications like Discord and Slack. We believe in simplicity, and our mission is to cater to your every simple need.

## Getting Started
First, clone the repository. To start chatting, one machine must be running `ChatServer.java` first. If you successfully compile and run `ChatServer.java`, you should get the following output:
```
Chat Server started.
Local IP: [YOUR IP]
Local Port: 54321 
```

Then, anyone who would like to chat must successfully compile and run `ChatClient.java` (Note: You cannot chat as a user through `ChatServer.java`). Upon running `ChatClient.java`, it should prompt you for an IP:
```
What's the server IP?
```

Type in the IP from `ChatServer.java` and hit enter. Then, you will be prompted for a port number:
```
What's the server IP?
[YOUR IP]
What's the server port?
```

Type in the port from `ChatServer.java` and hit enter.

Now, you should be prompted for a username. This is the name that will represent you in the chat room. The name must be unique, so if a name is already taken, it will prompt you again.
```
What's the server IP?
[YOUR IP]
What's the server port?
54321
Enter your username:
```
You have now joined the chat room and should be able to send and receive messages!

## Chatting Basics
Once you have joined a chat room, you can send a message anytime. You can do this by typing and hitting enter when you are done. Your message will appear in the form of `[NAME]: [MESSAGE]` to other people. Here's an example conversation below:
```
Lizard: Hey what up
Spider: shut up i have class now
Lizard: oh ok
```

Whenever anyone joins the chat room, a message will be brodcasted to everyone saying that `[NAME] has joined.` If you want to leave the chat room, type in `/quit`, which will also brodcast to everyone in the room `[NAME] has left.` For example:
```
Lizard has joined.
Lizard: Hey what up
Spider: shut up i have class now
Lizard: oh ok
Lizard has left.
```

## Advanced chatting features
### Private Messaging
If you want to message exactly one specific person but no one else in the chat room, you can use a private message to do so. Simply prefix your message with `@[THEIR USERNAME] ` and whoever's username you put next to the `@` will be the only person to receive the message. For example, if you were Lizard and typed `@Velociraptor all my homies hate spider`, then whoever is named "Velociraptor" (and only Velociraptor) will get the following:
```
Lizard (private): all my homies hate spider
```
### List of Users
`/users` is a command that will list all of the users currently in the chat room. Anyone who is not connected yet or has already left will not appear on the list. It is especially useful when you want to private message someone but are not sure who your choices are or what their username is. It can also be used to reassure yourself that you have friends to talk to. It can also have the opposite effect.
```
/user
Users: Lizard, Spider, Velociraptor
```
