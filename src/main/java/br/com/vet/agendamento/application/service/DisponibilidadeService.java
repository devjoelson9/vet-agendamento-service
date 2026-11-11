package br.com.vet.agendamento.application.service;

import br.com.vet.agendamento.application.dto.request.DisponibilidadeRequest;
import br.com.vet.agendamento.application.dto.response.DisponibilidadeResponse;
import br.com.vet.agendamento.application.dto.response.HorarioDisponivelResponse;
import br.com.vet.agendamento.application.exception.BusinessException;
import br.com.vet.agendamento.application.exception.ConflictException;
import br.com.vet.agendamento.application.exception.ResourceNotFoundException;
import br.com.vet.agendamento.domain.enums.DiaSemana;
import br.com.vet.agendamento.domain.enums.StatusAgendamento;
import br.com.vet.agendamento.domain.model.Agendamento;
import br.com.vet.agendamento.domain.model.Disponibilidade;
import br.com.vet.agendamento.domain.repository.AgendamentoRepository;
import br.com.vet.agendamento.domain.repository.DisponibilidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DisponibilidadeService {

    private static final Comparator<Disponibilidade> ORDEM_AGENDA =
            Comparator.comparing(Disponibilidade::getDiaSemana)
                    .thenComparing(Disponibilidade::getHoraInicio);

    private final DisponibilidadeRepository disponibilidadeRepository;
    private final AgendamentoRepository agendamentoRepository;

    @Transactional
    public DisponibilidadeResponse criar(DisponibilidadeRequest request) {
        validar(request, null);
        Disponibilidade disponibilidade = new Disponibilidade();
        aplicarDados(disponibilidade, request);
        return DisponibilidadeResponse.from(disponibilidadeRepository.save(disponibilidade));
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadeResponse> listar(Long veterinarioId) {
        List<Disponibilidade> disponibilidades = veterinarioId == null
                ? disponibilidadeRepository.findByDeletadoEmIsNull()
                : disponibilidadeRepository.findByVeterinarioIdAndDeletadoEmIsNull(veterinarioId);
        return disponibilidades.stream()
                .sorted(ORDEM_AGENDA)
                .map(DisponibilidadeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DisponibilidadeResponse buscar(Long id) {
        return DisponibilidadeResponse.from(buscarAtiva(id));
    }

    @Transactional
    public DisponibilidadeResponse atualizar(Long id, DisponibilidadeRequest request) {
        Disponibilidade disponibilidade = buscarAtiva(id);
        validar(request, id);
        aplicarDados(disponibilidade, request);
        return DisponibilidadeResponse.from(disponibilidadeRepository.save(disponibilidade));
    }

    @Transactional
    public void excluir(Long id) {
        Disponibilidade disponibilidade = buscarAtiva(id);
        disponibilidade.setDeletadoEm(LocalDateTime.now());
        disponibilidadeRepository.save(disponibilidade);
    }

    @Transactional(readOnly = true)
    public List<HorarioDisponivelResponse> horariosDisponiveis(Long veterinarioId, LocalDate data) {
        if (veterinarioId == null || data == null) {
            throw new BusinessException("Veterinário e data são obrigatórios");
        }
        if (data.isBefore(LocalDate.now())) {
            throw new BusinessException("Não é possível consultar horários de datas passadas");
        }

        List<Disponibilidade> janelas = disponibilidadeRepository
                .findByVeterinarioIdAndDiaSemanaAndDeletadoEmIsNull(veterinarioId, DiaSemana.from(data));
        if (janelas.isEmpty()) {
            return List.of();
        }

        List<Agendamento> ocupados = agendamentoRepository
                .findByVeterinarioIdAndDataAndDeletadoEmIsNullAndStatusNot(
                        veterinarioId, data, StatusAgendamento.CANCELADO);

        LocalDate hoje = LocalDate.now();
        LocalTime agora = LocalTime.now();
        Set<LocalTime> iniciosAdicionados = new HashSet<>();
        List<HorarioDisponivelResponse> horarios = new ArrayList<>();

        for (Disponibilidade janela : janelas) {
            LocalTime cursor = janela.getHoraInicio();
            while (!cursor.plusMinutes(janela.getDuracaoConsulta()).isAfter(janela.getHoraFim())) {
                LocalTime inicioSlot = cursor;
                LocalTime fim = inicioSlot.plusMinutes(janela.getDuracaoConsulta());
                boolean passado = data.equals(hoje) && !inicioSlot.isAfter(agora);
                boolean conflita = ocupados.stream().anyMatch(agendamento ->
                        agendamento.getHoraInicio().isBefore(fim) && agendamento.getHoraFim().isAfter(inicioSlot));

                if (!passado && !conflita && iniciosAdicionados.add(inicioSlot)) {
                    horarios.add(new HorarioDisponivelResponse(veterinarioId, data, inicioSlot, fim));
                }
                cursor = fim;
            }
        }

        horarios.sort(Comparator.comparing(HorarioDisponivelResponse::horaInicio));
        return horarios;
    }

    /**
     * Resolve a janela de disponibilidade que contém o início informado e valida
     * o alinhamento do slot com a duração padrão do veterinário (RN07).
     */
    @Transactional(readOnly = true)
    public Disponibilidade resolverDisponibilidade(Long veterinarioId, LocalDate data, LocalTime horaInicio) {
        if (veterinarioId == null || data == null || horaInicio == null) {
            throw new BusinessException("Veterinário, data e horário são obrigatórios");
        }
        if (data.isBefore(LocalDate.now())) {
            throw new BusinessException("Não é possível agendar em data passada");
        }
        if (data.equals(LocalDate.now()) && horaInicio.isBefore(LocalTime.now())) {
            throw new BusinessException("Não é possível agendar em horário já passado");
        }

        return disponibilidadeRepository
                .findByVeterinarioIdAndDiaSemanaAndDeletadoEmIsNull(veterinarioId, DiaSemana.from(data))
                .stream()
                .filter(janela -> !horaInicio.isBefore(janela.getHoraInicio()))
                .filter(janela -> !horaInicio.plusMinutes(janela.getDuracaoConsulta())
                        .isAfter(janela.getHoraFim()))
                .filter(janela -> Duration.between(janela.getHoraInicio(), horaInicio)
                        .toMinutes() % janela.getDuracaoConsulta() == 0)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "O horário informado não está dentro da disponibilidade do veterinário"));
    }

    private void validar(DisponibilidadeRequest request, Long idIgnorado) {
        if (!request.horaFim().isAfter(request.horaInicio())) {
            throw new BusinessException("O horário final deve ser posterior ao inicial");
        }
        if (request.duracaoConsulta() == null || request.duracaoConsulta() <= 0) {
            throw new BusinessException("A duração da consulta deve ser maior que zero");
        }

        long minutosJanela = Duration.between(request.horaInicio(), request.horaFim()).toMinutes();
        if (request.duracaoConsulta() > minutosJanela) {
            throw new BusinessException("A duração da consulta não pode ser maior que a janela de atendimento");
        }

        if (disponibilidadeRepository.existeSobreposicao(
                request.veterinarioId(), request.diaSemana(),
                request.horaInicio(), request.horaFim(), idIgnorado)) {
            throw new ConflictException(
                    "Já existe disponibilidade do veterinário nesse dia e intervalo");
        }
    }

    private void aplicarDados(Disponibilidade disponibilidade, DisponibilidadeRequest request) {
        disponibilidade.setVeterinarioId(request.veterinarioId());
        disponibilidade.setDiaSemana(request.diaSemana());
        disponibilidade.setHoraInicio(request.horaInicio());
        disponibilidade.setHoraFim(request.horaFim());
        disponibilidade.setDuracaoConsulta(request.duracaoConsulta());
    }

    private Disponibilidade buscarAtiva(Long id) {
        return disponibilidadeRepository.findByIdAndDeletadoEmIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Disponibilidade não encontrada: " + id));
    }
}
