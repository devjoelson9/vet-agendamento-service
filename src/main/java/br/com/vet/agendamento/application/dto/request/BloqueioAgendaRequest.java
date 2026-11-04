package br.com.vet.agendamento.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record BloqueioAgendaRequest(
        @Positive Long veterinarioId,
        @NotBlank @Size(max = 160) String motivo,
        @NotNull LocalDateTime inicioEm,
        @NotNull LocalDateTime fimEm
) {
}
