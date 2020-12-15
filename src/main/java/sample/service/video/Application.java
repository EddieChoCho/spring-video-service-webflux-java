package sample.service.video;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@SpringBootApplication
public class Application implements WebFluxConfigurer {

    @Override
    public void configureHttpMessageCodecs(final ServerCodecConfigurer configurer) {
        configurer.customCodecs().writer(new ResourceRegionMessageWriter());
    }

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
