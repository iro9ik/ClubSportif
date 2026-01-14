package com.clubsportif.service;

import com.clubsportif.dao.ReactiveMemberDAO;
import com.clubsportif.dao.ReactiveRequestDAO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Service for providing reactive statistics streams.
 */
public class ReactiveStatsService {

    private final ReactiveMemberDAO reactiveMemberDAO;
    private final ReactiveRequestDAO reactiveRequestDAO;

    public ReactiveStatsService() {
        this.reactiveMemberDAO = new ReactiveMemberDAO();
        this.reactiveRequestDAO = new ReactiveRequestDAO();
    }

    public ReactiveStatsService(ReactiveMemberDAO memberDAO, ReactiveRequestDAO requestDAO) {
        this.reactiveMemberDAO = memberDAO;
        this.reactiveRequestDAO = requestDAO;
    }

    /**
     * Get current dashboard statistics.
     */
    public Mono<DashboardStats> getCurrentStats() {
        return Mono.zip(
            reactiveMemberDAO.getTotalMemberCount(),
            reactiveMemberDAO.getActiveMemberCount(),
            reactiveRequestDAO.getDailyRequestsCount()
        ).map(tuple -> new DashboardStats(
            tuple.getT1(),
            tuple.getT2().intValue(),
            tuple.getT3()
        ));
    }

    /**
     * Watch dashboard statistics at the specified interval.
     * First ensures member statuses are updated before fetching stats.
     */
    public Flux<DashboardStats> watchStats(Duration interval) {
        return Flux.interval(Duration.ZERO, interval)
                .flatMap(tick -> reactiveMemberDAO.updateMemberStatuses()
                        .then(getCurrentStats()));
    }

    /**
     * Dashboard statistics data class.
     */
    public static class DashboardStats {
        private final int totalMembers;
        private final int activeMembers;
        private final int dailyRequests;

        public DashboardStats(int totalMembers, int activeMembers, int dailyRequests) {
            this.totalMembers = totalMembers;
            this.activeMembers = activeMembers;
            this.dailyRequests = dailyRequests;
        }

        public int getTotalMembers() { return totalMembers; }
        public int getActiveMembers() { return activeMembers; }
        public int getDailyRequests() { return dailyRequests; }

        @Override
        public String toString() {
            return "DashboardStats{" +
                    "totalMembers=" + totalMembers +
                    ", activeMembers=" + activeMembers +
                    ", dailyRequests=" + dailyRequests +
                    '}';
        }
    }
}
