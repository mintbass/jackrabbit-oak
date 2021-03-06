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
package org.apache.jackrabbit.oak.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.PropertyType;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.oak.api.CoreValue;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.tika.mime.MediaType;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

class JsonRepresentation implements Representation {

    private final MediaType type;

    private final JsonFactory factory;

    public JsonRepresentation(MediaType type, JsonFactory factory) {
        this.type = type;
        this.factory = factory;
    }

    @Override
    public MediaType getType() {
        return type;
    }

    public void render(Tree tree, HttpServletResponse response)
            throws IOException {
        JsonGenerator generator = startResponse(response);
        render(tree, generator);
        generator.close();
    }

    public void render(PropertyState property, HttpServletResponse response)
            throws IOException {
        JsonGenerator generator = startResponse(response);
        render(property, generator);
        generator.close();
    }

    protected JsonGenerator startResponse(HttpServletResponse response)
            throws IOException {
        response.setContentType(type.toString());
        return factory.createJsonGenerator(response.getOutputStream());
    }

    private void render(Tree tree, JsonGenerator generator)
            throws IOException {
        generator.writeStartObject();
        for (PropertyState property : tree.getProperties()) {
            generator.writeFieldName(property.getName());
            render(property, generator);
        }
        for (Tree child : tree.getChildren()) {
            generator.writeFieldName(child.getName());
            generator.writeStartObject();
            generator.writeEndObject();
        }
        generator.writeEndObject();
    }

    private void render(PropertyState property, JsonGenerator generator)
            throws IOException {
        if (property.isArray()) {
            generator.writeStartArray();
            for (CoreValue value : property.getValues()) {
                render(value, generator);
            }
            generator.writeEndArray();
        } else {
            render(property.getValue(), generator);
        }
    }

    private void render(CoreValue value, JsonGenerator generator)
            throws IOException {
        // TODO: Type info?
        if (value.getType() == PropertyType.BOOLEAN) {
            generator.writeBoolean(value.getBoolean());
        } else if (value.getType() == PropertyType.DECIMAL) {
            generator.writeNumber(value.getDecimal());
        } else if (value.getType() == PropertyType.DOUBLE) {
            generator.writeNumber(value.getDouble());
        } else if (value.getType() == PropertyType.LONG) {
            generator.writeNumber(value.getLong());
        } else if (value.getType() == PropertyType.BINARY) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            InputStream stream = value.getNewStream();
            try {
                byte[] b = new byte[1024];
                int n = stream.read(b);
                while (n != -1) {
                    buffer.write(b, 0, n);
                    n = stream.read(b);
                }
            } finally {
                stream.close();
            }
            generator.writeBinary(buffer.toByteArray());
        } else {
            generator.writeString(value.getString());
        }
    }

}
