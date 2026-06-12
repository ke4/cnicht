package co.uk.karel.dev.cnicht;

import org.springframework.boot.SpringApplication;

public class TestCnichtApplication {

    public static void main(String[] args) {
        SpringApplication.from(CnichtApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
