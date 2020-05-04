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

import org.bytedeco.llvm.LLVM.LLVMBuilderRef;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode;

import java.nio.file.Path;

import static org.bytedeco.llvm.global.LLVM.LLVMCreateBuilder;
import static org.bytedeco.llvm.global.LLVM.LLVMModuleCreateWithName;
import static org.wso2.ballerinalang.compiler.bir.codegen.nativecodegen.utils.Utils.getPackageNameFromString;

/**
 * Generate native objects using the BIR Package.
 *
 * @Since 1.3.0
 */
public class NativePackageGen {

    NativeFunctionBuilder nativeFunctionBuilder;
    LLVMModuleRef module;
    BIRNode.BIRPackage pkg;
    LLVMBuilderRef builder;

    /**
     * @param pkg BIR package node.
     */
    public NativePackageGen(BIRNode.BIRPackage pkg) {
        this.pkg = pkg;
        initPackageGen();
    }

    private void initPackageGen() {
        createModule();
        createNativeFuncBuilder();
        createBuilder();
    }

    private void createBuilder() {
        this.builder = LLVMCreateBuilder();
    }

    private void createNativeFuncBuilder() {
        this.nativeFunctionBuilder = new NativeFunctionBuilder(this.module);
    }

    private void createModule() {
        String orgName = pkg.org.value;
        String moduleName = pkg.name.value;
        String pkgName = getPackageNameFromString(orgName, moduleName);
        this.module = LLVMModuleCreateWithName(pkgName);
    }

    /**
     * Generate object files on the path defined by @param targetObjectFilePath.
     *
     * @param targetObjectFilePath path for target object file creation.
     * @param dumpLLVMIR dump LLVM IR for debugging purposes.
     * @param noOptimize stop optimization of LLVM IR for debugging purposes.
     */
    public void genPackage(Path targetObjectFilePath, boolean dumpLLVMIR, boolean noOptimize) {
        genFunctions();
    }

    /**
     * Generate the functions for this BIRPackage.
     */
    private void genFunctions() {
        this.nativeFunctionBuilder.genFunctions();
    }

}
