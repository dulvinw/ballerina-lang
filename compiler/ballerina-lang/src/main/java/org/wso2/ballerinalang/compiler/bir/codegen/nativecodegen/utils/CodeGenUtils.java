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
package org.wso2.ballerinalang.compiler.bir.codegen.nativecodegen.utils;

import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.wso2.ballerinalang.compiler.bir.codegen.nativecodegen.BbTermGenerator;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;

import java.util.Map;

import static org.bytedeco.llvm.global.LLVM.LLVMInt1Type;
import static org.bytedeco.llvm.global.LLVM.LLVMInt64Type;
import static org.bytedeco.llvm.global.LLVM.LLVMVoidType;

public class CodeGenUtils {

    public static LLVMTypeRef genLLVMType(BType type) {

        LLVMTypeRef llvmType;
        switch (type.getKind()) {
            case INT:
                llvmType = LLVMInt64Type();
                break;
            case BOOLEAN:
                llvmType = LLVMInt1Type();
                break;
            case NIL:
            case VOID:
                llvmType = LLVMVoidType();
                break;
            default:
                throw new RuntimeException("Unsupported Type Found");
        }

        return llvmType;
    }

    public static LLVMBasicBlockRef findBbRefById(Map<String, BbTermGenerator> bbGenerators, String id) {
        BbTermGenerator termGenerator = bbGenerators.get(id);
        if (termGenerator == null) {
            throw new RuntimeException("No BB By That name when finding using reference");
        }
        return termGenerator.getBbRef();
    }

}
