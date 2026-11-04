package br.com.vet.agendamento.web.controller;

import br.com.vet.agendamento.application.dto.request.BloqueioAgendaRequest;
import br.com.vet.agendamento.application.dto.response.BloqueioAgendaResponse;
import br.com.vet.agendamento.application.service.BloqueioAgendaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bloqueios-agenda")
@RequiredArgsConstructor
public class BloqueioAgendaController {

    private final BloqueioAgendaService service;

    @PostMapping
    public ResponseEntity<BloqueioAgendaResponse> criar(@Valid @RequestBody BloqueioAgendaRequest request) {
        return ResponseEntity.status(201).body(service.criar(request));
    }

    @GetMapping
    public ResponseEntity<List<BloqueioAgendaResponse>> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(required = false) Long veterinarioId
    ) {
        return ResponseEntity.ok(service.listar(inicio, fim, veterinarioId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
