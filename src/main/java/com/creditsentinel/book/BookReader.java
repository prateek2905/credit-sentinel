package com.creditsentinel.book;

import com.creditsentinel.config.CreditProperties;
import com.creditsentinel.domain.Borrower;
import com.creditsentinel.domain.CaseStatus;
import com.creditsentinel.domain.Covenant;
import com.creditsentinel.domain.CovenantReading;
import com.creditsentinel.domain.Facility;
import com.creditsentinel.domain.RatingEvent;
import com.creditsentinel.domain.Sector;
import com.creditsentinel.domain.math.CovenantMath;
import com.creditsentinel.domain.math.ExposureMath;
import com.creditsentinel.domain.math.PortfolioShare;
import com.creditsentinel.domain.math.RatingWatch;
import com.creditsentinel.detection.CovenantSnapshot;
import com.creditsentinel.detection.FacilitySnapshot;
import com.creditsentinel.repo.BorrowerRepository;
import com.creditsentinel.repo.CovenantReadingRepository;
import com.creditsentinel.repo.CovenantRepository;
import com.creditsentinel.repo.CreditCaseRepository;
import com.creditsentinel.repo.CreditMemoRepository;
import com.creditsentinel.repo.FacilityRepository;
import com.creditsentinel.repo.RatingEventRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookReader {

    private final BorrowerRepository borrowers;
    private final FacilityRepository facilities;
    private final CovenantRepository covenants;
    private final CovenantReadingRepository readings;
    private final RatingEventRepository ratings;
    private final CreditCaseRepository cases;
    private final CreditMemoRepository memos;
    private final CreditProperties properties;

    public BookReader(
            BorrowerRepository borrowers,
            FacilityRepository facilities,
            CovenantRepository covenants,
            CovenantReadingRepository readings,
            RatingEventRepository ratings,
            CreditCaseRepository cases,
            CreditMemoRepository memos,
            CreditProperties properties) {
        this.borrowers = borrowers;
        this.facilities = facilities;
        this.covenants = covenants;
        this.readings = readings;
        this.ratings = ratings;
        this.cases = cases;
        this.memos = memos;
        this.properties = properties;
    }

    public Facility requireFacility(UUID facilityId) {
        return facilities.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("No facility for id " + facilityId));
    }

    public Borrower requireBorrower(UUID borrowerId) {
        return borrowers.findById(borrowerId)
                .orElseThrow(() -> new IllegalArgumentException("No borrower for id " + borrowerId));
    }

    public FacilitySnapshot snapshot(UUID facilityId) {
        Facility facility = requireFacility(facilityId);
        RatingHistoryView history = ratingHistory(facility.getBorrower().getId());
        return new FacilitySnapshot(
                facility.getId(),
                facility.getCommittedLimit(),
                facility.getOutstanding(),
                covenantSnapshots(facilityId),
                history.previous() == null ? null : com.creditsentinel.domain.Rating.valueOf(history.previous()),
                history.latest() == null ? null : com.creditsentinel.domain.Rating.valueOf(history.latest()));
    }

    public BorrowerProfileView borrowerProfile(UUID borrowerId) {
        Borrower borrower = requireBorrower(borrowerId);
        return new BorrowerProfileView(
                borrower.getId().toString(),
                borrower.getLegalName(),
                borrower.getSector().name(),
                borrower.getHeadquarters(),
                borrower.getRelationshipStart().toString());
    }

    public ExposureView exposure(UUID facilityId) {
        Facility facility = requireFacility(facilityId);
        BigDecimal utilization = ExposureMath.utilization(facility.getOutstanding(), facility.getCommittedLimit());
        return new ExposureView(
                facility.getBorrower().getId().toString(),
                facility.getId().toString(),
                facility.getFacilityType().name(),
                ExposureMath.money(facility.getCommittedLimit()),
                ExposureMath.money(facility.getOutstanding()),
                ExposureMath.ratio(utilization),
                ExposureMath.overLimit(facility.getOutstanding(), facility.getCommittedLimit()),
                facility.getCurrency());
    }

    public CovenantStatusView covenantStatus(UUID facilityId) {
        requireFacility(facilityId);
        List<CovenantLineView> lines = new ArrayList<>();
        for (Covenant covenant : covenants.findByFacilityId(facilityId)) {
            CovenantReading reading = latestReading(covenant);
            lines.add(new CovenantLineView(
                    covenant.getCovenantType().name(),
                    ExposureMath.ratio(reading.getThreshold()),
                    ExposureMath.ratio(reading.getActual()),
                    CovenantMath.breached(covenant.getCovenantType(), reading.getActual(), reading.getThreshold()),
                    CovenantMath.direction(covenant.getCovenantType())));
        }
        return new CovenantStatusView(facilityId.toString(), List.copyOf(lines));
    }

    public RatingHistoryView ratingHistory(UUID borrowerId) {
        requireBorrower(borrowerId);
        List<RatingEvent> events = ratings.findByBorrowerIdOrderByEffectiveDateAsc(borrowerId);
        List<RatingEventView> views = events.stream()
                .map(event -> new RatingEventView(event.getEffectiveDate().toString(), event.getRating().name()))
                .toList();
        String latest = events.isEmpty() ? null : events.get(events.size() - 1).getRating().name();
        String previous = events.size() < 2 ? null : events.get(events.size() - 2).getRating().name();
        boolean crossed = RatingWatch.crossedBelowInvestmentGrade(
                previous == null ? null : com.creditsentinel.domain.Rating.valueOf(previous),
                latest == null ? null : com.creditsentinel.domain.Rating.valueOf(latest));
        return new RatingHistoryView(borrowerId.toString(), views, latest, previous, crossed);
    }

    public SectorConcentrationView concentration(Sector sector) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal sectorOutstanding = BigDecimal.ZERO;
        for (Facility facility : facilities.findAll()) {
            total = total.add(facility.getOutstanding());
            if (facility.getBorrower().getSector() == sector) {
                sectorOutstanding = sectorOutstanding.add(facility.getOutstanding());
            }
        }
        BigDecimal share = PortfolioShare.share(sectorOutstanding, total);
        BigDecimal cap = properties.getConcentrationCap();
        return new SectorConcentrationView(
                sector.name(),
                ExposureMath.money(sectorOutstanding),
                ExposureMath.money(total),
                ExposureMath.ratio(share),
                ExposureMath.ratio(cap),
                PortfolioShare.aboveCap(share, cap));
    }

    public PriorCasesView priorCases(UUID borrowerId) {
        requireBorrower(borrowerId);
        List<PriorCaseView> views = cases
                .findByBorrowerIdAndStatusIn(borrowerId, EnumSet.of(CaseStatus.APPROVED, CaseStatus.REJECTED))
                .stream()
                .map(creditCase -> {
                    var memo = memos.findByCreditCaseId(creditCase.getId()).orElse(null);
                    return new PriorCaseView(
                            creditCase.getId().toString(),
                            creditCase.getCaseTrigger().name(),
                            creditCase.getStatus().name(),
                            memo == null ? null : memo.getRecommendation().name(),
                            memo == null ? null : memo.getSummary());
                })
                .toList();
        return new PriorCasesView(views.size(), views);
    }

    public List<Facility> allFacilities() {
        return facilities.findAll();
    }

    private List<CovenantSnapshot> covenantSnapshots(UUID facilityId) {
        List<CovenantSnapshot> snapshots = new ArrayList<>();
        for (Covenant covenant : covenants.findByFacilityId(facilityId)) {
            CovenantReading reading = latestReading(covenant);
            snapshots.add(new CovenantSnapshot(covenant.getCovenantType(), reading.getThreshold(), reading.getActual()));
        }
        return List.copyOf(snapshots);
    }

    private CovenantReading latestReading(Covenant covenant) {
        return readings.findFirstByCovenantIdOrderByAsOfDateDesc(covenant.getId())
                .orElseThrow(() -> new IllegalStateException("Covenant " + covenant.getId() + " has no reading"));
    }
}
