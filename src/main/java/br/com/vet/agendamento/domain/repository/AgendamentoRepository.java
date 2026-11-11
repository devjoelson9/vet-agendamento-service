package br.com.vet.agendamento.domain.repository;

import br.com.vet.agendamento.domain.enums.StatusAgendamento;
import br.com.vet.agendamento.domain.model.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    Optional<Agendamento> findByIdAndDeletadoEmIsNull(Long id);

    @Query("""
            select a from Agendamento a
            where a.deletadoEm is null
              and a.data between :inicio and :fim
              and (:veterinarioId is null or a.veterinarioId = :veterinarioId)
              and (:animalId is null or a.animalId = :animalId)
              and (:status is null or a.status = :status)
            order by a.data, a.horaInicio
            """)
    List<Agendamento> buscarNoPeriodo(
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim,
            @Param("veterinarioId") Long veterinarioId,
            @Param("animalId") Long animalId,
            @Param("status") StatusAgendamento status
    );

    @Query("""
            select count(a) > 0 from Agendamento a
            where a.deletadoEm is null
              and a.veterinarioId = :veterinarioId
              and a.data = :data
              and a.status <> :statusCancelado
              and a.horaInicio < :horaFim
              and a.horaFim > :horaInicio
              and (:idIgnorado is null or a.id <> :idIgnorado)
            """)
    boolean existeConflito(
            @Param("veterinarioId") Long veterinarioId,
            @Param("data") LocalDate data,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFim") LocalTime horaFim,
            @Param("statusCancelado") StatusAgendamento statusCancelado,
            @Param("idIgnorado") Long idIgnorado
    );

    List<Agendamento> findByVeterinarioIdAndDataAndDeletadoEmIsNullAndStatusNot(
            Long veterinarioId,
            LocalDate data,
            StatusAgendamento status
    );
}
