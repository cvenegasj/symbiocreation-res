package com.simbiocreacion.resource.service;

import com.opencsv.CSVWriter;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvException;
import com.simbiocreacion.resource.model.Symbiocreation;
import com.simbiocreacion.resource.model.User;
import com.simbiocreacion.resource.repository.SymbiocreationRepository;
import com.simbiocreacion.resource.util.ByteArrayInOutStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class SymbiocreationService implements ISymbiocreationService {

    @Autowired
    private SymbiocreationRepository symbioRepository; // like the JPA EntityManager wrapper w find-get/save/delete/update operations

    @Override
    public Mono<Symbiocreation> create(Symbiocreation e) {
        return symbioRepository.save(e); //.subscribe() ??
    }

    @Override
    public Mono<Symbiocreation> findById(String id) {
        return symbioRepository.findById(id);
    }

    @Override
    public Flux<Symbiocreation> findAll() {
        return symbioRepository.findAll();
    }

    @Override
    public Flux<Symbiocreation> findByUserId(String userId) {
        return symbioRepository.findByUserId(userId);
    }

    @Override
    public Flux<Symbiocreation> findAllByUser(String userId) {
        return symbioRepository.findAllByUser(userId);
    }

    @Override
    public Flux<Symbiocreation> findAllByVisibility(String visibility) {
        return symbioRepository.findAllByVisibility(visibility);
    }

    @Override
    public Flux<Symbiocreation> findUpcomingByVisibility(String visibility, Date now) {
        return symbioRepository.findUpcomingByVisibility(visibility, now);
    }

    @Override
    public Flux<Symbiocreation> findPastByVisibility(String visibility, Date now) {
        return symbioRepository.findPastByVisibility(visibility, now);
    }

    @Override
    public Mono<Symbiocreation> update(Symbiocreation e) {
        return symbioRepository.save(e);
    }

    @Override
    public Mono<Void> delete(String id) {
        return symbioRepository.deleteById(id);
    }

    @Override
    public Mono<Void> deleteAll() {
        return symbioRepository.deleteAll();
    }

    // CSV writer
    public Mono<ByteArrayInputStream> generateCsv(List<User> users){
        String[] columns = {"Id", "Name", "FirstName", "LastName", "Email"};

        return Mono.fromCallable(() -> {
            try {
                ByteArrayInOutStream stream = new ByteArrayInOutStream();
                OutputStreamWriter streamWriter = new OutputStreamWriter(stream);
                CSVWriter writer = new CSVWriter(streamWriter);

                ColumnPositionMappingStrategy mappingStrategy = new ColumnPositionMappingStrategy();
                mappingStrategy.setType(User.class);
                mappingStrategy.setColumnMapping(columns);
                writer.writeNext(columns);

                StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder(writer)
                        .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                        .withMappingStrategy(mappingStrategy)
                        .withSeparator(',')
                        .build();

                beanToCsv.write(users);
                streamWriter.flush();
                return stream.getInputStream();
            }
            catch (CsvException | IOException e) {
                throw new RuntimeException(e);
            }

        }).subscribeOn(Schedulers.boundedElastic());
    }

}
