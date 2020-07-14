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
package com.alibaba.csp.sentinel;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.util.TimeUtil;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.node.Node;
import com.alibaba.csp.sentinel.slotchain.ResourceWrapper;
import com.alibaba.csp.sentinel.context.Context;

/**
 * Each {@link SphU}#entry() will return an {@link Entry}. This class holds information of current invocation:<br/>
 *
 * <ul>
 * <li>createTime, the create time of this entry, using for rt statistics.</li>
 * <li>current {@link Node}, that is statistics of the resource in current context.</li>
 * <li>origin {@link Node}, that is statistics for the specific origin. Usually the
 * origin could be the Service Consumer's app name, see
 * {@link ContextUtil#enter(String name, String origin)} </li>
 * <li>{@link ResourceWrapper}, that is resource name.</li>
 * <br/>
 * </ul>
 *
 * <p>
 * A invocation tree will be created if we invoke SphU#entry() multi times in the same {@link Context},
 * so parent or child entry may be held by this to form the tree. Since {@link Context} always holds
 * the current entry in the invocation tree, every {@link Entry#exit()} call should modify
 * {@link Context#setCurEntry(Entry)} as parent entry of this.
 * </p>
 *
 * @author qinan.qn
 * @author jialiang.linjl
 * @author leyou(lihao)
 * @author Eric Zhao
 * @see SphU
 * @see Context
 * @see ContextUtil
 */
/*
在Sentinel里面，所有的资源都对应一个资源名称(resourceName)，每次资源调用都会创建一个Entry对象，Entry可能通过对主流框架的适配自动创建，也可以
通过注解的方式或调用 SphU API显式创建，Entry创建的时间，同时也会创建一系列功能插槽(slot chain)，这些插槽有不同的职责，例如默认情况下会创建7个、
插槽(这里的插槽都是一一对应资源名称的)：
NodeSelectorSlot：负责收集资源的路径，并将这些资源的调用路径，以树状结构存储起来，用于根据调用路径来限流降级；
ClusterBuilderSlot：则用于存储资源的统计信息以及调用者信息，例如该资源的 RT, QPS, thread count 等等，这些信息将用作为多维度限流，降级的依据；
StatisticSlot：则用于记录、统计不同纬度的 runtime 指标监控信息；
FlowSlot：则用于根据预设的限流规则以及前面 slot 统计的状态，来进行流量控制；
AuthoritySlot：则根据配置的黑白名单和调用来源信息，来做黑白名单控制；
DegradeSlot：则通过统计信息以及预设的规则，来做熔断降级；
SystemSlot： 则通过系统的状态，例如 load1 等，来控制总的入口流量。
 */
public abstract class Entry implements AutoCloseable {

    private static final Object[] OBJECTS0 = new Object[0];

    private final long createTimestamp;
    private long completeTimestamp;

    private Node curNode;
    /**
     * {@link Node} of the specific origin, Usually the origin is the Service Consumer.
     */
    private Node originNode;

    private Throwable error;
    private BlockException blockError;

    protected final ResourceWrapper resourceWrapper;

    public Entry(ResourceWrapper resourceWrapper) {
        this.resourceWrapper = resourceWrapper;
        this.createTimestamp = TimeUtil.currentTimeMillis();
    }

    public ResourceWrapper getResourceWrapper() {
        return resourceWrapper;
    }

    /**
     * Complete the current resource entry and restore the entry stack in context.
     *
     * @throws ErrorEntryFreeException if entry in current context does not match current entry
     */
    public void exit() throws ErrorEntryFreeException {
        exit(1, OBJECTS0);
    }

    public void exit(int count) throws ErrorEntryFreeException {
        exit(count, OBJECTS0);
    }

    /**
     * Equivalent to {@link #exit()}. Support try-with-resources since JDK 1.7.
     *
     * @since 1.5.0
     */
    @Override
    public void close() {
        exit();
    }

    /**
     * Exit this entry. This method should invoke if and only if once at the end of the resource protection.
     *
     * @param count tokens to release.
     * @param args  extra parameters
     * @throws ErrorEntryFreeException, if {@link Context#getCurEntry()} is not this entry.
     */
    public abstract void exit(int count, Object... args) throws ErrorEntryFreeException;

    /**
     * Exit this entry.
     *
     * @param count tokens to release.
     * @param args  extra parameters
     * @return next available entry after exit, that is the parent entry.
     * @throws ErrorEntryFreeException, if {@link Context#getCurEntry()} is not this entry.
     */
    protected abstract Entry trueExit(int count, Object... args) throws ErrorEntryFreeException;

    /**
     * Get related {@link Node} of the parent {@link Entry}.
     *
     * @return
     */
    public abstract Node getLastNode();

    public long getCreateTimestamp() {
        return createTimestamp;
    }

    public long getCompleteTimestamp() {
        return completeTimestamp;
    }

    public Entry setCompleteTimestamp(long completeTimestamp) {
        this.completeTimestamp = completeTimestamp;
        return this;
    }

    public Node getCurNode() {
        return curNode;
    }

    public void setCurNode(Node node) {
        this.curNode = node;
    }

    public BlockException getBlockError() {
        return blockError;
    }

    public Entry setBlockError(BlockException blockError) {
        this.blockError = blockError;
        return this;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    /**
     * Get origin {@link Node} of the this {@link Entry}.
     *
     * @return origin {@link Node} of the this {@link Entry}, may be null if no origin specified by
     * {@link ContextUtil#enter(String name, String origin)}.
     */
    public Node getOriginNode() {
        return originNode;
    }

    public void setOriginNode(Node originNode) {
        this.originNode = originNode;
    }

}
