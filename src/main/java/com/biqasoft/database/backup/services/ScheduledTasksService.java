/*
* Copyright (c) 2016 biqasoft.com




 */

package com.biqasoft.database.backup.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Created by Nikita Bakaev, ya@nbakaev.ru on 12/26/2015.
 * All Rights Reserved
 */
@Service
public class ScheduledTasksService {

    private final BackupService backupService;
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasksService.class);

    @Autowired
    public ScheduledTasksService(BackupService backupService) {
        this.backupService = backupService;
    }

    // http://stackoverflow.com/questions/26147044/spring-cron-expression-for-every-day-101am
    @Scheduled(cron = "0 0 0,12 * * *")
    public void scheduledLeadGen (){
        logger.debug("START: Scheduled backup");
        backupService.backup();
        logger.debug("END: Scheduled backup");
    }

}
