package org.edu.eci.aws.Socket;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

/**
 *
 * @author Andres Florez
 */
//Heredamos de la clase thread (hilo). Se podr�n ejecutar varias instancias a la vez.
public class ClientSocket extends Thread {

    //Variables
    private Socket scliente = null;		     // Socket para atender al cliente
    private BufferedReader in;                       // Donde leemos lo que env�a el cliente
    private String cadena = "";                      // Aqu� guardamos lo que se recibe
    private String rutabase;                         // Donde est� la web

    //Constructor - Esto se ejecuta al crear la instancia.
    public ClientSocket(Socket clientesocket, String rutabase) {
        this.scliente = clientesocket; //Nuestra conexi�n con el cliente. Como un tel�fono.
        this.rutabase = rutabase; //Ruta de los archivos html.
        setPriority(NORM_PRIORITY - 1); // Bajamos la prioridad del hilo, el importante es el de escucha de peticiones.
    }


	// Esto se ejecuta al ejecutar el m�todo start().
    @Override
    public void run() {
        try {
            //Aqu� leo lo que dice el cliente. El navegador que hay al otro lado.
            in = new BufferedReader(new InputStreamReader(scliente.getInputStream()));

            //El servidor es simple. Solo leemos la p�gina que pide y la enviamos. No tendremos en cuenta el resto de informaci�n.
            //Leemos solo la primera linea. El resto las ignoramos.
            cadena = in.readLine();

            //Solo nos permite ver visualmente la actividad en el servidor.
            System.out.println("Me llega: " + cadena);

            //Seleccionamos lo que nos interesa del mensaje del cliente
            StringTokenizer st = new StringTokenizer(cadena);

            //Si es un GET o HEAD. El primero significa que quiere el archivo o p�gina. El segundo que solo quiere la informaci�n del mismo.
            String temp = "";

            // GET / HTTP/1.1 Este es el formato de la petici�n. M�todo - ruta - protocolo
            if ((st.countTokens() >= 2) && (temp = st.nextToken()).equals("GET")) {
                //Llamamos al m�todo que env�a el archivo pas�ndole la ruta del archivo que pide.
                enviaGetOHead(st.nextToken(), rutabase, "GET");
            } //Si es un HEAD
            else if ((st.countTokens() >= 2) && temp.equals("HEAD")) { // HEAD / HTTP/1.1
                enviaGetOHead(st.nextToken(), rutabase, "HEAD");
                //Si nos env�a otra cosa. Como no la entendemos le indicamos al navegador que existe un error.
            } else {
                //Error 400
                //Mensaje al navegador
                enviaCabeceraEspecial(scliente, null, "HTTP/1.0 400 Petici�n mal formulada");
                //Lo que enviamos a la ventana del navegador. 
                textoPantallaNavegador(scliente, "Petici�n mal formulada. El servidor no puede realizar esa funci�n.");
                scliente.getOutputStream().write("\nHTTP/1.0 400 \n".getBytes());
            }
        } catch (Exception ex) {
            System.err.println("Error durante el env�o " + ex.toString());
        }

        //Cerramos la conexi�n al terminar el env�o de archivo
        try {
            scliente.close();
        } catch (IOException ex) {
            System.err.println("No se cierra el socket " + ex.getMessage());
        }
    }

    //M�todo para mandar ficheros
    void enviaGetOHead(String sfichero, String base, String getHead) throws IOException { //sfichero = archivo pedido

        // Comprobamos si tiene una barra al principio
        if (sfichero.startsWith("/")) {
            sfichero = sfichero.substring(1);
            //Las peticiones ser�n todas con la forma "/" o /directorio/fichero.html"
            //Quitamos la "/" del comienzo para evitar que al montar la ruta completa (base+sfichero) que ser� la ruta donde
            //se encuentra el archivo se produzca algo as� "/home/usuario/public_html//directorio/fichero.html"
        }

        //Cerrar programa
        //Esta es la forma de cerrar el servidor.
        if (sfichero.equalsIgnoreCase("salir")) {
            //Esto aparece en la m�quina donde est� el servidor.
            System.out.println("Programa finalizado correctamente.");

            //Cierra el programa.
            System.exit(0);
        }

        // si acaba en /, le retornamos el index.htm de ese directorio
        // si la cadena esta vacia, retorna el index.htm principal
        if (sfichero.endsWith("/") || sfichero.equals("")) {
            //Buscamos un index.htm*
            try {
                File tempdir = new File(base + sfichero);

                // Lee archivos index.htm* hay y lo enviamos
                if (tempdir.list().length > 0) {

                    boolean continuar = true;
                    for (int i = 0; i < tempdir.list().length && continuar; i++) {
                        //Miramos si existe el archivo index
                        if (tempdir.list()[i].equalsIgnoreCase("index.html") || tempdir.list()[i].equalsIgnoreCase("index.htm")) {
                            //Completamos la ruta
                            sfichero = sfichero + tempdir.list()[i];

                            //Salgo del bucle al encontrarlo.
                            continuar = false;
                        }
                    }

                    //Si no hay ficheros en la carpeta damos un error 404 - fichero no encontrado
                }
            } catch (Exception ex) {
                System.err.println("No se puede abrir / index.___ " + ex.getMessage());
            }
        }

        //Al llegar aqu� la ruta que hay en sfichero no podr� ser un directorio. Ser� un archivo, exista o no.
        try {
            // Ahora leemos el fichero y lo retornamos
            File mifichero = new File(base + sfichero);
            //Si el fichero existe lo enviamos
            if (mifichero.exists() && !mifichero.isDirectory()) {


                //Cabecera
                enviaCabeceraEspecial(scliente, mifichero, "HTTP/1.0 200 ok");

                //Si es una petici�n Get env�o el arhivo
                if (getHead.equalsIgnoreCase("get")) {
                    //Archivo pedido
                    enviaArchivoPorSocket(scliente, mifichero);
                    System.out.println("Le env�o: " + sfichero);
                }

            } // fin de si el fichero existe
            else {
                System.out.println("No existe: " + sfichero);

                //Enviamos cabecera de error
                enviaCabeceraEspecial(scliente, null, "HTTP/1.0 404 Pagina no encontrada");

                //Si es una petici�n Get env�o el texto en pantalla
                if (getHead.equalsIgnoreCase("get")) //Enviamos p�gina de error html 404
                {
                    textoPantallaNavegador(scliente, "ERROR 404. P�gina no encontrada.");
                }


            }
            //Si se produce la excepci�n ha habido un error del servidor
        } catch (Exception ex) {
            System.err.println("Error: " + sfichero + " - " + ex.getMessage());
            //Enviamos la cabecera de error
            enviaCabeceraEspecial(scliente, null, "HTTP/1.0 500 Error interno del servidor.");

            //Si es una petici�n Get env�o el texto en pantalla
            if (getHead.equalsIgnoreCase("get")) //Texto en la pantalla del navegador.
            {
                textoPantallaNavegador(scliente, "ERROR 500. Error interno del servidor.");
            }
        }
    }

    void enviaCabeceraEspecial(Socket socketsalida, File archivoaenviar, String primeraLinea) throws IOException {

        //Metemos las cabeceras
        //La primera l�nea informa del estado
        socketsalida.getOutputStream().write((primeraLinea + "\n").getBytes());

        //Si no hay que enviar ning�n archivo esto no tiene sentido hacerlo.
        if (archivoaenviar != null) {
            //El contenttype es una forma est�ndar de denominar a los tipos de archivos. Estos son algunos.
            String contenttype = null;
            if (archivoaenviar.getName().endsWith(".htm") || archivoaenviar.getName().endsWith(".html")) {
                contenttype = "text/html";
            } else if (archivoaenviar.getName().endsWith(".jpg") || archivoaenviar.getName().endsWith(".jpeg")) {
                contenttype = "image/jpeg";
            } else if (archivoaenviar.getName().endsWith(".css")) {
                contenttype = "text/css";
            }  

            if (contenttype != null) {
                socketsalida.getOutputStream().write(("Content-Type: " + contenttype + "\n").getBytes());
            }

            if (archivoaenviar.exists()) {
                socketsalida.getOutputStream().write(("Content-Length: " + archivoaenviar.length() + "\n").getBytes());
            }
        }
        socketsalida.getOutputStream().write("\n".getBytes());
        //Se env�an dos saltos de l�nea seguidos para indicar que se acaba la cabecera \n
    }

    void enviaArchivoPorSocket(Socket socketsalida, File archivoaenviar) throws FileNotFoundException, IOException {

        //FileInputStream sirve para poder sacar bytes
        //getOutputStream para escribirlos en el buffer de salida del socket
        //byte[] sirve como un punto de intercambio. Un saco en la memoria. El archivo va a la memoria antes de ser enviado.
        FileInputStream inputenviar = new FileInputStream(archivoaenviar);

        //Nuestro saco tendr� 4KB
        byte[] buffer = new byte[4096];

        while (true) {
            //Carga el archivo de disco a memoria en trozos. Lo que cabe en el buffer.
            int n = inputenviar.read(buffer);
            //n es el n�mero de bytes que ha podido leer. Importante para cuando no se llene el buffer enteramente.
            if (n < 0) {
                //Si llega al final del archivo n ser� -1 y el break cortar� el bucle.
                break;
            }
            //Esto escribe lo que hay en el saco hacia el cliente en cada pasada del bucle.
            scliente.getOutputStream().write(buffer, 0, n);
        }

        //Cerramos en enlace al archivo.
        inputenviar.close();
    }

    void textoPantallaNavegador(Socket socketsalida, String textoAEnviar) throws IOException {

        //Con esto podemos enviar texto a la pantalla del navegador que ver� el usuario. Siempre despu�s de enviar una cabecera.
        socketsalida.getOutputStream().write(("<HTML>" + "\n").getBytes());
        socketsalida.getOutputStream().write((" <HEAD>" + "\n").getBytes());
        //La codificaci�n utf-8 (unicode) nos permitir� escribir acentos, �...
        socketsalida.getOutputStream().write(("  <META HTTP-EQUIV= \"Content-Type\"CONTENT=\"text/html;charset= utf-8\"> " + "\n").getBytes());
        socketsalida.getOutputStream().write(("  <TITLE>INFORMACI�N</TITLE>" + "\n").getBytes());
        socketsalida.getOutputStream().write((" </HEAD>" + "\n").getBytes());
        socketsalida.getOutputStream().write((" <BODY>" + "\n").getBytes());
        socketsalida.getOutputStream().write(("  <H1>ERROR</H1>" + "\n").getBytes());
        //El texto personalizado.
        socketsalida.getOutputStream().write(("  <P>" + textoAEnviar + "</P>" + "\n").getBytes());

        socketsalida.getOutputStream().write((" </BODY>" + "\n").getBytes());
        socketsalida.getOutputStream().write(("</HTML>" + "\n").getBytes());


    }
}