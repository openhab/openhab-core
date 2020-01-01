/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.thing.binding.firmware;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link ProgressCallback} is injected into the
 * {@link FirmwareUpdateHandler#updateFirmware(Firmware, ProgressCallback)} operation in order to post progress
 * information about the firmware update process.
 *
 * The progress of a firmware update can be defined by a sequence of {@link ProgressStep}s, a percentage progress or
 * both.
 *
 * In order to use a sequence of {@link ProgressStep}s to indicate the update progress, it is necessary to first define
 * a sequence using the method {@link ProgressCallback#defineSequence(ProgressStep...)}. To indicate that the next
 * progress step is going to be executed the method {@link ProgressCallback#next()} has to be used.
 *
 * For updates which are based on a percentage progress it is optional to define a sequence of {@link ProgressStep}s and
 * to use the {@link ProgressCallback#next()} method. In order to indicate that the percentage progress has changed, the
 * method {@link ProgressCallback#update(progress)} has to be used. It allows to update the percentage progress to a
 * value between 0 and 100.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Christop Knauf - Added canceled, pending and update
 */
@NonNullByDefault
public interface ProgressCallback {

    /**
     * Callback operation to define the {@link ProgressStep}s for the sequence of the firmware update. So if the
     * operation is invoked with the following progress steps
     * <ul>
     * <li>{@link ProgressStep#DOWNLOADING}</li>
     * <li>{@link ProgressStep#TRANSFERRING}</li>
     * <li>{@link ProgressStep#UPDATING}</li>
     * </ul>
     * then this will mean that the firmware update implementation will initially download the firmware, then
     * it will transfer the firmware to the actual device and in a final step it will trigger the update.
     *
     * @param sequence the progress steps describing the sequence of the firmware update process (must not be null
     *            or empty)
     * @throws IllegalArgumentException if given sequence is null or empty
     */
    void defineSequence(ProgressStep... sequence);

    /**
     * Callback operation to indicate that the next progress step is going to be executed. Following the example of the
     * {@link ProgressCallback#defineSequence(ProgressStep...)} operation then the first invocation of this operation
     * will indicate that firmware update handler is going to download the firmware, the second invocation will indicate
     * that the handler is going to transfer the firmware to the device and consequently the third invocation will
     * indicate that the handler is going to trigger the update. If the update is pending calling next indicates that
     * the update is continued.
     *
     * @throws IllegalStateException if
     *             <ul>
     *             <li>update is not pending and there is no further step to be executed</li>
     *             <li>if no sequence was defined</li>
     *             </ul>
     */
    void next();

    /**
     * Callback operation to indicate that the firmware update has failed.
     *
     * @param errorMessageKey the key of the error message to be internationalized (must not be null or empty)
     * @param arguments the arguments to be injected into the internationalized error message (can be null)
     * @throws IllegalArgumentException if given error message key is null or empty
     * @throws IllegalStateException if update is already finished
     */
    void failed(String errorMessageKey, Object... arguments);

    /**
     * Callback operation to indicate that the firmware update was successful.
     *
     * @throws IllegalStateException if update is finished
     */
    void success();

    /**
     * Callback operation to indicate that the firmware update is pending.
     *
     * @throws IllegalStateException if update is finished
     */
    void pending();

    /**
     * Callback operation to indicate that the firmware update was canceled.
     *
     * @throws IllegalStateException if update is finished
     */
    void canceled();

    /**
     * Callback operation to update the percentage progress of the firmware update.
     * This method can be used to provide detailed progress information additional to the sequence or even without a
     * previous defined sequence.
     *
     * @param progress the progress between 0 and 100
     * @throws IllegalArgumentException if given progress is < 0 or > 100
     * @throws IllegalArgumentException if given progress is smaller than old progress
     * @throws IllegalStateException if update is finished
     */
    void update(int progress);
}
