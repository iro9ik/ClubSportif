package com.clubsportif.dao;

import com.clubsportif.model.Member;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;

/**
 * Reactive wrapper for MemberDAO using Project Reactor.
 * All database operations are executed on a bounded elastic scheduler
 * to avoid blocking the main thread.
 */
public class ReactiveMemberDAO {

    private final MemberDAO memberDAO;

    public ReactiveMemberDAO() {
        this.memberDAO = new MemberDAO();
    }

    public ReactiveMemberDAO(MemberDAO memberDAO) {
        this.memberDAO = memberDAO;
    }

    /**
     * Get all members as a Flux.
     */
    public Flux<Member> getAllMembers() {
        return Mono.fromCallable(memberDAO::getAllMembers)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    /**
     * Get member by ID.
     */
    public Mono<Member> getMemberById(int id) {
        return Mono.fromCallable(() -> memberDAO.getMemberById(id))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Get member by user ID.
     * Returns empty Mono if member not found.
     */
    public Mono<Member> getMemberByUserId(int userId) {
        return Mono.fromCallable(() -> memberDAO.getMemberByUserId(userId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(member -> member != null ? Mono.just(member) : Mono.empty());
    }

    /**
     * Create a new member.
     */
    public Mono<Void> createMember(Member member) {
        return Mono.fromRunnable(() -> memberDAO.createMember(member))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * Update a member.
     */
    public Mono<Void> updateMember(Member member) {
        return Mono.fromRunnable(() -> memberDAO.updateMember(member))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * Delete a member.
     */
    public Mono<Void> deleteMember(int id) {
        return Mono.fromRunnable(() -> memberDAO.deleteMember(id))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * Update member statuses based on expiration dates.
     */
    public Mono<Void> updateMemberStatuses() {
        return Mono.fromRunnable(memberDAO::updateMemberStatuses)
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * Get count of all members.
     */
    public Mono<Integer> getTotalMemberCount() {
        return Mono.fromCallable(() -> memberDAO.getAllMembers().size())
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Get count of active members.
     */
    public Mono<Long> getActiveMemberCount() {
        return Mono.fromCallable(() -> {
            List<Member> members = memberDAO.getAllMembers();
            return members.stream()
                    .filter(m -> "ACTIVE".equals(m.getStatus()))
                    .count();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Watch for member changes using polling.
     * Emits the full list of members at the specified interval.
     */
    public Flux<List<Member>> watchMembers(Duration interval) {
        return Flux.interval(interval)
                .flatMap(tick -> Mono.fromCallable(memberDAO::getAllMembers)
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * Get the underlying DAO for direct access if needed.
     */
    public MemberDAO getDao() {
        return memberDAO;
    }
}
