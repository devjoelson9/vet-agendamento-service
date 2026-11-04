package br.com.vet.agendamento.application.service;

import br.com.vet.agendamento.application.dto.request.AgendamentoRequest;
import br.com.vet.agendamento.application.dto.response.AgendamentoResponse;
import br.com.vet.agendamento.application.exception.BusinessException;
import br.com.vet.agendamento.application.exception.ConflictException;
import br.com.vet.agendamento.application.exception.ResourceNotFoundException;
import br.com.vet.agendamento.domain.enums.StatusAgendamento;
import br.com.vet.agendamento.domain.model.Agendamento;
import br.com.vet.agendamento.domain.repository.AgendamentoRepository;
import br.com.vet.agendamento.domain.repository.BloqueioAgendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private static final List<StatusAgendamento> STATUS_QUE_OCUPAM_HORARIO = List.of(
            StatusAgendamento.AGUARDANDO_CONFIRMACAO,
            StatusAgendamento.CONFIRMADO,
            StatusAgendamento.CHECK_IN,
            StatusAgendamento.EM_ATENDIMENTO
    );

    private final AgendamentoRepository agendamentoRepository;
    private final BloqueioAgendaRepository bloqueioRepository;

    @Transactional
    public AgendamentoResponse criar(AgendamentoRequest request) {
        validarDuracao(request.duracaoMinutos());
        Agendamento agendamento = new Agendamento();
        atualizarDados(agendamento, request);
        agendamento.setStatus(StatusAgendamento.AGUARDANDO_CONFIRMACAO);
        validarDisponibilidade(agendamento, null);
        return AgendamentoResponse.from(agendamentoRepository.save(agendamento));
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listar(
            LocalDateTime inicio,
            LocalDateTime fim,
            Long veterinarioId,
            Long animalId,
            StatusAgendamento status
    ) {
        validarPeriodo(inicio, fim);
        return agendamentoRepository.buscarNoPeriodo(
                        inicio, fim, veterinarioId, animalId, status == null ? null : status.name())
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
        validarDuracao(request.duracaoMinutos());
        Agendamento agendamento = buscarAtivo(id);
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO
                || agendamento.getStatus() == StatusAgendamento.CONCLUIDO
                || agendamento.getStatus() == StatusAgendamento.FALTOU) {
            throw new BusinessException("Agendamentos encerrados não podem ser alterados");
        }
        atualizarDados(agendamento, request);
        validarDisponibilidade(agendamento, id);
        return AgendamentoResponse.from(agendamentoRepository.save(agendamento));
    }

    @Transactional
    public AgendamentoResponse alterarStatus(Long id, StatusAgendamento status) {
        Agendamento agendamento = buscarAtivo(id);
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO
                || agendamento.getStatus() == StatusAgendamento.CONCLUIDO
                || agendamento.getStatus() == StatusAgendamento.FALTOU) {
            throw new BusinessException("Agendamentos encerrados não podem ter o status alterado");
        }
        if (status == StatusAgendamento.CONFIRMADO
                || status == StatusAgendamento.CHECK_IN
                || status == StatusAgendamento.EM_ATENDIMENTO) {
            validarDisponibilidade(agendamento, id);
        }
        agendamento.setStatus(status);
        return AgendamentoResponse.from(agendamentoRepository.save(agendamento));
    }

    @Transactional
    public void cancelar(Long id) {
        Agendamento agendamento = buscarAtivo(id);
        if (agendamento.getStatus() == StatusAgendamento.CONCLUIDO
                || agendamento.getStatus() == StatusAgendamento.FALTOU) {
            throw new BusinessException("Agendamentos concluídos ou com falta não podem ser cancelados");
        }
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamentoRepository.save(agendamento);
    }

    private void atualizarDados(Agendamento agendamento, AgendamentoRequest request) {
        agendamento.setTutorId(request.tutorId());
        agendamento.setAnimalId(request.animalId());
        agendamento.setVeterinarioId(request.veterinarioId());
        agendamento.setTipo(request.tipo());
        agendamento.setServicoNome(request.servicoNome());
        agendamento.setInicioEm(request.inicioEm());
        agendamento.setDuracaoMinutos(request.duracaoMinutos());
        agendamento.setObservacoes(request.observacoes());
    }

    private void validarDisponibilidade(Agendamento agendamento, Long idIgnorado) {
        LocalDateTime inicio = agendamento.getInicioEm();
        LocalDateTime fim = agendamento.getFimEm();
        if (agendamentoRepository.existeConflito(
                agendamento.getVeterinarioId(), inicio, fim,
                STATUS_QUE_OCUPAM_HORARIO.stream().map(Enum::name).toList(), idIgnorado)) {
            throw new ConflictException("O veterinário já possui um agendamento nesse horário");
        }
        if (bloqueioRepository.existeConflito(agendamento.getVeterinarioId(), inicio, fim)) {
            throw new ConflictException("O horário está bloqueado na agenda");
        }
    }

    private void validarDuracao(Integer duracaoMinutos) {
        if (duracaoMinutos < 5 || duracaoMinutos > 480) {
            throw new BusinessException("A duração deve ser entre 5 e 480 minutos");
        }
    }

    private void validarPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null || !fim.isAfter(inicio)) {
            throw new BusinessException("O período deve conter início e fim válidos, com fim posterior ao início");
        }
    }

    private Agendamento buscarAtivo(Long id) {
        return agendamentoRepository.findByIdAndDeletadoEmIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado: " + id));
    }
}
