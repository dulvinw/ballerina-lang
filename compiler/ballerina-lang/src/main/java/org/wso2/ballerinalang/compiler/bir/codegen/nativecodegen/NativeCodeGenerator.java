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

import org.ballerinalang.codegen.CodeGenerator;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.diagnotic.BLangDiagnosticLogHelper;

import java.nio.file.Path;
import java.util.Set;

/**
 * This class will generate the native code from ballerina BIR.
 *
 * @Since 1.3.0
 */
public class NativeCodeGenerator implements CodeGenerator {
    private static final CompilerContext.Key<NativeCodeGenerator> CODE_GEN = new CompilerContext.Key<>();

    public static BLangDiagnosticLogHelper dlog;

    private NativeCodeGenerator(CompilerContext context) {
        context.put(CODE_GEN, this);
    }

    public static CodeGenerator getInstance(CompilerContext context) {

        CodeGenerator codeGenerator = context.get(CODE_GEN);
        if (codeGenerator == null) {
            codeGenerator = new NativeCodeGenerator(context);
        }

        return codeGenerator;
    }

    @Override
    public void generate(BIRNode.BIRPackage entryMod, Path target, Set<Path> moduleDependencies) {
        return;
    }
}
