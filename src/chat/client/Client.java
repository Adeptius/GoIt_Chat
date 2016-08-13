package chat.client;

import chat.util.Connection;
import chat.util.ConsoleHelper;
import chat.util.Message;
import chat.util.MessageType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.*;


public class Client {

    protected Connection connection;
    private volatile boolean clientConnected;

    public class SocketThread extends Thread {
        protected void processIncomingMessage(String message){
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName){
            ConsoleHelper.writeMessage(userName + " присоединился к чату.");
        }

        protected void informAboutDeletingNewUser(String userName){
            ConsoleHelper.writeMessage(userName + " покинул чат.");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected){
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this){
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException{
            while (true){
                Message message = connection.receive();
                if (message.getType() == MessageType.NAME_REQUEST)
                    connection.send(new Message(MessageType.USER_NAME, getUserName()));
                else if (message.getType() == MessageType.NAME_ACCEPTED) {
                    notifyConnectionStatusChanged(true);
                    return;
                }else if(message.getType() == MessageType.SEND_FILE){
                    ConsoleHelper.writeMessage("Входяший файл: " + message.getFileName());
                }else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException{
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT){
                    processIncomingMessage(message.getData());
                }else if (message.getType() == MessageType.USER_ADDED){
                    informAboutAddingNewUser(message.getData());
                }else if (message.getType() == MessageType.SEND_FILE){
                    processIncomingMessage(message.getData());
                    Files.copy(new ByteArrayInputStream(message.getBytesArray()),
                            Paths.get("C:\\ReceivedFiles\\" + message.getFileName()),
                            StandardCopyOption.REPLACE_EXISTING);

                }else throw new IOException("Unexpected MessageType");
            }
        }

        @Override
        public void run() {
            try {
                Socket socket = new Socket(getServerAddress(), getServerPort());
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (Exception e) {
                e.printStackTrace();
                ConsoleHelper.writeMessage("Вылетел цикл клиента!");
                notifyConnectionStatusChanged(false);
            }
        }
    }

    protected String getServerAddress() {
        ConsoleHelper.writeMessage("Введите айпи сервера");
        String ip = ConsoleHelper.readString();
        return ip;
    }

    protected int getServerPort() {
        ConsoleHelper.writeMessage("Введите порт сервера");
        int port = ConsoleHelper.readInt();
        return port;
    }

    protected String getUserName() {
        ConsoleHelper.writeMessage("Введите ваш ник");
        String userName = ConsoleHelper.readString();
        return userName;
    }

    protected boolean shouldSentTextFromConsole() {
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            clientConnected = false;
            ConsoleHelper.writeMessage("Ошибка отправки сообщения");
        }
    }

    protected void sendFile(Path path) {
        System.out.println(path);

        try {
            byte[] bytes = Files.readAllBytes(path);
            connection.send(new Message(MessageType.SEND_FILE,
                    "",
                    bytes,
                    path.getFileName().toString()));
        } catch (IOException e) {
            clientConnected = false;
            ConsoleHelper.writeMessage("Ошибка отправки сообщения");
        }
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        try {
            synchronized (this) {
                this.wait();
            }
        } catch (InterruptedException e) {
            ConsoleHelper.writeMessage("Error !!!");
            return;
        }

        if (clientConnected) ConsoleHelper.writeMessage(
                "Соединение установлено. Для выхода наберите команду 'exit'.");
        else ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");

        while (clientConnected) {
            String message = ConsoleHelper.readString();
            if (message.equalsIgnoreCase("exit")) break;
            if (shouldSentTextFromConsole())
                sendTextMessage(message);
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}
