package com.biqasoft.database.backup.distributedstorage;

import com.biqasoft.entity.core.Domain;

import java.io.Serializable;

/**
 * Created by ya on 11/16/2016.
 */
public class BackupDomain implements Serializable {

    private Domain domain;

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }
}
