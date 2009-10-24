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

public final class SMTPCodes {

    public static final int SERVICE_READY                        = 220;
    public static final int SERVICE_TERMINATING                  = 221;
    public static final int OK                                   = 250;

    public static final int START_MAIL_INPUT                     = 354;

    public static final int ERR_TRANS_SERVICE_NOT_AVAILABLE      = 421;
    public static final int ERR_TRANS_MAILBOX_UNAVAILABLE        = 450;
    public static final int ERR_TRANS_ACTION_ABORTED             = 451;
    public static final int ERR_TRANS_INSUFFICIENT_STORAGE       = 452;

    public static final int ERR_PERM_SYNTAX_ERR_COMMAND          = 500;
    public static final int ERR_PERM_SYNTAX_ERR_PARAM            = 501;
    public static final int ERR_PERM_COMMAND_NOT_IMPLEMENTED     = 502;
    public static final int ERR_PERM_BAD_SEQUENCE                = 503;
    public static final int ERR_PERM_PARAM_NOT_IMPLEMENTED       = 504;
    public static final int ERR_PERM_MAILBOX_UNAVAILABLE         = 550;
    public static final int ERR_PERM_USER_NOT_LOCAL              = 551;
    public static final int ERR_PERM_STORAGE_EXCEEDED            = 552;
    public static final int ERR_PERM_MAILBOX_NOT_ALLOWED         = 553;
    public static final int ERR_PERM_TRX_FAILED                  = 554;

}
