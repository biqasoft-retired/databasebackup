/*
* Copyright (c) 2016 biqasoft.com




 */

package com.biqasoft.database.backup.services;

import com.biqasoft.database.backup.distributedstorage.BackupDomain;
import com.biqasoft.entity.core.Domain;
import com.biqasoft.microservice.common.MicroserviceDomain;
import com.hazelcast.core.IQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);
    private final MicroserviceDomain microserviceDomain;
    private IQueue<BackupDomain> queue;

    @Autowired
    public BackupService(MicroserviceDomain microserviceDomain, IQueue<BackupDomain> queue) {
        this.microserviceDomain = microserviceDomain;
        this.queue = queue;
    }

    /**
     * Backup all mongodb databases to temp folder
     * and then backup to amazon s3
     */
    public void backup() {
        List<Domain> domains = microserviceDomain.unsafeFindAllDomains();

        for (Domain domain : domains) {
            if (!domain.isActive()) {
                logger.info("skipping inactive domain {}", domain.getDomain());
                return;
            }

            backupDomain(domain);
        }
    }

    public void backupDomain(Domain domain) {
        BackupDomain backupDomain = new BackupDomain();
        backupDomain.setDomain(domain);
        addDomainToBackupQueue(backupDomain);
    }

    public void backupDomain(String id){
        Domain domain = microserviceDomain.unsafeFindDomainById(id);
        backupDomain(domain);
    }

    private void addDomainToBackupQueue(BackupDomain backupDomain) {
        queue.add(backupDomain);
    }

}

