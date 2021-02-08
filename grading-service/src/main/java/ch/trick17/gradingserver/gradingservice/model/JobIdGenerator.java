package ch.trick17.gradingserver.gradingservice.model;

import ch.trick17.gradingserver.util.RandomHexStringGenerator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import static org.hibernate.LockMode.NONE;

public class JobIdGenerator implements IdentifierGenerator {

    // generate 128-bit hex strings, similar to Git commit hashes
    private final RandomHexStringGenerator generator = new RandomHexStringGenerator(32);

    @Override
    public String generate(SharedSessionContractImplementor session, Object o) {
        var persister = session.getEntityPersister(GradingJob.class.getName(), o);
        return generator.generate(id -> persister.load(id, null, NONE, session) != null);
    }
}
