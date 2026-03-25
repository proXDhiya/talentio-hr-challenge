package me.dhiya.hr.dto.employee.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyPageDto {
    private List<CurrencyDto> items;
    private int total;
}
