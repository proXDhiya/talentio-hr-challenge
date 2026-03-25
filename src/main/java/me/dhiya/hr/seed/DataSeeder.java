package me.dhiya.hr.seed;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import me.dhiya.hr.domain.CurrencyEntity;
import me.dhiya.hr.repositories.CurrencyRepository;
import java.util.List;

@Component
public class DataSeeder implements ApplicationRunner {

    private final CurrencyRepository currencyRepository;

    public DataSeeder(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (currencyRepository.count() > 0) return;

        currencyRepository.saveAll(List.of(
                CurrencyEntity.builder().code("DZD").name("Algerian Dinar").symbol("د.ج").build(),
                CurrencyEntity.builder().code("EUR").name("Euro").symbol("€").build(),
                CurrencyEntity.builder().code("USD").name("US Dollar").symbol("$").build()
        ));
    }
}
