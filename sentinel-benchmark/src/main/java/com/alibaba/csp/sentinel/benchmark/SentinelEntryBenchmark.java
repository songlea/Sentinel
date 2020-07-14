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
package com.alibaba.csp.sentinel.benchmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;

import org.openjdk.jmh.annotations.*;

/**
 * Benchmark for Sentinel entries.
 *
 * @author Eric Zhao
 */
/*
什么是JMH
    MH是一个在Openjdk项目中发布的,专门用于性能测试的框架,其精度可以达到毫秒级.可以执行一个函数需要多少时间,或者一个算法有多种不同实现等情况下,选择一个性能最好的那个.

maven依赖(见pom.xml)
    <dependency>
        <groupId>org.openjdk.jmh</groupId>
        <artifactId>jmh-core</artifactId>
        <version>${jmh.version}</version>
    </dependency>
    <dependency>
        <groupId>org.openjdk.jmh</groupId>
        <artifactId>jmh-generator-annprocess</artifactId>
        <version>${jmh.version}</version>
        <scope>provided</scope>
    </dependency>

常用注解说明
@BenchmarkMode:对应Mode选项,可用于类或者方法上,需要注意的是,这个注解的value是一个数组,可以把几种Mode集合在一起执行,还可以设置为Mode.All,即全部执行一遍.
    1、Throughput:整体吞吐量,例如‘1s内可以执行多少次调用’,单位是 操作数/时间
    2、AverageTime:调用的平均时间,例如‘每次调用平均耗时x毫秒’,单位是 时间/操作数
    3、SampleTime:随机取样,最后输出取样结果的分布,例如’99%的调用在x毫秒以内,99.99%的调用在xx毫秒以内‘
    4、SingleShotTime:以上模式都是默认一次迭代是1s,唯有SingleShotTime是只运行一次,往往同时把warmup次数设为0,用于测试冷启动时的性能.
@State:类注解,JMH测试类必须使用@State注解,State定义了一个类实例的生命周期,可以类比Spring Bean的Scope.由于JMH允许多线程同时执行测试,不同的选项含义如下:
    1、Scope.Thread:默认的 State,每个测试线程分配一个实例;
    2、Scope.Benchmark:所有测试线程共享一个实例,用于测试有状态实例在多线程共享下的性能;
    3、Scope.Group:每个线程组共享一个实例;
@OutputTimeUnit:benchmark 结果所使用的时间单位,可用于类或者方法注解,使用java.util.concurrent.TimeUnit中的标准时间单位
@WarmUp:是指在实际进行 Benchmark 前先进行预热的行为
    为什么需要预热？因为 JVM 的 JIT 机制的存在,如果某个函数被调用多次之后,JVM 会尝试将其编译成为机器码从而提高执行速度.
    为了让 Benchmark 的结果更加接近真实情况就需要进行预热.
@Setup:方法注解,会在执行 benchmark 之前被执行,主要用于初始化.
@TearDown:方法注解,与@Setup 相对的,会在所有 benchmark 执行结束以后执行,主要用于资源的回收等.
    @Setup/@TearDown注解使用Level参数来指定何时调用fixture：
        Level.Trial:默认level,全部benchmark运行(一组迭代)之前/之后
        Level.Iteration:一次迭代之前/之后(一组调用)
        Level.Invocation:每个方法调用之前/之后(不推荐使用,除非你清楚这样做的目的)
@Fork:进行 fork 的次数,如果 fork 数是2的话,则 JMH 会 fork 出两个进程来进行测试.
@Threads:使用的线程数,-1表示Runtime.getRuntime().availableProcessors()线程数量.
@Meansurement:提供真正的测试阶段参数,指定迭代的次数,每次迭代的运行时间和每次迭代测试调用的数量,
    通常使用 @BenchmarkMode(Mode.SingleShotTime) 测试一组操作的开销——而不使用循环.
@Param:成员注解,可以用来指定某项参数的多种情况,特别适合用来测试一个函数在不同的参数输入的情况下的性能,
    @Param 注解接收一个String数组,在 @Setup 方法执行前转化为为对应的数据类型.
    多个 @Param 注解的成员之间是乘积关系,譬如有两个用 @Param 注解的字段,第一个有5个值,第二个字段有2个值,那么每个测试方法会跑5*2=10次.
@Benchmark:方法注解,表示该方法是需要进行 benchmark 的对象.
 */
// @Warmup(iterations = 1)
@Warmup(iterations = 10)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
// @Fork(0)
public class SentinelEntryBenchmark {

    @Param({"25", "50", "100", "200", "500", "1000"})
    private int length;

    private List<Integer> numbers;

    @Setup
    public void prepare() {
        numbers = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            numbers.add(ThreadLocalRandom.current().nextInt());
        }
    }

    private void doSomething() {
        Collections.shuffle(numbers);
        Collections.sort(numbers);
    }

    private void doSomethingWithEntry() {
        Entry e0 = null;
        try {
            e0 = SphU.entry("benchmark");
            doSomething();
        } catch (BlockException e) {
        } finally {
            if (e0 != null) {
                e0.exit();
            }
        }
    }

    @Benchmark
    @Threads(1)
    public void testSingleThreadDirectly() {
        doSomething();
    }

    @Benchmark
    @Threads(1)
    public void testSingleThreadSingleEntry() {
        doSomethingWithEntry();
    }

    @Benchmark
    @Threads(2)
    public void test2ThreadsSingleEntry() {
        doSomethingWithEntry();
    }

    @Benchmark
    @Threads(3)
    public void test3ThreadsSingleEntry() {
        doSomethingWithEntry();
    }

    @Benchmark
    @Threads(4)
    public void test4ThreadsDirectly() {
        doSomething();
    }

    @Benchmark
    @Threads(4)
    public void test4ThreadsSingleEntry() {
        doSomethingWithEntry();
    }

    @Benchmark
    @Threads(8)
    public void test8ThreadsDirectly() {
        doSomething();
    }

    @Benchmark
    @Threads(8)
    public void test8ThreadsSingleEntry() {
        doSomethingWithEntry();
    }

    @Benchmark
    @Threads(16)
    public void test16ThreadsDirectly() {
        doSomething();
    }

    @Benchmark
    @Threads(16)
    public void test16ThreadsSingleEntry() {
        doSomethingWithEntry();
    }
}
