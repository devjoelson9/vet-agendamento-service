package br.com.vet.agendamento.application.dto.request;

import br.com.vet.agendamento.domain.enums.StatusAgendamento;
import jakarta.validation.constraints.NotNull;

public record StatusAgendamentoRequest(@NotNull StatusAgendamento status) {
}
