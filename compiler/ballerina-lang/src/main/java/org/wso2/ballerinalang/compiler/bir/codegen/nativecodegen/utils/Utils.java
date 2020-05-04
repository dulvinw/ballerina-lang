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

import org.wso2.ballerinalang.compiler.bir.model.BIRNode;
import org.wso2.ballerinalang.compiler.util.Name;

import static org.wso2.ballerinalang.compiler.bir.codegen.jvmcodegen.JvmConstants.FILE_NAME_PERIOD_SEPERATOR;

public class Utils {

    public static String getPackageName(Name orgName, Name moduleName) {

        return getPackageNameFromString(orgName.getValue(), moduleName.getValue());
    }

    public static String getPackageNameFromString(String orgName, String moduleName) {

        String packageName = "";
        if (!moduleName.equals(".")) {
            packageName = cleanupName(moduleName) + "/";
        }

        if (!orgName.equalsIgnoreCase("$anon")) {
            packageName = cleanupName(orgName) + "/" + packageName;
        }

        return packageName;
    }

    public static String cleanupName(String name) {

        return name.replace(".", "_");
    }

    public static String cleanupSourceFileName(String name) {

        return name.replace(".", FILE_NAME_PERIOD_SEPERATOR);
    }

    public static String cleanupPackageName(String pkgName) {

        int index = pkgName.lastIndexOf("/");
        if (index > 0) {
            return pkgName.substring(0, index);
        } else {
            return pkgName;
        }
    }

    public static String localVariableNameWithPrefix(BIRNode.BIRVariableDcl localVar) {
        String localVarName = localVariableName(localVar);
        return addPrefixToLocalVarName(localVarName);
    }

    private static String addPrefixToLocalVarName(String localVarName) {
        return "local" + localVarName;
    }

    public static String localVariableName(BIRNode.BIRVariableDcl localVar) throws RuntimeException {
        String name = localVar.name.value;
        if (name != null) {
            return name.substring(1);
        }

        throw new RuntimeException("Local Variable either doesn't have a name or is not string");
    }
}
