package me.dhiya.hr.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import me.dhiya.hr.dto.employee.response.CurrencyDto;
import me.dhiya.hr.dto.employee.response.CurrencyListResponse;
import me.dhiya.hr.dto.employee.response.CurrencyPageDto;
import me.dhiya.hr.services.CurrencyService;
import me.dhiya.hr.util.ApiExamples;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/apis/v1/currencies")
@Tag(name = "Currencies", description = "Currency management endpoints")
public class CurrencyController {

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @Operation(summary = "List currencies", description = "Returns all available currencies. HR and Manager only.")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Currencies retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CurrencyListResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.UNAUTHORIZED_PROFILE))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.FORBIDDEN)))
    })
    @GetMapping(produces = "application/json")
    public ResponseEntity<CurrencyListResponse> listCurrencies() {
        List<CurrencyDto> items = currencyService.listCurrencies().stream()
                .map(c -> CurrencyDto.builder()
                        .code(c.getCode())
                        .name(c.getName())
                        .symbol(c.getSymbol())
                        .build())
                .toList();

        return ResponseEntity.ok(CurrencyListResponse.builder()
                .message("Currencies retrieved successfully")
                .data(CurrencyPageDto.builder()
                        .items(items)
                        .total(items.size())
                        .build())
                .timestamp(Instant.now())
                .build());
    }
}
