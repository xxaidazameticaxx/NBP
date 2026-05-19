package ba.unsa.etf.NBP;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NbpApplication {

	public static void main(String[] args) {
		SpringApplication.run(NbpApplication.class, args);
	}

}
