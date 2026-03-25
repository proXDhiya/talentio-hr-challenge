package me.dhiya.hr.dto.employee.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeePageDto {
    private List<EmployeeListItemDto> items;
    private String nextCursor;
    private boolean hasMore;
    private int size;
}
