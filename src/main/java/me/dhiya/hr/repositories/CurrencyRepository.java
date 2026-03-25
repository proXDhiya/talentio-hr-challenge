package me.dhiya.hr.repositories;

import me.dhiya.hr.domain.CurrencyEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurrencyRepository extends CrudRepository<CurrencyEntity, String> {
    Optional<CurrencyEntity> findByCode(String code);
}
