package me.dhiya.hr.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "EmployeeResponse")
public class EmployeeResponse {
    private String message;
    private EmployeeProfileDto data;
    private Instant timestamp;
}
