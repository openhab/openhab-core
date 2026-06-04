/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.voice.text.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link org.openhab.core.voice.text.conversation.Conversation}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class ConversationTest {
    @Test
    public void addMessageRespectsHistoryLimit() throws ConversationException {
        Conversation conversation = new Conversation("conversation");
        conversation.setMaxMessages(5);

        conversation.addMessage(ConversationRole.USER, "1");
        conversation.addMessage(ConversationRole.OPENHAB, "2");
        conversation.addMessage(ConversationRole.USER, "3");
        conversation.addMessage(ConversationRole.OPENHAB, "4");
        conversation.addMessage(ConversationRole.USER, "5");
        conversation.addMessage(ConversationRole.OPENHAB, "6");
        conversation.addMessage(ConversationRole.USER, "7");

        assertEquals(5, conversation.getMessages().size());
        assertEquals("3", conversation.getMessages().get(0).content());
        assertEquals("4", conversation.getMessages().get(1).content());
    }

    @Test
    public void concurrentAddsWithSamePreviousMessageIdAllowOnlyOneAppend() throws Exception {
        Conversation conversation = new Conversation("conversation");
        int previousMessageId = conversation.addMessage(ConversationRole.USER, "question");
        int attempts = 20;
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(attempts);
        AtomicInteger successes = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(attempts);

        try {
            for (int i = 0; i < attempts; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        if (start.await(5, TimeUnit.SECONDS)) {
                            try {
                                conversation.addMessage(ConversationRole.OPENHAB, "answer", previousMessageId);
                                successes.incrementAndGet();
                            } catch (ConversationException ignored) {
                                // Expected for stale concurrent appends.
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertTrue(done.await(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, successes.get());
        assertEquals(2, conversation.getMessages().size());
        assertEquals(1, conversation.getMessages().get(1).id());
    }
}
