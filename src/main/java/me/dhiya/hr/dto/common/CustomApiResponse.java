package me.dhiya.hr.dto.common;

import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CustomApiResponse<T> {
    private String message;
    private T data;
    private Instant timestamp;
}
