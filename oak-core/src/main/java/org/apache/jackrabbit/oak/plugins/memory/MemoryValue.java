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
package org.apache.jackrabbit.oak.plugins.memory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.PropertyType;

import org.apache.jackrabbit.oak.api.CoreValue;
import org.apache.jackrabbit.util.ISO8601;

public abstract class MemoryValue implements CoreValue {

    @Override
    public long getLong() {
        return Long.parseLong(getString());
    }

    @Override
    public double getDouble() {
        return Double.parseDouble(getString());
    }

    @Override
    public boolean getBoolean() {
        throw new UnsupportedOperationException("Unsupported conversion.");
    }

    @Override
    public BigDecimal getDecimal() {
        return new BigDecimal(getString());
    }

    @Override
    public InputStream getNewStream() {
        try {
            return new ByteArrayInputStream(getString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is not supported", e);
        }
    }

    @Override
    public long length() {
        return getString().length();
    }

    //----------------------------------------------------------< Comparable >

    @Override
    public int compareTo(CoreValue o) {
        if (this == o) {
            return 0;
        }

        int type = getType();
        if (type != o.getType()) {
            return o.getType() - type;
        } else if (type == PropertyType.LONG) {
            return Long.signum(getLong() - o.getLong());
        } else if (type == PropertyType.DOUBLE) {
            return Double.compare(getDouble(), o.getDouble());
        } else if (type == PropertyType.DECIMAL) {
            return getDecimal().compareTo(o.getDecimal());
        } else if (type == PropertyType.BOOLEAN) {
            return (getBoolean() ? 1 : 0) - (o.getBoolean() ? 1 : 0);
        } else if (type == PropertyType.DATE) {
            Calendar a = ISO8601.parse(getString());
            Calendar b = ISO8601.parse(o.getString());
            if (a != null && b != null) {
                return a.compareTo(b);
            } else {
                return getString().compareTo(o.getString());
            }
        } else {
            return getString().compareTo(o.getString());
        }
    }

    //--------------------------------------------------------------< Object >

    @Override
    public int hashCode() {
        return getType() ^ getString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof CoreValue) {
            return compareTo((CoreValue) o) == 0;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return getString();
    }

}
