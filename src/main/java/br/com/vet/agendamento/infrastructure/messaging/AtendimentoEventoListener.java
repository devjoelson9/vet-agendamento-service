package br.com.vet.agendamento.infrastructure.messaging;

import br.com.vet.agendamento.domain.enums.StatusAgendamento;
import br.com.vet.agendamento.domain.model.Agendamento;
import br.com.vet.agendamento.domain.repository.AgendamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class AtendimentoEventoListener {

    private final AgendamentoRepository agendamentoRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.ATENDIMENTO_QUEUE)
    public void receber(String payload) {
        try {
            AtendimentoEvento evento = objectMapper.readValue(payload, AtendimentoEvento.class);

            if (evento.agendamentoId() == null) {
                log.warn("Evento de atendimento sem agendamentoId: {}", payload);
                return;
            }

            agendamentoRepository.findByIdAndDeletadoEmIsNull(evento.agendamentoId())
                    .ifPresent(agendamento -> aplicarEvento(agendamento, evento));
        } catch (Exception exception) {
            log.error("Falha ao processar evento de atendimento: {}", payload, exception);
        }
    }

    private void aplicarEvento(Agendamento agendamento, AtendimentoEvento evento) {
        StatusAgendamento novoStatus = switch (String.valueOf(evento.status())) {
            // O início do atendimento não altera o agendamento: ele permanece
            // AGUARDANDO_ATENDIMENTO até a finalização clínica (RN09).
            case "EM_ANDAMENTO" -> null;
            case "FINALIZADO" -> StatusAgendamento.ATENDIMENTO_REALIZADO;
            case "CANCELADO" -> StatusAgendamento.AGUARDANDO_ATENDIMENTO;
            default -> null;
        };

        if (novoStatus == null || agendamento.getStatus() == novoStatus) {
            return;
        }

        // Não sobrescreve estados finais nem reagenda um agendamento já concluído.
        if (agendamento.getStatus() == StatusAgendamento.CONCLUIDO
                || agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            return;
        }

        agendamento.setStatus(novoStatus);
        agendamentoRepository.save(agendamento);
        log.info("Agendamento {} atualizado para {} pelo evento do atendimento {}",
                agendamento.getId(), novoStatus, evento.atendimentoId());
    }
}
