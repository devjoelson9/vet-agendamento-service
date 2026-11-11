package br.com.vet.agendamento.web.controller;

import br.com.vet.agendamento.application.dto.request.DisponibilidadeRequest;
import br.com.vet.agendamento.application.dto.response.DisponibilidadeResponse;
import br.com.vet.agendamento.application.service.DisponibilidadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/disponibilidades")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DisponibilidadeController {

    private final DisponibilidadeService service;

    @PostMapping
    public ResponseEntity<DisponibilidadeResponse> criar(@Valid @RequestBody DisponibilidadeRequest request) {
        return ResponseEntity.status(201).body(service.criar(request));
    }

    @GetMapping
    public ResponseEntity<List<DisponibilidadeResponse>> listar(
            @RequestParam(required = false) Long veterinarioId
    ) {
        return ResponseEntity.ok(service.listar(veterinarioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DisponibilidadeResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscar(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DisponibilidadeResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody DisponibilidadeRequest request
    ) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
