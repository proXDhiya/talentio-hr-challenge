package me.dhiya.hr.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ApiError")
public class CustomApiError {
    private String message;
    private List<CustomFieldError> errors;
    private Instant timestamp;
}
