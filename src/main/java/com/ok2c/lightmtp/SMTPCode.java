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
package com.ok2c.lightmtp;

public final class SMTPCode {

    private final int codeClass;
    private final int subject;
    private final int detail;

    public SMTPCode(int codeClass, int subject, int detail) {
        super();
        if (codeClass <= 0) {
            throw new IllegalArgumentException("Code class may not be nagtive or zero");
        }
        if (subject < 0) {
            throw new IllegalArgumentException("Code subject may not be nagtive");
        }
        if (detail < 0) {
            throw new IllegalArgumentException("Code detail may not be nagtive");
        }
        this.codeClass = codeClass;
        this.subject = subject;
        this.detail = detail;
    }

    public int getCodeClass() {
        return this.codeClass;
    }

    public int getSubject() {
        return this.subject;
    }

    public int getDetail() {
        return this.detail;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.codeClass;
        result = prime * result + this.subject;
        result = prime * result + this.detail;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SMTPCode that = (SMTPCode) obj;
        if (this.codeClass == that.codeClass
                && this.detail == that.detail
                && this.subject == that.subject) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(this.codeClass);
        buffer.append('.');
        buffer.append(this.subject);
        buffer.append('.');
        buffer.append(this.detail);
        return buffer.toString();
    }

}
