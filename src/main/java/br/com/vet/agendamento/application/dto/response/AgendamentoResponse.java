package br.com.vet.agendamento.application.dto.response;

import br.com.vet.agendamento.domain.model.Agendamento;

import java.time.LocalDateTime;

public record AgendamentoResponse(
        Long id,
        Long tutorId,
        Long animalId,
        Long veterinarioId,
        String tipo,
        String servicoNome,
        LocalDateTime inicioEm,
        LocalDateTime fimEm,
        Integer duracaoMinutos,
        String status,
        String observacoes,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static AgendamentoResponse from(Agendamento agendamento) {
        return new AgendamentoResponse(
                agendamento.getId(),
                agendamento.getTutorId(),
                agendamento.getAnimalId(),
                agendamento.getVeterinarioId(),
                agendamento.getTipo().name(),
                agendamento.getServicoNome(),
                agendamento.getInicioEm(),
                agendamento.getFimEm(),
                agendamento.getDuracaoMinutos(),
                agendamento.getStatus().name(),
                agendamento.getObservacoes(),
                agendamento.getCriadoEm(),
                agendamento.getAtualizadoEm()
        );
    }
}
