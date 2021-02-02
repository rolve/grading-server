package ch.trick17.gradingserver.gradingservice;

import ch.trick17.jtt.grader.Grader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@SpringBootApplication
public class GradingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GradingServiceApplication.class, args);
    }

    @Bean
    public Grader grader() {
        var grader = new Grader();
        grader.setLogDir(null);
        grader.setResultsDir(null);
        grader.setParallelism(1); // no need for parallelism, grading one at a time
        return grader;
    }

    @Bean
    RestTemplateCustomizer unwantedConvertersRemovingCustomizer() {
        // see https://blog.trifork.com/2020/05/26/i-used-springs-resttemplate-to-fetch-some-json-and-you-wont-believe-what-happened-next/
        return template -> {
            var foundFirst = false;
            for (var i = template.getMessageConverters().iterator(); i.hasNext();) {
                if (i.next() instanceof MappingJackson2HttpMessageConverter) {
                    if (foundFirst) {
                        i.remove();
                    } else {
                        foundFirst = true;
                    }
                }
            }
        };
    }
}
