package com.ubs.ws.service;

import org.knowm.xchange.currency.CurrencyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping(path = "alert")
@CrossOrigin
public class RestService {

    private static final Logger logger = LoggerFactory.getLogger(RestService.class);

    @Autowired
    private Checker checker;

    @RequestMapping(params = {"pair", "limit"}, method = RequestMethod.PUT, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> setsAlert(@RequestParam("pair") String pair, @RequestParam("limit") Double limit) {

        logger.info("HTTP PUT /alert?pair=" + pair + "&limit=" + limit);
        try {
            CurrencyPair cp = new CurrencyPair(pair.replaceAll("[_-]", "/"));
            checker.putPair(cp, limit);

            String response = HtmlUtils.htmlEscape(cp + " Added");
            logger.debug("setsAlert: " + response);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    //curl -X PUT -d 'pair=BTC_USD&limit=500' http://localhost:8080/alert

    @RequestMapping(params = {"pair"}, method = RequestMethod.DELETE, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> removeAlert(@RequestParam("pair") String pair) {

        logger.info("HTTP DELETE /alert?pair=" + pair);
        try {
            CurrencyPair cp = new CurrencyPair(pair.replaceAll("[_-]", "/"));
            String response = HtmlUtils.htmlEscape(cp + " Deleted");

            if (checker.pairsContainsKey(cp))
                checker.removePair(cp);
            else
                response = HtmlUtils.htmlEscape(cp + " Not found");

            logger.debug("removeAlert: " + response);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    //curl -X DELETE -d 'pair=BTC_USD' http://localhost:8080/alert

}

