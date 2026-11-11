package br.com.vet.agendamento.web.controller;

import br.com.vet.agendamento.application.dto.request.AgendamentoRequest;
import br.com.vet.agendamento.application.dto.request.StatusAgendamentoRequest;
import br.com.vet.agendamento.application.dto.response.AgendamentoResponse;
import br.com.vet.agendamento.application.dto.response.HorarioDisponivelResponse;
import br.com.vet.agendamento.application.service.AgendamentoService;
import br.com.vet.agendamento.application.service.DisponibilidadeService;
import br.com.vet.agendamento.domain.enums.StatusAgendamento;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/agendamentos")
@RequiredArgsConstructor
public class AgendamentoController {

    private final AgendamentoService service;
    private final DisponibilidadeService disponibilidadeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TUTOR', 'RECEPCIONISTA')")
    public ResponseEntity<AgendamentoResponse> criar(@Valid @RequestBody AgendamentoRequest request) {
        return ResponseEntity.status(201).body(service.criar(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TUTOR', 'RECEPCIONISTA', 'VETERINARIO', 'ADMIN')")
    public ResponseEntity<List<AgendamentoResponse>> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Long tutorId,
            @RequestParam(required = false) Long veterinarioId,
            @RequestParam(required = false) Long animalId,
            @RequestParam(required = false) StatusAgendamento status
    ) {
        return ResponseEntity.ok(service.listar(inicio, fim, tutorId, veterinarioId, animalId, status));
    }

    @GetMapping("/horarios-disponiveis")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'TUTOR', 'VETERINARIO')")
    public ResponseEntity<List<HorarioDisponivelResponse>> horariosDisponiveis(
            @RequestParam Long veterinarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data
    ) {
        return ResponseEntity.ok(disponibilidadeService.horariosDisponiveis(veterinarioId, data));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TUTOR', 'RECEPCIONISTA', 'VETERINARIO', 'ADMIN')")
    public ResponseEntity<AgendamentoResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscar(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TUTOR', 'RECEPCIONISTA')")
    public ResponseEntity<AgendamentoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AgendamentoRequest request
    ) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'VETERINARIO', 'ADMIN')")
    public ResponseEntity<AgendamentoResponse> alterarStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusAgendamentoRequest request
    ) {
        return ResponseEntity.ok(service.alterarStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TUTOR', 'RECEPCIONISTA')")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        service.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}
