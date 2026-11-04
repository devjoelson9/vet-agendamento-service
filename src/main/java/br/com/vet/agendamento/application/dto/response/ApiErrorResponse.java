package br.com.vet.agendamento.application.dto.response;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        Integer status,
        String error,
        String message,
        LocalDateTime timestamp,
        String path
) {
}
