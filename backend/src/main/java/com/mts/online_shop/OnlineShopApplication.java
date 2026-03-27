package com.mts.online_shop;

import com.mts.online_shop.client.bank.BankClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BankClientProperties.class)
public class OnlineShopApplication {

	public static void main(String[] args) {
		SpringApplication.run(OnlineShopApplication.class, args);
	}

}
