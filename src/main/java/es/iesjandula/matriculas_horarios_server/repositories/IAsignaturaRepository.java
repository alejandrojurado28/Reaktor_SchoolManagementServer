package es.iesjandula.matriculas_horarios_server.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.iesjandula.matriculas_horarios_server.models.Asignatura;
import es.iesjandula.matriculas_horarios_server.models.ids.IdAsignatura;

/**
 * Interfaz que define los métodos para acceder y manipular los datos de la entidad {@link Asignatura}.
 * -----------------------------------------------------------------------------------------------------------------
 * Esta interfaz extiende {@link JpaRepository}, lo que permite realizar operaciones CRUD sobre la tabla que representa
 * la entidad {@link Asignatura}.
 * -----------------------------------------------------------------------------------------------------------------
 * El uso de esta interfaz facilita la persistencia y la manipulación de objetos {@link Asignatura} en la base de datos,
 * sin necesidad de implementar manualmente los métodos.
 * -----------------------------------------------------------------------------------------------------------------
 * Esta interfaz utiliza {@link IdAsignatura} como tipo para la clave primaria de la entidad {@link Asignatura}.
 * -----------------------------------------------------------------------------------------------------------------
 */
@Repository
public interface IAsignaturaRepository extends JpaRepository<Asignatura, IdAsignatura>
{
	
}
