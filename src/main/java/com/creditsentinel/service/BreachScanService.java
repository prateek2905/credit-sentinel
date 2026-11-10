package com.creditsentinel.service;

import com.creditsentinel.book.BookReader;
import com.creditsentinel.detection.Breach;
import com.creditsentinel.detection.BreachDetector;
import com.creditsentinel.domain.AlertStatus;
import com.creditsentinel.domain.CreditAlert;
import com.creditsentinel.domain.Facility;
import com.creditsentinel.repo.CreditAlertRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BreachScanService implements ApplicationRunner {

    private final BookReader book;
    private final BreachDetector detector;
    private final CreditAlertRepository alerts;

    public BreachScanService(BookReader book, BreachDetector detector, CreditAlertRepository alerts) {
        this.book = book;
        this.detector = detector;
        this.alerts = alerts;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Facility facility : book.allFacilities()) {
            for (Breach breach : detector.detect(book.snapshot(facility.getId()))) {
                boolean alreadyOpen = alerts.findFirstByFacilityIdAndBreachTypeAndStatusNot(
                        facility.getId(), breach.type(), AlertStatus.CLOSED).isPresent();
                if (!alreadyOpen) {
                    alerts.save(CreditAlert.open(facility, breach.type(), breach.detail()));
                }
            }
        }
    }
}
