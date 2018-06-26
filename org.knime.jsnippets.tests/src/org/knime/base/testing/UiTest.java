package org.knime.base.testing;

import javax.swing.JFrame;

import org.junit.After;

/**
 * Base class for tests that require a JFrame.
 *
 * @author Jonathan Hale
 */
public class UiTest {
    private JFrame testFrame;

    @After
    public void tearDown() throws Exception {
        if (this.testFrame != null) {
            this.testFrame.dispose(  );
            this.testFrame = null;
        }
    }

    /**
     * @return frame used for testing.
     */
    public JFrame getTestFrame() {
        if (this.testFrame == null) {
            this.testFrame = new JFrame("Test");
        }
        return this.testFrame;
    }
}