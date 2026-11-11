package br.com.vet.agendamento.application.dto.response;

import br.com.vet.agendamento.domain.model.Disponibilidade;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record DisponibilidadeResponse(
        Long id,
        Long veterinarioId,
        String diaSemana,
        LocalTime horaInicio,
        LocalTime horaFim,
        Integer duracaoConsulta,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static DisponibilidadeResponse from(Disponibilidade disponibilidade) {
        return new DisponibilidadeResponse(
                disponibilidade.getId(),
                disponibilidade.getVeterinarioId(),
                disponibilidade.getDiaSemana().name(),
                disponibilidade.getHoraInicio(),
                disponibilidade.getHoraFim(),
                disponibilidade.getDuracaoConsulta(),
                disponibilidade.getCriadoEm(),
                disponibilidade.getAtualizadoEm()
        );
    }
}
