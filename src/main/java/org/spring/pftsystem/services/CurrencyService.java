package org.spring.pftsystem.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@Component
public class CurrencyService {

    @Value("${currency.exchange.api.url}")
    private String API_URL;
    @Value("${currency.exchange.api.key}")
    private String API_KEY;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> convertCurrency(String from, String to, double amount) {
        String url = UriComponentsBuilder.fromUriString(API_URL)
                .queryParam("api_key", API_KEY)
                .queryParam("from", from)
                .queryParam("to", to)
                .queryParam("amount", amount)
                .toUriString();

        return restTemplate.getForObject(url, Map.class);
    }
}
