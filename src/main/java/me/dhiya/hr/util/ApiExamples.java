package me.dhiya.hr.util;

public final class ApiExamples {

    private ApiExamples() {}

    public static final String VALIDATION_ERROR = """
            {
              "message": "Validation failed",
              "errors": [
                { "field": "email", "message": "Email must be a valid email address" }
              ],
              "timestamp": "2026-03-25T10:00:00Z"
            }""";

    public static final String UNAUTHORIZED = """
            {
              "message": "Invalid email or password",
              "errors": [],
              "timestamp": "2026-03-25T10:00:00Z"
            }""";

    public static final String CONFLICT = """
            {
              "message": "Setup has already been completed",
              "errors": [],
              "timestamp": "2026-03-25T10:00:00Z"
            }""";

    public static final String UNAUTHORIZED_PROFILE = """
            {
              "message": "Authentication required. Please login.",
              "errors": [],
              "timestamp": "2026-03-25T10:00:00Z"
            }""";

    public static final String FORBIDDEN = """
            {
              "message": "Access denied.",
              "errors": [],
              "timestamp": "2026-03-25T10:00:00Z"
            }""";

    public static final String ALREADY_INACTIVE = """
            {
              "message": "Employee is already deactivated",
              "errors": [],
              "timestamp": "2026-03-25T10:00:00Z"
            }""";

    public static final String NOT_FOUND = """
            {
              "message": "Employee not found",
              "errors": [],
              "timestamp": "2026-03-25T10:00:00Z"
            }""";

    public static final String EMAIL_CONFLICT = """
            {
              "message": "An employee with this email already exists",
              "errors": [
                { "field": "email", "message": "Email is already in use" }
              ],
              "timestamp": "2026-03-25T10:00:00Z"
            }""";
}
