package com.traverse.travel.controller;

import com.traverse.travel.dto.AdminOverviewResponse;
import com.traverse.travel.dto.ManagerStatsResponse;
import com.traverse.travel.dto.TravelerStatsResponse;
import com.traverse.travel.security.AuthenticatedUser;
import com.traverse.travel.service.StatsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/travels/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /** Admin dashboard: platform KPIs + leaderboards. */
    @GetMapping("/admin/overview")
    public AdminOverviewResponse adminOverview() {
        return statsService.adminOverview();
    }

    /** The current Travel Manager's own performance dashboard. */
    @GetMapping("/manager/me")
    public ManagerStatsResponse myManagerStats(@AuthenticationPrincipal AuthenticatedUser principal) {
        return statsService.managerStats(principal.id());
    }

    /** Public manager snapshot -- a traveler can inspect a manager before joining. */
    @GetMapping("/manager/{managerId}")
    public ManagerStatsResponse managerStats(@PathVariable Long managerId) {
        return statsService.managerStats(managerId);
    }

    /** The current Traveler's personal dashboard. */
    @GetMapping("/traveler/me")
    public TravelerStatsResponse myTravelerStats(@AuthenticationPrincipal AuthenticatedUser principal) {
        return statsService.travelerStats(principal.id());
    }
}
