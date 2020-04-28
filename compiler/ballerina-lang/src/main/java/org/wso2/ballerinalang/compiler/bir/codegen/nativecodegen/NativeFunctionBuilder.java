/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.ballerinalang.compiler.bir.codegen.nativecodegen;

import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.wso2.ballerinalang.compiler.bir.codegen.nativecodegen.constants.functionconstants.FunctionConstants;

import static org.bytedeco.llvm.global.LLVM.LLVMFunctionType;
import static org.bytedeco.llvm.global.LLVM.LLVMInt32Type;
import static org.bytedeco.llvm.global.LLVM.LLVMInt64Type;
import static org.bytedeco.llvm.global.LLVM.LLVMInt8Type;
import static org.bytedeco.llvm.global.LLVM.LLVMPointerType;

import java.util.HashMap;
import java.util.Map;

public class NativeFunctionBuilder {
    Map<String, LLVMValueRef> functionMap;
    LLVMModuleRef module;
    LLVMTypeRef int64PointerType;

    public NativeFunctionBuilder(LLVMModuleRef module) {
        this.functionMap = new HashMap<>();
        this.module = module;
        this.int64PointerType = LLVMPointerType(LLVMInt64Type(), 0);
    }

    public void genFunctions() {
        generatePrintfDecl();
    }

    private void generatePrintfDecl() {
        LLVMTypeRef[] pointerToCharType = {LLVMPointerType(LLVMInt8Type(), 0)};
        LLVMTypeRef printfType = LLVMFunctionType(LLVMInt32Type(), pointerToCharType, 1, 1)
    }
}
