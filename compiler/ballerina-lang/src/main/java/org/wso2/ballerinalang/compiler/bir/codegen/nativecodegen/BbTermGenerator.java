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

import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;
import org.bytedeco.llvm.LLVM.LLVMBuilderRef;
import org.wso2.ballerinalang.compiler.bir.codegen.nativecodegen.utils.CodeGenUtils;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode;
import org.wso2.ballerinalang.compiler.bir.model.BIRTerminator;

import java.util.Map;

import static org.bytedeco.llvm.global.LLVM.LLVMBuildBr;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildCondBr;
import static org.bytedeco.llvm.global.LLVM.LLVMPositionBuilderAtEnd;

public class BbTermGenerator {
    LLVMBuilderRef builder;
    BIRNode.BIRBasicBlock bb;
    LLVMBasicBlockRef bbRef;
    FunctionBuilder parent;

    public BbTermGenerator(LLVMBuilderRef builder, BIRNode.BIRBasicBlock bb, LLVMBasicBlockRef bbRef,
            FunctionBuilder parent) {
        this.builder = builder;
        this.bb = bb;
        this.bbRef = bbRef;
        this.parent = parent;
    }

    public void genBasicBlockTerminator(Map<String, FunctionBuilder> funcGenerators,
            Map<String, BbTermGenerator> bbGenerators) {
        LLVMPositionBuilderAtEnd(this.builder, this.bbRef);

        BIRTerminator terminator = this.bb.terminator;

        switch (terminator.kind) {
            case GOTO:
                genGotoTerm((BIRTerminator.GOTO) terminator, bbGenerators);
                break;
            case BRANCH:
                genBranchTerm((BIRTerminator.Branch) terminator, bbGenerators);
        }
    }

    private void genBranchTerm(BIRTerminator.Branch terminator, Map<String, BbTermGenerator> bbGenerators) {
        LLVMBasicBlockRef ifTrue = CodeGenUtils.findBbRefById(bbGenerators, terminator.trueBB.id.value);
        LLVMBasicBlockRef ifFalse = CodeGenUtils.findBbRefById(bbGenerators, terminator.falseBB.id.value);
        LLVMBuildCondBr(this.builder, this.parent.genLoadLocalToTempVar(terminator.op), ifTrue, ifFalse);
    }

    private void genGotoTerm(BIRTerminator.GOTO gotoIns, Map<String, BbTermGenerator> bbGenerators) {
        LLVMBuildBr(this.builder, CodeGenUtils.findBbRefById(bbGenerators, gotoIns.targetBB.id.value));
    }

    public LLVMBasicBlockRef getBbRef() {
        return bbRef;
    }
}
