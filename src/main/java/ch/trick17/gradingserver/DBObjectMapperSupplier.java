package ch.trick17.gradingserver;

import ch.trick17.jtt.testsuitegrader.TaskJacksonModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hypersistence.utils.hibernate.type.util.ObjectMapperSupplier;

public class DBObjectMapperSupplier implements ObjectMapperSupplier {
    public ObjectMapper get() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .registerModule(new TaskJacksonModule());
    }
}
