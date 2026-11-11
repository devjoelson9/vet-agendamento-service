package br.com.vet.agendamento.application.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnimalInfo(
        Long id,
        Long tutorId,
        String nome
) {
}
