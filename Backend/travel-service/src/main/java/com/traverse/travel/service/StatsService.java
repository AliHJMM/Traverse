package com.traverse.travel.service;

import com.traverse.travel.dto.AdminOverviewResponse;
import com.traverse.travel.dto.ManagerStatsResponse;
import com.traverse.travel.dto.MonthlyIncome;
import com.traverse.travel.dto.TravelStatSummary;
import com.traverse.travel.dto.TravelerStatsResponse;
import com.traverse.travel.entity.Feedback;
import com.traverse.travel.entity.ReportStatus;
import com.traverse.travel.entity.ReportSubjectType;
import com.traverse.travel.entity.Subscription;
import com.traverse.travel.entity.SubscriptionStatus;
import com.traverse.travel.entity.Travel;
import com.traverse.travel.repository.FeedbackRepository;
import com.traverse.travel.repository.ReportRepository;
import com.traverse.travel.repository.SubscriptionRepository;
import com.traverse.travel.repository.TravelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Read-only aggregation of the per-role dashboards. Kept out of TravelService
 * so the write-path stays lean; all numbers are derived from the travel,
 * subscription, feedback and report tables.
 */
@Service
@Transactional(readOnly = true)
public class StatsService {

    private static final int TOP_N = 5;
    private static final int MONTHS = 6;

    private final TravelRepository travelRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final FeedbackRepository feedbackRepository;
    private final ReportRepository reportRepository;

    public StatsService(TravelRepository travelRepository, SubscriptionRepository subscriptionRepository,
                        FeedbackRepository feedbackRepository, ReportRepository reportRepository) {
        this.travelRepository = travelRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.feedbackRepository = feedbackRepository;
        this.reportRepository = reportRepository;
    }

    public ManagerStatsResponse managerStats(Long managerId) {
        List<Travel> travels = travelRepository.findByManagerId(managerId);
        long activeTravelers = 0;
        BigDecimal income = BigDecimal.ZERO;
        for (Travel t : travels) {
            long subs = subscriptionRepository.countByTravelIdAndStatus(t.getId(), SubscriptionStatus.SUBSCRIBED);
            activeTravelers += subs;
            income = income.add(t.getPrice().multiply(BigDecimal.valueOf(subs)));
        }
        List<Long> ids = travels.stream().map(Travel::getId).toList();
        List<Feedback> feedback = ids.isEmpty() ? List.of() : feedbackRepository.findByTravelIdIn(ids);
        double avgRating = feedback.stream().mapToInt(Feedback::getRating).average().orElse(0.0);
        long reports = reportRepository.countBySubjectTypeAndSubjectId(ReportSubjectType.MANAGER, managerId);

        return new ManagerStatsResponse(managerId, travels.size(), activeTravelers, income,
                round(avgRating), feedback.size(), reports);
    }

    public TravelerStatsResponse travelerStats(Long travelerId) {
        long active = subscriptionRepository.countByTravelerIdAndStatus(travelerId, SubscriptionStatus.SUBSCRIBED);
        long cancelled = subscriptionRepository.countByTravelerIdAndStatus(travelerId, SubscriptionStatus.CANCELLED);
        long feedbackGiven = feedbackRepository.findByTravelerId(travelerId).size();
        long reportsFiled = reportRepository.findByReporterId(travelerId).size();
        return new TravelerStatsResponse(travelerId, active, cancelled, feedbackGiven, reportsFiled);
    }

    public AdminOverviewResponse adminOverview() {
        List<Travel> travels = travelRepository.findAll();
        List<Long> ids = travels.stream().map(Travel::getId).toList();

        // Average rating per travel, computed once for the whole platform.
        Map<Long, Double> avgByTravel = (ids.isEmpty() ? List.<Feedback>of() : feedbackRepository.findByTravelIdIn(ids))
                .stream()
                .collect(Collectors.groupingBy(Feedback::getTravelId,
                        Collectors.averagingInt(Feedback::getRating)));

        long totalActiveSubs = 0;
        BigDecimal totalIncome = BigDecimal.ZERO;
        List<TravelStatSummary> travelSummaries = new java.util.ArrayList<>();
        for (Travel t : travels) {
            long subs = subscriptionRepository.countByTravelIdAndStatus(t.getId(), SubscriptionStatus.SUBSCRIBED);
            BigDecimal income = t.getPrice().multiply(BigDecimal.valueOf(subs));
            totalActiveSubs += subs;
            totalIncome = totalIncome.add(income);
            travelSummaries.add(new TravelStatSummary(t.getId(), t.getTitle(), t.getManagerId(), subs, income,
                    round(avgByTravel.getOrDefault(t.getId(), 0.0))));
        }

        List<Long> managerIds = travels.stream()
                .map(Travel::getManagerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<ManagerStatsResponse> topManagers = managerIds.stream()
                .map(this::managerStats)
                .sorted(Comparator.comparing(ManagerStatsResponse::totalIncome).reversed()
                        .thenComparing(Comparator.comparingDouble(ManagerStatsResponse::averageRating).reversed()))
                .limit(TOP_N)
                .toList();

        List<TravelStatSummary> topTravels = travelSummaries.stream()
                .sorted(Comparator.comparingLong(TravelStatSummary::subscribers).reversed())
                .limit(TOP_N)
                .toList();

        long openReports = reportRepository.countByStatus(ReportStatus.OPEN);

        return new AdminOverviewResponse(managerIds.size(), travels.size(), totalActiveSubs, totalIncome,
                openReports, topManagers, topTravels, monthlyIncome(travels));
    }

    /**
     * Income booked per calendar month over the last {@value MONTHS} months
     * (oldest first), derived from when each still-active subscription was
     * created times its travel's price. Months with no bookings are included
     * as zero so the dashboard renders a continuous series.
     */
    private List<MonthlyIncome> monthlyIncome(List<Travel> travels) {
        Map<Long, BigDecimal> priceByTravel = travels.stream()
                .collect(Collectors.toMap(Travel::getId, Travel::getPrice));

        YearMonth current = YearMonth.now(ZoneOffset.UTC);
        Map<YearMonth, BigDecimal> buckets = new LinkedHashMap<>();
        for (int i = MONTHS - 1; i >= 0; i--) {
            buckets.put(current.minusMonths(i), BigDecimal.ZERO);
        }

        for (Subscription sub : subscriptionRepository.findByStatus(SubscriptionStatus.SUBSCRIBED)) {
            BigDecimal price = priceByTravel.get(sub.getTravelId());
            if (price == null) {
                continue;
            }
            YearMonth ym = YearMonth.from(sub.getCreatedAt().atZone(ZoneOffset.UTC));
            buckets.computeIfPresent(ym, (k, v) -> v.add(price));
        }

        return buckets.entrySet().stream()
                .map(e -> new MonthlyIncome(e.getKey().toString(), e.getValue()))
                .toList();
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
