package ch.trick17.gradingserver.view;

import ch.trick17.gradingserver.Internationalization;
import ch.trick17.gradingserver.model.Author;
import ch.trick17.gradingserver.model.ProblemSet;
import ch.trick17.gradingserver.service.AccessController;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static ch.trick17.gradingserver.model.ProblemSet.DisplaySetting.ANONYMOUS;
import static ch.trick17.gradingserver.model.ProblemSet.DisplaySetting.WITH_FULL_NAMES;
import static java.time.ZoneOffset.UTC;
import static java.time.format.FormatStyle.MEDIUM;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;

@Component
public class GradingServerPebbleExtension extends AbstractExtension {

    private final Internationalization i18n;
    private final AccessController access;

    public GradingServerPebbleExtension(Internationalization i18n,
                                        AccessController access) {
        this.i18n = i18n;
        this.access = access;
    }

    @Override
    public Map<String, Filter> getFilters() {
        return Map.of(
                "i18n_date", new DateFilter(),
                "authors", new AuthorsFilter());
    }

    public class DateFilter implements Filter {

        @Override
        public List<String> getArgumentNames() {
            return emptyList();
        }

        @Override
        public String apply(Object input, Map<String, Object> args,
                            PebbleTemplate self, EvaluationContext context,
                            int lineNumber) {
            return i18n.dateTimeFormatter(MEDIUM).format(((Instant) input).atZone(UTC));
        }
    }

    public class AuthorsFilter implements Filter {

        @Override
        public List<String> getArgumentNames() {
            return emptyList();
        }

        @Override
        public String apply(Object input, Map<String, Object> args,
                            PebbleTemplate self, EvaluationContext context,
                            int lineNumber) {
            var solution = (Solution) input;
            if (solution.getProblemSet().getDisplaySetting() == ANONYMOUS &&
                !access.checkWriteAccess(solution.getProblemSet())) {
                return i18n.message("problem-set.anonymous");
            } else {
                return solution.getAuthors().stream()
                        .map(a -> formatAuthor(a, solution.getProblemSet()))
                        .map(s -> s.replace(' ', ' '))
                        .collect(joining(", "));
            }
        }

        private String formatAuthor(Author author, ProblemSet problemSet) {
            return problemSet.getDisplaySetting() == WITH_FULL_NAMES || access.checkWriteAccess(problemSet)
                    ? author.getDisplayName()
                    : author.getShortenedDisplayName();
        }
    }
}
