package com.almoxarifado;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.modulith.Modulithic;

@Modulithic(systemName = "Almoxarifado", sharedModules = "shared")
@SpringBootApplication
@ConfigurationPropertiesScan
public class AlmoxarifadoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlmoxarifadoApplication.class, args);
    }
}
