package me.dhiya.hr.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import me.dhiya.hr.domain.CurrencyEntity;
import me.dhiya.hr.repositories.CurrencyRepository;
import me.dhiya.hr.services.CurrencyService;
import java.util.List;
import java.util.stream.StreamSupport;

@Service
public class CurrencyServiceImpl implements CurrencyService {

    private final CurrencyRepository currencyRepository;

    public CurrencyServiceImpl(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurrencyEntity> listCurrencies() {
        return StreamSupport.stream(currencyRepository.findAll().spliterator(), false).toList();
    }
}
