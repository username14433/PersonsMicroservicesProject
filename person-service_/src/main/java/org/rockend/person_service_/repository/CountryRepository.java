package org.rockend.person_service_.repository;

import org.rockend.person_service_.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CountryRepository extends JpaRepository<Country, Integer> {

    Optional<Country> findByCode(String code);


}
