package br.com.vet.agendamento.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AtendimentoEvento(
        Long atendimentoId,
        Long agendamentoId,
        Long animalId,
        Long veterinarioId,
        String status
) {
}
