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

    public static final String LEAVE_INSUFFICIENT_BALANCE = """
            {
              "message": "Insufficient leave balance",
              "errors": [
                { "field": "totalDays", "message": "Requested 10 days but only 3 annual leave days remaining" }
              ],
              "timestamp": "2026-03-25T10:00:00Z"
            }""";

    public static final String LEAVE_OVERLAP = """
            {
              "message": "You already have a leave request overlapping these dates",
              "errors": [
                { "field": "startDate", "message": "Overlaps with existing leave request from 2026-03-30 to 2026-04-03" }
              ],
              "timestamp": "2026-03-25T10:00:00Z"
            }""";
}
