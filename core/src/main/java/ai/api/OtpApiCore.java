package ai.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
public class OtpApiCore {
    RestClient otpRestClient;

    ObjectMapper objectMapper;

    public <T> T get(String endPoint, MultiValueMap<String,String> params, ParameterizedTypeReference<T> responseType){
        return otpRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(endPoint)
                        .queryParams(params!=null ? params : new LinkedMultiValueMap<>())
                        .build())
                .retrieve()
                .onStatus(status -> status.value() == 401, (request, response) -> {
                    try {
                        handle401();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .onStatus(status -> status.value() == 500, (request, response) -> {
                    handle500();
                })
                .body(responseType);
    }

    public <T> T post(String endPoint, Object body, ParameterizedTypeReference<T> responseType) throws JsonProcessingException {
        return otpRestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(endPoint)
                        .build())
                .body(objectMapper.writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status.value() == 401, (request, response) -> {
                    try {
                        handle401();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .onStatus(status -> status.value() == 500, (request, response) -> {
                    handle500();
                })
                .body(responseType);
    }

    public <T> T get(String endPoint, ParameterizedTypeReference<T> responseType){
        return get(endPoint, null, responseType);
    }

    private void handle401() throws Exception {
        System.out.println("handling 401");
    }

    private void handle500(){

    }
}
