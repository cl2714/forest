package com.dtflys.forest.interceptor;

import com.dtflys.forest.callback.OnError;
import com.dtflys.forest.callback.OnProgress;
import com.dtflys.forest.callback.OnSuccess;
import com.dtflys.forest.exceptions.ForestRuntimeException;
import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.http.ForestResponse;
import com.dtflys.forest.reflection.ForestMethod;
import com.dtflys.forest.utils.ForestProgress;
import scala.Int;

import java.util.Map;

/**
 * @author gongjun[dt_flys@hotmail.com]
 * @since 2016-06-26
 */
public interface Interceptor<T> extends OnSuccess<T>, OnError, OnProgress {


    default void onInvokeMethod(ForestRequest request, ForestMethod method, Object[] args) {
    }

    default boolean beforeExecute(ForestRequest request) {
        return true;
    }

    default void afterExecute(ForestRequest request, ForestResponse response) {
    }

    @Override
    default void onProgress(ForestProgress progress) {
    }

    default InterceptorAttributes getAttributes(ForestRequest request) {
        return request.getInterceptorAttributes(this.getClass());
    }

    default void addAttribute(ForestRequest request, String name, Object value) {
        request.addInterceptorAttribute(this.getClass(), name, value);
    }

    default Object getAttribute(ForestRequest request, String name) {
        return request.getInterceptorAttribute(this.getClass(), name);
    }

    default <T> T getAttribute(ForestRequest request, String name, Class<T> clazz) {
        Object obj = request.getInterceptorAttribute(this.getClass(), name);
        if (obj == null) {
            return null;
        }
        return (T) obj;
    }


    default String getAttributeAsString(ForestRequest request, String name) {
        Object attr = getAttribute(request, name);
        if (attr == null) {
            return null;
        }
        return String.valueOf(attr);
    }

    default Integer getAttributeAsInteger(ForestRequest request, String name) {
        Object attr = getAttribute(request, name);
        if (attr == null) {
            return null;
        }
        return (Integer) attr;
    }

    default Float getAttributeAsFloat(ForestRequest request, String name) {
        Object attr = getAttribute(request, name);
        if (attr == null) {
            return null;
        }
        return (Float) attr;
    }

    default Double getAttributeAsDouble(ForestRequest request, String name) {
        Object attr = getAttribute(request, name);
        if (attr == null) {
            return null;
        }
        return (Double) attr;
    }

}
