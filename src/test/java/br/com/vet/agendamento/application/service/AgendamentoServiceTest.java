package br.com.vet.agendamento.application.service;

import br.com.vet.agendamento.application.dto.request.AgendamentoRequest;
import br.com.vet.agendamento.application.dto.response.AgendamentoResponse;
import br.com.vet.agendamento.application.exception.ConflictException;
import br.com.vet.agendamento.domain.enums.TipoAgendamento;
import br.com.vet.agendamento.domain.model.Agendamento;
import br.com.vet.agendamento.domain.repository.AgendamentoRepository;
import br.com.vet.agendamento.domain.repository.BloqueioAgendaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private BloqueioAgendaRepository bloqueioRepository;

    private AgendamentoService service;

    @BeforeEach
    void setUp() {
        service = new AgendamentoService(agendamentoRepository, bloqueioRepository);
    }

    @Test
    void criaAgendamentoAguardandoConfirmacaoQuandoHorarioDisponivel() {
        AgendamentoRequest request = request();
        when(agendamentoRepository.existeConflito(
                eq(4L), any(LocalDateTime.class), any(LocalDateTime.class), eq(List.of(
                        "AGUARDANDO_CONFIRMACAO", "CONFIRMADO", "CHECK_IN", "EM_ATENDIMENTO")), eq(null)))
                .thenReturn(false);
        when(bloqueioRepository.existeConflito(eq(4L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(agendamentoRepository.save(any(Agendamento.class)))
                .thenAnswer(invocation -> {
                    Agendamento agendamento = invocation.getArgument(0);
                    agendamento.setId(8L);
                    agendamento.setCriadoEm(LocalDateTime.now());
                    return agendamento;
                });

        AgendamentoResponse response = service.criar(request);

        assertThat(response.id()).isEqualTo(8L);
        assertThat(response.status()).isEqualTo("AGUARDANDO_CONFIRMACAO");
        assertThat(response.fimEm()).isEqualTo(LocalDateTime.of(2025, 5, 24, 15, 15));
    }

    @Test
    void rejeitaAgendamentoQuandoVeterinarioJaEstaOcupado() {
        when(agendamentoRepository.existeConflito(
                eq(4L), any(LocalDateTime.class), any(LocalDateTime.class), any(), eq(null)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.criar(request()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("já possui um agendamento");
    }

    @Test
    void rejeitaDuracaoForaDoLimite() {
        AgendamentoRequest invalido = new AgendamentoRequest(
                15L, 28L, 4L, TipoAgendamento.CONSULTA, "Retorno",
                LocalDateTime.of(2025, 5, 24, 14, 45), 600, null);

        assertThatThrownBy(() -> service.criar(invalido))
                .hasMessageContaining("entre 5 e 480 minutos");
    }

    private AgendamentoRequest request() {
        return new AgendamentoRequest(
                15L,
                28L,
                4L,
                TipoAgendamento.CONSULTA,
                "Consulta de retorno",
                LocalDateTime.of(2025, 5, 24, 14, 45),
                30,
                "Pós-operatório"
        );
    }
}
