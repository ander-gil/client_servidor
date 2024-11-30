package cliente;

import java.io.*;
import java.net.Socket;

public class ChatClientPostgre {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT)) {
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Conectado al servidor.");
            showMenu();

            String command;
            while (true) {
                System.out.print("> ");
                command = console.readLine();

                if (command.equalsIgnoreCase("chao")) {
                    out.println(command);
                    System.out.println("Desconectándose del servidor...");
                    break;
                }

                out.println(command);

                String response;
                while ((response = in.readLine()) != null) {
                    if (response.equals("END")) {
                        break;
                    }
                    System.out.println("Respuesta del servidor: " + response);
                }
            }
        } catch (IOException e) {
            System.out.println("Error en el cliente: " + e.getMessage());
        }
    }

    private static void showMenu() {
        System.out.println("Comandos disponibles:");
        System.out.println("1. insertar empleado: Para insertar un empleado.");
        System.out.println("   Formato: insertar empleado:primer_nombre,segundo_nombre,email,fecha_nac,sueldo,comision,cargo_id,dpto_id,ciudad_id");

        System.out.println("2. insertar localizacion: Para insertar una localización.");
        System.out.println("   Formato: insertar localizacion:direccion,ciudad_id");

        System.out.println("3. insertar departamento: Para insertar un departamento.");
        System.out.println("   Formato: insertar departamento:nombre_departamento");

        System.out.println("4. insertar cargo: Para insertar un cargo.");
        System.out.println("   Formato: insertar cargo:nombre_cargo,sueldo_minimo,sueldo_maximo");

        System.out.println("5. insertar pais: Para insertar un país.");
        System.out.println("   Formato: insertar pais:nombre_pais");

        System.out.println("6. insertar ciudad: Para insertar una ciudad.");
        System.out.println("   Formato: insertar ciudad:nombre_ciudad,pais_id");

        System.out.println("7. retirar empleado: Para retirar un empleado, marcándolo como inactivo y agregándolo al histórico.");
        System.out.println("   Formato: retirar empleado:empleado_id");

        System.out.println("8. actualizar ciudad del empleado: Para actualizar la dirección y la ciudad de un empleado.");
        System.out.println("   Formato: actualizar ciudad del empleado:empleado_id,nuevo_ciudad_id");

        System.out.println("9. seleccionar un empleado: Para consultar un empleado.");
        System.out.println("   Formato: seleccionar un empleado:empleado_id");

        System.out.println("10. chao: Para desconectarse del servidor.");
    }

}
