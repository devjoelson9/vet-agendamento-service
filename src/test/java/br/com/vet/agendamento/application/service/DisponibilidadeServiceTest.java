package br.com.vet.agendamento.application.service;

import br.com.vet.agendamento.application.dto.request.DisponibilidadeRequest;
import br.com.vet.agendamento.application.dto.response.DisponibilidadeResponse;
import br.com.vet.agendamento.application.dto.response.HorarioDisponivelResponse;
import br.com.vet.agendamento.application.exception.BusinessException;
import br.com.vet.agendamento.application.exception.ConflictException;
import br.com.vet.agendamento.domain.enums.DiaSemana;
import br.com.vet.agendamento.domain.enums.StatusAgendamento;
import br.com.vet.agendamento.domain.model.Agendamento;
import br.com.vet.agendamento.domain.model.Disponibilidade;
import br.com.vet.agendamento.domain.repository.AgendamentoRepository;
import br.com.vet.agendamento.domain.repository.DisponibilidadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisponibilidadeServiceTest {

    private static final LocalDate PROXIMA_SEGUNDA = LocalDate.now()
            .plusWeeks(1)
            .with(DayOfWeek.MONDAY);

    @Mock
    private DisponibilidadeRepository disponibilidadeRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    private DisponibilidadeService service;

    @BeforeEach
    void setUp() {
        service = new DisponibilidadeService(disponibilidadeRepository, agendamentoRepository);
    }

    @Test
    void criaDisponibilidadeValida() {
        DisponibilidadeRequest request = request(LocalTime.of(8, 0), LocalTime.of(12, 0), 30);
        when(disponibilidadeRepository.existeSobreposicao(
                eq(4L), eq(DiaSemana.SEGUNDA), eq(LocalTime.of(8, 0)), eq(LocalTime.of(12, 0)), eq(null)))
                .thenReturn(false);
        when(disponibilidadeRepository.save(any(Disponibilidade.class)))
                .thenAnswer(invocation -> {
                    Disponibilidade disponibilidade = invocation.getArgument(0);
                    disponibilidade.setId(1L);
                    return disponibilidade;
                });

        DisponibilidadeResponse response = service.criar(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.diaSemana()).isEqualTo("SEGUNDA");
        assertThat(response.duracaoConsulta()).isEqualTo(30);
    }

    @Test
    void rejeitaJanelaComFimAntesDoInicio() {
        DisponibilidadeRequest request = request(LocalTime.of(12, 0), LocalTime.of(8, 0), 30);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("horário final deve ser posterior");
    }

    @Test
    void rejeitaDuracaoMaiorQueJanela() {
        DisponibilidadeRequest request = request(LocalTime.of(8, 0), LocalTime.of(9, 0), 120);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não pode ser maior que a janela");
    }

    @Test
    void rejeitaSobreposicaoDeFaixas() {
        DisponibilidadeRequest request = request(LocalTime.of(10, 0), LocalTime.of(14, 0), 30);
        when(disponibilidadeRepository.existeSobreposicao(
                eq(4L), eq(DiaSemana.SEGUNDA), eq(LocalTime.of(10, 0)), eq(LocalTime.of(14, 0)), eq(null)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Já existe disponibilidade");
    }

    @Test
    void excluirFazSoftDelete() {
        Disponibilidade disponibilidade = disponibilidade(LocalTime.of(8, 0), LocalTime.of(12, 0), 30);
        when(disponibilidadeRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(disponibilidade));
        when(disponibilidadeRepository.save(any(Disponibilidade.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.excluir(1L);

        assertThat(disponibilidade.getDeletadoEm()).isNotNull();
    }

    @Test
    void geraHorariosRemovendoOcupados() {
        Disponibilidade janela = disponibilidade(LocalTime.of(8, 0), LocalTime.of(10, 0), 30);
        when(disponibilidadeRepository.findByVeterinarioIdAndDiaSemanaAndDeletadoEmIsNull(
                4L, DiaSemana.from(PROXIMA_SEGUNDA)))
                .thenReturn(List.of(janela));
        when(agendamentoRepository.findByVeterinarioIdAndDataAndDeletadoEmIsNullAndStatusNot(
                4L, PROXIMA_SEGUNDA, StatusAgendamento.CANCELADO))
                .thenReturn(List.of(agendamentoOcupado(LocalTime.of(9, 0), LocalTime.of(9, 30))));

        List<HorarioDisponivelResponse> horarios = service.horariosDisponiveis(4L, PROXIMA_SEGUNDA);

        assertThat(horarios)
                .extracting(HorarioDisponivelResponse::horaInicio)
                .containsExactly(LocalTime.of(8, 0), LocalTime.of(8, 30), LocalTime.of(9, 30));
    }

    @Test
    void retornaVazioQuandoNaoHaDisponibilidadeNoDia() {
        when(disponibilidadeRepository.findByVeterinarioIdAndDiaSemanaAndDeletadoEmIsNull(
                4L, DiaSemana.from(PROXIMA_SEGUNDA)))
                .thenReturn(List.of());

        assertThat(service.horariosDisponiveis(4L, PROXIMA_SEGUNDA)).isEmpty();
    }

    @Test
    void rejeitaConsultaDeHorariosNoPassado() {
        assertThatThrownBy(() -> service.horariosDisponiveis(4L, LocalDate.now().minusDays(1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("datas passadas");
    }

    @Test
    void aceitaHorarioAlinhadoComAGrade() {
        Disponibilidade janela = disponibilidade(LocalTime.of(8, 0), LocalTime.of(12, 0), 30);
        when(disponibilidadeRepository.findByVeterinarioIdAndDiaSemanaAndDeletadoEmIsNull(
                4L, DiaSemana.from(PROXIMA_SEGUNDA)))
                .thenReturn(List.of(janela));

        Disponibilidade resolvida = service.resolverDisponibilidade(
                4L, PROXIMA_SEGUNDA, LocalTime.of(9, 30));

        assertThat(resolvida).isEqualTo(janela);
    }

    @Test
    void rejeitaHorarioForaDoAlinhamentoDaGrade() {
        Disponibilidade janela = disponibilidade(LocalTime.of(8, 0), LocalTime.of(12, 0), 30);
        when(disponibilidadeRepository.findByVeterinarioIdAndDiaSemanaAndDeletadoEmIsNull(
                4L, DiaSemana.from(PROXIMA_SEGUNDA)))
                .thenReturn(List.of(janela));

        assertThatThrownBy(() -> service.resolverDisponibilidade(
                4L, PROXIMA_SEGUNDA, LocalTime.of(9, 15)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não está dentro da disponibilidade");
    }

    private DisponibilidadeRequest request(LocalTime inicio, LocalTime fim, int duracao) {
        return new DisponibilidadeRequest(4L, DiaSemana.SEGUNDA, inicio, fim, duracao);
    }

    private Disponibilidade disponibilidade(LocalTime inicio, LocalTime fim, int duracao) {
        Disponibilidade disponibilidade = new Disponibilidade();
        disponibilidade.setId(1L);
        disponibilidade.setVeterinarioId(4L);
        disponibilidade.setDiaSemana(DiaSemana.SEGUNDA);
        disponibilidade.setHoraInicio(inicio);
        disponibilidade.setHoraFim(fim);
        disponibilidade.setDuracaoConsulta(duracao);
        return disponibilidade;
    }

    private Agendamento agendamentoOcupado(LocalTime inicio, LocalTime fim) {
        Agendamento agendamento = new Agendamento();
        agendamento.setId(2L);
        agendamento.setVeterinarioId(4L);
        agendamento.setData(PROXIMA_SEGUNDA);
        agendamento.setHoraInicio(inicio);
        agendamento.setHoraFim(fim);
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        return agendamento;
    }
}
