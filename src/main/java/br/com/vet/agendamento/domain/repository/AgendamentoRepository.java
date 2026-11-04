package br.com.vet.agendamento.domain.repository;

import br.com.vet.agendamento.domain.model.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    Optional<Agendamento> findByIdAndDeletadoEmIsNull(Long id);

    @Query(value = """
            select * from agendamento
            where deletado_em is null
              and inicio_em < :fim
              and inicio_em + (duracao_minutos * interval '1 minute') > :inicio
              and (cast(:veterinarioId as bigint) is null or veterinario_id = :veterinarioId)
              and (cast(:animalId as bigint) is null or animal_id = :animalId)
              and (cast(:status as varchar) is null or status = :status)
            order by inicio_em
            """, nativeQuery = true)
    List<Agendamento> buscarNoPeriodo(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("veterinarioId") Long veterinarioId,
            @Param("animalId") Long animalId,
            @Param("status") String status
    );

    @Query(value = """
            select count(*) > 0 from agendamento
            where deletado_em is null
              and (cast(:veterinarioId as bigint) is null or veterinario_id = :veterinarioId)
              and status not in (:statusIgnorados)
              and inicio_em < :fim
              and inicio_em + (duracao_minutos * interval '1 minute') > :inicio
              and (cast(:idIgnorado as bigint) is null or id <> :idIgnorado)
            """, nativeQuery = true)
    boolean existeConflito(
            @Param("veterinarioId") Long veterinarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("statusIgnorados") List<String> statusIgnorados,
            @Param("idIgnorado") Long idIgnorado
    );
}
