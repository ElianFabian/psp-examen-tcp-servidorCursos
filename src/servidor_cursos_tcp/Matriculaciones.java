package servidor_cursos_tcp;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Elián
 */
public class Matriculaciones
{
    // <editor-fold desc="Atributos" defaultstate="collapsed">
    private ArrayList<String> cursos = new ArrayList<>();
    private ArrayList<String> alumnos = new ArrayList<>();
    private HashMap<String, ArrayList<String>> alumnosPorCurso = new HashMap<>();
    // </editor-fold>

    // <editor-fold desc="Métodos" defaultstate="collapsed">
    public synchronized boolean nuevoCurso(String curso)
    {
        if (!existeCurso(curso))
        {
            cursos.add(curso);
            alumnosPorCurso.put(curso, new ArrayList<>());

            return false;
        }
        return true; // Devuelve verdad si existe el curso
    }

    public synchronized boolean nuevoAlumno(String alumno)
    {
        if (!existeAlumno(alumno))
        {
            alumnos.add(alumno);

            return false;
        }
        return true; // Devuelve verdad si existe el alumno
    }

    public synchronized void matricular(String curso, String alumno)
    {
        if (curso == null)
        {
            alumnos.add(alumno);
            return;
        }
        if (alumnos.contains(alumno) && !alumnosPorCurso.get(curso).contains(alumno))
        {
            // Si el alumno existe pero no tiene curso se añade al HashMap
            alumnosPorCurso.get(curso).add(alumno);
        }
        if (!alumnos.contains(alumno))
        {
            alumnos.add(alumno);

            if (!cursos.contains(curso))
            {
                alumnosPorCurso.put(curso, new ArrayList<>());
            }
            alumnosPorCurso.get(curso).add(alumno);
        }
    }

    public synchronized String getListaAlumnos()
    {
        String lista = "";
        for (var item : alumnos)
        {
            lista += item + "\n";
        }
        return lista;
    }

    public synchronized String getListaCursos()
    {
        String lista = "";

        for (var item : cursos)
        {
            lista += item + "\n";
        }
        return lista;
    }

    public synchronized String getListaAlumnosPorCurso()
    {
        var lista = "";

        for (String curso : alumnosPorCurso.keySet())
        {
            lista += curso + ":";
            var sep = "";
            
            for (var alumno : alumnosPorCurso.get(curso))
            {
                lista += sep + alumno;
                sep = ";";
            }
            
            lista += "\n";
        }
        return lista;
    }

    public synchronized boolean estaAlumnoMatriculado(String alumno, String curso)
    {
        return alumnosPorCurso.get(curso).contains(alumno);
    }

    public synchronized boolean existeAlumno(String alumno)
    {
        return alumnos.contains(alumno);
    }

    public synchronized boolean existeCurso(String curso)
    {
        return cursos.contains(curso);
    }
    // </editor-fold>
}
