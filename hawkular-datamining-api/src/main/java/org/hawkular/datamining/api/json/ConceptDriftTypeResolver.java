/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hawkular.datamining.api.json;

import org.hawkular.datamining.forecast.AutomaticForecaster;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * @author Pavol Loffay
 */
public class ConceptDriftTypeResolver implements TypeIdResolver {

    private static final String PACKAGE_FOR_INNER_CLASS =
            AutomaticForecaster.ConceptDriftStrategy.class.getName().substring(0,
                    AutomaticForecaster.ConceptDriftStrategy.class.getName().indexOf("$"));

    private JavaType mBaseType;

    @Override
    public void init(JavaType baseType) {
        this.mBaseType = baseType;
    }

    @Override
    public String idFromValue(Object value) {
        return idFromValueAndType(value, value.getClass());
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        String simpleName = suggestedType.getSimpleName();

        return simpleName;
    }

    @Override
    public String idFromBaseType() {
        return idFromValueAndType(null, mBaseType.getRawClass());
    }

    @Override
    public JavaType typeFromId(String id) {
        Class<?> clazz;
        String clazzName = PACKAGE_FOR_INNER_CLASS + "$" + id;
        try {
            clazz = ClassUtil.findClass(clazzName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("cannot find class '" + clazzName + "'");
        }
        return TypeFactory.defaultInstance().constructSpecializedType(mBaseType, clazz);
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) {
        return typeFromId(id);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }
}
