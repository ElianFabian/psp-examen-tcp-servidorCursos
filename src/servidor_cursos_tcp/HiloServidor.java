package servidor_cursos_tcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Elián
 */
public class HiloServidor extends Thread
{
    //<editor-fold desc="Atributos" defaultstate="collapsed">
    private final Socket socketComunicacion;
    private final Matriculaciones matriculaciones;
    private Estado estado = Estado.INCIAL;

    static private final String COD_TEXTO = "UTF-8";
    static final int NUM_PUERTO = 7890;

    static final String STR_PAT_LETRAS = "[a-zA-Z]+";

    static final String strPatCurso = "[A-Za-z0-9 ]+";
    static final String strPatAlumno = "[A-Za-z0-9 ]+";
    static final Pattern patNuevoCurso = Pattern.compile("^NUEVOCUR (" + strPatCurso + ")$");
    static final Pattern patNuevoAlumno = Pattern.compile("^NUEVOAL (" + strPatAlumno + ")$");
    static final Pattern patNuevaMatricula = Pattern.compile("^MATRIC (" + strPatCurso + ")$");

    static final String STR_AL = "?AL";
    static final String STR_CUR = "?CUR";

    static final String STR_ERROR = "ERRSYNT ";
    static final String STR_ERR_NOAL = "ERR: NOAL ";
    static final String STR_ERR_NOCUR = "ERR: NOCUR ";
    static final String STR_WARN_YAMAT = "WARN: YAMAT ";
    static final String STR_WARN_EXISTEAL = "WARN EXISTEAL ";
    static final String STR_WARN_EXISTECUR = "WARN EXISTECUR ";

    static final String STR_OK_AL = "OK AL ";
    static final String STR_OK_CUR = "OK CUR ";
    static final String STR_OK_MATRIC = "OK MATRIC ";

    static final String STR_EXIT = "exit";
    static final String STR_QUIT = "quit";
    static final String STR_BYE = "bye";

    private static String cursoActual = "";
    //</editor-fold>

    HiloServidor(Socket socketComunicacion, Matriculaciones matriculaciones)
    {
        this.socketComunicacion = socketComunicacion;
        this.matriculaciones = matriculaciones;
    }

    @Override
    public void run()
    {
        try ( var isDeCliente = socketComunicacion.getInputStream();
              var osACliente = socketComunicacion.getOutputStream();
              var isrDeCliente = new InputStreamReader(isDeCliente, COD_TEXTO);
              var brDeCliente = new BufferedReader(isrDeCliente);
              var oswACliente = new OutputStreamWriter(osACliente, COD_TEXTO);
              var bwACliente = new BufferedWriter(oswACliente))
        {
            String lineaRecibida;
            while ((lineaRecibida = brDeCliente.readLine()) != null)
            {
                if (analizarLineaRecibida(lineaRecibida, bwACliente, matriculaciones)) break;

                bwACliente.newLine();
                bwACliente.flush();
            }
        }
        catch (IOException ex)
        {
            Logger.getLogger(HiloServidor.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally
        {
            if (socketComunicacion != null) try
            {
                socketComunicacion.close();
                System.out.printf("Cliente desconectado desde %s:%d.\n",
                        socketComunicacion.getInetAddress().getHostAddress(),
                        socketComunicacion.getPort());
            }
            catch (IOException ex)
            {
                Logger.getLogger(HiloServidor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Analiza el contenido de la línea recibida y escribe la correspondiente
     * respuesta.
     *
     * @return Devuelve verdadero en caso de recibir "exit"/"quit" en
     * lineaRecibida para salir del bucle.
     */
    private boolean analizarLineaRecibida(
            final String lineaRecibida,
            final BufferedWriter bwACliente,
            final Matriculaciones matriculaciones) throws IOException
    {
        Matcher m;
        boolean error = false;

        switch (estado)
        {
            case INCIAL:
                // <editor-fold desc="Salir" defaultstate="collapsed">
                if (lineaRecibida.equals(STR_EXIT) || lineaRecibida.equals(STR_QUIT))
                {
                    bwACliente.write(STR_BYE);
                    return true;
                }
                // </editor-fold>

                // <editor-fold desc="Nuevo curso - NUEVOCUR $curso" defaultstate="collapsed">
                if ((m = patNuevoCurso.matcher(lineaRecibida)).matches())
                {
                    var curso = m.group(1);
                    var existeCurso = matriculaciones.nuevoCurso(curso);

                    if (existeCurso) bwACliente.write(STR_WARN_EXISTECUR + curso);
                    else bwACliente.write(STR_OK_CUR + curso);
                }
                // </editor-fold>

                // <editor-fold desc="Nuevo alumno - NUEVOAL $alumno" defaultstate="collapsed">
                else if ((m = patNuevoAlumno.matcher(lineaRecibida)).matches())
                {
                    var alumno = m.group(1);
                    var existeAlumno = matriculaciones.nuevoAlumno(alumno);

                    if (existeAlumno) bwACliente.write(STR_WARN_EXISTEAL + alumno);
                    else bwACliente.write(STR_OK_AL + alumno);
                }
                // </editor-fold>

                // <editor-fold desc="Consultar lista cursos - ?CUR" defaultstate="collapsed">
                else if (lineaRecibida.equals(STR_CUR))
                {
                    bwACliente.write(matriculaciones.getListaAlumnosPorCurso());
                }
                // </editor-fold>
                
                // <editor-fold desc="Consultar lista alumnos - ?AL" defaultstate="collapsed">
                else if (lineaRecibida.equals(STR_AL))
                {
                    bwACliente.write(matriculaciones.getListaAlumnos());
                }
                // </editor-fold>

                // <editor-fold desc="Matricular - MATRIC $curso" defaultstate="collapsed">
                else if ((m = patNuevaMatricula.matcher(lineaRecibida)).matches())
                {
                    cursoActual = m.group(1);
                    var existeCurso = matriculaciones.existeCurso(cursoActual);

                    if (existeCurso) estado = Estado.MATRIC;
                    else bwACliente.write(STR_ERR_NOCUR + cursoActual);
                }
                // </editor-fold>

                else error = true;

                break;

            case MATRIC:
                // <editor-fold desc="Matricular - (Se introducen alumnos separados por salto de línea." defaultstate="collapsed">
                if (lineaRecibida.matches(strPatAlumno))
                {
                    var alumno = lineaRecibida;

                    if (!matriculaciones.existeAlumno(alumno))
                    {
                        bwACliente.write(STR_ERR_NOAL + alumno);
                    }
                    else
                    {
                        var estaAlumnoMatriculadoEnCurso = matriculaciones.estaAlumnoMatriculado(alumno, cursoActual);
                        if (estaAlumnoMatriculadoEnCurso)
                        {
                            bwACliente.write(STR_WARN_YAMAT + alumno + " " + cursoActual);
                        }
                        else
                        {
                            matriculaciones.matricular(cursoActual, alumno);
                            bwACliente.write(STR_OK_MATRIC + alumno + " " + cursoActual);
                        }
                    }
                }
                // </editor-fold>

                //<editor-fold desc="Finalizar estado - "PRESIONAR ENTER" defaultstate="collapsed">
                else if (lineaRecibida.length() == 0) estado = Estado.INCIAL;
                //</editor-fold>

                else error = true;

                break;
        }

        // <editor-fold desc="Error" defaultstate="collapsed">
        if (error) bwACliente.write(STR_ERROR + "#" + lineaRecibida + "#");
        // </editor-fold>

        return false;
    }
}
