package io.maverick.database.breeze;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class BreezeApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(BreezeApplication.class, args);
	}

}
