package me.dhiya.hr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomApiResponse<T> {
    private String message;
    private T data;
    private Instant timestamp;
}
