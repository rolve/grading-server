package ch.trick17.gradingserver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.LongStream.range;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RandomHexStringGeneratorTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 15, 16, 17, 32, 64, 128, 255, 256, 257})
    void generateLength(int length) {
        var generator = new RandomHexStringGenerator(length);
        for (int i = 0; i < 100; i++) {
            var string = generator.generate(s -> false);
            assertEquals(length, string.length());
        }
    }

    @Test
    void generateExists() {
        var existing = range(0, 255).mapToObj("%016x"::formatted).collect(toSet());
        var generator = new RandomHexStringGenerator(2);
        for (int i = 0; i < 100; i++) {
            var string = generator.generate(existing::contains);
            assertEquals(2, string.length());
            assertFalse(existing.contains(string));
        }
    }
}