package ch.trick17.gradingserver;

import ch.trick17.gradingserver.model.GradingResult;
import ch.trick17.gradingserver.model.OutdatedResult;
import ch.trick17.jtt.testsuitegrader.TaskJacksonModule;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.hypersistence.utils.hibernate.type.util.ObjectMapperSupplier;

import java.io.IOException;

public class DBObjectMapperSupplier implements ObjectMapperSupplier {
    public ObjectMapper get() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .registerModule(new TaskJacksonModule())
                .registerModule(new SimpleModule()
                        .setDeserializerModifier(new GradingResultDeserializerModifier()));
    }

    private static class GradingResultDeserializerModifier extends BeanDeserializerModifier {
        @Override
        public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,
                                                      BeanDescription beanDesc,
                                                      JsonDeserializer<?> deserializer) {
            return beanDesc.getBeanClass() == GradingResult.class
                    ? new GradingResultDeserializer(deserializer)
                    : deserializer;
        }
    }

    private static class GradingResultDeserializer extends StdDeserializer<GradingResult>
            implements ResolvableDeserializer {
        private final JsonDeserializer<?> delegate;

        protected GradingResultDeserializer(JsonDeserializer<?> delegate) {
            super(GradingResult.class);
            this.delegate = delegate;
        }

        @Override
        public GradingResult deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            try {
                return (GradingResult) delegate.deserialize(p, ctxt);
            } catch (DatabindException e) {
                return new OutdatedResult(e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        @Override
        public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
                                          TypeDeserializer typeDeserializer) throws IOException {
            try {
                return delegate.deserializeWithType(p, ctxt, typeDeserializer);
            } catch (DatabindException e) {
                return new OutdatedResult(e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        @Override
        public void resolve(DeserializationContext ctxt) throws JsonMappingException {
            if (delegate instanceof ResolvableDeserializer d) {
                d.resolve(ctxt);
            }
        }
    }
}
