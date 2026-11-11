package br.com.vet.agendamento.application.service;

import br.com.vet.agendamento.application.dto.request.AgendamentoRequest;
import br.com.vet.agendamento.application.dto.response.AgendamentoResponse;
import br.com.vet.agendamento.application.exception.BusinessException;
import br.com.vet.agendamento.application.exception.ConflictException;
import br.com.vet.agendamento.application.exception.ResourceNotFoundException;
import br.com.vet.agendamento.application.gateway.AnimalInfo;
import br.com.vet.agendamento.application.gateway.IdentidadeGateway;
import br.com.vet.agendamento.application.gateway.TutorInfo;
import br.com.vet.agendamento.domain.enums.StatusAgendamento;
import br.com.vet.agendamento.domain.model.Agendamento;
import br.com.vet.agendamento.domain.model.Disponibilidade;
import br.com.vet.agendamento.domain.repository.AgendamentoRepository;
import br.com.vet.agendamento.infrastructure.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
    private final IdentidadeGateway identidadeGateway;
    private final UsuarioAutenticado usuarioAutenticado;

    @Transactional
    public AgendamentoResponse criar(AgendamentoRequest request) {
        TutorInfo tutor = resolverTutor(request.tutorId());
        validarTutorAtivo(tutor);
        validarAnimalDoTutor(tutor.id(), request.animalId());

        Disponibilidade disponibilidade = disponibilidadeService.resolverDisponibilidade(
                request.veterinarioId(), request.data(), request.horaInicio());
        LocalTime horaFim = request.horaInicio().plusMinutes(disponibilidade.getDuracaoConsulta());

        validarConflito(request.veterinarioId(), request.data(), request.horaInicio(), horaFim, null);

        Agendamento agendamento = new Agendamento();
        aplicarDados(agendamento, request, tutor.id(), horaFim);
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        return AgendamentoResponse.from(agendamentoRepository.save(agendamento));
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listar(
            LocalDate inicio,
            LocalDate fim,
            Long tutorId,
            Long veterinarioId,
            Long animalId,
            StatusAgendamento status
    ) {
        validarPeriodo(inicio, fim);
        Long tutorFiltro = resolverTutorFiltro(tutorId);
        return agendamentoRepository
                .buscarNoPeriodo(inicio, fim, tutorFiltro, veterinarioId, animalId, status)
                .stream()
                .map(AgendamentoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgendamentoResponse buscar(Long id) {
        Agendamento agendamento = buscarAtivo(id);
        validarAcessoAoTutor(agendamento.getTutorId());
        return AgendamentoResponse.from(agendamento);
    }

    @Transactional
    public AgendamentoResponse atualizar(Long id, AgendamentoRequest request) {
        Agendamento agendamento = buscarAtivo(id);
        validarAcessoAoTutor(agendamento.getTutorId());
        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new BusinessException("Somente agendamentos com status AGENDADO podem ser reagendados");
        }
        if (request.tutorId() != null && !request.tutorId().equals(agendamento.getTutorId())) {
            throw new BusinessException("Não é permitido alterar o tutor no reagendamento");
        }

        TutorInfo tutor = buscarTutorDoAgendamento(agendamento.getTutorId());
        validarTutorAtivo(tutor);
        validarAnimalDoTutor(tutor.id(), request.animalId());

        Disponibilidade disponibilidade = disponibilidadeService.resolverDisponibilidade(
                request.veterinarioId(), request.data(), request.horaInicio());
        LocalTime horaFim = request.horaInicio().plusMinutes(disponibilidade.getDuracaoConsulta());

        validarConflito(request.veterinarioId(), request.data(), request.horaInicio(), horaFim, id);

        aplicarDados(agendamento, request, agendamento.getTutorId(), horaFim);
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
    public AgendamentoResponse realizarCheckIn(Long id) {
        Agendamento agendamento = buscarAtivo(id);

        if (agendamento.getStatus() == StatusAgendamento.AGUARDANDO_ATENDIMENTO) {
            if (agendamento.getHorarioCheckin() == null) {
                agendamento.setHorarioCheckin(LocalDateTime.now());
                return AgendamentoResponse.from(agendamentoRepository.save(agendamento));
            }
            return AgendamentoResponse.from(agendamento);
        }

        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new BusinessException(
                    "Somente agendamentos com status AGENDADO podem realizar check-in");
        }

        agendamento.setHorarioCheckin(LocalDateTime.now());
        agendamento.setStatus(StatusAgendamento.AGUARDANDO_ATENDIMENTO);
        return AgendamentoResponse.from(agendamentoRepository.save(agendamento));
    }

    @Transactional
    public void cancelar(Long id) {
        Agendamento agendamento = buscarAtivo(id);
        validarAcessoAoTutor(agendamento.getTutorId());
        if (agendamento.getStatus() != StatusAgendamento.AGENDADO
                && agendamento.getStatus() != StatusAgendamento.AGUARDANDO_ATENDIMENTO) {
            throw new BusinessException(
                    "Somente agendamentos AGENDADO ou AGUARDANDO_ATENDIMENTO podem ser cancelados");
        }
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamentoRepository.save(agendamento);
    }

    private TutorInfo resolverTutor(Long tutorIdInformado) {
        if (usuarioAutenticado.isTutor()) {
            TutorInfo tutor = identidadeGateway.buscarTutorAutenticado();
            if (tutorIdInformado != null && !tutorIdInformado.equals(tutor.id())) {
                throw new AccessDeniedException("Tutor não pode agendar para outro tutor");
            }
            return tutor;
        }
        if (usuarioAutenticado.isRecepcionista()) {
            if (tutorIdInformado == null) {
                throw new BusinessException("O tutor é obrigatório no agendamento feito pela recepção");
            }
            return identidadeGateway.buscarTutor(tutorIdInformado);
        }
        throw new AccessDeniedException("Perfil sem permissão para agendar consultas");
    }

    private void validarTutorAtivo(TutorInfo tutor) {
        if (!tutor.ativo()) {
            throw new BusinessException(
                    "A conta do tutor está desativada e não pode possuir consultas agendadas");
        }
    }

    private void validarAnimalDoTutor(Long tutorId, Long animalId) {
        AnimalInfo animal = usuarioAutenticado.isTutor()
                ? identidadeGateway.buscarAnimalDoTutorAutenticado(animalId)
                : identidadeGateway.buscarAnimalDoTutor(tutorId, animalId);

        if (!tutorId.equals(animal.tutorId())) {
            throw new AccessDeniedException("O animal não pertence ao tutor informado");
        }
    }

    private Long resolverTutorFiltro(Long tutorIdInformado) {
        if (usuarioAutenticado.isTutor()) {
            return identidadeGateway.buscarTutorAutenticado().id();
        }
        return tutorIdInformado;
    }

    private TutorInfo buscarTutorDoAgendamento(Long tutorIdDoAgendamento) {
        return usuarioAutenticado.isTutor()
                ? identidadeGateway.buscarTutorAutenticado()
                : identidadeGateway.buscarTutor(tutorIdDoAgendamento);
    }

    private void validarAcessoAoTutor(Long tutorIdDoAgendamento) {
        if (usuarioAutenticado.isTutor()
                && !identidadeGateway.buscarTutorAutenticado().id().equals(tutorIdDoAgendamento)) {
            throw new AccessDeniedException("Tutor não pode acessar o agendamento de outro tutor");
        }
    }

    private void aplicarDados(
            Agendamento agendamento, AgendamentoRequest request, Long tutorId, LocalTime horaFim) {
        agendamento.setTutorId(tutorId);
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
