package br.com.vet.agendamento.infrastructure.messaging;

import br.com.vet.agendamento.domain.enums.StatusAgendamento;
import br.com.vet.agendamento.domain.model.Agendamento;
import br.com.vet.agendamento.domain.repository.AgendamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtendimentoEventoListenerTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;

    private AtendimentoEventoListener listener;

    @BeforeEach
    void setUp() {
        listener = new AtendimentoEventoListener(agendamentoRepository, new ObjectMapper());
    }

    @Test
    void deveManterAguardandoQuandoAtendimentoEstiverEmAndamento() {
        Agendamento agendamento = agendamento(StatusAgendamento.AGUARDANDO_ATENDIMENTO);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(50L)).thenReturn(Optional.of(agendamento));

        listener.receber(payload(50L, "EM_ANDAMENTO"));

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.AGUARDANDO_ATENDIMENTO);
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    void deveMarcarAtendimentoRealizadoQuandoEventoEstiverFinalizado() {
        Agendamento agendamento = agendamento(StatusAgendamento.AGUARDANDO_ATENDIMENTO);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(50L)).thenReturn(Optional.of(agendamento));

        listener.receber(payload(50L, "FINALIZADO"));

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.ATENDIMENTO_REALIZADO);
        verify(agendamentoRepository).save(agendamento);
    }

    @Test
    void deveVoltarParaAguardandoAtendimentoQuandoAtendimentoForCancelado() {
        Agendamento agendamento = agendamento(StatusAgendamento.ATENDIMENTO_REALIZADO);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(50L)).thenReturn(Optional.of(agendamento));

        listener.receber(payload(50L, "CANCELADO"));

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.AGUARDANDO_ATENDIMENTO);
        verify(agendamentoRepository).save(agendamento);
    }

    @Test
    void naoDeveAlterarAgendamentoJaConcluido() {
        Agendamento agendamento = agendamento(StatusAgendamento.CONCLUIDO);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(50L)).thenReturn(Optional.of(agendamento));

        listener.receber(payload(50L, "FINALIZADO"));

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CONCLUIDO);
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    void naoDeveAlterarAgendamentoJaCancelado() {
        Agendamento agendamento = agendamento(StatusAgendamento.CANCELADO);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(50L)).thenReturn(Optional.of(agendamento));

        listener.receber(payload(50L, "FINALIZADO"));

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    void naoDeveSalvarQuandoStatusJaForOEsperado() {
        Agendamento agendamento = agendamento(StatusAgendamento.AGUARDANDO_ATENDIMENTO);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(50L)).thenReturn(Optional.of(agendamento));

        listener.receber(payload(50L, "CANCELADO"));

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.AGUARDANDO_ATENDIMENTO);
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    void deveIgnorarEventoSemAgendamentoId() {
        listener.receber(payload(null, "EM_ANDAMENTO"));

        verifyNoInteractions(agendamentoRepository);
    }

    @Test
    void deveIgnorarStatusDesconhecido() {
        Agendamento agendamento = agendamento(StatusAgendamento.AGENDADO);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(50L)).thenReturn(Optional.of(agendamento));

        listener.receber(payload(50L, "STATUS_INEXISTENTE"));

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    void naoDeveFalharQuandoPayloadForInvalido() {
        assertThatCode(() -> listener.receber("{ payload inválido"))
                .doesNotThrowAnyException();

        verifyNoInteractions(agendamentoRepository);
    }

    @Test
    void deveIgnorarEventoDeAgendamentoInexistente() {
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(50L)).thenReturn(Optional.empty());

        listener.receber(payload(50L, "EM_ANDAMENTO"));

        verify(agendamentoRepository, never()).save(any());
    }

    private Agendamento agendamento(StatusAgendamento status) {
        Agendamento agendamento = new Agendamento();
        agendamento.setId(50L);
        agendamento.setTutorId(1L);
        agendamento.setAnimalId(2L);
        agendamento.setVeterinarioId(3L);
        agendamento.setStatus(status);
        return agendamento;
    }

    private String payload(Long agendamentoId, String status) {
        return """
                {
                  "atendimentoId": 99,
                  "agendamentoId": %s,
                  "animalId": 2,
                  "veterinarioId": 3,
                  "status": "%s"
                }
                """.formatted(String.valueOf(agendamentoId), status);
    }
}
