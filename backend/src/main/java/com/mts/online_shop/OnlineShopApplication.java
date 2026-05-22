package com.mts.online_shop;

import com.mts.online_shop.client.bank.BankClientProperties;
import com.mts.online_shop.client.bitrix.BitrixEisProperties;
import com.mts.online_shop.config.MqttProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({BankClientProperties.class, BitrixEisProperties.class, MqttProperties.class})
@EnableJpaRepositories(basePackages = "com.mts.online_shop.repository")
public class OnlineShopApplication {

	public static void main(String[] args) {
		SpringApplication.run(OnlineShopApplication.class, args);
	}

}
