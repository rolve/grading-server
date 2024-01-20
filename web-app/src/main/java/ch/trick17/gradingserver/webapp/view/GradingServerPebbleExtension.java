package ch.trick17.gradingserver.webapp.view;

import ch.trick17.gradingserver.webapp.Internationalization;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Filter;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.time.format.FormatStyle.MEDIUM;
import static java.util.Collections.emptyList;

@Component
public class GradingServerPebbleExtension extends AbstractExtension {

    private final Internationalization i18n;

    public GradingServerPebbleExtension(Internationalization i18n) {
        this.i18n = i18n;
    }

    @Override
    public Map<String, Filter> getFilters() {
        return Map.of(
                "date", new DateFilter(),
                "pretty", new PrettyFilter());
    }

    public class DateFilter implements Filter {

        @Override
        public List<String> getArgumentNames() {
            return List.of("style");
        }

        @Override
        public Object apply(Object input, Map<String, Object> args,
                            PebbleTemplate self, EvaluationContext context,
                            int lineNumber) {
            var style = Optional.ofNullable((String) args.get("style"))
                    .map(FormatStyle::valueOf)
                    .orElse(MEDIUM);
            return i18n.dateTimeFormatter(style).format((ZonedDateTime) input);
        }
    }

    public class PrettyFilter implements Filter {

        @Override
        public List<String> getArgumentNames() {
            return emptyList();
        }

        @Override
        public Object apply(Object input, Map<String, Object> args,
                            PebbleTemplate self, EvaluationContext context,
                            int lineNumber) {
            return i18n.prettyTime().format((ZonedDateTime) input);
        }
    }
}
