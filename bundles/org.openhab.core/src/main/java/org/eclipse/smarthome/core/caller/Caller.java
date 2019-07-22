/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.caller;

import java.io.Closeable;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Definition of a simple caller.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public interface Caller extends Closeable {

    // Need some special casting to provide a void that succeed the nullness checks
    static Void VOID = (@NonNull Void) null;

    @Override
    void close();

    /**
     * Runs a function.
     *
     * <p>
     * The caller implementations should use the calling thread for execution of the function (the constraints can be
     * handled by another thread).
     *
     * This method returns after the function has been finished.
     * So, the completion stage has already been completed (regardless if successful or by an exception).
     *
     * @param <R> the type of the return value
     * @param func the function to execute
     * @param constraints the execution constraints
     * @return a completion stage of the execution
     */
    default CompletionStage<Void> exec(Runnable func, final ExecutionConstraints constraints) {
        return exec(() -> {
            func.run();
            return Caller.VOID;
        }, constraints);
    }

    /**
     * Runs a function.
     *
     * <p>
     * The caller implementations should use the calling thread for execution of the function (the constraints can be
     * handled by another thread).
     *
     * This method returns after the function has been finished.
     * So, the completion stage has already been completed (regardless if successful or by an exception).
     *
     * @param <R> the type of the return value
     * @param func the function to execute
     * @param constraints the execution constraints
     * @return a completion stage of the execution
     */
    <R> CompletionStage<R> exec(Supplier<R> func, final ExecutionConstraints constraints);

    /**
     * Runs a function.
     *
     * <p>
     * The caller implementations should use an executor, so you should prepared that the function is executed in a
     * different thread.
     * This method returns after the function has been finished.
     * So, the completion stage has already been completed (regardless if successful or by an exception).
     *
     * @param <R> the type of the return value
     * @param func the function to execute
     * @param constraints the execution constraints
     * @return a completion stage of the execution
     */
    default CompletionStage<Void> execSync(Runnable func, final ExecutionConstraints constraints) {
        return execSync(() -> {
            func.run();
            return Caller.VOID;
        }, constraints);
    }

    /**
     * Runs a function.
     *
     * <p>
     * The caller implementations should use an executor, so you should prepared that the function is executed in a
     * different thread.
     * This method returns after the function has been finished.
     * So, the completion stage has already been completed (regardless if successful or by an exception).
     *
     * @param <R> the type of the return value
     * @param func the function to execute
     * @param constraints the execution constraints
     * @return a completion stage of the execution
     */
    <R> CompletionStage<R> execSync(Supplier<R> func, final ExecutionConstraints constraints);

    /**
     * Runs a function.
     *
     * <p>
     * The caller implementations should use an executor, so you should prepared that the function is executed in a
     * different thread.
     * This method returns regardless if the function is already been executed completely (or at all).
     * You can use the return value to further act on the special execution.
     *
     * @param <R> the type of the return value
     * @param func the function to execute
     * @param constraints the execution constraints
     * @return a completion stage of the execution
     */
    default CompletionStage<Void> execAsync(Runnable func, final ExecutionConstraints constraints) {
        return execAsync(() -> {
            func.run();
            return Caller.VOID;
        }, constraints);
    }

    /**
     * Runs a function.
     *
     * <p>
     * The caller implementations should use an executor, so you should prepared that the function is executed in a
     * different thread.
     * This method returns regardless if the function is already been executed completely (or at all).
     * You can use the return value to further act on the special execution.
     *
     * @param <R> the type of the return value
     * @param func the function to execute
     * @param constraints the execution constraints
     * @return a completion stage of the execution
     */
    <R> CompletionStage<R> execAsync(Supplier<R> func, final ExecutionConstraints constraints);
}
