package me.dhiya.hr.controllers.currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import me.dhiya.hr.controllers.BaseControllerTest;
import me.dhiya.hr.domain.enums.Role;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CurrencyListTests extends BaseControllerTest {

    private static final String URL = "/apis/v1/currencies";

    private String managerToken;
    private String hrToken;
    private String employeeToken;

    @BeforeEach
    void createUsers() {
        managerToken = token(saveEmployee("John", "Doe", "manager@company.com", Role.MANAGER));
        hrToken = token(saveEmployee("Jane", "Smith", "hr@company.com", Role.HR));
        employeeToken = token(saveEmployee("Bob", "Jones", "bob@company.com", Role.EMPLOYEE));
    }

    @Test
    void listCurrenciesReturns401WhenNoTokenProvided() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listCurrenciesReturns403WhenCalledByEmployee() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void listCurrenciesReturns200WhenCalledByManager() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Currencies retrieved successfully"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void listCurrenciesReturns200WhenCalledByHr() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk());
    }

    @Test
    void listCurrenciesReturnsCorrectFields() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].code").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].name").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].symbol").isNotEmpty());
    }

    @Test
    void listCurrenciesReturnsAllSeededCurrencies() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.code == 'USD')]").isNotEmpty())
                .andExpect(jsonPath("$.data.items[?(@.code == 'EUR')]").isNotEmpty());
    }
}
