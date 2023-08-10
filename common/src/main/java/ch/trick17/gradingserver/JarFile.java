package ch.trick17.gradingserver;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;

public class JarFile implements Serializable {

    private String filename;
    private byte[] content;

    protected JarFile() {}

    @JsonCreator
    public JarFile(String filename, byte[] content) {
        this.filename = requireNonNull(filename);
        this.content = requireNonNull(content);
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getContent() {
        return content;
    }

    @Override
    public String toString() {
        return filename + " (" + content.length + " bytes)";
    }
}
