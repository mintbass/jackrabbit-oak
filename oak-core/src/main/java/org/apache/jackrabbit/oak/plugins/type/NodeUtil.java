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
package org.apache.jackrabbit.oak.plugins.type;

import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.oak.api.CoreValue;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.namepath.NameMapper;

/**
 * Utility class for accessing typed content of a node.
 */
class NodeUtil {

    private final NameMapper mapper;

    private final Tree tree;

    public NodeUtil(NameMapper mapper, Tree tree) {
        this.mapper = mapper;
        this.tree = tree;
    }

    public String getName() {
        return mapper.getJcrName(tree.getName());
    }

    public boolean getBoolean(String name) {
        PropertyState property = tree.getProperty(name);
        return property != null && !property.isArray()
                && property.getValue().getBoolean();
    }

    public String getString(String name, String defaultValue) {
        PropertyState property = tree.getProperty(name);
        if (property != null && !property.isArray()) {
            return property.getValue().getString();
        } else {
            return defaultValue;
        }
    }

    public String[] getStrings(String name) {
        PropertyState property = tree.getProperty(name);
        if (property == null) {
            return null;
        }

        List<CoreValue> values = property.getValues();
        String[] strings = new String[values.size()];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = values.get(i).getString();
        }
        return strings;
    }

    public String getName(String name) {
        return getName(name, null);
    }

    public String getName(String name, String defaultValue) {
        PropertyState property = tree.getProperty(name);
        if (property != null && !property.isArray()) {
            return mapper.getJcrName(property.getValue().getString());
        } else {
            return defaultValue;
        }
    }

    public String[] getNames(String name, String... defaultValues) {
        String[] strings = getStrings(name);
        if (strings == null) {
            strings = defaultValues;
        }
        for (int i = 0; i < strings.length; i++) {
            strings[i] = mapper.getJcrName(strings[i]);
        }
        return strings;
    }

    public NodeUtil[] getNodes(String name) {
        List<NodeUtil> nodes = new ArrayList<NodeUtil>();
        Tree child = tree.getChild(name);
        if (child != null) {
            for (Tree tree : child.getChildren()) {
                nodes.add(new NodeUtil(mapper, tree));
            }
        }
        return nodes.toArray(new NodeUtil[nodes.size()]);
    }

}