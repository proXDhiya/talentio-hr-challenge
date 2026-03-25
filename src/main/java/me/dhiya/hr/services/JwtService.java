package me.dhiya.hr.services;

import me.dhiya.hr.domain.EmployeeEntity;

public interface JwtService {
    String generateToken(EmployeeEntity employee);
    JwtPayload extractPayload(String token);
    boolean isTokenValid(String token);
}
