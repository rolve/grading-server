package ch.trick17.gradingserver.util;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.net.URI;
import java.util.List;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

@Converter
public class UriListConverter implements AttributeConverter<List<URI>, String> {

    private static final String SPLIT_CHAR = " "; // cannot appear unencoded in a URI

    @Override
    public String convertToDatabaseColumn(List<URI> urlList) {
        return urlList == null ? "" : urlList.stream()
                .map(URI::toString)
                .collect(joining(" "));
    }

    @Override
    public List<URI> convertToEntityAttribute(String string) {
        return stream(string.split(SPLIT_CHAR))
                .map(URI::create)
                .toList();
    }
}
