package server;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;

public class ClientManager implements Runnable {

    private final Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String name;

    public final static ArrayList<ClientManager> clients = new ArrayList<>();

    public ClientManager(Socket socket) {
        this.socket = socket;
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            name = bufferedReader.readLine();

            clients.add(this);

            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
            String messageConnection = "Server info <" + time + "> " + name + " подключился к чату ";

            System.out.println(messageConnection);
            broadcastMessage(messageConnection);
        } catch (IOException e) {
            closeEverything();
        }
    }

    @Override
    public void run() {
        String massageFromClient;

        while (socket.isConnected()) {
            try {
                massageFromClient = bufferedReader.readLine();
                broadcastMessage(massageFromClient);
            } catch (IOException e) {
                closeEverything();
                break;
            }
        }
    }

    /**
     * Рассылка сообщения по сети всем подключенным клиентам
     * с возможностью передачи личного сообщения клиенту
     * дописав в начале сообщения @имя клиента
     */
    private void broadcastMessage(String message) {
        String[] messageParse1 = new String[2];
        String[] messageParse2 = new String[2];
        boolean haveSymbol = message.contains("@");

        if (haveSymbol) {
            message = message.replace("@", "");
            messageParse1 = message.split(":", 2);
            messageParse2 = messageParse1[1].stripLeading().split(" ", 2);
            message = messageParse1[0]
                    .concat(": ")
                    .concat(messageParse2[1]);
        }

        for (ClientManager client : clients) {
            try {
                if (haveSymbol && client.name.equals(messageParse2[0])) {
                    client.bufferedWriter.write(message);
                    client.bufferedWriter.newLine();
                    client.bufferedWriter.flush();
                    break;
                } else if (!haveSymbol && !client.name.equals(name)) {
                    client.bufferedWriter.write(message);
                    client.bufferedWriter.newLine();
                    client.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything();
            }
        }
    }


    private void closeEverything() {
        // Удаление клиента из коллекции
        removeClient();
        try {
            // Завершаем работу буфера на чтение данных
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            // Завершаем работу буфера для записи данных
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            // Закрытие соединения с клиентским сокетом
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeClient() {
        clients.remove(this);

        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        String messageDisconnection = "Server info <" + time + "> " + name + " покинул чат.";

        System.out.println(messageDisconnection);
        broadcastMessage(messageDisconnection);
    }
}
