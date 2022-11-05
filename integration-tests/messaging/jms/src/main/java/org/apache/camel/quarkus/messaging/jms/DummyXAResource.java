/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.camel.quarkus.messaging.jms;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.jboss.logging.Logger;

public class DummyXAResource implements XAResource {
    private static final Logger LOG = Logger.getLogger(DummyXAResource.class);

    @Override
    public void commit(Xid xid, boolean b) throws XAException {
        LOG.info("DummyXAResource commit " + xid);
    }

    @Override
    public void end(Xid xid, int i) throws XAException {
    }

    @Override
    public void forget(Xid xid) throws XAException {

    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return 0;
    }

    @Override
    public boolean isSameRM(XAResource xaResource) throws XAException {
        if (!(xaResource instanceof DummyXAResource)) {
            return false;
        } else {
            return this.equals(xaResource);
        }
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        return XA_OK;
    }

    @Override
    public Xid[] recover(int i) throws XAException {
        return new Xid[0];
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        LOG.info("DummyXAResource rollback " + xid);
    }

    @Override
    public boolean setTransactionTimeout(int i) throws XAException {
        return true;
    }

    @Override
    public void start(Xid xid, int i) throws XAException {

    }
}
