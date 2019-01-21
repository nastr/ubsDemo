package com.ubs.ws.service;

import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.bitstamp.BitstampExchange;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.IOException;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class XChange {

    private MarketDataService marketDataService = ExchangeFactory.INSTANCE.createExchange(BitstampExchange.class.getName()).getMarketDataService(); // BitstampMarketDataService

    public Ticker getMarketData(@NotNull CurrencyPair pair) throws IOException {
        return marketDataService.getTicker(pair);
    }

}
