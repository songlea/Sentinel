/*
 * Copyright 1999-2019 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.spi;

import java.util.ServiceLoader;

import com.alibaba.csp.sentinel.config.SentinelConfig;

/**
 * @author Eric Zhao
 * @since 1.7.0
 */
/*
JDK中，基于SPI的思想，提供了默认具体的实现 ServiceLoader。利用JDK自带的ServiceLoader，可以轻松实现面向服务的注册与发现，完成服务提供与使用的解耦。
ServiceLoader：
    外部使用时，往往通过 load(Class<S> service, ClassLoader loader) 或 load(Class<S> service) 调用，最后都是在reload方法中创建了
    LazyIterator对象，LazyIterator是ServiceLoader的内部类，实现了Iterator接口，其作用是一个懒加载的迭代器，在hasNextService方法中，完成
    了对位于META-INF/services目录下的配置文件的解析，并在nextService方法中，完成了对具体实现类的实例化。
META-INF/services/：是ServiceLoader中约定的接口与实现类的关系配置目录，文件名是接口全限定类名，内部是接口对应的具体实现类，如果有多个实现类，
    分别将不同的实现类都分别作为每一行去配置。解析过程中，通过LinkedHashMap<String,S>数据结构的providers，将已经发现了的接口实现类进行缓存，
    并对外提供的iterator()方法，方便外部遍历。
 */
public final class ServiceLoaderUtil { 

    private static final String CLASSLOADER_DEFAULT = "default";
    private static final String CLASSLOADER_CONTEXT = "context";

    public static <S> ServiceLoader<S> getServiceLoader(Class<S> clazz) {
        if (shouldUseContextClassloader()) {
            return ServiceLoader.load(clazz);
        } else {
            return ServiceLoader.load(clazz, clazz.getClassLoader());
        }
    }

    public static boolean shouldUseContextClassloader() {
        String classloaderConf = SentinelConfig.getConfig(SentinelConfig.SPI_CLASSLOADER);
        return CLASSLOADER_CONTEXT.equalsIgnoreCase(classloaderConf);
    }

    private ServiceLoaderUtil() {}
}
