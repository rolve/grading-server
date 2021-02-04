package ch.trick17.gradingserver.util;

import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

public class RandomHexStringGenerator {

    private final int length;

    public RandomHexStringGenerator(int length) {
        this.length = length;
    }

    public String generate(Predicate<String> exists) {
        var random = new Random();
        String string;
        do {
            string = random.longs((length - 1) / 16 + 1)
                    .mapToObj("%016x"::formatted)
                    .collect(joining())
                    .substring(0, length);
        } while (exists.test(string));
        return string;
    }
}
