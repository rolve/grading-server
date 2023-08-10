package ch.trick17.gradingserver.util;

import ch.trick17.gradingserver.JarFile;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.*;
import java.util.List;

import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

@Converter
public class JarFileListConverter implements AttributeConverter<List<JarFile>, byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(List<JarFile> list) {
        if (list.isEmpty()) {
            return new byte[0];
        }
        var bytes = new ByteArrayOutputStream();
        try (var out = new ObjectOutputStream(bytes)) {
            out.writeObject(list.toArray(JarFile[]::new));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return bytes.toByteArray();
    }

    @Override
    public List<JarFile> convertToEntityAttribute(byte[] bytes) {
        if (bytes.length == 0) {
            return emptyList();
        }
        try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return asList((JarFile[]) in.readObject());
        } catch (IOException | ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }
}
