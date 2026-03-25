package me.dhiya.hr.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import me.dhiya.hr.domain.CurrencyEntity;
import me.dhiya.hr.repositories.CurrencyRepository;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CurrencyServiceTests {

    @Autowired private CurrencyService currencyService;
    @Autowired private CurrencyRepository currencyRepository;

    @Test
    void listCurrenciesReturnsAllCurrencies() {
        List<CurrencyEntity> result = currencyService.listCurrencies();

        assertThat(result).isNotEmpty();
    }

    @Test
    void listCurrenciesIncludesSeededCurrencies() {
        List<CurrencyEntity> result = currencyService.listCurrencies();

        assertThat(result).anyMatch(c -> c.getCode().equals("USD"));
        assertThat(result).anyMatch(c -> c.getCode().equals("EUR"));
    }

    @Test
    void listCurrenciesReturnsCorrectFields() {
        List<CurrencyEntity> result = currencyService.listCurrencies();

        assertThat(result).allMatch(c -> c.getCode() != null);
        assertThat(result).allMatch(c -> c.getName() != null);
        assertThat(result).allMatch(c -> c.getSymbol() != null);
    }

    @Test
    void listCurrenciesReflectsNewlyAddedCurrency() {
        currencyRepository.save(CurrencyEntity.builder()
                .code("JPY").name("Japanese Yen").symbol("¥").build());

        List<CurrencyEntity> result = currencyService.listCurrencies();

        assertThat(result).anyMatch(c -> c.getCode().equals("JPY"));
    }
}
