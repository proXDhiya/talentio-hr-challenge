package me.dhiya.hr.dto.employee.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Currency")
public class CurrencyDto {
    private String code;
    private String symbol;
}
