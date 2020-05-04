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

import javafx.util.Pair;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;
import org.bytedeco.llvm.LLVM.LLVMBuilderRef;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.wso2.ballerinalang.compiler.bir.codegen.nativecodegen.utils.CodeGenUtils;
import org.wso2.ballerinalang.compiler.bir.codegen.nativecodegen.utils.Utils;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode;
import org.wso2.ballerinalang.compiler.bir.model.BIROperand;
import org.wso2.ballerinalang.compiler.bir.model.VarKind;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.bytedeco.llvm.global.LLVM.LLVMAddFunction;
import static org.bytedeco.llvm.global.LLVM.LLVMAppendBasicBlock;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildAlloca;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildStore;
import static org.bytedeco.llvm.global.LLVM.LLVMFunctionType;
import static org.bytedeco.llvm.global.LLVM.LLVMGetParam;
import static org.bytedeco.llvm.global.LLVM.LLVMPositionBuilderAtEnd;
import static org.bytedeco.llvm.global.LLVM.LLVMVoidType;
import static org.wso2.ballerinalang.compiler.bir.codegen.nativecodegen.constants.functionconstants.FunctionConstants.VAR_ALLOC;

public class FunctionBuilder {

    BIRNode.BIRFunction func;
    LLVMValueRef funcRef;
    LLVMModuleRef module;
    Map<String, Pair<LLVMValueRef, LLVMTypeRef>> localVarRefs;
    LLVMBasicBlockRef varAllocBB;
    LLVMBuilderRef  builder;

    public FunctionBuilder(BIRNode.BIRFunction func, LLVMModuleRef module, LLVMBuilderRef builder) {
        this.func = func;
        this.module = module;
        this.builder = builder;
    }

    public void genFunctionDecl() {
        String name = this.func.name.value;
        PointerPointer args = getFunctionArgTypes();
        LLVMTypeRef retType = CodeGenUtils.genLLVMType(this.func.type.retType);
        LLVMTypeRef funcType = LLVMFunctionType(retType, args, this.func.argsCount, 0);
        this.funcRef = LLVMAddFunction(this.module, name, funcType);
    }

    private PointerPointer getFunctionArgTypes() {
        if (this.func.argsCount == 0) {
            return genVoidFunctionArgType();
        }

        return genNonVoidFunctionArgTypes();
    }

    private PointerPointer genNonVoidFunctionArgTypes() {
        List<LLVMTypeRef> args = new ArrayList<>();
        for (BType parameter : this.func.type.paramTypes) {
            args.add(CodeGenUtils.genLLVMType(parameter));
        }
        return new PointerPointer((LLVMTypeRef[]) args.toArray());
    }

    private PointerPointer genVoidFunctionArgType() {
        LLVMTypeRef[] args = { LLVMVoidType() };
        return new PointerPointer(args);
    }

    public void genFunctionBody(Map<String, FunctionBuilder> funcBuilders) {
        genLocalVarAllocatiosBbBody();
        Map<String, BbTermGenerator> bbTermGenerators = genBbBodies();
    }

    private Map<String, BbTermGenerator> genBbBodies() {
        return null;
    }

    private void genLocalVarAllocatiosBbBody() {
        this.varAllocBB = genBbDecl(VAR_ALLOC);
        for (int paramIndex = 0; paramIndex < this.func.argsCount; paramIndex++) {
            BIRNode.BIRVariableDcl localVar = this.func.localVars.get(paramIndex);
            LLVMValueRef localVarRef = buildAllocaForLocalVariable(localVar);
            cacheLocalVarRef(localVar, localVarRef);
            storeParams(localVar, paramIndex, localVarRef);
        }
    }

    private void storeParams(BIRNode.BIRVariableDcl localVar, int paramIndex, LLVMValueRef localVarRef) {
        if (localVar.kind == VarKind.ARG) {
            LLVMValueRef paramRef = LLVMGetParam(this.funcRef, paramIndex);
            LLVMBuildStore(this.builder, paramRef, localVarRef);
        }
    }

    private void cacheLocalVarRef(BIRNode.BIRVariableDcl localVar, LLVMValueRef localVarRef) {
        LLVMTypeRef varType = CodeGenUtils.genLLVMType(localVar.type);
        String varName = Utils.localVariableName(localVar);
        Pair<LLVMValueRef, LLVMTypeRef> entry = new Pair<>(localVarRef, varType);

        this.localVarRefs.put(varName, entry);
    }

    private LLVMValueRef buildAllocaForLocalVariable(BIRNode.BIRVariableDcl localVar) {
        String varName = Utils.localVariableNameWithPrefix(localVar);
        LLVMTypeRef varType = CodeGenUtils.genLLVMType(localVar.type);
        return LLVMBuildAlloca(this.builder, varType, varName);
    }

    private LLVMBasicBlockRef genBbDecl(String name) throws RuntimeException {
        if (this.funcRef != null) {
            LLVMBasicBlockRef bbRef = LLVMAppendBasicBlock(funcRef, name);
            LLVMPositionBuilderAtEnd(this.builder, bbRef);
            return bbRef;
        }

        throw new RuntimeException("No Functionref Available");
    }

    public LLVMValueRef genLoadLocalToTempVar(BIROperand op) {
        return null;
    }
}
