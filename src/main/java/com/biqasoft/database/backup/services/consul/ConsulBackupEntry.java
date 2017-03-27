package com.biqasoft.database.backup.services.consul;

import com.biqasoft.entity.core.BaseClass;
import com.ecwid.consul.v1.kv.model.GetValue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by ya on 3/20/2017.
 */
public class ConsulBackupEntry extends BaseClass{

    private List<GetValue> consulKVs = new ArrayList<>();
    private Date date;


    public List<GetValue> getConsulKVs() {
        return consulKVs;
    }

    public void setConsulKVs(List<GetValue> consulKVs) {
        this.consulKVs = consulKVs;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
