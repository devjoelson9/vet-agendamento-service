package br.com.vet.agendamento.application.service;

import br.com.vet.agendamento.application.dto.request.AgendamentoRequest;
import br.com.vet.agendamento.application.dto.response.AgendamentoResponse;
import br.com.vet.agendamento.application.exception.BusinessException;
import br.com.vet.agendamento.application.exception.ConflictException;
import br.com.vet.agendamento.domain.enums.StatusAgendamento;
import br.com.vet.agendamento.domain.model.Agendamento;
import br.com.vet.agendamento.domain.model.Disponibilidade;
import br.com.vet.agendamento.domain.repository.AgendamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    private static final LocalDate DATA = LocalDate.of(2026, 5, 24);

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private DisponibilidadeService disponibilidadeService;

    private AgendamentoService service;

    @BeforeEach
    void setUp() {
        service = new AgendamentoService(agendamentoRepository, disponibilidadeService);
    }

    @Test
    void criaAgendamentoAgendadoQuandoHorarioDisponivel() {
        when(disponibilidadeService.resolverDisponibilidade(4L, DATA, LocalTime.of(9, 0)))
                .thenReturn(disponibilidade());
        when(agendamentoRepository.existeConflito(
                eq(4L), eq(DATA), eq(LocalTime.of(9, 0)), eq(LocalTime.of(9, 30)),
                eq(StatusAgendamento.CANCELADO), eq(null)))
                .thenReturn(false);
        when(agendamentoRepository.save(any(Agendamento.class)))
                .thenAnswer(invocation -> {
                    Agendamento agendamento = invocation.getArgument(0);
                    agendamento.setId(8L);
                    return agendamento;
                });

        AgendamentoResponse response = service.criar(request(LocalTime.of(9, 0)));

        assertThat(response.id()).isEqualTo(8L);
        assertThat(response.status()).isEqualTo("AGENDADO");
        assertThat(response.horaFim()).isEqualTo(LocalTime.of(9, 30));
    }

    @Test
    void rejeitaAgendamentoQuandoVeterinarioJaEstaOcupado() {
        when(disponibilidadeService.resolverDisponibilidade(4L, DATA, LocalTime.of(9, 0)))
                .thenReturn(disponibilidade());
        when(agendamentoRepository.existeConflito(
                eq(4L), eq(DATA), eq(LocalTime.of(9, 0)), eq(LocalTime.of(9, 30)),
                eq(StatusAgendamento.CANCELADO), eq(null)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.criar(request(LocalTime.of(9, 0))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("já possui um agendamento");
    }

    @Test
    void naoPermiteReagendarAgendamentoJaEmAtendimento() {
        Agendamento agendamento = agendamentoExistente(StatusAgendamento.AGUARDANDO_ATENDIMENTO);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(8L))
                .thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> service.atualizar(8L, request(LocalTime.of(10, 0))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Somente agendamentos com status AGENDADO");
    }

    @Test
    void rejeitaTransicaoDeStatusForaDaSequencia() {
        Agendamento agendamento = agendamentoExistente(StatusAgendamento.AGENDADO);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(8L))
                .thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> service.alterarStatus(8L, StatusAgendamento.CONCLUIDO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição de status inválida");
    }

    @Test
    void checkInRegistraHorarioEAvancaStatus() {
        Agendamento agendamento = agendamentoExistente(StatusAgendamento.AGENDADO);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(8L))
                .thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(any(Agendamento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AgendamentoResponse response = service.alterarStatus(8L, StatusAgendamento.AGUARDANDO_ATENDIMENTO);

        assertThat(response.status()).isEqualTo("AGUARDANDO_ATENDIMENTO");
        assertThat(response.horarioCheckin()).isNotNull();
    }

    private AgendamentoRequest request(LocalTime horaInicio) {
        return new AgendamentoRequest(15L, 28L, 4L, DATA, horaInicio);
    }

    private Disponibilidade disponibilidade() {
        Disponibilidade disponibilidade = new Disponibilidade();
        disponibilidade.setId(1L);
        disponibilidade.setVeterinarioId(4L);
        disponibilidade.setHoraInicio(LocalTime.of(8, 0));
        disponibilidade.setHoraFim(LocalTime.of(12, 0));
        disponibilidade.setDuracaoConsulta(30);
        return disponibilidade;
    }

    private Agendamento agendamentoExistente(StatusAgendamento status) {
        Agendamento agendamento = new Agendamento();
        agendamento.setId(8L);
        agendamento.setTutorId(15L);
        agendamento.setAnimalId(28L);
        agendamento.setVeterinarioId(4L);
        agendamento.setData(DATA);
        agendamento.setHoraInicio(LocalTime.of(9, 0));
        agendamento.setHoraFim(LocalTime.of(9, 30));
        agendamento.setStatus(status);
        return agendamento;
    }
}
