package me.dhiya.hr.services;

import me.dhiya.hr.domain.CurrencyEntity;
import java.util.List;

public interface CurrencyService {
    List<CurrencyEntity> listCurrencies();
}
