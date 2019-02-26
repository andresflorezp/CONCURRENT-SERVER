package org.edu.eci.aws.Socket;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Date;

/**
 * @author Andres Florez
 */
public class ServidorWeb {

    //El puerto por defecto para un servidor web es el 4567
    private static final int puertoDefecto = 4567;

    public static void main(String[] args) {//puerto - ruta de los archivos


        //Variables
        //Puerto de escucha   
        int puertoservidor = puertoDefecto;
        //Leer� de la carpeta public_html en el home por defecto del usuario. /home/usuario/... o c:/documents and settings/usuario/...
        String ruta = System.getProperty("user.dir") + "/public_html/";
        //Socket de escucha. Todas las peticiones llegar�n aqu�.
        ServerSocket enlace = null;


        //Comprobaci�n de los argumentos
        //El primer argumento ser� el puerto de escucha. Ser� el puerto elegido.
        if (args.length > 0) {
            try {
                puertoservidor = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                //Si falla intentar� abrir el puerto por defecto. Si salta esto es porque has escrito algo que no es un n�mero.
                System.err.println("Formato de puerto err�neo. Se intentar� abrir el puerto: " + puertoservidor + ".\n" + ex.getMessage());
            }

            //Comprobamos que el puerto est� entro los posibles.
            if (puertoservidor < 0 || puertoservidor > 65535) {
                try {
                    throw new Exception("El puerto " + puertoservidor + " est� fuera de rango. ");
                } catch (Exception ex) {
                    //Si el puerto no est� en rango ni nos molestamos en intentar abrirlo.
                    puertoservidor = puertoDefecto;
                    System.err.println(ex.getMessage() + "Se intentar� abrir el puerto: " + puertoservidor);
                }
            }
        }


        //Si hay dos argumentos se mirar� la ruta para los archivos del servidor.
        if (args.length > 1) {

            if (!args[1].endsWith("/")) //Le incluimos "/" si es necesario
            {
                args[1] += "/";
            }

            File tempruta = new File(args[1]);
            //Si la ruta existe y es un directorio la cargamos
            if (tempruta.exists() && tempruta.isDirectory()) {
                ruta = args[1];
            } else {
                //Si no existe cargamos la ruta por defecto.
                System.err.println(" No es una ruta v�lida. Se enlaza el servidor a la siguiente ruta: " + ruta);
            }
        }


        //Abrimos el puerto de escucha
        try {
            enlace = new ServerSocket(puertoservidor);
        } catch (IOException ex) {
            //Si da este error ser� porque el puerto est� en uso.
            System.err.println("No se puede abrir el puerto " + ex.getMessage());
            //Cerramos el programa. Si el n� pasado es distinto de 0 significa que se cierra por un error.
            System.exit(-1);
        }

        //Atendemos las peticiones si las hay
        Date bucle = new Date();
        while (bucle.getTime() > 0) { //Bucle infinito
            try {
                //Lanzamos la acci�n en hilos y hacemos un servidor concurrente.
                ClientSocket hiloCliente = new ClientSocket(enlace.accept(), ruta);
                hiloCliente.start();

                //Limpiamos la memoria antes de atender a otra petici�n para evitar que se llene de basura si el colector de basura no se ejecuta solo.
                System.gc();
            } catch (IOException ex) {
                System.err.println("No se puede aceptar la conexi�n. " + ex.getMessage());
            }
        }
    }
}