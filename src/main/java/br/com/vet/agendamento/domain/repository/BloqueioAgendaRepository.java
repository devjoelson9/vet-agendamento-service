package br.com.vet.agendamento.domain.repository;

import br.com.vet.agendamento.domain.model.BloqueioAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BloqueioAgendaRepository extends JpaRepository<BloqueioAgenda, Long> {

    @Query("""
            select b from BloqueioAgenda b
            where b.inicioEm < :fim
              and b.fimEm > :inicio
              and (b.veterinarioId is null or :veterinarioId is null or b.veterinarioId = :veterinarioId)
            order by b.inicioEm
            """)
    List<BloqueioAgenda> buscarNoPeriodo(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("veterinarioId") Long veterinarioId
    );

    @Query("""
            select count(b) > 0 from BloqueioAgenda b
            where b.inicioEm < :fim
              and b.fimEm > :inicio
              and (b.veterinarioId is null or b.veterinarioId = :veterinarioId)
            """)
    boolean existeConflito(
            @Param("veterinarioId") Long veterinarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );
}
