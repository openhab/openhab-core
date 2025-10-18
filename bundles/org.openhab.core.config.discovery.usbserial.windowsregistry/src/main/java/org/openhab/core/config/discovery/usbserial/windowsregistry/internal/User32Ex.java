/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.config.discovery.usbserial.windowsregistry.internal;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.NTStatus;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.win32.W32APIOptions;

/**
 * Extra {@code user32.dll} mappings not defined in {@link User32}.
 *
 * @author Ravi Nadahar - Initial contribution.
 */
public interface User32Ex extends User32 {

    /** The instance. */
    User32Ex INSTANCE = Native.load("user32", User32Ex.class, W32APIOptions.DEFAULT_OPTIONS);

    int WAIT_OBJECT_0 = NTStatus.STATUS_WAIT_0 + 0;
    int WAIT_ABANDONED_0 = NTStatus.STATUS_ABANDONED_WAIT_0 + 0;
    int WAIT_ABANDONED = WAIT_ABANDONED_0;
    int WAIT_TIMEOUT = 258;
    int WAIT_FAILED = 0xFFFFFFFF;
    int MAXIMUM_WAIT_OBJECTS = 64;

    int PM_NOREMOVE = 0;
    int PM_REMOVE = 1;
    int PM_NOYIELD = 2;

    /**
     * A {@code WM_KEYUP}, {@code WM_KEYDOWN}, {@code WM_SYSKEYUP}, or {@code WM_SYSKEYDOWN} message is in the queue.
     */
    int QS_KEY = 0x0001;

    /** A {@code WM_MOUSEMOVE} message is in the queue. */
    int QS_MOUSEMOVE = 0x0002;

    /** A mouse-button message ({@code WM_LBUTTONUP}, {@code WM_RBUTTONDOWN}, and so on). */
    int QS_MOUSEBUTTON = 0x0004;

    /**
     * A posted message (other than those listed here) is in the queue.
     * <p>
     * This value is cleared when you call {@link #GetMessage(MSG, HWND, int, int)} or
     * {@link #PeekMessage(MSG, HWND, int, int, int)}, whether or not you are filtering messages.
     *
     * @see #PostMessage(HWND, int, WPARAM, LPARAM)
     */
    int QS_POSTMESSAGE = 0x0008;

    /** A {@code WM_TIMER} message is in the queue. */
    int QS_TIMER = 0x0010;

    /** A {@code WM_PAINT} message is in the queue. */
    int QS_PAINT = 0x0020;

    /**
     * A message sent by another thread or application is in the queue.
     *
     * @see #SendMessage(HWND, int, WPARAM, LPARAM)
     */
    int QS_SENDMESSAGE = 0x0040;

    /** A {@code WM_HOTKEY} message is in the queue. */
    int QS_HOTKEY = 0x0080;

    /**
     * A posted message (other than those listed here) is in the queue.
     * <p>
     * This value is cleared when you call {@link #GetMessage(MSG, HWND, int, int)} or
     * {@link #PeekMessage(MSG, HWND, int, int, int)} without filtering messages.
     *
     * @see #PostMessage(HWND, int, WPARAM, LPARAM)
     */
    int QS_ALLPOSTMESSAGE = 0x0100;

    /** Windows XP and newer: A raw input message is in the queue. For more information, see Raw Input. */
    int QS_RAWINPUT = 0x0400;

    /** Windows 8 and newer: A touch input message is in the queue. For more information, see Touch Input. */
    int QS_TOUCH = 0x0800;

    /** Windows 8 and newer: A pointer input message is in the queue. For more information, see Pointer Input. */
    int QS_POINTER = 0x1000;

    /**
     * A {@code WM_MOUSEMOVE} message or mouse-button message ({@code WM_LBUTTONUP}, {@code WM_RBUTTONDOWN}, and so on).
     */
    final int QS_MOUSE = (QS_MOUSEMOVE | QS_MOUSEBUTTON);

    /** An input message is in the queue. */
    final int QS_INPUT = (QS_MOUSE | QS_KEY | QS_RAWINPUT | QS_TOUCH | QS_POINTER);

    /** An input, {@code WM_TIMER}, {@code WM_PAINT}, {@code WM_HOTKEY}, or posted message is in the queue. */
    final int QS_ALLEVENTS = (QS_INPUT | QS_POSTMESSAGE | QS_TIMER | QS_PAINT | QS_HOTKEY);

    /** Any message is in the queue. */
    final int QS_ALLINPUT = (QS_INPUT | QS_POSTMESSAGE | QS_TIMER | QS_PAINT | QS_HOTKEY | QS_SENDMESSAGE);

    /**
     * Waits until one or all of the specified objects are in the signaled state or the time-out interval elapses.
     * The objects can include input event objects, which you specify using the dwWakeMask parameter.
     *
     * @param nCount The number of object handles in the array pointed to by pHandles. The maximum number of object
     *            handles is {@link #MAXIMUM_WAIT_OBJECTS} minus one. If this parameter has the value zero, then the
     *            function
     *            waits only for an input event.
     * @param pHandles An array of object handles. The array can contain handles of objects of different types.
     *            It may not contain multiple copies of the same handle.
     *            If one of these handles is closed while the wait is still pending, the function's behavior is
     *            undefined.
     *            The handles must have the {@code SYNCHRONIZE} access right.
     * @param fWaitAll If this parameter is {@code true} the function returns when the states of all objects in the
     *            {@code pHandles} array have been set to signaled and an input event has been received.
     *            If this parameter is {{@code false}, the function returns when the state of any one of the objects is
     *            set to signaled or an input event has been received. In this case, the return value indicates the
     *            object whose state caused the function to return.
     * @param dwMilliseconds The time-out interval, in milliseconds. If a nonzero value is specified,
     *            the function waits until the specified objects are signaled or the interval elapses.
     *            If {@code dwMilliseconds} is zero, the function does not enter a wait state if the specified objects
     *            are not signaled;
     *            it always returns immediately. If {{@code dwMilliseconds} is {@link WinBase#INFINITE}, the function
     *            will return only when the specified objects are signaled.
     * @param dwWakeMask The input types for which an input event object handle will be added to the array of object
     *            handles. This parameter can be any combination of the {@code QS} constants defined in this interface.
     * @return If the function succeeds, the return value indicates the event that caused the function to return.
     *         It can be one of the following values:
     *         <ul>
     *         <li>{@link #WAIT_OBJECT_0} to ({@link #WAIT_OBJECT_0} + nCount– 1)</li>
     *         <li>{@link #WAIT_OBJECT_0} + nCount</li>
     *         <li>{@link #WAIT_ABANDONED_0} to ({@link #WAIT_ABANDONED_0} + nCount– 1)</li>
     *         <li>{@link #WAIT_TIMEOUT}</li>
     *         <li>{@link #WAIT_FAILED}</li>
     *         </ul>
     */
    int MsgWaitForMultipleObjects(int nCount, HANDLE[] pHandles, boolean fWaitAll, int dwMilliseconds, int dwWakeMask);
}
