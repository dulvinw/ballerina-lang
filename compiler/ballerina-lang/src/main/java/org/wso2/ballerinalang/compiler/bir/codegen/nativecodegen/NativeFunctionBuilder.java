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

import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.wso2.ballerinalang.compiler.bir.codegen.nativecodegen.constants.functionconstants.FunctionConstants;

import static org.bytedeco.llvm.global.LLVM.LLVMAddFunction;
import static org.bytedeco.llvm.global.LLVM.LLVMFunctionType;
import static org.bytedeco.llvm.global.LLVM.LLVMInt32Type;
import static org.bytedeco.llvm.global.LLVM.LLVMInt64Type;
import static org.bytedeco.llvm.global.LLVM.LLVMInt8Type;
import static org.bytedeco.llvm.global.LLVM.LLVMPointerType;
import static org.bytedeco.llvm.global.LLVM.LLVMVoidType;
import static org.bytedeco.llvm.global.LLVM.thinlto_codegen_add_cross_referenced_symbol;

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
        generateNewIntArrayDecl();
        generateIntArrayStore();
        generateIntArrayLoad();
    }

    private void generateIntArrayLoad() {
        LLVMTypeRef[] arguments = { LLVMPointerType(LLVMInt64Type(), 0), LLVMInt64Type() };
        PointerPointer argumentsPtr = new PointerPointer(arguments);
        LLVMTypeRef intArrayLoadType = LLVMFunctionType(LLVMPointerType(LLVMInt64Type(), 0), argumentsPtr, 2, 0);

        LLVMValueRef intArrayLoadDcl = LLVMAddFunction(this.module, FunctionConstants.INT_ARRAY_LOAD, intArrayLoadType);
        this.functionMap.put(FunctionConstants.INT_ARRAY_LOAD, intArrayLoadDcl);
    }

    private void generateIntArrayStore() {
        LLVMTypeRef[] arguments = { LLVMPointerType(LLVMInt64Type(), 0), LLVMInt64Type(),
                LLVMPointerType(LLVMInt64Type(), 0) };
        PointerPointer argumentsPtr = new PointerPointer(arguments);
        LLVMTypeRef intArrayStoreType = LLVMFunctionType(LLVMVoidType(), argumentsPtr, 3, 0);

        LLVMValueRef intArrayStoreDcl = LLVMAddFunction(this.module, FunctionConstants.INT_ARRAY_STORE,
                intArrayStoreType);
        this.functionMap.put(FunctionConstants.INT_ARRAY_STORE, intArrayStoreDcl);
    }

    private void generatePrintfDecl() {
        LLVMTypeRef[] pointerToCharType = {LLVMPointerType(LLVMInt8Type(), 0)};
        PointerPointer argumentTypes = new PointerPointer(pointerToCharType);
        LLVMTypeRef printfType = LLVMFunctionType(LLVMInt32Type(), argumentTypes, 1, 1);

        LLVMValueRef printfFunc = LLVMAddFunction(this.module, FunctionConstants.PRINT, printfType);
        this.functionMap.put(FunctionConstants.PRINT, printfFunc);
    }

    private void generateNewIntArrayDecl() {
        LLVMTypeRef[] arguments = { LLVMInt64Type() };
        PointerPointer argumentsPointer = new PointerPointer(arguments);
        LLVMTypeRef newIntArrayType = LLVMFunctionType(LLVMPointerType(LLVMInt64Type(), 0),
                argumentsPointer, 1, 0);

        LLVMValueRef newIntArrayDecl = LLVMAddFunction(this.module, FunctionConstants.NEW_INT_ARRAY, newIntArrayType);
        this.functionMap.put(FunctionConstants.NEW_INT_ARRAY, newIntArrayDecl);
    }
}
