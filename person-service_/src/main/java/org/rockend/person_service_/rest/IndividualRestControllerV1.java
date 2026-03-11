package org.rockend.person_service_.rest;

import lombok.RequiredArgsConstructor;
import org.rockend.person.api.PersonApi;
import org.rockend.person.dto.IndividualDto;
import org.rockend.person.dto.IndividualPageDto;
import org.rockend.person.dto.IndividualWriteDto;
import org.rockend.person.dto.IndividualWriteResponseDto;
import org.rockend.person_service_.service.IndividualService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Email;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
//REST контроллер для Individual на основе сгенерированного клиента с методами для:
// регистрации, получения по ID, получения по списку email, апдейта, soft delete, hard delete
public class IndividualRestControllerV1 implements PersonApi {

    private final IndividualService individualService;

    @Override
    public ResponseEntity<Void> compensateRegistration(UUID id) {
        individualService.hardDelete(id);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> delete(UUID id) {
        individualService.softDelete(id);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<IndividualPageDto> findAllByEmail(List<@Email String> email) {
        var individuals = individualService.findByEmails(email);
        return ResponseEntity.ok(individuals);
    }

    @Override
    public ResponseEntity<IndividualDto> findById(UUID id) {
        var individual = individualService.findById(id);
        return ResponseEntity.ok(individual);
    }

    @Override
    public ResponseEntity<IndividualWriteResponseDto> registration(IndividualWriteDto individualWriteDto) {
        var individual = individualService.register(individualWriteDto);
        return ResponseEntity.ok(individual);
    }

    @Override
    public ResponseEntity<IndividualWriteResponseDto> update(UUID id, IndividualWriteDto individualWriteDto) {
        return ResponseEntity.ok(individualService.update(id, individualWriteDto));
    }
}
