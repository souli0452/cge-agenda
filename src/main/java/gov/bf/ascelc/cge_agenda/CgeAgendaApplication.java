package gov.bf.ascelc.cge_agenda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class CgeAgendaApplication {

	public static void main(String[] args) {
		SpringApplication.run(CgeAgendaApplication.class, args);
	}

}
