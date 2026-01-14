package com.clubsportif.dao;

import com.clubsportif.model.Request;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;

/**
 * Reactive wrapper for RequestDAO using Project Reactor.
 * All database operations are executed on a bounded elastic scheduler
 * to avoid blocking the main thread.
 */
public class ReactiveRequestDAO {

    private final RequestDAO requestDAO;

    public ReactiveRequestDAO() {
        this.requestDAO = new RequestDAO();
    }

    public ReactiveRequestDAO(RequestDAO requestDAO) {
        this.requestDAO = requestDAO;
    }

    /**
     * Get all requests as a Flux.
     */
    public Flux<Request> getAllRequests() {
        return Mono.fromCallable(requestDAO::getAllRequests)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    /**
     * Get pending requests only.
     */
    public Flux<Request> getPendingRequests() {
        return getAllRequests()
                .filter(r -> "PENDING".equals(r.getStatus()));
    }

    /**
     * Get requests by user ID.
     */
    public Flux<Request> getRequestsByUserId(int userId) {
        return Mono.fromCallable(() -> requestDAO.getRequestsByUserId(userId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    /**
     * Create a new request.
     */
    public Mono<Void> createRequest(Request request) {
        return Mono.fromRunnable(() -> requestDAO.createRequest(request))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * Update request status.
     */
    public Mono<Void> updateRequestStatus(int requestId, String status) {
        return Mono.fromRunnable(() -> requestDAO.updateRequestStatus(requestId, status))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * Delete a request.
     */
    public Mono<Void> deleteRequest(int id) {
        return Mono.fromRunnable(() -> requestDAO.deleteRequest(id))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * Get daily requests count.
     */
    public Mono<Integer> getDailyRequestsCount() {
        return Mono.fromCallable(requestDAO::getDailyRequestsCount)
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Check if user has active (pending) request.
     */
    public Mono<Boolean> hasActiveRequest(int userId) {
        return Mono.fromCallable(() -> requestDAO.hasActiveRequest(userId))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Watch for request changes using polling.
     * Emits the full list of requests at the specified interval.
     */
    public Flux<List<Request>> watchRequests(Duration interval) {
        return Flux.interval(interval)
                .flatMap(tick -> Mono.fromCallable(requestDAO::getAllRequests)
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * Get the underlying DAO for direct access if needed.
     */
    public RequestDAO getDao() {
        return requestDAO;
    }
}
