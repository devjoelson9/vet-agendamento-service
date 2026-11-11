package br.com.vet.agendamento.domain.repository;

import br.com.vet.agendamento.domain.enums.DiaSemana;
import br.com.vet.agendamento.domain.model.Disponibilidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface DisponibilidadeRepository extends JpaRepository<Disponibilidade, Long> {

    Optional<Disponibilidade> findByIdAndDeletadoEmIsNull(Long id);

    List<Disponibilidade> findByDeletadoEmIsNull();

    List<Disponibilidade> findByVeterinarioIdAndDeletadoEmIsNull(Long veterinarioId);

    List<Disponibilidade> findByVeterinarioIdAndDiaSemanaAndDeletadoEmIsNull(
            Long veterinarioId, DiaSemana diaSemana);

    @Query("""
            select count(d) > 0 from Disponibilidade d
            where d.veterinarioId = :veterinarioId
              and d.diaSemana = :diaSemana
              and d.deletadoEm is null
              and d.horaInicio < :horaFim
              and d.horaFim > :horaInicio
              and (:idIgnorado is null or d.id <> :idIgnorado)
            """)
    boolean existeSobreposicao(
            @Param("veterinarioId") Long veterinarioId,
            @Param("diaSemana") DiaSemana diaSemana,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFim") LocalTime horaFim,
            @Param("idIgnorado") Long idIgnorado
    );
}
