package es.iesjandula.matriculas_horarios_server.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import es.iesjandula.matriculas_horarios_server.dtos.AlumnoDto;
import es.iesjandula.matriculas_horarios_server.dtos.AsignaturaDto;
import es.iesjandula.matriculas_horarios_server.dtos.CursoEtapaDto;
import es.iesjandula.matriculas_horarios_server.models.Asignatura;
import es.iesjandula.matriculas_horarios_server.models.Bloque;
import es.iesjandula.matriculas_horarios_server.models.CursoEtapa;
import es.iesjandula.matriculas_horarios_server.models.CursoEtapaGrupo;
import es.iesjandula.matriculas_horarios_server.models.DatosBrutoAlumnoMatricula;
import es.iesjandula.matriculas_horarios_server.models.DatosBrutoAlumnoMatriculaGrupo;
import es.iesjandula.matriculas_horarios_server.models.ids.IdCursoEtapa;
import es.iesjandula.matriculas_horarios_server.models.ids.IdCursoEtapaGrupo;
import es.iesjandula.matriculas_horarios_server.parsers.IParseoDatosBrutos;
import es.iesjandula.matriculas_horarios_server.repositories.IAsignaturaRepository;
import es.iesjandula.matriculas_horarios_server.repositories.ICursoEtapaGrupoRepository;
import es.iesjandula.matriculas_horarios_server.repositories.ICursoEtapaRepository;
import es.iesjandula.matriculas_horarios_server.repositories.IDatosBrutoAlumnoMatriculaGrupoRepository;
import es.iesjandula.matriculas_horarios_server.repositories.IDatosBrutoAlumnoMatriculaRepository;
import es.iesjandula.matriculas_horarios_server.utils.Constants;
import es.iesjandula.matriculas_horarios_server.utils.MatriculasHorariosServerException;
import lombok.extern.slf4j.Slf4j;

/**
 * Clase controlador - DireccionController 
 * --------------------------------------------------------------------------- 
 * REST para gestionar la dirección de cursos, grupos y asignación de alumnos.
 * ---------------------------------------------------------------------------
 */

@Slf4j
@RestController
@RequestMapping(value = "/direccion")
public class DireccionController 
{	
	@Autowired
	ICursoEtapaRepository iCursoEtapaRepository;
	
	@Autowired
	ICursoEtapaGrupoRepository iCursoEtapaGrupoRepository;
	
	@Autowired
	IDatosBrutoAlumnoMatriculaRepository iDatosBrutoAlumnoMatriculaRepository;
	
	@Autowired
	IDatosBrutoAlumnoMatriculaGrupoRepository iDatosBrutoAlumnoMatriculaGrupoRepository;
	
	@Autowired
	IParseoDatosBrutos iParseoDatosBrutos;
	
	@Autowired
	IAsignaturaRepository iAsignaturaRepository ;
    
    /**
     * Endpoint para cargar las matrículas a través de un archivo CSV.
     * 
     * Este método procesa un archivo CSV enviado como parte de una solicitud POST.
     * Si el archivo está vacío o hay un error durante el proceso, se lanzan excepciones personalizadas.
     * 
     * @param archivoCsv 		 - El archivo CSV que contiene las matrículas a procesar.
     * @param curso 	 		 - El identificador del curso al que se asignan las matrículas.
     * @param etapa      		 - La etapa educativa (por ejemplo, "Primaria", "Secundaria") asociada al curso.
     * @return ResponseEntity<?> - El mensaje de éxito o el detalle de un error ocurrido durante el procesamiento.
     * @throws IOException 
     */
    @RequestMapping(method = RequestMethod.POST, value = "/cargarMatriculas", consumes = "multipart/form-data")
    public ResponseEntity<?> cargarMatriculas 
    (
            @RequestParam(value = "csv", required = true) MultipartFile archivoCsv,
            @RequestHeader(value = "curso", required = true) Integer curso,
            @RequestHeader(value = "etapa", required = true) String etapa
    )
    {
        try 
        {
            // Si el archivo esta vacio
            if(archivoCsv.isEmpty())
            {
            	// Lanzar excepcion
            	String msgError = "ERROR - El archivo importado está vacío";
            	log.error(msgError);
                throw new MatriculasHorariosServerException(1, msgError);
            }
            
            // Convertir MultipartFile a String
            String archivoCsvReadable = new String(archivoCsv.getBytes());
            
            // Declarar Scanner para realizar lectura del fichero
            Scanner scanner = new Scanner(archivoCsvReadable);
            
            // Registro cursoEtapa
            CursoEtapa cursoEtapa = new CursoEtapa();
            IdCursoEtapa idCursoEtapa = new IdCursoEtapa();
            
            // Asignar los campos al id de cursoEtapa
            idCursoEtapa.setCurso(curso);
            idCursoEtapa.setEtapa(etapa);
            
            // Asignar id al registro cursoEtapa
            cursoEtapa.setIdCursoEtapa(idCursoEtapa);
            
            // Llamar al Service IParseoDatosBrutos para realizar parseo
            this.iParseoDatosBrutos.parseoDatosBrutos(scanner, cursoEtapa);
            
            log.info("INFO - Se ha enviado todo correctamente");
            
            // Devolver OK informando que se ha insertado los registros
            return ResponseEntity.ok().build();
        }
	    catch (MatriculasHorariosServerException matriculasHorariosServerException) 
	    {
	    	// Manejo de excepciones personalizadas
	    	log.error(matriculasHorariosServerException.getBodyExceptionMessage().toString());

	        // Devolver la excepción personalizada con código 1 y el mensaje de error
	        return ResponseEntity.status(404).body(matriculasHorariosServerException.getBodyExceptionMessage());
	    } 
	    catch (IOException ioException) 
	    {
	        // Manejo de excepciones generales
	        String msgError = "ERROR - No se pudo realizar la lectura del fichero";
	        log.error(msgError, ioException);

	        // Devolver una excepción personalizada con código 1, el mensaje de error y la excepcion general
	        MatriculasHorariosServerException matriculasHorariosServerException = new MatriculasHorariosServerException(1, msgError, ioException);
	        return ResponseEntity.status(500).body(matriculasHorariosServerException.getBodyExceptionMessage());
	    }
	}
		
    /**
     * Endpoint para obtener todos los registros de la tabla CursoEtapa.
     * 
     * Este método obtiene todos los registros de la tabla CursoEtapa y los devuelve
     * como una lista. Si la lista está vacía o ocurre algún error durante el proceso,
     * se lanzan excepciones personalizadas con los detalles correspondientes.
     * 
     * @return ResponseEntity<?> - La lista de registros de CursoEtapa si se encuentran datos, o una excepción con el mensaje de error si la lista está vacía o si ocurre un error.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/cursoEtapa")
    public ResponseEntity<?> obtenerCursoEtapa()
    {
        try 
        {
            // Lista usada para guardar los registros de la Tabla CursoEtapa
            List<CursoEtapaDto> listaCursoEtapa= new ArrayList<>();
            
            // Asignar los registros de la Tabla CursoEtapa
            listaCursoEtapa = this.iCursoEtapaRepository.findCursoEtapa();
            
            // Si la lista esta vacia, lanzar excepcion
            if(listaCursoEtapa.isEmpty())
            {
                // Lanzar excepcion y mostrar log con mensaje diferente
                log.error("ERROR - Lista vacía");
                throw new MatriculasHorariosServerException(404, "ERROR - No se ha encontrado ningun curso");
            }
            
            // Devolver la lista
            return ResponseEntity.status(200).body(listaCursoEtapa);
        }
        catch (MatriculasHorariosServerException matriculasHorariosServerException) 
        {
            // Manejo de excepciones personalizadas
            log.error(matriculasHorariosServerException.getBodyExceptionMessage().toString());

            // Devolver la excepción personalizada con código 1 y el mensaje de error
            return ResponseEntity.status(404).body(matriculasHorariosServerException);
        } 
        catch (Exception exception) 
        {
            // Manejo de excepciones generales
            String msgError = "ERROR - No se pudo cargar la lista";
            log.error(msgError, exception);
          
            // Devolver la excepción personalizada con código 1, el mensaje de error y la excepción general	        
            MatriculasHorariosServerException matriculasHorariosServerException = new MatriculasHorariosServerException(1, msgError, exception);
            return ResponseEntity.status(500).body(matriculasHorariosServerException);
        }
    }
    
    /**
     * Endpoint para crear un nuevo grupo en el sistema basado en el curso y etapa proporcionados.
     * 
     * Este método asigna un nuevo grupo a un curso y etapa específicos, asegurándose de que el nombre del grupo sea único 
     * en función de la cantidad de veces que ya existe dicho curso y etapa en la base de datos.
     * 
     * @param curso              - El identificador del curso para el cual se está creando el grupo.
     * @param etapa              - La etapa educativa asociada al curso para el cual se está creando el grupo.
     * @return ResponseEntity<?> - Un mensaje de éxito indicando que el grupo ha sido creado correctamente, o una excepción con el mensaje de error si ocurrió algún problema.
     */
    @RequestMapping(method = RequestMethod.POST, value = "/grupos")
    public ResponseEntity<?> crearGrupo
    (
            @RequestHeader(value = "curso", required = true) Integer curso, 
            @RequestHeader(value = "etapa", required = true) String etapa
    ) 	
    {
        try 
        {     
            // Numero de veces repetido el Curso Etapa en la BD
            int contador = this.iCursoEtapaGrupoRepository.findCountByCursoAndEtapa(curso, etapa);
            
            // Asignar la letra A
            char grupo = Constants.GROUP;
            
            // Asignar la letra según el numero de veces que este repetido en BD
            for (int i = 0; i < contador; i++) 
            {
                grupo++;
            }
            
            // Crear registro de Curso Etapa Grupo
            CursoEtapaGrupo cursoEtapaGrupo = new CursoEtapaGrupo();
            
            // Crear campo de id con todos los campos
            IdCursoEtapaGrupo idCursoEtapaGrupo = new IdCursoEtapaGrupo(curso, etapa, grupo);
            
            // Asignar el id al registro de Curso Etapa
            cursoEtapaGrupo.setIdCursoEtapaGrupo(idCursoEtapaGrupo);
            
            // Insertar en BD
            this.iCursoEtapaGrupoRepository.saveAndFlush(cursoEtapaGrupo);

            // Log de información antes de la respuesta
            log.info("INFO - Grupo creado correctamente para el curso: {} y etapa: {}", curso, etapa);

            // Devolver la respuesta indicando que el grupo ha sido creado correctamente
            return ResponseEntity.ok().build();
        } 
        catch (Exception exception) 
        {
            // Manejo de excepciones generales
            String msgError = "ERROR - No se ha podido crear el grupo";
            log.error(msgError, exception);
            
            // Devolver una excepción personalizada con código 1, el mensaje de error y la excepcion general
            MatriculasHorariosServerException matriculasHorariosServerException = new MatriculasHorariosServerException(1, msgError, exception);
            return ResponseEntity.status(500).body(matriculasHorariosServerException.getBodyExceptionMessage());
        }
    }

	

    /**
     * Endpoint para obtener los grupos asociados a un curso y etapa específicos.
     * 
     * Este método consulta los grupos disponibles para un curso y etapa determinados.
     * Si no se encuentran grupos para la combinación de curso y etapa proporcionados,
     * se lanza una excepción personalizada. En caso de un error general, se maneja adecuadamente 
     * la excepción y se devuelve un mensaje de error.
     * 
     * @param curso              - El identificador del curso para el cual se desean obtener los grupos.
     * @param etapa              - La etapa educativa asociada al curso para la cual se desean obtener los grupos.
     * @return ResponseEntity<?> - La lista de grupos encontrados o una excepción con el mensaje de error si no se encontraron grupos o si ocurre algún fallo durante el proceso.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/grupos")
    public ResponseEntity<?> obtenerGrupo
    (
            @RequestHeader(value = "curso", required = true) Integer curso, 
            @RequestHeader(value = "etapa", required = true) String etapa
    ) 
    {
        try 
        {
            // Crear el objeto CursoEtapa con los parámetros recibidos
            CursoEtapa cursoEtapa = new CursoEtapa();
            IdCursoEtapa idCursoEtapa = new IdCursoEtapa(curso, etapa);
            cursoEtapa.setIdCursoEtapa(idCursoEtapa);

            // Obtener la lista de grupos según curso y etapa
            List<String> grupos = this.iCursoEtapaGrupoRepository.findGrupoByCursoAndEtapa(curso, etapa);

            // Si la lista está vacía, lanzar una excepción
            if (grupos.isEmpty()) 
            {
            	// Lanzar excepcion y mostrar log con mensaje de Error
            	String msgError = "ERROR - No se encontraron grupos para el curso {} y etapa {}";
                log.error(msgError, curso, etapa);
                throw new MatriculasHorariosServerException(1, "ERROR - No se encontraron grupos para el curso y etapa especificados");
            }

            // Log de información antes de la respuesta
            log.info("INFO - Se han encontrado los siguientes grupos para el curso: {} y etapa: {}", curso, etapa);

            // Devolver la lista de grupos encontrados
            return ResponseEntity.status(200).body(grupos);
        } 
        catch (MatriculasHorariosServerException matriculasHorariosServerException) 
        {
        	// Manejo de excepciones personalizadas
        	log.error(matriculasHorariosServerException.getBodyExceptionMessage().toString());

            // Devolver la excepción personalizada con código 1 y el mensaje de error
            return ResponseEntity.status(404).body(matriculasHorariosServerException.getBodyExceptionMessage());
        } 
        catch (Exception exception) 
        {
            // Manejo de excepciones generales
            String msgError = "ERROR - No se pudieron buscar los grupos";
            log.error(msgError, exception);

            // Devolver una excepción personalizada con código 1, el mensaje de error y la excepcion general
            MatriculasHorariosServerException matriculasHorariosServerException = new MatriculasHorariosServerException(1, msgError, exception);
            return ResponseEntity.status(500).body(matriculasHorariosServerException.getBodyExceptionMessage());
        }
    }

    /**
     * Endpoint para eliminar un grupo específico de un curso y etapa determinados.
     * 
     * Este método elimina un grupo asociado a un curso y etapa. Antes de eliminar el grupo, 
     * se transfieren los alumnos asignados a ese grupo a la tabla `DatosBrutoAlumnoMatricula`.
     * Luego, se eliminan los registros del grupo y los alumnos asignados a él en las tablas 
     * correspondientes.
     * 
     * @param curso              - El identificador del curso para el cual se eliminará el grupo.
     * @param etapa              - La etapa educativa asociada al curso para la cual se eliminará el grupo.
     * @param grupo              - El identificador del grupo a eliminar (una letra que representa el grupo).
     * @return ResponseEntity<?> - Un mensaje de éxito indicando que el grupo ha sido eliminado correctamente, o una excepción con el mensaje de error si ocurre un fallo durante el proceso.
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/grupos")
    public ResponseEntity<?> eliminarGrupo
    (
            @RequestHeader(value = "curso", required = true) Integer curso, 
            @RequestHeader(value = "etapa", required = true) String etapa, 
            @RequestHeader(value = "grupo", required = true) Character grupo
    ) 
    {
        try 
        {
            // Crear el objeto CursoEtapaGrupo con los parámetros recibidos
            CursoEtapaGrupo cursoEtapaGrupo = new CursoEtapaGrupo();
            IdCursoEtapaGrupo idCursoEtapaGrupo = new IdCursoEtapaGrupo(curso, etapa, grupo);
            cursoEtapaGrupo.setIdCursoEtapaGrupo(idCursoEtapaGrupo);
            
            if(this.iCursoEtapaGrupoRepository.findById(idCursoEtapaGrupo).isEmpty())
            {
             	// Lanzar excepcion y mostrar log con mensaje de Error
            	String msgError = "ERROR - El grupo indicado no existe";
                log.error(msgError, curso, etapa);
                throw new MatriculasHorariosServerException(1, msgError);
            }

            // Obtener los Alumnos asignados al grupo a eliminar
            List<DatosBrutoAlumnoMatriculaGrupo> alumnosAsignadosAlGrupo = this.iDatosBrutoAlumnoMatriculaGrupoRepository.findAllByCursoEtapaGrupo(cursoEtapaGrupo);
            
            // Registro de DatosBrutoAlumnoMatriculaEntity
            DatosBrutoAlumnoMatricula datosBrutoAlumnoMatricula = new DatosBrutoAlumnoMatricula();

            // Crear el objeto CursoEtapa del Alumno
            CursoEtapa cursoEtapa = new CursoEtapa();
            IdCursoEtapa idCursoEtapa = new IdCursoEtapa();
            idCursoEtapa.setCurso(curso);
            idCursoEtapa.setEtapa(etapa);
            cursoEtapa.setIdCursoEtapa(idCursoEtapa);
            
            // Transferir los Alumnos a la tabla DatosBrutoAlumnoMatricula
            for(DatosBrutoAlumnoMatriculaGrupo alumno : alumnosAsignadosAlGrupo)
            {
                // Asignar valores de Alumno a registro de DatosBrutoAlumnoMatriculaEntity
                datosBrutoAlumnoMatricula.setNombre(alumno.getNombre());
                datosBrutoAlumnoMatricula.setApellidos(alumno.getApellidos());
                datosBrutoAlumnoMatricula.setAsignatura(alumno.getAsignatura());
                datosBrutoAlumnoMatricula.setCursoEtapa(cursoEtapa);
                
                // Guardar el registro en BD
                this.iDatosBrutoAlumnoMatriculaRepository.saveAndFlush(datosBrutoAlumnoMatricula);
            }
            
            // Eliminar los alumnos asignados al grupo de la tabla DatosBrutoAlumnoMatriculaGrupo
            this.iDatosBrutoAlumnoMatriculaGrupoRepository.deleteAll(alumnosAsignadosAlGrupo);
            
            // Eliminar el grupo en la tabla CursoEtapaGrupo
            this.iCursoEtapaGrupoRepository.delete(cursoEtapaGrupo);
            
            // Log de información antes de la respuesta
            log.info("INFO - Grupo eliminado correctamente para el curso: {} y etapa: {}", curso, etapa);
            
            // Devolver mensaje de OK
            return ResponseEntity.ok().build();
        } 
        catch (Exception exception) 
        {
            // Manejo de excepciones generales
            String msgError = "ERROR - No se pudo eliminar el grupo";
            log.error(msgError, exception);

            // Devolver una excepción personalizada con código 1, el mensaje de error y la excepcion general
            MatriculasHorariosServerException matriculasHorariosServerException = new MatriculasHorariosServerException(1, msgError, exception);
            return ResponseEntity.status(500).body(matriculasHorariosServerException.getBodyExceptionMessage());
        }
    }
	
    /**
     * Endpoint para asignar una lista de alumnos a un grupo específico de un curso y etapa.
     * 
     * Este método asigna los alumnos, que son recibidos como una lista de objetos `AlumnoDto`, 
     * a un grupo específico dentro de un curso y etapa determinados. Si el alumno ya existe en 
     * la tabla `DatosBrutoAlumnoMatricula`, se transfiere el alumno al grupo especificado y 
     * luego se elimina de la tabla `DatosBrutoAlumnoMatricula`.
     * 
     * @param alumnos 			 - Lista de objetos `AlumnoDto` que contiene los datos de los alumnos a asignar.
     * @param curso   			 - El identificador del curso al que pertenecen los alumnos.
     * @param etapa   			 - La etapa educativa asociada al curso.
     * @param grupo   			 - El identificador del grupo (una letra que representa el grupo) al que se asignarán los alumnos.
     * @return ResponseEntity<?> - Respuesta con un mensaje de éxito si los alumnos se asignaron correctamente, o una excepción con el mensaje de error si ocurre un fallo durante el proceso.
     */
    @RequestMapping(method = RequestMethod.POST, value = "/grupos/alumnos")
    public ResponseEntity<?> asignarAlumnos
    (
            @RequestBody List<AlumnoDto> alumnos,
            @RequestParam(value = "curso", required = true) Integer curso,
            @RequestParam(value = "etapa", required = true) String etapa,
            @RequestParam(value = "grupo", required = true) Character grupo
    ) 
    {
        try 
        {
            // Crear el objeto CursoEtapaGrupo con los parámetros recibidos
            CursoEtapaGrupo cursoEtapaGrupo = new CursoEtapaGrupo();
            IdCursoEtapaGrupo idCursoEtapaGrupo = new IdCursoEtapaGrupo(curso, etapa, grupo);
            cursoEtapaGrupo.setIdCursoEtapaGrupo(idCursoEtapaGrupo);

           
        

            // Por cada alumno buscarlo en DatosBrutosAlumnoMatricula y añadirlos a DatosBrutosAlumnoMatriculaGrupo
            for (AlumnoDto alumno : alumnos)
            {
            	
                // Optional de DatosBrutoAlumnoMatriculaEntity
                List<DatosBrutoAlumnoMatricula> datosBrutoAlumnoMatriculaAsignaturasOpt;

                // Buscar los registros del alumno en DatosBrutosAlumnoMatricula
                datosBrutoAlumnoMatriculaAsignaturasOpt = this.iDatosBrutoAlumnoMatriculaRepository.findByNombreAndApellidos(alumno.getNombre(), alumno.getApellidos());

                for (DatosBrutoAlumnoMatricula datosBrutoAlumnoMatriculaAsignaturaOpt : datosBrutoAlumnoMatriculaAsignaturasOpt)
                {
                	 // Crear registro de la Tabla DatosBrutosAlumnoMatriculaGrupo
                    DatosBrutoAlumnoMatriculaGrupo datosBrutoAlumnoMatriculaGrupo = new DatosBrutoAlumnoMatriculaGrupo();
                    // Asignar cada uno de los campos
                    datosBrutoAlumnoMatriculaGrupo.setNombre(datosBrutoAlumnoMatriculaAsignaturaOpt.getNombre());
                    datosBrutoAlumnoMatriculaGrupo.setApellidos(datosBrutoAlumnoMatriculaAsignaturaOpt.getApellidos());
                    datosBrutoAlumnoMatriculaGrupo.setAsignatura(datosBrutoAlumnoMatriculaAsignaturaOpt.getAsignatura());
                    datosBrutoAlumnoMatriculaGrupo.setCursoEtapaGrupo(cursoEtapaGrupo);

                    // Guardar el registro en la tabla DatosBrutoAlumnoMatriculaGrupo
                    this.iDatosBrutoAlumnoMatriculaGrupoRepository.saveAndFlush(datosBrutoAlumnoMatriculaGrupo);

                    // Eliminar el registro en la tabla DatosBrutoAlumnoMatricula
                    this.iDatosBrutoAlumnoMatriculaRepository.delete(datosBrutoAlumnoMatriculaAsignaturaOpt);
                }
            }

            // Log de información antes de la respuesta
            log.info("INFO - Alumnos asignados correctamente al grupo {} para el curso {} y etapa {}", grupo, curso, etapa);

            // Devolver mensaje de OK
            return ResponseEntity.ok().build();
        } 
        catch (Exception exception) 
        {
            // Manejo de excepciones generales
            String msgError = "ERROR - No se pudieron asignar los alumnos al grupo";
            log.error(msgError, exception);

            // Devolver una excepción personalizada con código 1, el mensaje de error y la excepcion general
            MatriculasHorariosServerException matriculasHorariosServerException = new MatriculasHorariosServerException(1, msgError, exception);
            return ResponseEntity.status(500).body(matriculasHorariosServerException.getBodyExceptionMessage());
        }
    }
	
    /**
     * Endpoint para desasignar un alumno de un grupo específico.
     * 
     * Este método busca al alumno en la tabla `DatosBrutoAlumnoMatriculaGrupo` utilizando 
     * su nombre y apellidos. Si se encuentra al alumno, se transfiere el registro a la 
     * tabla `DatosBrutoAlumnoMatricula` y luego se elimina el registro en la tabla 
     * `DatosBrutoAlumnoMatriculaGrupo`, desasignando así al alumno del grupo.
     * 
     * @param alumno 			 - El objeto `AlumnoDto` que contiene los datos del alumno a desasignar.
     * @return ResponseEntity<?> - Respuesta con un mensaje de éxito si el alumno fue desasignado correctamente, o una excepción con el mensaje de error si ocurre un fallo durante el proceso.
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/grupos/alumnos")
    public ResponseEntity<?> desasignarAlumno
    (
            @RequestBody AlumnoDto alumno
    ) 
    {
        try 
        {
            // Optional DatosBrutoAlumnoMatriculaGrupo
            List<Optional<DatosBrutoAlumnoMatriculaGrupo>> datosBrutoAlumnoMatriculaGrupoAsignaturasOpt = this.iDatosBrutoAlumnoMatriculaGrupoRepository.findAllByNombreAndApellidos(alumno.getNombre(), alumno.getApellidos());

            // Por cada asignatura del Alumno
            for (Optional<DatosBrutoAlumnoMatriculaGrupo> datosBrutoAlumnoMatriculaGrupoAsignaturaOpt : datosBrutoAlumnoMatriculaGrupoAsignaturasOpt)
            {
                // Si no existe el registro
                if (!datosBrutoAlumnoMatriculaGrupoAsignaturaOpt.isPresent())
                {
                    // Lanzar excepcion y mostrar log con mensaje de Error
                    String msgError = "ERROR - No se encontraron los datos del alumno";
                    log.error(msgError);
                    throw new MatriculasHorariosServerException(1, msgError);
                }

                // Crear registro DatosBrutoAlumnoMatricula
                DatosBrutoAlumnoMatricula datosBrutoAlumnoMatricula = new DatosBrutoAlumnoMatricula();

                // Crear CursoEtapa del Alumno
                CursoEtapa cursoEtapa = new CursoEtapa();
                IdCursoEtapa idCursoEtapa = new IdCursoEtapa();
                idCursoEtapa.setCurso(datosBrutoAlumnoMatriculaGrupoAsignaturaOpt.get().getCursoEtapaGrupo().getIdCursoEtapaGrupo().getCurso());
                idCursoEtapa.setEtapa(datosBrutoAlumnoMatriculaGrupoAsignaturaOpt.get().getCursoEtapaGrupo().getIdCursoEtapaGrupo().getEtapa());
                cursoEtapa.setIdCursoEtapa(idCursoEtapa);

                // Asignar alumno a registro de DatosBrutoAlumnoMatriculaEntity
                datosBrutoAlumnoMatricula.setNombre(datosBrutoAlumnoMatriculaGrupoAsignaturaOpt.get().getNombre());
                datosBrutoAlumnoMatricula.setApellidos(datosBrutoAlumnoMatriculaGrupoAsignaturaOpt.get().getApellidos());
                datosBrutoAlumnoMatricula.setAsignatura(datosBrutoAlumnoMatriculaGrupoAsignaturaOpt.get().getAsignatura());
                datosBrutoAlumnoMatricula.setCursoEtapa(cursoEtapa);

                // Guardar el registro en la tabla DatosBrutoAlumnoMatricula
                this.iDatosBrutoAlumnoMatriculaRepository.saveAndFlush(datosBrutoAlumnoMatricula);

                // Eliminar el registro en la tabla DatosBrutoAlumnoMatriculaGrupo
                this.iDatosBrutoAlumnoMatriculaGrupoRepository.delete(datosBrutoAlumnoMatriculaGrupoAsignaturaOpt.get());
            }

            // Log de información antes de la respuesta
            log.info("INFO - Alumno desasignado correctamente");

            // Devolver mensaje de OK
            return ResponseEntity.ok().build();
        } 			
        catch (MatriculasHorariosServerException matriculasHorariosServerException) 
        {
            // Manejo de excepciones personalizadas
            log.error(matriculasHorariosServerException.getBodyExceptionMessage().toString());

            // Devolver la excepción personalizada con código 1 y el mensaje de error
            return ResponseEntity.status(404).body(matriculasHorariosServerException.getBodyExceptionMessage());
        } 
        catch (Exception exception) 
        {
            // Manejo de excepciones generales
            String msgError = "ERROR - No se pudo desasignar el alumno del grupo";
            log.error(msgError, exception);

            // Devolver una excepción personalizada con código 1, el mensaje de error y la excepcion general
            MatriculasHorariosServerException matriculasHorariosServerException = new MatriculasHorariosServerException(1, msgError, exception);
            return ResponseEntity.status(500).body(matriculasHorariosServerException.getBodyExceptionMessage());
        }
    }
	
    /**
     * Endpoint para obtener la lista de alumnos pendientes por asignar o ya asignados a un grupo específico de un curso y etapa.
     * 
     * Este método recibe los parámetros del curso, la etapa y el grupo, y luego recupera 
     * una lista de alumnos pendientes y también asignados a ese grupo especifico, si hay algún 
     * error en el proceso, se captura la excepción y se devuelve un mensaje de error adecuado.
     * 
     * @param curso 			 - El curso para el que se solicita la lista de alumnos.
     * @param etapa 			 - La etapa para la cual se solicita la lista de alumnos.
     * @param grupo 			 - El grupo específico dentro del curso y etapa que se está consultando.
     * @return ResponseEntity<?> - Respuesta con la lista de alumnos del grupo o pendientes de asignar, o una excepción personalizada si ocurre algún error durante la operación.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/grupos/alumnos")
    public ResponseEntity<?> obtenerAlumnos
    (
            @RequestParam(value="curso", required = true) Integer curso, 
            @RequestParam(value="etapa", required = true) String etapa, 
            @RequestParam(value="grupo", required = true) Character grupo
    ) 
    {
        try
        {
        	// Crear el objeto CursoEtapaGrupo con los parámetros recibidos
            CursoEtapaGrupo cursoEtapaGrupo = new CursoEtapaGrupo();
            IdCursoEtapaGrupo idCursoEtapaGrupo = new IdCursoEtapaGrupo(curso, etapa, grupo);
            cursoEtapaGrupo.setIdCursoEtapaGrupo(idCursoEtapaGrupo);
           
            // Crear la lista de Alumnos a devolver
            List<AlumnoDto> alumnosPendientesDeAsignarYAsignados = List.of();
            
            // Añadir a la lista de Alumnos a devolver los Alumnos pendientes de asignar por el CursoEtapa
            alumnosPendientesDeAsignarYAsignados = this.iDatosBrutoAlumnoMatriculaRepository.findDistinctAlumnosByCursoEtapa(curso, etapa);
            
            // Añadir a la lista de Alumnos a devolver los Alumnos asignados al CursoEtapaGrupo
            alumnosPendientesDeAsignarYAsignados.addAll(this.iDatosBrutoAlumnoMatriculaGrupoRepository.findDistinctAlumnosByCursoEtapaGrupo(curso, etapa, grupo));
            
            // Si la lista esta vacía
            if(alumnosPendientesDeAsignarYAsignados.isEmpty())
            {
            	// Lanzar excepcion y mostrar log con mensaje de Error
                String msgError = "ERROR - Lista sin alumnos encontrados";
                log.error(msgError);
                throw new MatriculasHorariosServerException(1, msgError);
            }
            
            // Log de información antes de la respuesta
            log.info("INFO - Lista con nombres y apellidos de los alumnos asignados y pendientes de asignar");
            
            // Devolver la lista de Alumnos
            return ResponseEntity.status(200).body(alumnosPendientesDeAsignarYAsignados);
        }
        catch (MatriculasHorariosServerException matriculasHorariosServerException) 
        {
            // Manejo de excepciones personalizadas
            log.error(matriculasHorariosServerException.getBodyExceptionMessage().toString());

            // Devolver la excepción personalizada con código 1 y el mensaje de error
            return ResponseEntity.status(404).body(matriculasHorariosServerException.getBodyExceptionMessage());
        }
        catch(Exception exception)
        {
            // Manejo de excepciones generales
            String msgError = "ERROR - No se pudo obtener la lista de alumnos";
            log.error(msgError, exception);

            // Devolver una excepción personalizada con código 1, el mensaje de error y la excepción general
            MatriculasHorariosServerException matriculasHorariosServerException = new MatriculasHorariosServerException(1, msgError, exception);
            return ResponseEntity.status(500).body(matriculasHorariosServerException.getBodyExceptionMessage());
        }
    }
    
    @RequestMapping(method = RequestMethod.GET, value = "/etapaCursos")
    public ResponseEntity<?> obtenerCursosEtapas()
    {
    	try 
    	{
    		List<CursoEtapa> cursos = this.iCursoEtapaRepository.findAll() ;
        	
        	log.info("INFO - Lista de los cursos etapas") ;
        	return ResponseEntity.status(200).body(cursos) ;
		} 
    	catch (Exception exception) 
    	{
			String msgError = "ERROR - No se pudo obtener la lista de cursos etapas" ;
			log.error(msgError, exception) ;
			MatriculasHorariosServerException matriculasHorariosServerException = new MatriculasHorariosServerException(1, msgError, exception) ;
			return ResponseEntity.status(500).body(matriculasHorariosServerException.getBodyExceptionMessage()) ;
		}
    	
    }
    
    @RequestMapping(method = RequestMethod.GET, value = "/asignaturas")
    public ResponseEntity<?> obtenerAsignatura
    (
    		@RequestParam("curso") int curso,
    		@RequestParam("etapa") String etapa
    )
    {
    	
    	try 
    	{
			List<Asignatura> asignaturas = iAsignaturaRepository.findByCursoAndEtapa(curso, etapa) ;
			
			// Mapear a DTO y calcular el número de alumnos matriculados
			List<AsignaturaDto> asignaturasDto = asignaturas.stream().map(asignatura -> 
			{
				AsignaturaDto dto = new AsignaturaDto() ;
				dto.setNombre(asignatura.getId().getNombre()) ;
				dto.setGrupo(asignatura.getId().getGrupo()) ;
				dto.setEtapa(asignatura.getId().getEtapa()) ;
				dto.setCurso(asignatura.getId().getCurso()) ;
				dto.setNumeroDeAlumnos(asignatura.getMatriculas().size()) ;
				return dto ;
			}).collect(Collectors.toList()) ;
			
			return ResponseEntity.status(200).body(asignaturasDto) ;
		} 
    	catch (Exception exception) 
    	{
			String msgError = "ERROR - No se pudo obtener la lista de asignaturas" ;
			log.error(msgError, exception) ;
			MatriculasHorariosServerException matriculasHorariosServerException = new MatriculasHorariosServerException(1, msgError, exception) ;
			return ResponseEntity.status(500).body(matriculasHorariosServerException.getBodyExceptionMessage()) ;
		}
    	
    }
    
    @RequestMapping(method = RequestMethod.POST, value = "/bloques")
    public ResponseEntity<?> crearBloques
    (
    		@RequestParam("asignaturas") int curso,
    		@RequestParam("asignaturas") String etapa,
    		@RequestParam("asignaturas") List<Asignatura> asignaturas
    )
    {
    	try
    	{	
    		if (asignaturas.size() < 2)
    		{
                String msgError = "ERROR - Hay que seleccionar al menos 2 asignaturas";
                log.error(msgError);
                throw new MatriculasHorariosServerException(100, msgError);
    		}
    		
    		Bloque bloque = new Bloque();
    		
    		//Se crea un Atomic Integer para poder usar el método "getAndIncrement"
    		AtomicInteger integer = new AtomicInteger(1);
    		
    		int nuevoId = integer.getAndIncrement();
    		
    		//Se crea un id autogenerado artificialmente y se concatena para que cuente como un String
    		bloque.setId("BLOQUE_" + nuevoId);
    	}
    	catch (Exception exception)
    	{
			String msgError = "ERROR - No se pudo obtener la lista de asignaturas" ;
			log.error(msgError, exception) ;
			MatriculasHorariosServerException matriculasHorariosServerException = new MatriculasHorariosServerException(1, msgError, exception) ;
			return ResponseEntity.status(500).body(matriculasHorariosServerException.getBodyExceptionMessage()) ;
    	}
    	
    	/**Hacer un dto que contenga los datos que me interesan
    	 * - Lista de los nombres de las asignaturas
    	 * - Horas
    	 * - Alumnos
    	 * - Grupo
    	 * - Bloque
    	*/
    	
		return null;
    }

}
