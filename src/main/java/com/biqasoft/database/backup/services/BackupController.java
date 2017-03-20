/*
* Copyright (c) 2016 biqasoft.com




 */

package com.biqasoft.database.backup.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;

/**
 * @author Nikita Bakaev, ya@nbakaev.ru
 * Date: 10/5/2015
 * All Rights Reserved
 */
@Controller
@RequestMapping("/backup")
public class BackupController {

    private BackupService mongoConfiguration;

    @Autowired
    public BackupController(BackupService mongoConfiguration) {
        this.mongoConfiguration = mongoConfiguration;
    }

    @RequestMapping(method = RequestMethod.GET)
    public void backup(HttpServletResponse response) throws Exception {
        mongoConfiguration.backup();
    }

    @RequestMapping(value = "id/{id}")
    public void backupDomainById(@PathVariable("id") String id) throws Exception {
        mongoConfiguration.backupDomain(id);
    }

}