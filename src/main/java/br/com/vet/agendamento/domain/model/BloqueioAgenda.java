package br.com.vet.agendamento.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "bloqueio_agenda")
@Getter
@Setter
@NoArgsConstructor
public class BloqueioAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "veterinario_id")
    private Long veterinarioId;

    @Column(nullable = false, length = 160)
    private String motivo;

    @Column(name = "inicio_em", nullable = false)
    private LocalDateTime inicioEm;

    @Column(name = "fim_em", nullable = false)
    private LocalDateTime fimEm;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        criadoEm = LocalDateTime.now();
        normalizarMotivo();
    }

    @PreUpdate
    void preUpdate() {
        normalizarMotivo();
    }

    private void normalizarMotivo() {
        if (motivo != null) {
            motivo = motivo.trim();
        }
    }
}
