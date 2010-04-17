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

public class SMTPErrorException extends SMTPProtocolException {

    private static final long serialVersionUID = 6177195429674347053L;

    private final int code;
    private final SMTPCode enhancedCode;

    public SMTPErrorException(int code, final SMTPCode enhancedCode, final String message) {
        super(message);
        int codeClass = code / 100;
        if (codeClass != 4 && codeClass != 5) {
            throw new IllegalArgumentException("Invalid error code: " + code);
        }
        if (enhancedCode != null) {
            codeClass = enhancedCode.getCodeClass();
            if (codeClass != 4 && codeClass != 5) {
                throw new IllegalArgumentException("Invalid enhanced error code: " + enhancedCode);
            }
        }
        this.code = code;
        this.enhancedCode = enhancedCode;
    }

    public int getCode() {
        return this.code;
    }

    public SMTPCode getEnhancedCode() {
        return this.enhancedCode;
    }

}