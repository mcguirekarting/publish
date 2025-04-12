package com.manhattan.reconciliation.repository.impl;

import com.manhattan.reconciliation.model.ReconciliationHistory;
import com.manhattan.reconciliation.repository.ReconciliationHistoryRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * In-memory implementation of ReconciliationHistoryRepository for development/testing.
 * Only active in the "dev" and "test" profiles.
 */
@Repository
@Qualifier("inMemoryReconciliationHistoryRepository")
@Profile({"dev", "test"})
public class InMemoryReconciliationHistoryRepository implements ReconciliationHistoryRepository {
    
    private final Map<String, ReconciliationHistory> historyMap = new HashMap<>();
    
    @Override
    public List<ReconciliationHistory> findByItemIdAndLocationId(String itemId, String locationId) {
        return historyMap.values().stream()
                .filter(h -> h.getItemId().equals(itemId) && h.getLocationId().equals(locationId))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ReconciliationHistory> findByResultId(Long resultId) {
        return historyMap.values().stream()
                .filter(h -> resultId.equals(h.getResultId()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ReconciliationHistory> findByReconciliationTimeAfter(LocalDateTime time) {
        return historyMap.values().stream()
                .filter(h -> h.getReconciliationTime().isAfter(time))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ReconciliationHistory> findByReconciliationTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return historyMap.values().stream()
                .filter(h -> !h.getReconciliationTime().isBefore(startTime) && !h.getReconciliationTime().isAfter(endTime))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ReconciliationHistory> findAll() {
        return new ArrayList<>(historyMap.values());
    }
    
    @Override
    public List<ReconciliationHistory> findAll(Sort sort) {
        return findAll();
    }
    
    @Override
    public Page<ReconciliationHistory> findAll(Pageable pageable) {
        throw new UnsupportedOperationException("Pagination not supported in in-memory repository");
    }
    
    @Override
    public List<ReconciliationHistory> findAllById(Iterable<String> ids) {
        List<ReconciliationHistory> result = new ArrayList<>();
        for (String id : ids) {
            ReconciliationHistory history = historyMap.get(id);
            if (history != null) {
                result.add(history);
            }
        }
        return result;
    }
    
    @Override
    public long count() {
        return historyMap.size();
    }
    
    @Override
    public void deleteById(String id) {
        historyMap.remove(id);
    }
    
    @Override
    public void delete(ReconciliationHistory entity) {
        historyMap.remove(entity.getId());
    }
    
    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        for (String id : ids) {
            historyMap.remove(id);
        }
    }
    
    @Override
    public void deleteAll(Iterable<? extends ReconciliationHistory> entities) {
        for (ReconciliationHistory history : entities) {
            historyMap.remove(history.getId());
        }
    }
    
    @Override
    public void deleteAll() {
        historyMap.clear();
    }
    
    @Override
    public <S extends ReconciliationHistory> S save(S entity) {
        historyMap.put(entity.getId(), entity);
        return entity;
    }
    
    @Override
    public <S extends ReconciliationHistory> List<S> saveAll(Iterable<S> entities) {
        List<S> result = new ArrayList<>();
        for (S entity : entities) {
            result.add(save(entity));
        }
        return result;
    }
    
    @Override
    public Optional<ReconciliationHistory> findById(String id) {
        return Optional.ofNullable(historyMap.get(id));
    }
    
    @Override
    public boolean existsById(String id) {
        return historyMap.containsKey(id);
    }
    
    @Override
    public void flush() {
        // No-op for in-memory repository
    }
    
    @Override
    public <S extends ReconciliationHistory> S saveAndFlush(S entity) {
        return save(entity);
    }
    
    @Override
    public <S extends ReconciliationHistory> List<S> saveAllAndFlush(Iterable<S> entities) {
        return saveAll(entities);
    }
    
    @Override
    public void deleteAllInBatch(Iterable<ReconciliationHistory> entities) {
        deleteAll(entities);
    }
    
    @Override
    public void deleteAllByIdInBatch(Iterable<String> ids) {
        deleteAllById(ids);
    }
    
    @Override
    public void deleteAllInBatch() {
        deleteAll();
    }
    
    @Override
    public ReconciliationHistory getOne(String id) {
        return findById(id).orElse(null);
    }
    
    @Override
    public ReconciliationHistory getById(String id) {
        return findById(id).orElse(null);
    }
    
    @Override
    public ReconciliationHistory getReferenceById(String id) {
        return getById(id);
    }
    
    @Override
    public <S extends ReconciliationHistory> Optional<S> findOne(Example<S> example) {
        throw new UnsupportedOperationException("Example queries not supported in in-memory repository");
    }
    
    @Override
    public <S extends ReconciliationHistory> List<S> findAll(Example<S> example) {
        throw new UnsupportedOperationException("Example queries not supported in in-memory repository");
    }
    
    @Override
    public <S extends ReconciliationHistory> List<S> findAll(Example<S> example, Sort sort) {
        throw new UnsupportedOperationException("Example queries not supported in in-memory repository");
    }
    
    @Override
    public <S extends ReconciliationHistory> Page<S> findAll(Example<S> example, Pageable pageable) {
        throw new UnsupportedOperationException("Example queries not supported in in-memory repository");
    }
    
    @Override
    public <S extends ReconciliationHistory> long count(Example<S> example) {
        throw new UnsupportedOperationException("Example queries not supported in in-memory repository");
    }
    
    @Override
    public <S extends ReconciliationHistory> boolean exists(Example<S> example) {
        throw new UnsupportedOperationException("Example queries not supported in in-memory repository");
    }
    
    @Override
    public <S extends ReconciliationHistory, R> R findBy(Example<S> example, 
            Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        throw new UnsupportedOperationException("Example queries not supported in in-memory repository");
    }
}