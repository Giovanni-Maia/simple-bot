package com.trading.bot.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class Bot {
	public static class BotIni {
		private String coinPair;
		private double initialAmount;

		public BotIni(String coinPair, double initialAmount) {
			super();
			this.coinPair = coinPair;
			this.initialAmount = initialAmount;
		}

		public String getCoinPair() {
			return coinPair;
		}

		public double getInitialAmount() {
			return initialAmount;
		}
	}

	private static final Logger log = LoggerFactory.getLogger(Bot.class);

	private static final int MIN_IN_SECS = 60;
	private static final int SEC_IN_MILLIS = 1000;

	private String coinPair;
	private double amountOriginalCoin;
	private double amountPairCoin;
	private double lastPriceBought;

	@Autowired
	private RestTemplate restTemplate;

	public Bot(BotIni botIni) {
		this.coinPair = botIni.getCoinPair();
		this.amountOriginalCoin = botIni.getInitialAmount();
		amountPairCoin = 0;
	}

	@Scheduled(fixedRate = 5 * MIN_IN_SECS * SEC_IN_MILLIS)
	public void trade() {

		log.info("Considering to trade...");

		if (amountOriginalCoin > 0) { // Buying territory

			ResponseEntity<Prediction> predictionResponse = restTemplate
					.getForEntity("http://localhost:8123/trend/linear/" + coinPair + "/10m/5m", Prediction.class);

			Prediction prediction = predictionResponse.getBody();
			log.info("have money (" + amountOriginalCoin + " " + coinPair.split("-")[1] + ") and profit prediction is "
					+ prediction.getProfitPrediction() + " " + coinPair.split("-")[1]);
			// if the prediction is that we can make more than difference in buy/sell price
			// in the PairCoin, buy!
			if ((prediction.getProfitPrediction() > 0) && (prediction
					.getProfitPrediction() >= (prediction.getBuyPriceNow() - prediction.getSellPriceNow()))) {
				placeBuyOrder(amountPairCoin, coinPair, prediction.getBuyPriceNow());
			}
		} else if (amountPairCoin > 0) { // Selling territory

			ResponseEntity<Prediction> predictionResponse = restTemplate
					.getForEntity("http://localhost:8123/prediction/linear/" + coinPair + "/5m/5m", Prediction.class);

			Prediction prediction = predictionResponse.getBody();
			// no losses bigger than 10%
			log.info("have coin (" + amountPairCoin + " " + coinPair.split("-")[0] + "), last price bought is "
					+ lastPriceBought + " " + coinPair.split("-")[1] + ", sell price now is "
					+ prediction.getSellPriceNow() + " " + coinPair.split("-")[1] + " and profit prediction is "
					+ prediction.getProfitPrediction() + " " + coinPair.split("-")[1]);
			if ((lastPriceBought * 0.95) >= prediction.getSellPriceNow()) {
				placeSellOrder(amountPairCoin, coinPair, prediction.getSellPriceNow());
			} else if (prediction.getSellPriceNow() > lastPriceBought) {
				// if the prediction is that things are going down, sell!
				if (prediction.getProfitPrediction() <= 0) {
					placeSellOrder(amountPairCoin, coinPair, prediction.getSellPriceNow());
				}
			}
		}
	}

	private void placeSellOrder(double amount, String coinPair, double price) {
		ResponseEntity<BaseResponse> response = restTemplate
				.getForEntity("http://bot.cryptoinvest.money:31337/trading/order/sell/exchange/kucoin/pair/" + coinPair
						+ "/amount/" + amount + "/price/" + price + "/dryrun/true", BaseResponse.class);

		if (response.getBody().success) {
			log.info("SOLD: " + amount + " " + coinPair.split("-")[0] + "  price: " + price + " "
					+ coinPair.split("-")[1]);
			amountOriginalCoin = amount * price;
			amountPairCoin = 0;
		} else {
			log.info("ERROR PLACING ORDER: " + response.getBody().message);
		}
	}

	private void placeBuyOrder(double amount, String coinPair, double price) {
		amount = amountOriginalCoin / price;
		ResponseEntity<BaseResponse> response = restTemplate
				.getForEntity("http://bot.cryptoinvest.money:31337/trading/order/buy/exchange/kucoin/pair/" + coinPair
						+ "/amount/" + amount + "/price/" + price + "/dryrun/true", BaseResponse.class);

		if (response.getBody().success) {
			log.info("BOUGHT: " + amount + " " + coinPair.split("-")[0] + "  price: " + price + " "
					+ coinPair.split("-")[1]);
			amountPairCoin = amount;
			amountOriginalCoin = 0;
			lastPriceBought = price;
		} else {
			log.info("ERROR PLACING ORDER: " + response.getBody().message);
		}
	}
}
