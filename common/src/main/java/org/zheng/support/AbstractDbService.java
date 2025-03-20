package org.zheng.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.zheng.db.DbTemplate;

public abstract class AbstractDbService extends LoggerSupport {

    @Autowired
    protected DbTemplate db;
}