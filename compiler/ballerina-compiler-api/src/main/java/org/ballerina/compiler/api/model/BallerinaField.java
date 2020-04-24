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
package org.ballerina.compiler.api.model;

import org.ballerina.compiler.api.semantic.TypesFactory;
import org.ballerina.compiler.api.types.TypeDescriptor;
import org.wso2.ballerinalang.compiler.semantics.model.types.BField;
import org.wso2.ballerinalang.util.Flags;

/**
 * Represents a field with a name and type.
 * 
 * @since 1.3.0
 */
public class BallerinaField {
    // add the metadata field
    private BField bField;
    private TypeDescriptor typeDescriptor;

    public BallerinaField(BField bField) {
        this.bField = bField;
        this.typeDescriptor = TypesFactory.getTypeDescriptor(bField.getType());
    }

    /**
     * Get the field name.
     * 
     * @return {@link String} name of the field
     */
    public String getFieldName() {
        return this.bField.getName().getValue();
    }

    /**
     * Whether optional field or not.
     * 
     * @return {@link Boolean} optional status
     */
    public boolean isOptional() {
        return (this.bField.type.flags & Flags.OPTIONAL) == Flags.OPTIONAL;
    }

    /**
     * Get the type descriptor of the field.
     * 
     * @return {@link TypeDescriptor} of the field
     */
    public TypeDescriptor getTypeDescriptor() {
        return TypesFactory.getTypeDescriptor(this.bField.getType());
    }

    /**
     * Get the signature of the field.
     * 
     * @return {}
     */
    public String getSignature() {
        StringBuilder signature = new StringBuilder(this.typeDescriptor.getSignature() + " " + this.getFieldName());
        if (this.isOptional()) {
            signature.append("?");
        }
        
        return signature.toString();
    }
}
