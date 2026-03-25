package me.dhiya.hr.dto.employee.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import me.dhiya.hr.dto.common.CustomApiResponse;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(name = "CurrencyListResponse")
public class CurrencyListResponse extends CustomApiResponse<CurrencyPageDto> {
}
