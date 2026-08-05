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
    public void autoApprove() {
        try{
            int count = artisanAppointmentService.processAutoApprove();
            if(count > 0){
                log.info("{} appointments have been auto approved", count);
            }
        }catch(Exception e){
              log.error("Auto approve appointments failed.", e);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void scheduledAutoComplete() {
        try{
            int count = artisanAppointmentService.processAutoComplete();
            if(count > 0){
                log.info("{} appointments have been auto completed", count);
            }
        }catch(Exception e){
            log.error("Auto complete appointments failed.", e);
        }
    }
}
