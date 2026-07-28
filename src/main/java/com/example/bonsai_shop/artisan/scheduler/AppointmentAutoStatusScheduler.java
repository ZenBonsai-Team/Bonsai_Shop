package com.example.bonsai_shop.artisan.scheduler;

import com.example.bonsai_shop.artisan.service.ArtisanAppointmentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentAutoStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentAutoStatusScheduler.class);

    private final ArtisanAppointmentService artisanAppointmentService;

    @Scheduled(fixedRate = 60000)
    public void processAppointmentStatusUpdates() {
        try {
            int updatedCount = artisanAppointmentService.processAutomaticAppointmentStatusUpdates();
            if (updatedCount > 0) {
                log.info("Automatically updated {} appointment status(es).", updatedCount);
            }
        } catch (Exception e) {
            log.error("Failed to process automatic appointment status updates.", e);
        }
    }
}
