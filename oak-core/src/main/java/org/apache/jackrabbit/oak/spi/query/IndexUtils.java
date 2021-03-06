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
package org.apache.jackrabbit.oak.spi.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.commons.PathUtils;
import org.apache.jackrabbit.oak.spi.state.ChildNodeEntry;
import org.apache.jackrabbit.oak.spi.state.NodeState;

public class IndexUtils {

    /**
     * switch to "oak:index" as soon as it is possible
     */
    public static final String DEFAULT_INDEX_HOME = "/oak-index";

    /**
     * Builds an {@link IndexDefinition} out of a {@link ChildNodeEntry}
     * 
     */
    public static IndexDefinition getDefinition(String path, ChildNodeEntry def) {
        String name = def.getName();
        PropertyState typeProp = def.getNodeState().getProperty(
                IndexDefinition.TYPE_PROPERTY_NAME);
        if (typeProp == null || typeProp.isArray()) {
            return null;
        }
        String type = typeProp.getValue().getString();

        boolean unique = false;
        PropertyState uniqueProp = def.getNodeState().getProperty(
                IndexDefinition.UNIQUE_PROPERTY_NAME);
        if (uniqueProp != null && !uniqueProp.isArray()) {
            unique = uniqueProp.getValue().getBoolean();
        }

        Map<String, String> props = new HashMap<String, String>();
        for (PropertyState ps : def.getNodeState().getProperties()) {
            if (ps != null && !ps.isArray()) {
                String v = ps.getValue().getString();
                props.put(ps.getName(), v);
            }
        }
        return new IndexDefinitionImpl(name, type,
                PathUtils.concat(path, name), unique, props);
    }

    /**
     * Splits a give path into its segments and optionally appends a new path to
     * the resulting array
     * 
     * @return array containing the path segments
     */
    public static String[] split(String pathIn, String append) {
        List<String> paths = new ArrayList<String>();
        for (String p : pathIn.split("/")) {
            if (p.trim().length() != 0) {
                paths.add(p);
            }
        }
        if (append != null && append.trim().length() != 0) {
            paths.add(append);
        }
        return paths.toArray(new String[paths.size()]);
    }

    /**
     * @return the 'destination' node if the path exists, null if otherwise
     */
    public static NodeState getNode(NodeState nodeState, String destination) {
        NodeState retval = nodeState;
        Iterator<String> pathIterator = PathUtils.elements(destination)
                .iterator();
        while (pathIterator.hasNext()) {
            String path = pathIterator.next();
            if (retval.hasChildNode(path)) {
                retval = retval.getChildNode(path);
            } else {
                return null;
            }
        }
        return retval;
    }

}
