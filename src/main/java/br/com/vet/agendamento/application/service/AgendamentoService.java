package br.com.vet.agendamento.application.service;

import br.com.vet.agendamento.application.dto.request.AgendamentoRequest;
import br.com.vet.agendamento.application.dto.response.AgendamentoResponse;
import br.com.vet.agendamento.application.exception.BusinessException;
import br.com.vet.agendamento.application.exception.ConflictException;
import br.com.vet.agendamento.application.exception.ResourceNotFoundException;
import br.com.vet.agendamento.domain.enums.StatusAgendamento;
import br.com.vet.agendamento.domain.model.Agendamento;
import br.com.vet.agendamento.domain.model.Disponibilidade;
import br.com.vet.agendamento.domain.repository.AgendamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final DisponibilidadeService disponibilidadeService;

    @Transactional
    public AgendamentoResponse criar(AgendamentoRequest request) {
        Disponibilidade disponibilidade = disponibilidadeService.resolverDisponibilidade(
                request.veterinarioId(), request.data(), request.horaInicio());
        LocalTime horaFim = request.horaInicio().plusMinutes(disponibilidade.getDuracaoConsulta());

        validarConflito(request.veterinarioId(), request.data(), request.horaInicio(), horaFim, null);

        Agendamento agendamento = new Agendamento();
        aplicarDados(agendamento, request, horaFim);
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        return AgendamentoResponse.from(agendamentoRepository.save(agendamento));
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listar(
            LocalDate inicio,
            LocalDate fim,
            Long veterinarioId,
            Long animalId,
            StatusAgendamento status
    ) {
        validarPeriodo(inicio, fim);
        return agendamentoRepository.buscarNoPeriodo(inicio, fim, veterinarioId, animalId, status)
                .stream()
                .map(AgendamentoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgendamentoResponse buscar(Long id) {
        return AgendamentoResponse.from(buscarAtivo(id));
    }

    @Transactional
    public AgendamentoResponse atualizar(Long id, AgendamentoRequest request) {
        Agendamento agendamento = buscarAtivo(id);
        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new BusinessException("Somente agendamentos com status AGENDADO podem ser reagendados");
        }

        Disponibilidade disponibilidade = disponibilidadeService.resolverDisponibilidade(
                request.veterinarioId(), request.data(), request.horaInicio());
        LocalTime horaFim = request.horaInicio().plusMinutes(disponibilidade.getDuracaoConsulta());

        validarConflito(request.veterinarioId(), request.data(), request.horaInicio(), horaFim, id);

        aplicarDados(agendamento, request, horaFim);
        return AgendamentoResponse.from(agendamentoRepository.save(agendamento));
    }

    @Transactional
    public AgendamentoResponse alterarStatus(Long id, StatusAgendamento status) {
        Agendamento agendamento = buscarAtivo(id);
        validarTransicao(agendamento.getStatus(), status);

        if (status == StatusAgendamento.AGUARDANDO_ATENDIMENTO
                && agendamento.getHorarioCheckin() == null) {
            agendamento.setHorarioCheckin(LocalDateTime.now());
        }
        agendamento.setStatus(status);
        return AgendamentoResponse.from(agendamentoRepository.save(agendamento));
    }

    @Transactional
    public void cancelar(Long id) {
        Agendamento agendamento = buscarAtivo(id);
        if (agendamento.getStatus() != StatusAgendamento.AGENDADO
                && agendamento.getStatus() != StatusAgendamento.AGUARDANDO_ATENDIMENTO) {
            throw new BusinessException(
                    "Somente agendamentos AGENDADO ou AGUARDANDO_ATENDIMENTO podem ser cancelados");
        }
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamentoRepository.save(agendamento);
    }

    private void aplicarDados(Agendamento agendamento, AgendamentoRequest request, LocalTime horaFim) {
        agendamento.setTutorId(request.tutorId());
        agendamento.setAnimalId(request.animalId());
        agendamento.setVeterinarioId(request.veterinarioId());
        agendamento.setData(request.data());
        agendamento.setHoraInicio(request.horaInicio());
        agendamento.setHoraFim(horaFim);
    }

    private void validarConflito(
            Long veterinarioId, LocalDate data, LocalTime horaInicio, LocalTime horaFim, Long idIgnorado) {
        if (agendamentoRepository.existeConflito(
                veterinarioId, data, horaInicio, horaFim,
                StatusAgendamento.CANCELADO, idIgnorado)) {
            throw new ConflictException("O veterinário já possui um agendamento nesse horário");
        }
    }

    private void validarTransicao(StatusAgendamento atual, StatusAgendamento novo) {
        if (atual == novo) {
            return;
        }
        boolean valida = switch (atual) {
            case AGENDADO -> novo == StatusAgendamento.AGUARDANDO_ATENDIMENTO
                    || novo == StatusAgendamento.CANCELADO;
            case AGUARDANDO_ATENDIMENTO -> novo == StatusAgendamento.ATENDIMENTO_REALIZADO
                    || novo == StatusAgendamento.CANCELADO;
            case ATENDIMENTO_REALIZADO -> novo == StatusAgendamento.CONCLUIDO;
            case CONCLUIDO, CANCELADO -> false;
        };
        if (!valida) {
            throw new BusinessException("Transição de status inválida: " + atual + " -> " + novo);
        }
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new BusinessException(
                    "O período deve conter início e fim válidos, com fim igual ou posterior ao início");
        }
    }

    private Agendamento buscarAtivo(Long id) {
        return agendamentoRepository.findByIdAndDeletadoEmIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado: " + id));
    }
}
