package com.ubs.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubs.ws.model.Message;
import com.ubs.ws.service.Checker;
import com.ubs.ws.service.XChange;
import com.ubs.ws.utils.WsTestUtils.MyStompFrameHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.ubs.ws.utils.WsTestUtils.MyStompSessionHandler;
import static com.ubs.ws.utils.WsTestUtils.createWebSocketClient;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ActiveProfiles(value = "test")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
public class WsIntegrationTest {

    private final String host = "127.0.0.1";
    private final CurrencyPair currency = CurrencyPair.BTC_USD;
    @Value("${local.server.port}")
    private int port;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private XChange xService;
    @Autowired
    private Checker checker;
    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private Date date;
    private CopyOnWriteArrayList<Message> messages = new CopyOnWriteArrayList<>();
    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Before
    public void setUp() throws Exception {
        date = new Date();
        String url = "ws://" + host + ":" + port + "/alerts";
        stompClient = createWebSocketClient();
        stompSession = stompClient.connect(url, new MyStompSessionHandler()).get();

        Ticker.Builder builder = new Ticker.Builder();
        builder.last(new BigDecimal(2D));
        builder.timestamp(date);
        builder.currencyPair(currency);
        Ticker ticker = builder.build();
        when(xService.getMarketData(any())).thenAnswer((Answer<Ticker>) invocation -> ticker);

    }

    @After
    public void tearDown() {
        stompSession.disconnect();
        stompClient.stop();
    }

    @Test
    public void getMockedMarketData() throws IOException {

        Ticker.Builder builder = new Ticker.Builder();
        builder.last(new BigDecimal(2D));
        builder.timestamp(date);
        builder.currencyPair(currency);
        Ticker expected = builder.build();

        Ticker received = xService.getMarketData(currency);

        assertEquals(expected.getCurrencyPair(), received.getCurrencyPair());
        assertEquals(expected.getLast(), received.getLast());
        assertEquals(expected.getTimestamp(), received.getTimestamp());
    }

    @Test
    public void testREST() throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        //TESTING HTTP PUT
        assertTrue(checker.isPairsEmpty());

        HttpUriRequest request = new HttpPut("http://" + host + ":" + port + "/alert?limit=500&pair=" + currency);

        CloseableHttpResponse httpResponse = httpClient.execute(request);
        assertEquals(200, httpResponse.getStatusLine().getStatusCode());
        assertEquals(currency + " Added", EntityUtils.toString(httpResponse.getEntity()));
        assertFalse(checker.isPairsEmpty());
        assertTrue(checker.pairsContainsKey(currency));

        //TESTING HTTP DELETE
        request = new HttpDelete("http://" + host + ":" + port + "/alert?pair=" + currency);

        httpResponse = httpClient.execute(request);
        assertEquals(200, httpResponse.getStatusLine().getStatusCode());
        assertEquals(currency + " Deleted", EntityUtils.toString(httpResponse.getEntity()));
        assertTrue(checker.isPairsEmpty());
    }

    @Test
    public void webSocketTest() throws IOException {
        assertTrue(stompSession.isConnected());

        Executors.newSingleThreadExecutor().execute(() ->
        {
            try {
                subscribe();
            } catch (InterruptedException | ExecutionException | IOException | TimeoutException e) {
                e.printStackTrace();
            }
        });

        assertTrue(messages.isEmpty());
        checker.putPair(currency, 1D);

        long endTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(1L, TimeUnit.MINUTES);
        while (messages.size() == 0 && System.nanoTime() < endTime) {
        }

        assertTrue(messages.size() > 0);

        Message sentMessage = new Message(currency, 2D, date);
        Message recededMessage = messages.remove(0);
        assertEquals(sentMessage, recededMessage);
    }

    @Test
    public void integrationTest() throws IOException {
        Ticker ticker = xService.getMarketData(currency);
        assertNotNull(ticker);

        double last = ticker.getLast().doubleValue();
        assertTrue(last > 0);

        Executors.newSingleThreadExecutor().execute(() ->
        {
            try {
                subscribe();
            } catch (InterruptedException | ExecutionException | IOException | TimeoutException e) {
                e.printStackTrace();
            }
        });

        assertTrue(checker.isPairsEmpty());

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpUriRequest request = new HttpPut("http://" + host + ":" + port + "/alert?limit=" + (last - 1) + "&pair=" + currency);
        CloseableHttpResponse httpResponse = httpClient.execute(request);
        assertEquals(200, httpResponse.getStatusLine().getStatusCode());
        assertEquals(currency + " Added", EntityUtils.toString(httpResponse.getEntity()));

        long endTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(1L, TimeUnit.MINUTES);
        while (messages.size() == 0 && System.nanoTime() < endTime) {
        }

        assertTrue(messages.size() > 0);

        Message expected = new Message(currency, last, date);
        Message received = messages.remove(0);

        assertEquals(expected.getCurrencyPair(), received.getCurrencyPair());
        assertEquals(expected.getLimit(), received.getLimit(), Double.NaN);
    }

    private void subscribe() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        CompletableFuture<String> resultKeeper = new CompletableFuture<>();

        stompSession.subscribe("/topic/public",
                new MyStompFrameHandler(resultKeeper::complete));
        String res = resultKeeper.get(1, MINUTES);
        Message m = objectMapper.readValue(res, Message.class);
        messages.add(m);
    }
}