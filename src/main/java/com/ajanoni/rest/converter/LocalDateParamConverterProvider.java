package com.ajanoni.rest.converter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

@Provider
public class LocalDateParamConverterProvider implements ParamConverterProvider {

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType.isAssignableFrom(LocalDate.class)) {
            return (ParamConverter<T>) new DateParamConverter();
        }
        return null;
    }

    public static class DateParamConverter implements ParamConverter<LocalDate> {

        @Override
        public LocalDate fromString(String param) {
            try {
                return LocalDate.parse(param, DateTimeFormatter.ISO_DATE);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Invalid date format",e);
            }
        }

        @Override
        public String toString(LocalDate date) {
            return DateTimeFormatter.ISO_DATE.format(date);
        }
    }

}
