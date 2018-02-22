package com.trading.bot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.trading.bot.base.Bot;

@SpringBootApplication
@EnableScheduling
public class BotApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(BotApplication.class, args);
	}
	
	@Bean
	public Bot.BotIni getBotIni() {
		return new Bot.BotIni("BTC-USDT", 1000);
	}

	@Bean
	public RestTemplate getRestTemplate() throws RestClientException, Exception {
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();

		HttpClient httpClient = HttpClientBuilder.create().build();
		factory.setHttpClient(httpClient);
		RestTemplate restTemplate = new RestTemplate(factory);

		restTemplate.getForEntity(
				"http://bot.cryptoinvest.money:31337/trading/auth/login/apikey/sga!/environment/sandbox", String.class);

		restTemplate.getForEntity("http://bot.cryptoinvest.money:31337/management/ec2/allow/giovanni/ip/" + getMyIPv4(),
				String.class);

		return restTemplate;
	}

	private String getMyIPv4() throws Exception {
		URL whatismyip = new URL("http://checkip.amazonaws.com");
		BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
		return in.readLine();
	}
}
