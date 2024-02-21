package ch.trick17.gradingserver.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.hypersistence.utils.hibernate.type.util.JsonConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static ch.trick17.gradingserver.model.GradingOptions.Compiler.JAVAC;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GradingConfigTest {

    record Container(GradingConfig config) {}

    @Test
    void convertToJson() throws JsonProcessingException {
        var mapper = JsonConfiguration.INSTANCE.getObjectMapperWrapper().getObjectMapper();
        var config = new ImplGradingConfig(
                "class FooTest {}",
                new GradingOptions(JAVAC, 7, Duration.ofMillis(100), Duration.ofMillis(500), true));
        var json = mapper.writeValueAsString(new Container(config));
        assertEquals("""
                {
                    "config":{
                        "@class":"ch.trick17.gradingserver.model.ImplGradingConfig",
                        "testClass":"class FooTest {}",
                        "options":{
                            "compiler":"JAVAC",
                            "repetitions":7,
                            "repTimeout":0.100000000,
                            "testTimeout":0.500000000,
                            "permRestrictions":true
                        }
                    }
                }
                """.replaceAll("\n *", ""), json);
    }

    @Test
    void convertFromJson() throws JsonProcessingException {
        var json = """
                {
                    "@class":"ch.trick17.gradingserver.model.ImplGradingConfig",
                    "testClass":"class FooTest {}",
                    "options":{
                        "compiler":"JAVAC",
                        "repetitions":7,
                        "repTimeout":0.100000000,
                        "testTimeout":0.500000000,
                        "permRestrictions":true
                    }
                }
                """.replaceAll("\n *", "");
        var mapper = JsonConfiguration.INSTANCE.getObjectMapperWrapper().getObjectMapper();
        var config = mapper.readValue(json, GradingConfig.class);
        var expected = new ImplGradingConfig("class FooTest {}",
                new GradingOptions(JAVAC, 7, Duration.ofMillis(100), Duration.ofMillis(500), true));
        assertEquals(expected, config);
    }
}
