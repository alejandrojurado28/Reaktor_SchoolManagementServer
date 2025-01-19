package es.iesjandula.matriculas_horarios_server.dtos;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AsignaturaDto 
{

	private String nombre ; // Nombre asignatura
	
	private String grupo ;
	
	private String etapa ;
	
	private int curso ;
	
	private int numeroDeAlumnos ;
	
	private Map<String, Integer> numeroAlumnosEnGrupo ;
	
}
