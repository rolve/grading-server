package ch.trick17.gradingserver.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String SPLIT_CHAR = ";";
    
    @Override
    public String convertToDatabaseColumn(List<String> stringList) {
        return stringList == null ? null : join(SPLIT_CHAR, stringList);
    }

    @Override
    public List<String> convertToEntityAttribute(String string) {
        return string == null ? null :
                string.isEmpty() ? emptyList() :
                        asList(string.split(SPLIT_CHAR));
    }
}