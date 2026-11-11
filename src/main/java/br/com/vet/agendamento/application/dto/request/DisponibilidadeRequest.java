package br.com.vet.agendamento.application.dto.request;

import br.com.vet.agendamento.domain.enums.DiaSemana;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalTime;

public record DisponibilidadeRequest(
        @NotNull @Positive Long veterinarioId,
        @NotNull DiaSemana diaSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFim,
        @NotNull @Positive Integer duracaoConsulta
) {
}
