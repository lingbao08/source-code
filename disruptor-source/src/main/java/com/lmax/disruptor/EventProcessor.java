/*
 * Copyright 2011 LMAX Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lmax.disruptor;

/**
 * An EventProcessor needs to be an implementation of a runnable that will poll for events from the {@link RingBuffer}
 * using the appropriate wait strategy.  It is unlikely that you will need to implement this interface yourself.
 * Look at using the {@link EventHandler} interface along with the pre-supplied BatchEventProcessor in the first
 * instance.
 * <p>
 * An EventProcessor will generally be associated with a Thread for execution.
 *
 *
 * 一个 EventProcessor 需要实现runnable，然后轮训{@link RingBuffer}中的事件来使用合适的等待策略。
 * 你不太可能需要自己去实现的这个接口。看看在第一个实例中单独使用{@link EventHandler}接口和预支持的BatchEventProcessor。
 *
 * 一个 EventProcessor 一般将会关联一个线程去执行。
 */
public interface EventProcessor extends Runnable
{
    /**
     * Get a reference to the {@link Sequence} being used by this {@link EventProcessor}.
     *
     * @return reference to the {@link Sequence} for this {@link EventProcessor}
     */
    Sequence getSequence();

    /**
     * Signal that this EventProcessor should stop when it has finished consuming at the next clean break.
     * It will call {@link SequenceBarrier#alert()} to notify the thread to check status.
     *
     * 当在下一个断点消费完成时，通知EventProcessor应该停止了。
     * 将调用{@link SequenceBarrier#alert()}去唤醒线程去清除状态。
     *
     */
    void halt();

    boolean isRunning();
}
