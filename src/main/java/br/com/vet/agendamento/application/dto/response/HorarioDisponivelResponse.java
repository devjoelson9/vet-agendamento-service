package br.com.vet.agendamento.application.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

public record HorarioDisponivelResponse(
        Long veterinarioId,
        LocalDate data,
        LocalTime horaInicio,
        LocalTime horaFim
) {
}
