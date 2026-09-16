package co.sena.edu.themis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ThemisApplication {

	public static void main(String[] args) {
		SpringApplication.run(ThemisApplication.class, args);
	}

}
