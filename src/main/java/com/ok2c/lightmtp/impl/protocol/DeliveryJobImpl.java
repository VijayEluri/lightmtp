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
package com.ok2c.lightmtp.impl.protocol;

import com.ok2c.lightmtp.protocol.DeliveryResult;
import com.ok2c.lightmtp.protocol.DeliveryRequest;
import com.ok2c.lightmtp.protocol.DeliveryCallback;
import com.ok2c.lightmtp.protocol.DeliveryJob;

class DeliveryJobImpl implements DeliveryJob {

    private final DeliveryRequest request;
    private final DeliveryCallback callback;
    
    private volatile boolean completed;
    private volatile boolean aborted;
    private volatile Exception exception;
    private volatile DeliveryResult result;
    
    public DeliveryJobImpl(final DeliveryRequest request, final DeliveryCallback callback) {
        super();
        if (request == null) {
            throw new IllegalArgumentException("SMTP request may not be null");
        }
        this.request = request;
        this.callback = callback;
    }

    public DeliveryRequest getRequest() {
        return this.request;
    }

    public DeliveryResult getResult() {
        return this.result;
    }

    public Exception getException() {
        return this.exception;
    }

    public boolean isCompleted() {
        return this.completed;
    }

    public boolean isAborted() {
        return this.aborted;
    }

    public void waitFor() throws InterruptedException {
        if (this.completed) {
            return;
        }
        synchronized (this) {
            while (!this.completed) {
                wait();
            }
        }
    }
    
    public void completed(final DeliveryResult result) {
        if (result == null) {
            throw new IllegalArgumentException("Delivery result may not be null");
        }
        if (this.completed) {
            return;
        }
        synchronized (this) {
            this.completed = true;
            this.result = result;
            if (this.callback != null) {
                this.callback.completed(this);
            }
            notifyAll();
        }
    }

    public void failed(final DeliveryResult result) {
        if (result == null) {
            throw new IllegalArgumentException("Delivery result may not be null");
        }
        if (this.completed) {
            return;
        }
        synchronized (this) {
            this.completed = true;
            this.result = result;
            if (this.callback != null) {
                this.callback.failed(this);
            }
            notifyAll();
        }
    }

    public void failed(final Exception exception) {
        if (exception == null) {
            return;
        }
        if (this.completed) {
            return;
        }
        this.completed = true;
        synchronized (this) {
            this.exception = exception;
            this.result = null;
            if (this.callback != null) {
                this.callback.failed(this);
            }
            notifyAll();
        }
    }
    
    public void aborted() {
        if (this.completed) {
            return;
        }
        synchronized (this) {
            this.completed = true;
            this.aborted = true;
            this.result = null;
            if (this.callback != null) {
                this.callback.aborted(this);
            }
            notifyAll();
        }
    }

}
