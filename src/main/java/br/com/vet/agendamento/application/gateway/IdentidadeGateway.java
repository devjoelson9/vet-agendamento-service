package br.com.vet.agendamento.application.gateway;

public interface IdentidadeGateway {

    TutorInfo buscarTutorAutenticado();

    TutorInfo buscarTutor(Long tutorId);

    AnimalInfo buscarAnimalDoTutorAutenticado(Long animalId);

    AnimalInfo buscarAnimalDoTutor(Long tutorId, Long animalId);
}
