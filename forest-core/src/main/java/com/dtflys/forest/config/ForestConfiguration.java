/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Gong
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dtflys.forest.config;


import com.dtflys.forest.converter.auto.DefaultAutoConverter;
import com.dtflys.forest.converter.binary.DefaultBinaryConverter;
import com.dtflys.forest.converter.text.DefaultTextConverter;
import com.dtflys.forest.interceptor.DefaultInterceptorFactory;
import com.dtflys.forest.interceptor.InterceptorFactory;
import com.dtflys.forest.proxy.ProxyFactory;
import com.dtflys.forest.retryer.BackOffRetryer;
import com.dtflys.forest.retryer.Retryer;
import com.dtflys.forest.ssl.SSLKeyStore;
import com.dtflys.forest.ssl.SSLUtils;
import com.dtflys.forest.utils.ForestDataType;
import com.dtflys.forest.utils.RequestNameValue;
import com.dtflys.forest.backend.HttpBackendSelector;
import com.dtflys.forest.converter.ForestConverter;
import com.dtflys.forest.converter.json.JSONConverterSelector;
import com.dtflys.forest.converter.json.ForestJsonConverter;
import com.dtflys.forest.converter.xml.ForestJaxbConverter;
import com.dtflys.forest.converter.xml.ForestXmlConverter;
import com.dtflys.forest.exceptions.ForestRuntimeException;
import com.dtflys.forest.backend.HttpBackend;
import com.dtflys.forest.filter.Filter;
import com.dtflys.forest.filter.JSONFilter;
import com.dtflys.forest.filter.XmlFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 * global configuration
 * @author gongjun[dt_flys@hotmail.com]
 * @since 2016-03-24
 */
public class ForestConfiguration implements Serializable {

    private static Logger log = LoggerFactory.getLogger(ForestConfiguration.class);

    private static ForestConfiguration defaultConfiguration = configuration();

    private Map<Class, Object> instanceCache = new HashMap<>();

    private String id;

    /**
     * maximum number of conntections allowed
     */
    private Integer maxConnections;

    /**
     * maximum number of connections allowed per route
     */
    private Integer maxRouteConnections;

    /**
     * timeout in milliseconds
     */
    private Integer timeout;

    /**
     * request charset
     */
    private String charset = "UTF-8";

    /**
     * connect timeout in milliseconds
     */
    private Integer connectTimeout;

    /**
     * Class of retryer
     */
    private Class retryer;

    /**
     * count of retry times
     */
    private Integer retryCount;

    /**
     * max interval of retrying request
     */
    private long maxRetryInterval;

    /**
     * default SSL protocol
     */
    private String sslProtocol = SSLUtils.TLSv1_2;

    /**
     * Enable log forest request info
     */
    private boolean logEnabled = true;

    /**
     * Enable cache of request interface instance
     */
    private boolean cacheEnabled = true;

    private volatile HttpBackend backend;

    private String backendName;

    private List<RequestNameValue> defaultParameters;

    private List<RequestNameValue> defaultHeaders;

    private List<Class> interceptors;

    private Map<ForestDataType, ForestConverter> converterMap;

    private JSONConverterSelector jsonConverterSelector;

    private InterceptorFactory interceptorFactory;

    private HttpBackendSelector httpBackendSelector = new HttpBackendSelector();

    private Map<String, Class> filterRegisterMap = new HashMap<>();

    private Map<String, Object> variables = new HashMap<String, Object>();

    private Map<String, SSLKeyStore> sslKeyStores = new HashMap<>();

    private ForestConfiguration() {
    }


    public static ForestConfiguration getDefaultConfiguration() {
        return defaultConfiguration;
    }

    /**
     * 实例化ForestConfiguration对象，并初始化默认值
     * @return
     */
    public static ForestConfiguration configuration() {
        ForestConfiguration configuration = new ForestConfiguration();
        configuration.setId("forestConfiguration" + configuration.hashCode());
        configuration.setJsonConverterSelector(new JSONConverterSelector());
        configuration.setXmlConverter(new ForestJaxbConverter());
        configuration.setTextConverter();
        configuration.getConverterMap().put(ForestDataType.AUTO, new DefaultAutoConverter(configuration));
        configuration.getConverterMap().put(ForestDataType.BINARY, new DefaultBinaryConverter());
        setupJSONConverter(configuration);
        configuration.setTimeout(3000);
        configuration.setConnectTimeout(2000);
        configuration.setMaxConnections(500);
        configuration.setMaxRouteConnections(500);
        configuration.setRetryer(BackOffRetryer.class);
        configuration.setRetryCount(0);
        configuration.setMaxRetryInterval(0);
        configuration.setSslProtocol(SSLUtils.TLSv1_2);
        configuration.registerFilter("json", JSONFilter.class);
        configuration.registerFilter("xml", XmlFilter.class);
        return configuration;
    }

    public Map<Class, Object> getInstanceCache() {
        return instanceCache;
    }

    private ForestConfiguration setupBackend() {
        setBackend(httpBackendSelector.select(this));
//        log.info("[Forest] Http Backend: " + this.backend.getName());
        return this;
    }

    public ForestConfiguration setBackend(HttpBackend backend) {
        if (backend != null) {
            backend.init(this);
            log.info("[Forest] Http Backend: " + backend.getName());
        }
        this.backend = backend;
        return this;
    }

    public ForestConfiguration setBackendName(String backendName) {
        this.backendName = backendName;
        return this;
    }


    public String getBackendName() {
        return backendName;
    }


    public HttpBackend getBackend() {
        if (backend == null) {
            synchronized (this) {
                if (backend == null) {
                    setupBackend();
                }
            }
        }
        return backend;
    }

    public InterceptorFactory getInterceptorFactory() {
        if (interceptorFactory == null) {
            synchronized (this) {
                if (interceptorFactory == null) {
                    interceptorFactory = new DefaultInterceptorFactory();
                }
            }
        }
        return interceptorFactory;
    }

    public void setInterceptorFactory(InterceptorFactory interceptorFactory) {
        this.interceptorFactory = interceptorFactory;
    }

    public ForestConfiguration setHttpBackendSelector(HttpBackendSelector httpBackendSelector) {
        this.httpBackendSelector = httpBackendSelector;
        return this;
    }

    private static void setupJSONConverter(ForestConfiguration configuration) {
        configuration.setJsonConverter(configuration.jsonConverterSelector.select());
    }


    public String getId() {
        return id;
    }

    private ForestConfiguration setId(String id) {
        this.id = id;
        return this;
    }

    public Integer getMaxConnections() {
        return maxConnections;
    }

    public ForestConfiguration setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }

    public Integer getMaxRouteConnections() {
        return maxRouteConnections;
    }

    public ForestConfiguration setMaxRouteConnections(Integer maxRouteConnections) {
        this.maxRouteConnections = maxRouteConnections;
        return this;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public ForestConfiguration setTimeout(Integer timeout) {
        this.timeout = timeout;
        return this;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public ForestConfiguration setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public Class getRetryer() {
        return retryer;
    }

    public void setRetryer(Class retryer) {
        this.retryer = retryer;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public ForestConfiguration setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public long getMaxRetryInterval() {
        return maxRetryInterval;
    }

    public void setMaxRetryInterval(long maxRetryInterval) {
        this.maxRetryInterval = maxRetryInterval;
    }

    public String getSslProtocol() {
        return sslProtocol;
    }

    public void setSslProtocol(String sslProtocol) {
        this.sslProtocol = sslProtocol;
    }

    public boolean isLogEnabled() {
        return logEnabled;
    }

    public void setLogEnabled(boolean logEnabled) {
        this.logEnabled = logEnabled;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public List<RequestNameValue> getDefaultParameters() {
        return defaultParameters;
    }

    public ForestConfiguration setDefaultParameters(List<RequestNameValue> defaultParameters) {
        this.defaultParameters = defaultParameters;
        return this;
    }

    public List<RequestNameValue> getDefaultHeaders() {
        return defaultHeaders;
    }

    public ForestConfiguration setDefaultHeaders(List<RequestNameValue> defaultHeaders) {
        this.defaultHeaders = defaultHeaders;
        return this;
    }

    public List<Class> getInterceptors() {
        return interceptors;
    }

    public void setInterceptors(List<Class> interceptors) {
        this.interceptors = interceptors;
    }

    public ForestConfiguration setJsonConverter(ForestJsonConverter converter) {
        getConverterMap().put(ForestDataType.JSON, converter);
        return this;
    }

    public ForestJsonConverter getJsonConverter() {
        return (ForestJsonConverter) getConverterMap().get(ForestDataType.JSON);
    }

    public ForestConfiguration setXmlConverter(ForestXmlConverter converter) {
        getConverterMap().put(ForestDataType.XML, converter);
        return this;
    }

    private ForestConfiguration setTextConverter() {
        getConverterMap().put(ForestDataType.TEXT, new DefaultTextConverter());
        return this;
    }


    public ForestXmlConverter getXmlConverter() {
        return (ForestXmlConverter) getConverterMap().get(ForestDataType.XML);
    }

    public <T> ProxyFactory<T> getProxyFactory(Class<T> clazz) {
        return new ProxyFactory<T>(this, clazz);
    }


    public ForestConfiguration setVariableValue(String name, Object value) {
        getVariables().put(name, value);
        return this;
    }

    public Object getVariableValue(String name) {
        return getVariables().get(name);
    }

    public Map<String, SSLKeyStore> getSslKeyStores() {
        return sslKeyStores;
    }

    public ForestConfiguration setSslKeyStores(Map<String, SSLKeyStore> sslKeyStores) {
        this.sslKeyStores = sslKeyStores;
        return this;
    }

    /**
     * register a SSL KeyStore object
     * @param keyStore
     */
    public ForestConfiguration registerKeyStore(SSLKeyStore keyStore) {
        sslKeyStores.put(keyStore.getId(), keyStore);
        return this;
    }

    public SSLKeyStore getKeyStore(String id) {
        return sslKeyStores.get(id);
    }

    public ForestConverter getConverter(ForestDataType dataType) {
        ForestConverter converter = getConverterMap().get(dataType);
        if (converter == null) {
            throw new ForestRuntimeException("Can not found converter for type " + dataType.name());
        }
        return converter;
    }

    public Map<ForestDataType, ForestConverter> getConverterMap() {
        if (converterMap == null) {
            converterMap = new HashMap<ForestDataType, ForestConverter>();
        }
        return converterMap;
    }

    public ForestConfiguration setConverterMap(Map<ForestDataType, ForestConverter> converterMap) {
        this.converterMap = converterMap;
        return this;
    }


    public Map<String, Object> getVariables() {
        return variables;
    }

    public ForestConfiguration setVariables(Map<String, Object> variables) {
        this.variables = variables;
        return this;
    }


    public <T> T createInstance(Class<T> clazz) {
        ProxyFactory<T> proxyFactory = getProxyFactory(clazz);
        return proxyFactory.createInstance();
    }


    private ForestConfiguration setJsonConverterSelector(JSONConverterSelector jsonConverterSelector) {
        this.jsonConverterSelector = jsonConverterSelector;
        return this;
    }



    public ForestConfiguration registerFilter(String name, Class filterClass) {
        if (!(Filter.class.isAssignableFrom(filterClass))) {
            throw new ForestRuntimeException("Cannot register class \"" + filterClass.getName()
                    + "\" as a filter, filter class must implement Filter interface!");
        }
        if (filterRegisterMap.containsKey(name)) {
            throw new ForestRuntimeException("filter \"" + name + "\" already exists!");
        }
        filterRegisterMap.put(name, filterClass);
        return this;
    }


    public List<String> getRegisteredFilterNames() {
        Set<String> nameSet = filterRegisterMap.keySet();
        List<String> names = new ArrayList<>();
        Iterator<String> iterator = nameSet.iterator();
        for (; iterator.hasNext(); ) {
            names.add(iterator.next());
        }
        return names;
    }

    public boolean hasFilter(String name) {
        return filterRegisterMap.containsKey(name);
    }

    public Filter newFilterInstance(String name) {
        Class filterClass = filterRegisterMap.get(name);
        if (filterClass == null) {
            throw new ForestRuntimeException("filter \"" + name + "\" does not exists!");
        }
        try {
            return (Filter) filterClass.newInstance();
        } catch (InstantiationException e) {
            throw new ForestRuntimeException("An error occurred the initialization of filter \"" + name + "\" ! cause: " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new ForestRuntimeException("An error occurred the initialization of filter \"" + name + "\" ! cause: " + e.getMessage(), e);
        }
    }

}
