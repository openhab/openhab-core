package org.openhab.core.rulestest;

import static org.hamcrest.CoreMatchers.is;

import java.util.Arrays;
import java.util.List;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.junit.Test;
import org.junit.Ignore;

import static org.junit.Assert.assertTrue;


public class ExampleTest extends RuleTest {
    private static final List<String> requestedItems = Arrays.asList("Temperature_Setpoint");

    @Test
	@Ignore
    public void initializesTeamperature() throws Exception {
        assertThatItemState(getItem("Temperature_Setpoint"), is(new DecimalType(22)));
    }

    @Override
    protected List<String> requestedItems() {
        return requestedItems;
    }
}
