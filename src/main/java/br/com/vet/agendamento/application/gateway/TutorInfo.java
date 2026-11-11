package br.com.vet.agendamento.application.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TutorInfo(
        Long id,
        Long usuarioId,
        String nomeCompleto,
        String status
) {
    public boolean ativo() {
        return "ATIVO".equalsIgnoreCase(status);
    }
}
