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
@Schema(name = "Manager")
public class ManagerDto {
    private String id;
    private String firstName;
    private String lastName;
}
