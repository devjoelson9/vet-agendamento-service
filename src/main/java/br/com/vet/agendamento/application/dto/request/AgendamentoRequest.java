package br.com.vet.agendamento.application.dto.request;

import br.com.vet.agendamento.domain.enums.TipoAgendamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AgendamentoRequest(
        @NotNull @Positive Long tutorId,
        @NotNull @Positive Long animalId,
        @NotNull @Positive Long veterinarioId,
        @NotNull TipoAgendamento tipo,
        @NotBlank @Size(max = 120) String servicoNome,
        @NotNull LocalDateTime inicioEm,
        @NotNull @Positive Integer duracaoMinutos,
        @Size(max = 5000) String observacoes
) {
}
