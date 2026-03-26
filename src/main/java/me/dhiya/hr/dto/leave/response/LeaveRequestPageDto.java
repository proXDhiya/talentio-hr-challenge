package me.dhiya.hr.dto.leave.response;

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
public class LeaveRequestPageDto {
    private List<LeaveRequestListItemDto> items;
    private String nextCursor;
    private boolean hasMore;
    private int size;
}
