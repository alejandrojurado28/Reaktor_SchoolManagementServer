package es.iesjandula.matriculas_horarios_server.parsers;

import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import es.iesjandula.matriculas_horarios_server.models.CursoEtapa;
import es.iesjandula.matriculas_horarios_server.models.DatosBrutoAlumnoMatricula;
import es.iesjandula.matriculas_horarios_server.repositories.IDatosBrutoAlumnoMatriculaRepository;
import es.iesjandula.matriculas_horarios_server.utils.Constants;
import es.iesjandula.matriculas_horarios_server.utils.MatriculasHorariosServerException;

/**
 * Clase de implementación - ParseoDatosBrutosImpl
 * -----------------------------------------------------------------------------------------------------------------
 * Implementa la interfaz {@link IParseoDatosBrutos} para procesar datos en bruto relacionados con 
 * alumnos y sus matrículas. Utiliza el repositorio {@link IDatosBrutoAlumnoMatriculaRepository} para 
 * almacenar la información procesada en la base de datos.
 * -----------------------------------------------------------------------------------------------------------------
 */
@Service
public class ParseoDatosBrutosImpl implements IParseoDatosBrutos
{
    @Autowired
    private IDatosBrutoAlumnoMatriculaRepository iDatosBrutoAlumnoMatriculaRepository;

    /**
     * Implementación del método para parsear y persistir datos en bruto de alumnos y sus matrículas.
     * 
     * @param scanner    						 - Una instancia de {@link Scanner} con los datos de entrada en formato CSV.
     * @param cursoEtapa 						 - Un objeto {@link CursoEtapa} que representa el curso y etapa asociados a los datos.
     * @throws MatriculasHorariosServerException - Si ocurre algún error durante el parseo, como inconsistencias en los datos.
     */
    @Override
    public void parseoDatosBrutos(Scanner scanner, CursoEtapa cursoEtapa) throws MatriculasHorariosServerException 
    {
        try 
        {
            // Linea de campos -> alumno, lengua, mates, ingles
            String lineaCampos = scanner.nextLine();
        
            // Array con valores de los campos
            String[] valoresCampos = lineaCampos.split(Constants.CSV_DELIMITER);
        
            while(scanner.hasNext())
            {
                // Linea de registro -> Martinez Guerbos, Pablo, MATR, MATR, , ,
                String lineaRegistro = scanner.nextLine();
                
                // Array con valores del registro
                String[] valoresRegistro = lineaRegistro.split(Constants.CSV_DELIMITER);
                
                // Se suma 1 porque Alumno es (1) y Martinez Guerbos, Pablo son(2)
                if(valoresCampos.length + 1 == valoresRegistro.length)
                {
                    // Apellidos del Alumno -> Martinez Guerbos
                    String apellidosAlumno = valoresRegistro[0].trim();
                    // Nombre del Alumno -> Pablo
                    String nombreAlumno = valoresRegistro[1].trim();
                    
                    // Crear registro
                    DatosBrutoAlumnoMatricula datosBrutoAlumnoMatricula = new DatosBrutoAlumnoMatricula();
                    
                    // Añadir apellidos del alumno al registro -> Martinez Guerbos
                    datosBrutoAlumnoMatricula.setApellidos(apellidosAlumno);
                    
                    // Añadir nombre del alumno al registro -> Pablo
                    datosBrutoAlumnoMatricula.setNombre(nombreAlumno);
                    
                    for(int i = 1; i < valoresCampos.length; i++)
                    {
                        // Si tiene algun valor el campo de registro de la asignatura 
                        if(!"".equals(valoresRegistro[i + 1].trim()))
                        {
                            // Obtener a que campo de asignatura corresponde
                            String asignatura = valoresCampos[i].trim();
                            
                            // Añadir asignatura matriculada al registro -> LENGUA
                            datosBrutoAlumnoMatricula.setAsignatura(asignatura.toUpperCase());
                            
                            // Añadir curso al registro -> 2DAM
                            datosBrutoAlumnoMatricula.setCursoEtapa(cursoEtapa);
                            
                            // Guardar o actualizar en la tabla -> DatosBrutoAlumnoMatricula
                            iDatosBrutoAlumnoMatriculaRepository.saveAndFlush(datosBrutoAlumnoMatricula);
                        }
                    }
                }
            }
        } 
        catch (Exception e) 
        {
            // Captura cualquier excepción y lanza una excepción personalizada
            throw new MatriculasHorariosServerException(1, "ERROR - Los datos de matrícula no han podido ser procesados", e);
        }
        finally 
        {
            // Cerrar el scanner de forma segura
            if (scanner != null) 
            {
                scanner.close();
            }
        }
    }
}
