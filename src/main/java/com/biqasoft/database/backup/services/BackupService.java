/*
* Copyright (c) 2016 biqasoft.com




 */

package com.biqasoft.database.backup.services;

import com.biqasoft.database.backup.distributedstorage.BackupDomain;
import com.biqasoft.database.backup.services.consul.ConsulBackupEntry;
import com.biqasoft.entity.core.Domain;
import com.biqasoft.microservice.common.MicroserviceDomain;
import com.biqasoft.microservice.database.MainDatabase;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.hazelcast.core.IQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);
    private final MicroserviceDomain microserviceDomain;
    private IQueue<BackupDomain> queue;

    private final ConsulClient consulClient;

    private final MongoOperations mongoTemplate;


    @Autowired
    public BackupService(MicroserviceDomain microserviceDomain, IQueue<BackupDomain> queue, ConsulClient consulClient, @MainDatabase MongoOperations mongoTemplate) {
        this.microserviceDomain = microserviceDomain;
        this.queue = queue;
        this.consulClient = consulClient;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Backup all mongodb databases to temp folder
     * and then backup to amazon s3
     */
    public void backup() {
        logger.info("Start backup consul");
        backupConsul();
        logger.info("End backup consul");

        List<Domain> domains = microserviceDomain.unsafeFindAllDomains();

        for (Domain domain : domains) {
            if (!domain.isActive()) {
                logger.info("skipping inactive domain {}", domain.getDomain());
                return;
            }

            backupDomain(domain);
        }
    }

    public void backupConsul() {
        ConsulBackupEntry consulBackupEntry = new ConsulBackupEntry();
        consulBackupEntry.setDate(new Date());

        List<GetValue> config = consulClient.getKVValues("config").getValue();
        consulBackupEntry.setConsulKVs(config);

        mongoTemplate.insert(consulBackupEntry);
//        ObjectMapper objectMapper = new ObjectMapper();
//        try {
//            objectMapper.writeValue(new File("f:/test/123.json"), config);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public void backupDomain(Domain domain) {
        BackupDomain backupDomain = new BackupDomain();
        backupDomain.setDomain(domain);
        addDomainToBackupQueue(backupDomain);
    }

    public void backupDomain(String id) {
        Domain domain = microserviceDomain.unsafeFindDomainById(id);
        backupDomain(domain);
    }

    private void addDomainToBackupQueue(BackupDomain backupDomain) {
        queue.add(backupDomain);
    }

}

