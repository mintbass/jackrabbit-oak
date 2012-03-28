/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.jackrabbit.oak.jcr.query;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.QueryObjectModelFactory;
import org.apache.jackrabbit.mk.api.MicroKernel;
import org.apache.jackrabbit.oak.jcr.SessionContext;
import org.apache.jackrabbit.oak.jcr.SessionImpl;
import org.apache.jackrabbit.oak.jcr.WorkspaceImpl;
import org.apache.jackrabbit.oak.jcr.query.qom.QueryObjectModelFactoryImpl;
import org.apache.jackrabbit.oak.query.CoreValue;
import org.apache.jackrabbit.oak.query.QueryEngine;
import org.apache.jackrabbit.oak.query.Result;

/**
 * The implementation of the corresponding JCR interface.
 */
public class QueryManagerImpl implements QueryManager {

    private final QueryObjectModelFactoryImpl qomFactory = new QueryObjectModelFactoryImpl();
    private final QueryEngine qe;

    public QueryManagerImpl(WorkspaceImpl workspace, SessionContext<SessionImpl> sessionContext) {
        MicroKernel mk = sessionContext.getGlobalContext().getInstance(MicroKernel.class);
        qe = new QueryEngine(mk);
    }

    @Override
    public Query createQuery(String statement, String language) throws RepositoryException {
        return new QueryImpl(this, statement, language);
    }

    @Override
    public QueryObjectModelFactory getQOMFactory() {
        return qomFactory;
    }

    @Override
    public Query getQuery(Node node) throws RepositoryException {
        // TODO getQuery(Node node): is it needed?
        throw new RepositoryException("Feature not implemented");
    }

    @Override
    public String[] getSupportedQueryLanguages() throws RepositoryException {
        @SuppressWarnings("deprecation")
        // create a new instance each time because the array is mutable
        // (the caller could modify it)
        String[] s = {
            Query.JCR_JQOM,
            Query.JCR_SQL2,
            Query.XPATH
        };
        return s;
    }

    public List<String> parse(String statement, String language) throws InvalidQueryException {
        try {
            return qe.parse(statement, convertLanguage(language));
        } catch (ParseException e) {
            throw new InvalidQueryException(e);
        }
    }

    public QueryResult executeQuery(String statement, String language,
            HashMap<String, Value> bindVariableMap, long limit, long offset) throws RepositoryException {
        try {
            HashMap<String, CoreValue> bindMap = convertMap(bindVariableMap);
            Result r = qe.executeQuery(statement, convertLanguage(language), bindMap);
            return new QueryResultImpl(r);
        } catch (ParseException e) {
            throw new InvalidQueryException(e);
        }
    }

    @SuppressWarnings("deprecation")
    private static String convertLanguage(String jcrLanguage) throws InvalidQueryException {
        if (jcrLanguage.equals(Query.JCR_SQL2)) {
            return QueryEngine.SQL2;
        } else if (jcrLanguage.equals(Query.XPATH)) {
            return QueryEngine.XPATH;
        } else {
            throw new InvalidQueryException("Unsupported language: " + jcrLanguage);
        }
    }

    private HashMap<String, CoreValue> convertMap(HashMap<String, Value> bindVariableMap) throws RepositoryException {
        HashMap<String, CoreValue> map = new HashMap<String, CoreValue>();
        for (Entry<String, Value> e : bindVariableMap.entrySet()) {
            map.put(e.getKey(), ValueConverter.convert(e.getValue()));
        }
        return map;
    }

}