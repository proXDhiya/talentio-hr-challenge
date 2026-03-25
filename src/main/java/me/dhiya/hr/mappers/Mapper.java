package me.dhiya.hr.mappers;

public interface Mapper<T, R> {
    R mapTo(T t);
    T mapFrom(R r);
}
