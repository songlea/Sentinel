/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.csp.sentinel.adapter.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

// 在服务侧，服务端实现服务接口，运行一个 gRPC 服务器来处理客户端调用。gRPC 底层架构会解码传入的请求，执行服务方法，编码服务应答。
class GrpcTestServer {

    private Server server;

    GrpcTestServer() {
    }

    void start(int port, boolean shouldIntercept) throws IOException {
        if (server != null) {
            throw new IllegalStateException("Server already running!");
        }
        ServerBuilder<?> serverBuild = ServerBuilder.forPort(port)
                .addService(new FooServiceImpl());
        if (shouldIntercept) {
            serverBuild.intercept(new SentinelGrpcServerInterceptor());
        }
        server = serverBuild.build();
        server.start();
    }

    void stop() {
        if (server != null) {
            server.shutdown();
            server = null;
        }
    }
}