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
package org.openhab.core.io.webhook;

import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Service interface for requesting webhook URLs that are publicly reachable
 * and proxied to a local path on this openHAB instance.
 *
 * Implementations (e.g. the openHAB Cloud addon) register themselves as an
 * OSGi service. Bindings consume this service via {@code @Reference} to obtain
 * a public URL that external services can call; the implementation proxies
 * those incoming requests back to the specified local path.
 *
 * @author Dan Cunningham - Initial contribution
 */
@NonNullByDefault
public interface WebhookService {

    /**
     * Request a webhook for the given local path. The implementation returns
     * a {@link Webhook} carrying a public URL that proxies incoming requests to
     * the specified local path on this openHAB instance, together with the
     * instant at which the registration expires.
     *
     * <p>
     * Implementations should treat this call as idempotent when possible: calling
     * it with the same {@code localPath} should return a stable URL and refresh
     * the expiration.
     *
     * @param localPath the local openHAB path to forward webhook requests to
     *            (e.g. {@code "/rest/webhook/foo"}); must start with {@code "/"}
     * @return a {@link CompletableFuture} that completes with the registered
     *         {@link Webhook}, or completes exceptionally if registration fails
     */
    CompletableFuture<Webhook> requestWebhook(String localPath);

    /**
     * Remove a previously registered webhook for the given local path.
     *
     * @param localPath the local openHAB path whose webhook should be removed
     * @return a {@link CompletableFuture} that completes when the webhook is removed,
     *         or completes exceptionally if removal fails
     */
    CompletableFuture<Void> removeWebhook(String localPath);
}
