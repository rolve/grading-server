package ch.trick17.gradingserver.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import javax.persistence.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.util.Objects.requireNonNull;

@Entity
public class JarFile {

    private static final MessageDigest DIGEST;

    static {
        try {
            DIGEST = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    @Id
    @GeneratedValue
    private Integer id;
    private String filename;
    @Lob
    private byte[] content;
    @Column(columnDefinition = "BINARY(32)", unique = true)
    private byte[] hash;

    protected JarFile() {}

    @JsonCreator
    public JarFile(String filename, byte[] content) {
        this.filename = requireNonNull(filename);
        this.content = requireNonNull(content);
        this.hash = DIGEST.digest(content);
    }

    public Integer getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getContent() {
        return content;
    }

    public byte[] getHash() {
        return hash;
    }

    @Override
    public String toString() {
        return filename + " (" + content.length + " bytes)";
    }
}
