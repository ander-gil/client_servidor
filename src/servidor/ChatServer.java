package servidor;

import java.io.*;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/recursos_humanos";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "******";

    public static void main(String[] args) {
        System.out.println("Servidor iniciado. Esperando conexiones...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
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
        private Connection dbConnection;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Conectar a la base de datos
                dbConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                System.out.println("Cliente conectado desde: " + socket.getInetAddress());

                String command;
                while ((command = in.readLine()) != null) {
                    processCommand(command);
                }
            } catch (IOException | SQLException e) {
                System.out.println("Error en la comunicación con el cliente: " + e.getMessage());
            } finally {
                closeConnections();
            }
        }

        // Procesar los comandos enviados por el cliente
        private void processCommand(String command) {
            try {
                command = command.trim().toLowerCase();
                if (command.startsWith("insertar empleado:")) {
                    insertEmpleado(command.substring(18));
                } else if (command.startsWith("insertar localizacion:")) {
                    insertLocalizacion(command.substring(22));
                } else if (command.startsWith("insertar departamento:")) {
                    insertDepartamento(command.substring(22));
                } else if (command.startsWith("insertar cargo:")) {
                    insertCargo(command.substring(15));
                } else if (command.startsWith("insertar pais:")) {
                    insertPais(command.substring(14));
                } else if (command.startsWith("insertar ciudad:")) {
                    insertCiudad(command.substring(16));
                } else if (command.startsWith("retirar empleado:")) {
                    retirarEmpleado(command.substring(17));
                } else if (command.startsWith("actualizar ciudad del empleado:")) {
                    updateCiudadEmpleado(command.substring(31));
                } else if (command.startsWith("seleccionar un empleado:")) {
                    selectEmpleado(command.substring(24));
                }else {
                    out.println("Comando no reconocido.");
                }

            } catch (SQLException e) {
                out.println("Error al procesar comando: " + e.getMessage());
            } finally {
                out.println("END");
            }
        }

        // Métodos para operaciones con la base de datos
        private void insertEmpleado(String data) throws SQLException {
            String[] params = data.split(",");
            String sql = "INSERT INTO empleados (empl_primer_nombre, empl_segundo_nombre, empl_email, empl_fecha_nac, empl_sueldo, empl_comision, empl_cargo_id, empl_dpto_id, empl_ciudad_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setString(1, params[0]); // empl_primer_nombre
                stmt.setString(2, params[1]); // empl_segundo_nombre
                stmt.setString(3, params[2]); // empl_email
                stmt.setDate(4, Date.valueOf(params[3])); // empl_fecha_nac
                stmt.setBigDecimal(5, new BigDecimal(params[4])); // empl_sueldo
                stmt.setBigDecimal(6, new BigDecimal(params[5])); // empl_comision
                stmt.setInt(7, Integer.parseInt(params[6])); // empl_cargo_id
                stmt.setInt(8, Integer.parseInt(params[7])); // empl_dpto_id
                stmt.setInt(9, Integer.parseInt(params[8])); // empl_ciudad_id
                stmt.executeUpdate();
                out.println("Empleado insertado correctamente.");
            }
        }

        private void insertLocalizacion(String data) throws SQLException {
            String[] params = data.split(",");
            String sql = "INSERT INTO LOCALIZACIONES (localiz_direccion, localiz_ciudad_ID) VALUES (?, ?)";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setString(1, params[0]); // localiz_direccion
                stmt.setInt(2, Integer.parseInt(params[1])); // localiz_ciudad_ID
                stmt.executeUpdate();
                out.println("Localización insertada correctamente.");
            } catch (NumberFormatException e) {
                out.println("Error: formato inválido en el campo numérico. " + e.getMessage());
            }
        }

        private void updateCiudadEmpleado(String data) throws SQLException {
            String[] params = data.split(",");

            String sqlEmpleado = "UPDATE EMPLEADOS " +
                    "SET empl_ciudad_ID = ? " +
                    "WHERE empl_ID = ?";

            try (PreparedStatement stmtEmpleado = dbConnection.prepareStatement(sqlEmpleado)) {
                stmtEmpleado.setInt(1, Integer.parseInt(params[1]));
                stmtEmpleado.setInt(2, Integer.parseInt(params[0]));

                int rowsAffected = stmtEmpleado.executeUpdate();

                if (rowsAffected > 0) {
                    out.println("La ciudad del empleado ha sido actualizada correctamente.");
                } else {
                    out.println("Error: No se encontró el empleado con el ID especificado.");
                }
            } catch (NumberFormatException e) {
                out.println("Error: formato inválido en los campos numéricos. " + e.getMessage());
            }
        }


        private void insertDepartamento(String data) throws SQLException {
            String sql = "INSERT INTO DEPARTAMENTOS (dpto_nombre) VALUES (?)";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setString(1, data); // dpto_nombre
                stmt.executeUpdate();
                out.println("Departamento insertado correctamente.");
            }
        }

        private void retirarEmpleado(String data) throws SQLException {
            String[] params = data.split(",");
            String empleadoId = params[0]; // ID del empleado

            String sqlUpdateEmpleado = "UPDATE EMPLEADOS SET empl_activo = FALSE WHERE empl_ID = ?";

            String sqlInsertHistorico = "INSERT INTO HISTORICO (emphist_fecha_retiro, emphist_cargo_ID, emphist_dpto_ID) " +
                    "SELECT CURRENT_DATE, empl_cargo_ID, empl_dpto_ID FROM EMPLEADOS WHERE empl_ID = ?";

            try (
                    PreparedStatement stmtUpdateEmpleado = dbConnection.prepareStatement(sqlUpdateEmpleado);
                    PreparedStatement stmtInsertHistorico = dbConnection.prepareStatement(sqlInsertHistorico)
            ) {
                stmtUpdateEmpleado.setInt(1, Integer.parseInt(empleadoId));
                int rowsUpdated = stmtUpdateEmpleado.executeUpdate();

                if (rowsUpdated > 0) {
                    stmtInsertHistorico.setInt(1, Integer.parseInt(empleadoId)); // ID del empleado
                    stmtInsertHistorico.executeUpdate();

                    out.println("Empleado retirado correctamente y agregado al histórico.");
                } else {
                    out.println("Error: No se encontró un empleado con el ID especificado.");
                }
            } catch (NumberFormatException e) {
                out.println("Error: formato inválido en los campos. " + e.getMessage());
            }
        }


        private void insertCargo(String data) throws SQLException {
            String[] params = data.split(",");
            String sql = "INSERT INTO CARGOS (cargo_nombre, cargo_sueldo_minimo, cargo_sueldo_maximo) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setString(1, params[0]); // cargo_nombre
                stmt.setBigDecimal(2, new BigDecimal(params[1])); // cargo_sueldo_minimo
                stmt.setBigDecimal(3, new BigDecimal(params[2])); // cargo_sueldo_maximo
                stmt.executeUpdate();
                out.println("Cargo insertado correctamente.");
            } catch (NumberFormatException e) {
                out.println("Error: formato inválido en los campos numéricos. " + e.getMessage());
            }
        }

        private void insertPais(String data) throws SQLException {
            String sql = "INSERT INTO PAISES (pais_nombre) VALUES (?)";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setString(1, data); // pais_nombre
                stmt.executeUpdate();
                out.println("País insertado correctamente.");
            }
        }

        private void insertCiudad(String data) throws SQLException {
            String[] params = data.split(",");
            String sql = "INSERT INTO CIUDADES (ciud_nombre, ciud_pais_ID) VALUES (?, ?)";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setString(1, params[0]); // ciud_nombre
                stmt.setInt(2, Integer.parseInt(params[1])); // ciud_pais_ID
                stmt.executeUpdate();
                out.println("Ciudad insertada correctamente.");
            } catch (NumberFormatException e) {
                out.println("Error: formato inválido en el campo numérico. " + e.getMessage());
            }
        }


        private void updateEmpleado(String data) throws SQLException {
            String[] params = data.split(",");
            String sql = "UPDATE empleados SET " + params[1] + " = ? WHERE empl_id = ?";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setString(1, params[2]);
                stmt.setInt(2, Integer.parseInt(params[0]));
                stmt.executeUpdate();
                out.println("Empleado actualizado correctamente.");
            }
        }

        private void selectEmpleado(String data) throws SQLException {
            String sql = "SELECT * FROM empleados WHERE empl_id = ?";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, Integer.parseInt(data));
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    out.println("Empleado: " + rs.getString("empl_primer_nombre") + " " +
                            rs.getString("empl_segundo_nombre") + ", Email: " + rs.getString("empl_email")+" "  +
                            " Fecha de nacimiento : " + rs.getString("empl_fecha_nac"));
                } else {
                    out.println("Empleado no encontrado.");
                }
            }
        }



        // Cerrar conexiones
        private void closeConnections() {
            try {
                if (dbConnection != null && !dbConnection.isClosed()) dbConnection.close();
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
                System.out.println("Cliente desconectado.");
            } catch (IOException | SQLException e) {
                System.out.println("Error al cerrar las conexiones: " + e.getMessage());
            }
        }
    }
}
