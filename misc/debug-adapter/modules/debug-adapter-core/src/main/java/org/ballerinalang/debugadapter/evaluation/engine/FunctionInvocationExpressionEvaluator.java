/*
 * Copyright (c) 2020, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ballerinalang.debugadapter.evaluation.engine;

import com.sun.jdi.ClassNotPreparedException;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Value;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import org.ballerinalang.debugadapter.DebugSourceType;
import org.ballerinalang.debugadapter.SuspendedContext;
import org.ballerinalang.debugadapter.evaluation.BExpressionValue;
import org.ballerinalang.debugadapter.evaluation.EvaluationException;
import org.ballerinalang.debugadapter.evaluation.EvaluationExceptionKind;
import org.ballerinalang.debugadapter.evaluation.utils.EvaluationUtils;
import org.ballerinalang.debugadapter.utils.PackageUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.ballerinalang.debugadapter.evaluation.engine.InvocationArgProcessor.validateAndProcessArguments;
import static org.ballerinalang.debugadapter.utils.PackageUtils.BAL_FILE_EXT;

/**
 * Evaluator implementation for function invocation expressions.
 *
 * @since 2.0.0
 */
public class FunctionInvocationExpressionEvaluator extends Evaluator {

    private final FunctionCallExpressionNode syntaxNode;
    private final String functionName;
    private final List<Map.Entry<String, Evaluator>> argEvaluators;

    public FunctionInvocationExpressionEvaluator(SuspendedContext context, FunctionCallExpressionNode node,
                                                 List<Map.Entry<String, Evaluator>> argEvaluators) {
        super(context);
        this.syntaxNode = node;
        this.argEvaluators = argEvaluators;
        this.functionName = syntaxNode.functionName().toSourceCode().trim();
    }

    @Override
    public BExpressionValue evaluate() throws EvaluationException {
        try {
            Map<String, Value> argValueMap = validateAndProcessArguments(context, functionName, argEvaluators);
            // First we try to find the matching JVM method from the JVM backend, among already loaded classes.
            Optional<GeneratedStaticMethod> jvmMethod = findFunctionFromLoadedClasses();
            if (jvmMethod.isEmpty()) {
                // If we cannot find the matching method within the loaded classes, then we try to forcefully load
                // all the generated classes related to the current module using the JDI classloader, and search
                // again.
                jvmMethod = loadFunction();
            }
            if (jvmMethod.isEmpty()) {
                throw new EvaluationException(String.format(EvaluationExceptionKind.FUNCTION_NOT_FOUND.getString(),
                        functionName));
            }
            jvmMethod.get().setNamedArgValues(argValueMap);
            Value result = jvmMethod.get().invoke();
            return new BExpressionValue(context, result);
        } catch (EvaluationException e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluationException(String.format(EvaluationExceptionKind.INTERNAL_ERROR.getString(),
                    syntaxNode.toSourceCode().trim()));
        }
    }

    /**
     * Searches for a matching jvm method for a given ballerina function using its syntax node and the debug context
     * information.
     *
     * @return the matching JVM method, if available
     */
    private Optional<GeneratedStaticMethod> findFunctionFromLoadedClasses() {
        List<ReferenceType> allClasses = context.getAttachedVm().allClasses();
        DebugSourceType sourceType = context.getSourceType();
        for (ReferenceType cls : allClasses) {
            try {
                // Expected class name should end with the file name of the ballerina source, only for single
                // ballerina sources. (We cannot be sure about the module context, as we can invoke any method
                // defined within the module.)
                if (sourceType == DebugSourceType.SINGLE_FILE && !cls.name().endsWith(context.getFileName().get())) {
                    continue;
                }
                // If the sources reside inside a ballerina module/project, generated class name should start with the
                // organization name of the ballerina module/project source.
                if (sourceType == DebugSourceType.PACKAGE && !cls.name().startsWith(context.getPackageOrg().get())) {
                    continue;
                }
                List<Method> methods = cls.methodsByName(functionName);
                for (Method method : methods) {
                    // Note - All the ballerina functions are represented as java static methods and all the generated
                    // jvm methods contain strand as its first argument.
                    if (method.isStatic()) {
                        return Optional.of(new GeneratedStaticMethod(context, cls, method));
                    }
                }
            } catch (ClassNotPreparedException ignored) {
                // Unprepared classes should be skipped.
            }
        }
        return Optional.empty();
    }

    /**
     * Loads the generated jvm method of the particular ballerina function.
     *
     * @return JvmMethod instance
     */
    private Optional<GeneratedStaticMethod> loadFunction() throws EvaluationException {
        // If the debug source is a ballerina module file and the method is still not loaded into the JVM, we have
        // iterate over all the classes generated for this particular ballerina module and check each class for a
        // matching method.
        if (context.getSourceType() == DebugSourceType.PACKAGE) {
            List<String> moduleFiles = PackageUtils.getModuleClassNames(context);
            for (String fileName : moduleFiles) {
                String className = fileName.replace(BAL_FILE_EXT, "").replace(File.separator, ".");
                className = className.startsWith(".") ? className.substring(1) : className;
                String qualifiedClassName = PackageUtils.getQualifiedClassName(context, className);
                ReferenceType refType = EvaluationUtils.loadClass(context, qualifiedClassName, functionName);
                List<Method> methods = refType.methodsByName(functionName);
                if (!methods.isEmpty()) {
                    return Optional.of(new GeneratedStaticMethod(context, refType, methods.get(0)));
                }
            }
            return Optional.empty();
        } else {
            // If the source is a single bal file, the method(class)must be loaded by now already.
            throw new EvaluationException(String.format(EvaluationExceptionKind.FUNCTION_NOT_FOUND.getString(),
                    functionName));
        }
    }
}
