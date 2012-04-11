/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.api;

/**
 * Storage abstraction for content trees. At any given point in time
 * the stored content tree is rooted at a single immutable node state.
 * Changes in the tree are constructed by branching off a private copy
 * using the {@link #branch(NodeState)} method which can be modified
 * and merged back using the {@link #merge(NodeStateEditor, NodeState)}
 * method.
 * <p>
 * This is a low-level interface that doesn't cover functionality like
 * merging concurrent changes or rejecting new tree states based on some
 * higher-level consistency constraints.
 */
public interface NodeStore {

    /**
     * Returns the latest state of the content tree.
     *
     * @return root node state
     */
    NodeState getRoot();

    /**
     * Creates a private branch from a {@code base} node state
     * for editing. The branch can later be merged back into
     * the node store using the {@link #merge(NodeStateEditor, NodeState) merge}
     * method.
     *
     * @param base base node state
     * @return a private branch rooted at {@code base}
     */
    NodeStateEditor branch(NodeState base);

    /**
     * Atomically merges the changes from {@code branch} back into the
     * {@code target}.
     *
     * @param branch branch for merging into {@code target}
     * @param target target of the merge operation
     * @return node state resulting from merging {@code branch} into
     *         {@code target}.
     */
    NodeState merge(NodeStateEditor branch, NodeState target);

    /**
     * Compares the given two node states. Any found differences are
     * reported by calling the relevant added, changed or deleted methods
     * of the given handler.
     *
     * @param before node state before changes
     * @param after node state after changes
     * @param diff handler of node state differences
     */
    void compare(NodeState before, NodeState after, NodeStateDiff diff);

}