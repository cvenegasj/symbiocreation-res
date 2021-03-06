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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
    public Flux<Symbiocreation> findAllByUser(String userId, Pageable pageable) {
        return symbioRepository.findAllByUser(userId, pageable);
    }

    @Override
    public Flux<Symbiocreation> findByVisibilityOrderByLastModifiedDesc(String visibility, Pageable pageable) {
        return symbioRepository.findByVisibilityOrderByLastModifiedDesc(visibility, pageable);
    }

    @Override
    public Flux<Symbiocreation> findByVisibilityAndDateTimeLessThanEqual(String visibility, Date now, Pageable pageable) {
        return symbioRepository.findByVisibilityAndDateTimeLessThanEqual(visibility, now, pageable);
    }

    @Override
    public Flux<Symbiocreation> findByVisibilityAndDateTimeGreaterThanEqual(String visibility, Date now, Pageable pageable) {
        return symbioRepository.findByVisibilityAndDateTimeGreaterThanEqual(visibility, now, pageable);
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

    public Mono<Long> countByVisibility(String visibility) {
        return symbioRepository.countByVisibility(visibility);
    }

    public Mono<Long> countByVisibilityAndDateTimeLessThanEqual(String visibility, Date dateTime) {
        return symbioRepository.countByVisibilityAndDateTimeLessThanEqual(visibility, dateTime);
    }

    public Mono<Long> countByVisibilityAndDateTimeGreaterThanEqual(String visibility, Date dateTime) {
        return symbioRepository.countByVisibilityAndDateTimeGreaterThanEqual(visibility, dateTime);
    }

    public Mono<Long> countByUser(String userId) {
        return symbioRepository.countByParticipantsU_id(userId);
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
