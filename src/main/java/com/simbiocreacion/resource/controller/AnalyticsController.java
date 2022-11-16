package com.simbiocreacion.resource.controller;

import com.simbiocreacion.resource.service.ISymbiocreationService;
import com.simbiocreacion.resource.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bson.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Log4j2
public class AnalyticsController {

    private final ISymbiocreationService symbiocreationService;

    private final IUserService userService;

    @GetMapping("/analytics/counts-summary")
    public Mono<Map<String, Long>> getCountsSummary() {
        final Map<String, Long> counts = new HashMap<>();

        return Mono.zip(
                symbiocreationService.count(),
                userService.count(),
                symbiocreationService.countIdeasAll()
        ).map(tuple3 -> {
            counts.put("symbiocreations", tuple3.getT1());
            counts.put("users", tuple3.getT2());
            counts.put("ideas", tuple3.getT3());

            return counts;
        }).defaultIfEmpty(new HashMap<>());
    }

    @GetMapping("/analytics/symbio-counts-daily-chart")
    public Flux<Document> getSymbioCountsDaily() {

        return symbiocreationService.groupAndCountByDate()
                .map(document -> {
                    if (document.getString("_id") == null) {
                        document.append("_id", "2022-10-10");
                    }
                    return document;
                });

//                .map(document -> document.getString("_id") != null ?
//                        Map.entry(document.getString("_id"), document.getInteger("count")) :
//                        Map.entry("2022-11-10", document.getInteger("count")))
////                .sort((entry1, entry2) -> {
////                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
////                    Date date1 = null;
////                    Date date2 = null;
////
////                    if (entry1.getKey().equals("null") || entry2.getKey().equals("null")) return 0;
////
////                    try {
////                        date1 = sdf.parse(entry1.getKey());
////                        date2 = sdf.parse(entry2.getKey());
////                    } catch (ParseException e) {
////                        e.printStackTrace();
////                        //throw new RuntimeException(e);
////                    }
////
////                    return date1.compareTo(date2);
////                })
//                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    @GetMapping("/analytics/user-counts-daily-chart")
    public Flux<Document> getUserCountsDaily() {

        return userService.groupAndCountByDate()
                .map(document -> {
                    if (document.getString("_id") == null) {
                        document.append("_id", "2022-10-10");
                    }
                    return document;
                });

//                .map(document -> document.getString("_id") != null ?
//                        Map.entry(document.getString("_id"), document.getInteger("count")) :
//                        Map.entry("2022-11-10", document.getInteger("count")))
//                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }
}
