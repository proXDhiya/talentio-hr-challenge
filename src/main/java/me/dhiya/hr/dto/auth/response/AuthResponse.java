package me.dhiya.hr.dto.auth.response;

import me.dhiya.hr.dto.common.CustomApiResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.experimental.SuperBuilder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(name = "AuthResponse")
public class AuthResponse extends CustomApiResponse<LoginResponse> {
}
