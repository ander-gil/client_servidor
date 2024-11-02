package cliente;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private static String username;

    public static void main(String[] args) {
        System.out.println("Intentando conectar al servidor...");
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT)) {
            System.out.println("Conectado al servidor");

            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Ingrese su nombre de usuario: ");
            username = console.readLine(); // Solicita el nombre de usuario
            new ReadThread(socket).start();
            new WriteThread(socket).start();

            // Enviar el nombre de usuario al servidor al conectarse
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("USUARIO:" + username);

            Thread.sleep(Long.MAX_VALUE);
        } catch (IOException | InterruptedException e) {
            System.out.println("Error al conectar con el servidor: " + e.getMessage());
        }
    }

    // Clase para leer mensajes del servidor
    private static class ReadThread extends Thread {
        private BufferedReader in;
        private Socket socket;

        public ReadThread(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                System.out.println("Error al obtener el flujo de entrada: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            String message;
            try {
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("LISTA_USUARIOS:")) {
                        System.out.println("Usuarios conectados: " + message.substring(14)); // Muestra los usuarios conectados
                    } else {
                        System.out.println("Mensaje recibido: " + message);
                    }
                }
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    System.out.println("Error en la lectura de mensajes: " + e.getMessage());
                }
            } finally {
                closeSocket();
            }
        }

        private void closeSocket() {
            try {
                if (in != null) in.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                System.out.println("Error al cerrar el socket o flujo de entrada: " + e.getMessage());
            }
        }
    }

    // Clase para enviar mensajes al servidor
    private static class WriteThread extends Thread {
        private PrintWriter out;
        private BufferedReader console;
        private Socket socket;

        public WriteThread(Socket socket) {
            this.socket = socket;
            console = new BufferedReader(new InputStreamReader(System.in));
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                System.out.println("Error al obtener el flujo de salida: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            String message;
            try {
                System.out.println("Elija un usuario para chatear:");
                String selectedUser = console.readLine();
                out.println("ELEGIR_USUARIO:" + selectedUser);

                while ((message = console.readLine()) != null) {
                    if (message.equalsIgnoreCase("chao")) {
                        out.println("DESCONEXION:" + username);
                        break;
                    }
                    out.println(message);
                    out.flush();
                }
            } catch (IOException e) {
                System.out.println("Error al enviar mensaje: " + e.getMessage());
            } finally {
                closeSocket();
            }
        }

        private void closeSocket() {
            try {
                if (console != null) console.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                System.out.println("Error al cerrar el socket o flujo de salida: " + e.getMessage());
            }
        }
    }
}
