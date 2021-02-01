package ch.trick17.gradingserver.gradingservice.model;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.util.Random;

import static java.lang.String.format;
import static org.hibernate.LockMode.NONE;

public class JobIdGenerator implements IdentifierGenerator {

    @Override
    public String generate(SharedSessionContractImplementor session, Object o) {
        // generate 128-bit hex strings, similar to Git commit hashes
        var random = new Random();
        String id;
        Object existing;
        do {
            id = format("%016x%016x", random.nextLong(), random.nextLong());
            existing = session.getEntityPersister(GradingJob.class.getName(), o).load(id, null, NONE, session);
        } while (existing != null);
        return id;
    }
}
