package org.rockend.api;

import org.rockend.keycloak.api.AuthApiClient;
import org.rockend.person.api.PersonApiClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

//Данная аннотация необходима для корректной работы с feign-клиентом
@EnableFeignClients(basePackageClasses = {AuthApiClient.class, PersonApiClient.class})
//Данная аннотация необходима для корректной работы KeycloakProperties
@ConfigurationPropertiesScan
@SpringBootApplication
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

}
