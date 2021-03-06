/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.security.privilege;

import java.util.Collections;
import java.util.Set;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeDefinition;

/**
 * PrivilegeDefinitionImpl... TODO
 */
class PrivilegeDefinitionImpl implements PrivilegeDefinition {

    private final String name;
    private final boolean isAbstract;
    private final Set<String> declaredAggregateNames;

    PrivilegeDefinitionImpl(String name, boolean isAbstract,
                            Set<String> declaredAggregateNames) {
        this.name = name;
        this.isAbstract = isAbstract;
        this.declaredAggregateNames = declaredAggregateNames;
    }

    PrivilegeDefinitionImpl(String name, boolean isAbstract,
                            String... declaredAggregateNames) {
        this(name, isAbstract, (declaredAggregateNames == null) ?
                Collections.<String>emptySet() :
                ImmutableSet.copyOf(declaredAggregateNames));
    }

    //------------------------------------------------< PrivilegeDefinition >---
    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isAbstract() {
        return isAbstract;
    }

    @Nonnull
    @Override
    public Set<String> getDeclaredAggregateNames() {
        return declaredAggregateNames;
    }

    //-------------------------------------------------------------< Object >---
    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (isAbstract ? 1 : 0);
        result = 31 * result + declaredAggregateNames.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof PrivilegeDefinitionImpl) {
            PrivilegeDefinitionImpl other = (PrivilegeDefinitionImpl) o;
            return name.equals(other.name) &&
                    isAbstract == other.isAbstract &&
                    declaredAggregateNames.equals(other.declaredAggregateNames);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "PrivilegeDefinition: " + name;
    }
}