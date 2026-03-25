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

    public static final String INTERNAL_SERVER_ERROR = """
            {
              "message": "An unexpected error occurred. Please try again later.",
              "errors": [],
              "timestamp": "2026-03-25T10:00:00Z"
            }""";
}
