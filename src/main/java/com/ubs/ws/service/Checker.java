package com.ubs.ws.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubs.ws.model.Message;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class Checker {

    private static final Logger logger = LoggerFactory.getLogger(Checker.class);

    @Autowired
    XChange xChange;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    private boolean runningNow;
    private Map<CurrencyPair, Double> pairs = new ConcurrentHashMap<>();

    public void putPair(CurrencyPair key, Double last) {
        pairs.put(key, last);
        if (!runningNow) {
            runningNow = true;
            Executors.newSingleThreadExecutor().execute(this::observingCurrencys);
        }
    }

    void removePair(CurrencyPair key) {
        pairs.remove(key);
    }

    private void observingCurrencys() {
        while (pairs.size() > 0) {
            pairs.forEach((key, value) -> {
                try {
                    Ticker ticker = xChange.getMarketData(key);
                    if (ticker.getLast().doubleValue() > value) {
                        Message event = new Message(ticker.getCurrencyPair(), ticker.getLast().doubleValue(), ticker.getTimestamp());
                        String response = objectMapper.writeValueAsString(event);
                        logger.info("event: " + response);
                        messagingTemplate.convertAndSend("/topic/public", response);
                        pairs.remove(key);
                    } else
                        logger.debug("ticker: " + ticker.toString());
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            });
        }
        runningNow = false;
    }

    public boolean pairsContainsKey(CurrencyPair cp) {
        return pairs.containsKey(cp);
    }

    public boolean isPairsEmpty() {
        return pairs.isEmpty();
    }
}
