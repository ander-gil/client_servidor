package servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {
    private static final int PORT = 12345;
    private static List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Servidor iniciado. Esperando conexiones...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.start();
            }
        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
    }

    // Clase interna para manejar cada cliente
    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Pedir al cliente su nombre de usuario
                out.println("Por favor, ingresa tu nombre de usuario:");
                username = in.readLine();
                synchronized (clients) {
                    clients.add(this);
                    broadcastUserList(); // Enviar lista de usuarios conectados a todos
                }
                System.out.println("Cliente conectado: " + username);

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equalsIgnoreCase("chao")) {
                        // El cliente abandona el chat
                        break;
                    }
                    System.out.println(username + ": " + message);
                    broadcastMessage(username + ": " + message);
                }
            } catch (IOException e) {
                System.out.println("Error en la comunicación con el cliente: " + e.getMessage());
            } finally {
                closeConnections();
            }
        }

        // Enviar mensaje a todos los clientes conectados
        private void broadcastMessage(String message) {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    if (client != this) {
                        client.out.println(message);
                    }
                }
            }
        }

        // Enviar lista de usuarios a todos los clientes
        private void broadcastUserList() {
            StringBuilder userList = new StringBuilder("Usuarios conectados: ");
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    userList.append(client.username).append(", ");
                }
            }
            for (ClientHandler client : clients) {
                client.out.println(userList.toString());
            }
        }

        private void closeConnections() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();

                synchronized (clients) {
                    clients.remove(this); // Remover cliente de la lista
                    broadcastUserList(); // Actualizar lista para los demás
                }
                System.out.println(username + " ha abandonado el chat.");
            } catch (IOException e) {
                System.out.println("Error al cerrar las conexiones: " + e.getMessage());
            }
        }
    }
}
