package chat.server;

import chat.util.Connection;
import chat.util.ConsoleHelper;
import chat.util.Message;
import chat.util.MessageType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            ConsoleHelper.writeMessage("Установлено удаленное соединение с "
                    + socket.getRemoteSocketAddress());
            String userName = null;
            try (Connection connection = new Connection(socket)) {
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                sendListOfUsers(connection, userName);
                serverMainLoop(connection, userName);
            } catch (Exception e) {
                ConsoleHelper.writeMessage(
                        "произошла ошибка при обмене данными с удаленным адресом");
            }
            connectionMap.remove(userName);
            sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
            ConsoleHelper.writeMessage("Connection with remote socket address closed.");
        }

        private String serverHandshake(Connection connection)
                throws IOException, ClassNotFoundException {
            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST));
                Message nameAnsver = connection.receive();
                if (nameAnsver.getType() == MessageType.USER_NAME) {
                    String userName = nameAnsver.getData();
                    if (userName != null && !userName.isEmpty()
                            && !connectionMap.containsKey(userName)) {
                        connectionMap.put(userName, connection);
                        connection.send(new Message(MessageType.NAME_ACCEPTED));
                        return userName;
                    }
                }
            }
        }

        private void sendListOfUsers(Connection connection, String userName) throws IOException {
            for (String s : connectionMap.keySet()) {
                if (!s.equals(userName)) {
                    connection.send(new Message(MessageType.USER_ADDED, s));
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws
                IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) {
                    Message messageForAll = new Message(MessageType.TEXT,
                            userName + ": " + message.getData());
                    sendBroadcastMessage(messageForAll);
                } else ConsoleHelper.writeMessage("Ошибка");

            }
        }
    }


    public static void sendBroadcastMessage(Message message) {
        for (Connection connection : connectionMap.values()) {
            try {
                connection.send(message);
            } catch (IOException e) {
                ConsoleHelper.writeMessage("Ошибка отправки сообщения");
            }
        }
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(ConsoleHelper.readInt())) {
            ConsoleHelper.writeMessage("Сервер запущен.");
            while (true) {
                Handler handler = new Handler(serverSocket.accept());
                handler.start();
            }
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Ошибка! Сервер не доступен.");
        }
    }
}