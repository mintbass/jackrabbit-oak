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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.commons.iterator.NodeTypeIteratorAdapter;
import org.apache.jackrabbit.oak.util.NodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.JcrConstants.JCR_CHILDNODEDEFINITION;
import static org.apache.jackrabbit.JcrConstants.JCR_HASORDERABLECHILDNODES;
import static org.apache.jackrabbit.JcrConstants.JCR_ISMIXIN;
import static org.apache.jackrabbit.JcrConstants.JCR_NODETYPENAME;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYITEMNAME;
import static org.apache.jackrabbit.JcrConstants.JCR_PROPERTYDEFINITION;
import static org.apache.jackrabbit.JcrConstants.JCR_SUPERTYPES;
import static org.apache.jackrabbit.oak.plugins.type.NodeTypeConstants.JCR_IS_ABSTRACT;
import static org.apache.jackrabbit.oak.plugins.type.NodeTypeConstants.JCR_IS_QUERYABLE;

/**
 * <pre>
 * [nt:nodeType]
 * - jcr:nodeTypeName (NAME) protected mandatory
 * - jcr:supertypes (NAME) protected multiple
 * - jcr:isAbstract (BOOLEAN) protected mandatory
 * - jcr:isQueryable (BOOLEAN) protected mandatory
 * - jcr:isMixin (BOOLEAN) protected mandatory
 * - jcr:hasOrderableChildNodes (BOOLEAN) protected mandatory
 * - jcr:primaryItemName (NAME) protected
 * + jcr:propertyDefinition (nt:propertyDefinition) = nt:propertyDefinition protected sns
 * + jcr:childNodeDefinition (nt:childNodeDefinition) = nt:childNodeDefinition protected sns
 * </pre>
 */
class NodeTypeImpl implements NodeType {

    private static final Logger log =
            LoggerFactory.getLogger(NodeTypeImpl.class);

    private final NodeTypeManager manager;

    private final ValueFactory factory;

    private final NodeUtil node;

    public NodeTypeImpl(
            NodeTypeManager manager, ValueFactory factory, NodeUtil node) {
        this.manager = manager;
        this.factory = factory;
        this.node = node;
    }

    @Override
    public String getName() {
        String name = node.getName(JCR_NODETYPENAME);
        if (name == null) {
            name = node.getName();
        }
        return name;
    }

    @Override
    public String[] getDeclaredSupertypeNames() {
        return node.getNames(JCR_SUPERTYPES);
    }

    @Override
    public boolean isAbstract() {
        return node.getBoolean(JCR_IS_ABSTRACT);
    }

    @Override
    public boolean isMixin() {
        return node.getBoolean(JCR_ISMIXIN);
    }

    @Override
    public boolean hasOrderableChildNodes() {
        return node.getBoolean(JCR_HASORDERABLECHILDNODES);
    }

    @Override
    public boolean isQueryable() {
        return node.getBoolean(JCR_IS_QUERYABLE);
    }

    @Override
    public String getPrimaryItemName() {
        return node.getName(JCR_PRIMARYITEMNAME);
    }

    @Override
    public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        List<NodeUtil> nodes = node.getNodes(JCR_PROPERTYDEFINITION);
        PropertyDefinition[] definitions = new PropertyDefinition[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            definitions[i] = new PropertyDefinitionImpl(
                    this, factory, nodes.get(i));
        }
        return definitions;
    }

    @Override
    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        List<NodeUtil> nodes = node.getNodes(JCR_CHILDNODEDEFINITION);
        NodeDefinition[] definitions = new NodeDefinition[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            definitions[i] = new NodeDefinitionImpl(manager, this, nodes.get(i));
        }
        return definitions;
    }

    @Override
    public NodeType[] getSupertypes() {
        Collection<NodeType> types = new ArrayList<NodeType>();
        Set<String> added = new HashSet<String>();
        Queue<String> queue = new LinkedList<String>(Arrays.asList(
                getDeclaredSupertypeNames()));
        while (!queue.isEmpty()) {
            String name = queue.remove();
            if (added.add(name)) {
                try {
                    NodeType type = manager.getNodeType(name);
                    types.add(type);
                    queue.addAll(Arrays.asList(type.getDeclaredSupertypeNames()));
                } catch (RepositoryException e) {
                    throw new IllegalStateException("Inconsistent node type: " + this, e);
                }
            }
        }
        return types.toArray(new NodeType[types.size()]);
    }

    @Override
    public NodeType[] getDeclaredSupertypes() {
        String[] names = getDeclaredSupertypeNames();
        List<NodeType> types = new ArrayList<NodeType>(names.length);
        for (String name : names) {
            try {
                NodeType type = manager.getNodeType(name);
                types.add(type);
            }
            catch (RepositoryException e) {
                log.warn("Unable to access declared supertype "
                        + name + " of " + getName(), e);
            }
        }
        return types.toArray(new NodeType[types.size()]);
    }

    @Override
    public NodeTypeIterator getSubtypes() {
        Collection<NodeType> types = new ArrayList<NodeType>();
        try {
            NodeTypeIterator iterator = manager.getAllNodeTypes();
            while (iterator.hasNext()) {
                NodeType type = iterator.nextNodeType();
                if (type.isNodeType(getName()) && !isNodeType(type.getName())) {
                    types.add(type);
                }
            }
        } catch (RepositoryException e) {
            log.warn("Unable to access subtypes of " + getName(), e);
        }
        return new NodeTypeIteratorAdapter(types);
    }

    @Override
    public NodeTypeIterator getDeclaredSubtypes() {
        Collection<NodeType> types = new ArrayList<NodeType>();
        try {
            NodeTypeIterator iterator = manager.getAllNodeTypes();
            while (iterator.hasNext()) {
                NodeType type = iterator.nextNodeType();
                String name = type.getName();
                if (type.isNodeType(getName()) && !isNodeType(name)) {
                    List<String> declaredSuperTypeNames = Arrays.asList(type.getDeclaredSupertypeNames());
                    if (declaredSuperTypeNames.contains(name)) {
                        types.add(type);
                    }
                }
            }
        } catch (RepositoryException e) {
            log.warn("Unable to access declared subtypes of " + getName(), e);
        }
        return new NodeTypeIteratorAdapter(types);
    }

    @Override
    public boolean isNodeType(String nodeTypeName) {
        if (nodeTypeName.equals(getName())) {
            return true;
        }

        for (NodeType type : getDeclaredSupertypes()) {
            if (type.isNodeType(nodeTypeName)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public PropertyDefinition[] getPropertyDefinitions() {
        // TODO distinguish between additive and overriding property definitions. See 3.7.6.8 Item Definitions in Subtypes
        Collection<PropertyDefinition> definitions =
                new ArrayList<PropertyDefinition>();
        for (NodeType type : getSupertypes()) {
            definitions.addAll(Arrays.asList(
                    type.getDeclaredPropertyDefinitions()));
        }
        definitions.addAll(Arrays.asList(getDeclaredPropertyDefinitions()));
        return definitions.toArray(new PropertyDefinition[definitions.size()]);
    }

    @Override
    public NodeDefinition[] getChildNodeDefinitions() {
        // TODO distinguish between additive and overriding node definitions. See 3.7.6.8 Item Definitions in Subtypes
        Collection<NodeDefinition> definitions =
                new ArrayList<NodeDefinition>();
        for (NodeType type : getSupertypes()) {
            definitions.addAll(Arrays.asList(
                    type.getDeclaredChildNodeDefinitions()));
        }
        definitions.addAll(Arrays.asList(getDeclaredChildNodeDefinitions()));
        return definitions.toArray(new NodeDefinition[definitions.size()]);
    }

    @Override
    public boolean canSetProperty(String propertyName, Value value) {
        for (PropertyDefinition definition : getPropertyDefinitions()) {
            String name = definition.getName();
            if ((propertyName.equals(name) && !definition.isProtected())
                    || "*".equals(name)) {
                if (!definition.isMultiple()) {
                    // TODO: Check value type, constraints, etc.
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean canSetProperty(String propertyName, Value[] values) {
        for (PropertyDefinition definition : getPropertyDefinitions()) {
            String name = definition.getName();
            if ((propertyName.equals(name) && !definition.isProtected())
                    || "*".equals(name)) {
                if (definition.isMultiple()) {
                    // TODO: Check value type, constraints, etc.
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean canAddChildNode(String childNodeName) {
        for (NodeDefinition definition : getChildNodeDefinitions()) {
            String name = definition.getName();
            if ((childNodeName.equals(name) && !definition.isProtected())
                    || "*".equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canAddChildNode(String childNodeName, String nodeTypeName) {
        NodeType type;
        try {
            type = manager.getNodeType(nodeTypeName);
        } catch (NoSuchNodeTypeException e) {
            return false;
        } catch (RepositoryException e) {
            log.warn("Unable to access node type " + nodeTypeName, e);
            return false;
        }
        for (NodeDefinition definition : getChildNodeDefinitions()) {
            String name = definition.getName();
            if ((childNodeName.equals(name) && !definition.isProtected())
                    || "*".equals(name)) {
                for (String required : definition.getRequiredPrimaryTypeNames()) {
                    if (type.isNodeType(required)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean canRemoveItem(String itemName) {
        return canRemoveNode(itemName) || canRemoveProperty(itemName);
    }

    @Override
    public boolean canRemoveNode(String nodeName) {
        for (PropertyDefinition definition : getPropertyDefinitions()) {
            String name = definition.getName();
            if (nodeName.equals(name)) {
                if (definition.isMandatory() || definition.isProtected()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean canRemoveProperty(String propertyName) {
        for (PropertyDefinition definition : getPropertyDefinitions()) {
            String name = definition.getName();
            if (propertyName.equals(name)) {
                if (definition.isMandatory() || definition.isProtected()) {
                    return false;
                }
            }
        }
        return true;
    }

}
