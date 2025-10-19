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

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.DBT;
import com.sun.jna.platform.win32.DBT.DEV_BROADCAST_DEVICEINTERFACE;
import com.sun.jna.platform.win32.DBT.DEV_BROADCAST_HDR;
import com.sun.jna.platform.win32.DBT.DEV_BROADCAST_PORT;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef.HMODULE;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HDEVNOTIFY;
import com.sun.jna.platform.win32.WinUser.MSG;
import com.sun.jna.platform.win32.WinUser.WNDCLASSEX;
import com.sun.jna.platform.win32.WinUser.WindowProc;

/**
 * This class, when run as a {@link Runnable}, creates an invisible window and uses that to listen for device change
 * events for USB devices and serial ports. It listens in a blocking message loop, so it's necessary to call
 * {@link #terminate()} for the loop to exit and the {@link #run()} method to exit.
 * <p>
 * The results of the device change events are delivered to subscribing {@link WindowMessageListener}s.
 *
 * @author Ravi Nadahar - Initial contribution.
 */
@NonNullByDefault
public class WindowMessageHandler implements Runnable, WindowProc {

    private final Logger logger = LoggerFactory.getLogger(WindowMessageHandler.class);

    private final AtomicInteger threadCounter = new AtomicInteger(0);

    private final Set<WindowMessageListener> listeners = new CopyOnWriteArraySet<>();

    /** A Windows event that can be used to stop the message loop */
    private final HANDLE terminateEvent = Kernel32.INSTANCE.CreateEvent(null, false, false, null);

    /**
     * Registers a {@link WindowMessageListener} that will receive USB device and serial port events.
     *
     * @param listener the {@link WindowMessageListener} to register.
     * @return {@code true} if the listener was added, {@code false} if it was already registered.
     */
    public boolean addListener(WindowMessageListener listener) {
        return listeners.add(listener);
    }

    /**
     * Unregisters a {@link WindowMessageListener} so that it will no longer receive USB device and serial port events.
     *
     * @param listener the {@link WindowMessageListener} to unregister.
     * @return {@code true} if the listener was removed, {@code false} if it wasn't registered.
     */
    public boolean removeListener(WindowMessageListener listener) {
        return listeners.remove(listener);
    }

    @Override
    public void run() {
        Thread currentThread = Thread.currentThread();
        String threadName = currentThread.getName();
        currentThread.setName("OH-window-message-handler");

        // Create and register window class
        String windowClass = "OHMessageHandlerWindowClass";
        User32Ex user32 = User32Ex.INSTANCE;
        HMODULE hInst = Kernel32.INSTANCE.GetModuleHandle(null);
        if (hInst == null) {
            logger.debug("Failed to get module handle, aborting message window creation");
            notifyTerminate();
            currentThread.setName(threadName);
            return;
        }
        WNDCLASSEX wClass = new WNDCLASSEX();
        wClass.hInstance = hInst;
        wClass.lpfnWndProc = WindowMessageHandler.this;
        wClass.lpszClassName = windowClass;
        if (user32.RegisterClassEx(wClass).intValue() == 0) {
            logger.debug("Failed to register window class, aborting message window creation");
            notifyTerminate();
            currentThread.setName(threadName);
            return;
        }

        HWND hWnd = null;
        HDEVNOTIFY hDevNotify = null;
        try {
            // Parent can't be the recommended HWND_MESSAGE, because WM_DEVICECHANGE is a broadcast message,
            // which aren't sent to message-only windows.
            hWnd = user32.CreateWindowEx(User32.WS_EX_TOPMOST, windowClass,
                    "OH helper window, used only to receive window events", 0, 0, 0, 0, 0, null, null, hInst, null);
            if (hWnd == null) {
                logger.debug("Failed to create window, aborting message window creation");
                return;
            }

            DEV_BROADCAST_DEVICEINTERFACE notificationFilter = new DEV_BROADCAST_DEVICEINTERFACE();
            notificationFilter.dbcc_size = notificationFilter.size();
            notificationFilter.dbcc_devicetype = DBT.DBT_DEVTYP_DEVICEINTERFACE;
            notificationFilter.dbcc_classguid = DBT.GUID_DEVINTERFACE_USB_DEVICE;

            hDevNotify = user32.RegisterDeviceNotification(hWnd, notificationFilter,
                    User32.DEVICE_NOTIFY_WINDOW_HANDLE);
            if (hDevNotify == null) {
                logger.debug("Failed to register for device notification, terminating message window");
                return;
            }

            MSG msg = new MSG();
            HANDLE[] handles = new HANDLE[] { terminateEvent };
            boolean running = true;
            while (running) {
                switch (user32.MsgWaitForMultipleObjects(handles.length, handles, false, WinBase.INFINITE,
                        User32Ex.QS_ALLINPUT)) {
                    case User32Ex.WAIT_OBJECT_0:
                        // terminateEvent was triggered, terminate
                        logger.debug("Terminate event received, terminating message loop");
                        running = false;
                        user32.PostQuitMessage(0);
                        break;
                    case User32Ex.WAIT_OBJECT_0 + 1:
                        // A window message has been queued, process the message queue
                        while (user32.PeekMessage(msg, hWnd, 0, 0, User32Ex.PM_REMOVE)) {
                            if (msg.message == WinUser.WM_QUIT) {
                                user32.PostQuitMessage(msg.wParam.intValue());
                                running = false;
                                break;
                            }
                            user32.TranslateMessage(msg);
                            user32.DispatchMessage(msg);
                        }
                        break;
                    default:
                        // This should not happen, something is very wrong
                        int lastError = Native.getLastError();
                        logger.warn(
                                "An error ({}) occurred while waiting for a window message, terminating message loop: {}",
                                lastError, Kernel32Util.formatMessage(lastError));
                        running = false;
                        user32.PostQuitMessage(0);
                        break;
                }
            }
        } finally {
            if (hDevNotify != null) {
                user32.UnregisterDeviceNotification(hDevNotify);
            }
            user32.UnregisterClass(windowClass, hInst);
            if (hWnd != null) {
                user32.DestroyWindow(hWnd);
            }

            notifyTerminate();
            currentThread.setName(threadName);
        }
    }

    /**
     * Signals the event loop (the {@link #run()} method) that it should terminate.
     */
    public void terminate() {
        Kernel32.INSTANCE.SetEvent(terminateEvent);
    }

    private void notifyTerminate() {
        Set<WindowMessageListener> listeners = Set.copyOf(this.listeners);
        if (!listeners.isEmpty()) {
            createNotificationThread(() -> {
                for (WindowMessageListener listener : listeners) {
                    listener.serviceTerminated();
                }
            }).start();
        }
    }

    private Thread createNotificationThread(Runnable runnable) {
        return new Thread(runnable, "OH-window-message-notifier-" + threadCounter.incrementAndGet());
    }

    @Override
    @NonNullByDefault({})
    public LRESULT callback(HWND hWnd, int uMsg, WPARAM wParam, LPARAM lParam) {
        switch (uMsg) {
            case WinUser.WM_CREATE:
                logger.trace("Window message handler created");
                return new LRESULT(0);
            case WinUser.WM_DESTROY:
                logger.trace("Window message handler destroyed");
                User32Ex.INSTANCE.PostQuitMessage(0);
                return new LRESULT(0);
            case WinUser.WM_DEVICECHANGE: {
                LRESULT lResult = onDeviceChange(wParam, lParam);
                return lResult != null ? lResult : User32Ex.INSTANCE.DefWindowProc(hWnd, uMsg, wParam, lParam);
            }
            default:
                return User32Ex.INSTANCE.DefWindowProc(hWnd, uMsg, wParam, lParam);
        }
    }

    @Nullable
    private LRESULT onDeviceChange(WPARAM wParam, LPARAM lParam) {
        switch (wParam.intValue()) {
            case DBT.DBT_DEVICEARRIVAL:
                return onDeviceAddedOrRemoved(lParam, true);
            case DBT.DBT_DEVICEREMOVECOMPLETE:
                return onDeviceAddedOrRemoved(lParam, false);
            case DBT.DBT_DEVNODES_CHANGED:
                // LRESULT(1) aka TRUE means that the message was processed. This message is non-specific
                // (basically means "something changed"), so we don't want to take any action.
                return new LRESULT(1);
            default:
                return null;
        }
    }

    @Nullable
    private LRESULT onDeviceAddedOrRemoved(LPARAM lParam, boolean added) {
        DEV_BROADCAST_HDR bhdr = new DEV_BROADCAST_HDR(lParam.longValue());
        Set<WindowMessageListener> listeners;
        switch (bhdr.dbch_devicetype) {
            case DBT.DBT_DEVTYP_DEVICEINTERFACE:
                DEV_BROADCAST_DEVICEINTERFACE bdif = new DEV_BROADCAST_DEVICEINTERFACE(bhdr.getPointer());
                listeners = Set.copyOf(this.listeners);
                if (!listeners.isEmpty()) {
                    createNotificationThread(() -> {
                        for (WindowMessageListener listener : listeners) {
                            if (added) {
                                listener.deviceAdded(bdif.getDbcc_name());
                            } else {
                                listener.deviceRemoved(bdif.getDbcc_name());
                            }
                        }
                    }).start();
                }
                break;
            case DBT.DBT_DEVTYP_PORT:
                DEV_BROADCAST_PORT bpt = new DEV_BROADCAST_PORT(bhdr.getPointer());
                listeners = Set.copyOf(this.listeners);
                if (!listeners.isEmpty()) {
                    createNotificationThread(() -> {
                        for (WindowMessageListener listener : listeners) {
                            if (added) {
                                listener.portAdded(bpt.getDbcpName());
                            } else {
                                listener.portRemoved(bpt.getDbcpName());
                            }
                        }
                    }).start();
                }
                break;
            // Don't process the remaining types
            case DBT.DBT_DEVTYP_HANDLE:
            case DBT.DBT_DEVTYP_OEM:
            case DBT.DBT_DEVTYP_VOLUME:
            default:
                return null;
        }
        // LRESULT(1) aka TRUE means that the message was processed.
        return new LRESULT(1);
    }

    /**
     * A listener that listens for {@link WindowMessageHandler} events.
     *
     * @author Ravi Nadahar - Initial contribution.
     */
    public interface WindowMessageListener {

        /**
         * A USB device was added.
         *
         * @param devicePath the device path of the added device.
         */
        void deviceAdded(String devicePath);

        /**
         * A USB device was removed.
         *
         * @param devicePath the device path of the removed device.
         */
        void deviceRemoved(String devicePath);

        /**
         * A serial port was added.
         *
         * @param portName the name of the port, e.g. {@code COM3}.
         */
        void portAdded(String portName);

        /**
         * A serial port was removed.
         *
         * @param portName the name of the port, e.g. {@code COM3}.
         */
        void portRemoved(String portName);

        /**
         * {@link WindowMessageHandler} was terminated, no more events will be sent.
         */
        void serviceTerminated();
    }
}
