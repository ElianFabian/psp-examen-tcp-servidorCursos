package servidor_cursos_tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author elian
 */
public class ServidorCursosTCP
{
    static final int NUM_PUERTO = 8550;

    public static void main(String[] args)
    {
        try ( var socketServidor = new ServerSocket(NUM_PUERTO))
        {
            System.out.printf("--- Creado socket de servidor en puerto %d. Esperando conexiones de clientes. ---\n", NUM_PUERTO);

            var matriculaciones = new Matriculaciones();

            while (true)
            {
                // Acepta una conexión de cliente tras otra
                var socketConNuevoCliente = socketServidor.accept();

                System.out.printf("Cliente conectado desde %s:%d.\n",
                        socketConNuevoCliente.getInetAddress().getHostAddress(),
                        socketConNuevoCliente.getPort());

                var hiloSesion = new HiloServidor(socketConNuevoCliente, matriculaciones);
                hiloSesion.start();
            }
        }
        catch (IOException ex)
        {
            Logger.getLogger(ServidorCursosTCP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
