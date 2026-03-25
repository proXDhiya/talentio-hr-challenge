package me.dhiya.hr.util;

public final class ValidationConstants {
    public static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    public static final String PASSWORD_STRENGTH_REGEX = "^(?=.*[A-Z])(?=.*\\d).+$";
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 64;

    public static final String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
}
