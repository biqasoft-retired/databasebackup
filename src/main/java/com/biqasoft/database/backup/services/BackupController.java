/*
* Copyright (c) 2016 biqasoft.com




 */

package com.biqasoft.database.backup.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 10/5/2015
 *         All Rights Reserved
 */
@RestController
@RequestMapping("/v1/backup")
public class BackupController {

    private BackupService mongoConfiguration;

    @Autowired
    public BackupController(BackupService mongoConfiguration) {
        this.mongoConfiguration = mongoConfiguration;
    }

    @RequestMapping(method = RequestMethod.GET)
    public void backup() {
        mongoConfiguration.backup();
    }

    @RequestMapping(value = "consul")
    public void backupDomainById() {
        mongoConfiguration.backupConsul();
    }

    @RequestMapping(value = "id/{id}")
    public void backupDomainById(@PathVariable("id") String id) {
        mongoConfiguration.backupDomain(id);
    }

}