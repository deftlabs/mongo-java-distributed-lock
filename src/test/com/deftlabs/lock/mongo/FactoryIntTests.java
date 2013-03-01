package com.deftlabs.lock.mongo;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Tests service creation semantics. You must be running an instance of mongo on localhost:27017 and localhost:27018
 */
public final class FactoryIntTests {

    private static final String MONGO_URI1 = "mongodb://127.0.0.1:27017/?maxpoolsize=10&waitqueuemultiple=5&connecttimeoutms=20000&sockettimeoutms=20000&autoconnectretry=true";
    private static final String MONGO_URI2 = "mongodb://127.0.0.1:27018/?maxpoolsize=10&waitqueuemultiple=5&connecttimeoutms=20000&sockettimeoutms=20000&autoconnectretry=true";

    @Test
    public void factoryMaintainsSingleInstance() throws Exception {
        final DistributedLockSvcOptions options1 = new DistributedLockSvcOptions(MONGO_URI1);
        final DistributedLockSvc svc1 = new DistributedLockSvcFactory(options1).getLockSvc();

        final DistributedLockSvcOptions options2 = new DistributedLockSvcOptions(MONGO_URI1);
        final DistributedLockSvc svc2 = new DistributedLockSvcFactory(options2).getLockSvc();

        assertSame(svc1, svc2);
        assertEquals(svc1, svc2);
    }

    @Test public void canCreateSeparatelyConfiguredServices() throws Exception {
        final DistributedLockSvcOptions options1 = new DistributedLockSvcOptions(MONGO_URI1);
        final DistributedLockSvc svc1 = new DistributedLockSvcFactory(options1).getLockSvc();

        final DistributedLockSvcOptions options2 = new DistributedLockSvcOptions(MONGO_URI2);
        final DistributedLockSvc svc2 = new DistributedLockSvcFactory(options2).getLockSvc();

        assertEquals(MONGO_URI1, svc1.getSvcOptions().getMongoUri());
        assertEquals(MONGO_URI2, svc2.getSvcOptions().getMongoUri());
    }

    @Ignore
    @Test(expected = IllegalArgumentException.class)
    public void cannotRedefineMongoOptionsForSameHosts() throws Exception {
        // TODO
    }

}
