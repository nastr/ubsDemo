package com.ubs.ws.model;

import org.knowm.xchange.currency.CurrencyPair;

import java.util.Date;

public class Message {

    private CurrencyPair currencyPair;
    private double limit;
    private Date timestamp;

    public Message(CurrencyPair currencyPair, double limit, Date timestamp) {
        this.currencyPair = currencyPair;
        this.limit = limit;
        this.timestamp = timestamp;
    }

    public CurrencyPair getCurrencyPair() {
        return currencyPair;
    }

    public void setCurrencyPair(CurrencyPair currencyPair) {
        this.currencyPair = currencyPair;
    }

    public double getLimit() {
        return limit;
    }

    public void setLimit(double limit) {
        this.limit = limit;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Message{" +
                "currencyPair=" + currencyPair +
                ", limit=" + limit +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        if (Double.compare(message.limit, limit) != 0) return false;
        if (!currencyPair.equals(message.currencyPair)) return false;
        return timestamp.equals(message.timestamp);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = currencyPair.hashCode();
        temp = Double.doubleToLongBits(limit);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + timestamp.hashCode();
        return result;
    }
}
