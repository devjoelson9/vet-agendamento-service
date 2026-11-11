package br.com.vet.agendamento.application.dto.response;

import br.com.vet.agendamento.domain.model.Agendamento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AgendamentoResponse(
        Long id,
        Long tutorId,
        Long animalId,
        Long veterinarioId,
        LocalDate data,
        LocalTime horaInicio,
        LocalTime horaFim,
        LocalDateTime horarioCheckin,
        String status,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static AgendamentoResponse from(Agendamento agendamento) {
        return new AgendamentoResponse(
                agendamento.getId(),
                agendamento.getTutorId(),
                agendamento.getAnimalId(),
                agendamento.getVeterinarioId(),
                agendamento.getData(),
                agendamento.getHoraInicio(),
                agendamento.getHoraFim(),
                agendamento.getHorarioCheckin(),
                agendamento.getStatus().name(),
                agendamento.getCriadoEm(),
                agendamento.getAtualizadoEm()
        );
    }
}
