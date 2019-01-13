package org.openhab.rulestest.manipulator;

import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.test.java.JavaTest;

public class TestSwitchItem extends JavaTest {
    private final SwitchItem switchItem;

    private TestSwitchItem(SwitchItem item) {
        switchItem = item;
    }

    public static void send(SwitchItem item, OnOffType newState) {
        new TestSwitchItem(item).send(newState);
    }

    private void send(OnOffType newState) {
        switchItem.send(newState);
        waitFor(() -> switchItem.getState().equals(newState));
    }
}
