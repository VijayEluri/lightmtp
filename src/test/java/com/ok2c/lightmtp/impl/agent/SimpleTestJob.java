/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.ok2c.lightmtp.impl.agent;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryResult;
import com.ok2c.lightnio.concurrent.BasicFuture;

public class SimpleTestJob {

    private final LinkedList<DeliveryRequest> requests;
    private final LinkedList<DeliveryResult> results;
    private final BasicFuture<List<DeliveryResult>> future;
    
    private final int total;
    private int completed;
    
    public SimpleTestJob(final List<DeliveryRequest> requests) {
        super();
        this.requests = new LinkedList<DeliveryRequest>();
        this.results = new LinkedList<DeliveryResult>();
        this.future = new BasicFuture<List<DeliveryResult>>(null);
        this.requests.addAll(requests);
        this.total = this.requests.size();
        this.completed = 0;
    }
    
    public synchronized DeliveryRequest removeRequest() {
        return this.requests.poll();
    }
    
    public synchronized void addResult(final DeliveryResult result) {
        if (result == null) {
            return;
        }
        this.results.add(result);
        this.completed++;
        if (this.completed == this.total) {
            this.future.completed(new ArrayList<DeliveryResult>(this.results));
        }
    }

    public void failure(final Exception ex) {
        this.future.failed(ex);
    }
    
    public void cancel() {
        this.future.cancel(true);
    }
    
    public List<DeliveryResult> waitForResults(
            long timeout, 
            final TimeUnit unit) throws TimeoutException, ExecutionException, InterruptedException {
        return this.future.get(timeout, unit);
    }
    
    public List<DeliveryResult> waitForResults() throws ExecutionException, InterruptedException {
        return this.future.get();
    }

}
