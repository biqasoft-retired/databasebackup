package com.biqasoft.database.backup.services;

import com.biqasoft.entity.core.BaseClass;
import com.biqasoft.storage.entity.StorageFile;

import java.util.Date;

/**
 * Created by ya on 3/20/2017.
 */
public class BackupEntry extends BaseClass {

    private StorageFile storageFile;
    private Date date = new Date();

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public StorageFile getStorageFile() {
        return storageFile;
    }

    public void setStorageFile(StorageFile storageFile) {
        this.storageFile = storageFile;
    }
}
