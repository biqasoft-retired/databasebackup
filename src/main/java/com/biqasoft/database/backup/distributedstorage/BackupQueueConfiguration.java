package com.biqasoft.database.backup.distributedstorage;

import com.hazelcast.core.IQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BackupQueueConfiguration {

    private HazelcastService hazelcastService;

    @Autowired
    public BackupQueueConfiguration(HazelcastService hazelcastService) {
        this.hazelcastService = hazelcastService;
    }

    @Bean
    public IQueue<BackupDomain> backupQueue(){
        return hazelcastService.getClient().getQueue("backup-domains");
    }

}
