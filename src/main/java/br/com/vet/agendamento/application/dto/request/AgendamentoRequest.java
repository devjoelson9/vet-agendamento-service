package br.com.vet.agendamento.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

public record AgendamentoRequest(
        @NotNull @Positive Long tutorId,
        @NotNull @Positive Long animalId,
        @NotNull @Positive Long veterinarioId,
        @NotNull LocalDate data,
        @NotNull LocalTime horaInicio
) {
}
