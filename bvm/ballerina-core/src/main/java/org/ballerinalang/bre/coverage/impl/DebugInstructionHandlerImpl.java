/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.ballerinalang.bre.coverage.impl;

import org.ballerinalang.bre.bvm.WorkerExecutionContext;
import org.ballerinalang.bre.coverage.InstructionHandler;

/**
 * This is CPU Ip interceptor implementation for debugger.
 *
 * @since 0.985
 */
public class DebugInstructionHandlerImpl implements InstructionHandler {

    /**
     * Ip interceptor method to handle each Ip for the CPU for debugger.
     *
     * @param ctx worker execution context for the Ip
     */
    public void handle(WorkerExecutionContext ctx) {

    }

}
