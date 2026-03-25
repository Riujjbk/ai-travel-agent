package com.ui.ailvyou.rag;

import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;


public class TravelAppQueryTransformer implements QueryTransformer {

    private final String apiKey;

    public TravelAppQueryTransformer(String apiKey) {
        this.apiKey = apiKey;
    }
    @Override
    public Query transform(Query query) {


//        String expandedQuery = callQueryExpansionApi(query.text())
//                .block(); // 在实际应用中可能需要更优雅的异步处理






        return null;
    }

//    private Mono<String> callQueryExpansionApi(String originalQuery) {
//        return webClient.post()
//                .uri("/query/expand")
//                .header("Authorization", "Bearer " + apiKey)
//                .bodyValue(Map.of(
//                        "query", originalQuery,
//                        "domain", "travel"
//                ))
//                .retrieve()
//                .bodyToMono(QueryExpansionResponse.class)
//                .map(QueryExpansionResponse::expandedQuery);
//    }
//

}
