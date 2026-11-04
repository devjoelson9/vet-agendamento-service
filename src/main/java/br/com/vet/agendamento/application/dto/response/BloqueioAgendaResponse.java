package br.com.vet.agendamento.application.dto.response;

import br.com.vet.agendamento.domain.model.BloqueioAgenda;

import java.time.LocalDateTime;

public record BloqueioAgendaResponse(
        Long id,
        Long veterinarioId,
        String motivo,
        LocalDateTime inicioEm,
        LocalDateTime fimEm,
        LocalDateTime criadoEm
) {
    public static BloqueioAgendaResponse from(BloqueioAgenda bloqueio) {
        return new BloqueioAgendaResponse(
                bloqueio.getId(),
                bloqueio.getVeterinarioId(),
                bloqueio.getMotivo(),
                bloqueio.getInicioEm(),
                bloqueio.getFimEm(),
                bloqueio.getCriadoEm()
        );
    }
}
