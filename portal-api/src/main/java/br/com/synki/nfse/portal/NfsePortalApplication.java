package br.com.synki.nfse.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NfsePortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(NfsePortalApplication.class, args);
    }
}
