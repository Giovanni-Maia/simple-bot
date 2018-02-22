package com.trading.bot.base;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoryData extends BaseResponse {
	static class Payload {
		static class Aggregations {
			static class All {
				static class Buckets {
					static class Value {
						public double value;
					}

					public long key;
					public Value volume, volumeValue, sell, buy;
				}

				public List<Buckets> buckets;
			}

			public All all;
		}

		public Aggregations aggregations;
	}

	public Payload payload;
}
