/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Jun 3, 2010 (wiswedel): created
 */
package org.knime.time.node.window;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Configuration object to loop start chunking node.
 *
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 */
final class LoopStartWindowConfiguration {

    /** Window definition */
    enum WindowDefinition {
            /** Point of interest in the center of window. */
            CENTRAL,
            /** Point of interest at the beginning of the window. */
            FORWARD,
            /** Point of interest at the end of the window. */
            BACKWARD
    }

    enum Trigger {
            /** Row triggered. */
            ROW,
            /** Time triggered. */
            TIME
    }

    enum Unit {
            NO_UNIT("duration", ""), MILLISECONDS("millisecond(s)", "ms"), SECONDS("second(s)", "s"),
            MINUTES("minute(s)", "m"), HOURS("hour(s)", "h"), DAYS("day(s)", "d"), MONTHS("month(s)", "M"),
            YEARS("year(s)", "y");

        private String m_name;

        private String m_unitLetter;

        private Unit(final String name, final String unitLetter) {
            this.m_name = name;
            this.m_unitLetter = unitLetter;
        }

        /**
         * @return the letter representing the unit.
         */
        public String getUnitLetter() {
            return m_unitLetter;
        }

        /**
         * Gets the unit from the specific string
         *
         * @param unitName name of the unit
         * @return the unit if the given name is valid. {@code NO_UNIT} if the given name does not correspond to any
         *         unit.
         */
        public static Unit getUnit(final String unitName) {
            for (Unit unit : Unit.values()) {
                if (unit.toString().equals(unitName)) {
                    return unit;
                }
            }

            return NO_UNIT;
        }

        @Override
        public String toString() {
            return m_name;
        }
    }

    private WindowDefinition m_windowDefinition = WindowDefinition.FORWARD;

    private Trigger m_trigger = Trigger.ROW;

    private int m_eventStepSize = 1;

    private int m_eventWindowSize = 1;

    private String m_timeStepSize;

    private String m_timeWindowSize;

    private boolean m_limitWindow;

    private boolean m_useSpecifiedStartTime;

    private Unit m_timeWindowUnit;

    private Unit m_timeStepUnit;

    /* Start of the keys used to save and load the settings. */
    private final String m_windowDefinitionKey = "windowDefinition";

    private final String m_triggerKey = "trigger";

    private final String m_evenStepSizeKey = "eventStepSize";

    private final String m_eventWindowSizeKey = "eventWindowSize";

    private final String m_timeStepSizeKey = "timeStepSize";

    private final String m_timeWindowSizeKey = "timeWindowSize";

    private final String m_limitWindowKey = "limitWindow";

    private final String m_specifiedStartTimeKey = "specifiedStartTime";

    private final String m_useSpecifiedStartTimeKey = "useSpecifiedStartTime";

    private final String m_timeWindowUnitKey = "timeWindowUnit";

    private final String m_timeStepUnitKey = "timeStepUnit";

    /** @return the window definition */
    WindowDefinition getWindowDefinition() {
        return m_windowDefinition;
    }

    /** @return the trigger */
    Trigger getTrigger() {
        return m_trigger;
    }

    /**
     * @param definition the window definition to set
     * @throws InvalidSettingsException If argument is null.
     */
    void setWindowDefinition(final WindowDefinition definition) throws InvalidSettingsException {
        if (definition == null) {
            throw new InvalidSettingsException("Window definition must not be null");
        }

        m_windowDefinition = definition;
    }

    /**
     * @param trigger the trigger to set
     * @throws InvalidSettingsException If argument is null.
     */
    void setTrigger(final Trigger trigger) throws InvalidSettingsException {
        if (trigger == null) {
            throw new InvalidSettingsException("Trigger must not be null");
        }

        this.m_trigger = trigger;
    }

    /** @return the step size. */
    int getEventStepSize() {
        return m_eventStepSize;
    }

    /**
     * @param stepSize the stepSize to set
     * @throws InvalidSettingsException If argument &lt; 1
     */
    void setEventStepSize(final int stepSize) throws InvalidSettingsException {
        if (stepSize < 1) {
            throw new IllegalArgumentException("Step size must be at least 1: " + stepSize);
        }

        this.m_eventStepSize = stepSize;
    }

    /** @return the size of the window. */
    int getEventWindowSize() {
        return m_eventWindowSize;
    }

    /**
     * @param windowSize the size of window to set
     * @throws InvalidSettingsException If argument &lt; 1
     */
    void setEventWindowSize(final int windowSize) throws InvalidSettingsException {
        if (windowSize < 1) {
            throw new IllegalArgumentException("Window size must be at least 1: " + windowSize);
        }

        this.m_eventWindowSize = windowSize;
    }

    /**
     * Saves current settings to argument.
     *
     * @param settings To save to.
     */
    void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addInt(m_evenStepSizeKey, m_eventStepSize);
        settings.addInt(m_eventWindowSizeKey, m_eventWindowSize);
        settings.addString(m_triggerKey, m_trigger.name());
        settings.addString(m_windowDefinitionKey, m_windowDefinition.name());
        settings.addBoolean(m_limitWindowKey, m_limitWindow);

        if (m_timeStepSize != null) {
            settings.addString(m_timeStepSizeKey, m_timeStepSize);
            settings.addString(m_timeWindowSizeKey, m_timeWindowSize);
        } else {
            settings.addString(m_timeStepSizeKey, null);
            settings.addString(m_timeWindowSizeKey, null);
        }

        settings.addBoolean(m_useSpecifiedStartTimeKey, m_useSpecifiedStartTime);

        if (m_timeWindowUnit != null) {
            settings.addString(m_timeWindowUnitKey, m_timeWindowUnit.toString());
            settings.addString(m_timeStepUnitKey, m_timeStepUnit.toString());
        }
    }

    /**
     * Load settings in model, fails if incomplete.
     *
     * @param settings To load from.
     * @throws InvalidSettingsException If invalid.
     */
    void loadSettingsInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        try {
            setWindowDefinition(WindowDefinition.valueOf(settings.getString(m_windowDefinitionKey)));
        } catch (Exception e) {
            throw new InvalidSettingsException(
                "Invalid window definition: " + settings.getString(m_windowDefinitionKey));
        }

        try {
            setTrigger(Trigger.valueOf(settings.getString(m_triggerKey)));
        } catch (Exception e) {
            throw new InvalidSettingsException("Invalid trigger: " + settings.getString(m_triggerKey));
        }

        setEventStepSize(settings.getInt(m_evenStepSizeKey));
        setEventWindowSize(settings.getInt(m_eventWindowSizeKey));

        setTimeStepSize(settings.getString(m_timeStepSizeKey));
        setTimeWindowSize(settings.getString(m_timeWindowSizeKey));

        setTimeStepUnit(Unit.getUnit(settings.getString(m_timeStepUnitKey, null)));
        m_timeWindowUnit = Unit.getUnit(settings.getString(m_timeWindowUnitKey, null));

        setLimitWindow(settings.getBoolean(m_limitWindowKey, false));
        setUseSpecifiedStartTime(settings.getBoolean(m_useSpecifiedStartTimeKey, false));
    }

    /**
     * Load settings in dialog, use default if invalid.
     *
     * @param settings To load from.
     */
    void loadSettingsInDialog(final NodeSettingsRO settings) {
        try {
            m_trigger = Trigger.valueOf(settings.getString(m_triggerKey, Trigger.ROW.name()));
        } catch (IllegalStateException e) {
            m_trigger = Trigger.ROW;
        }

        try {
            m_windowDefinition =
                WindowDefinition.valueOf(settings.getString(m_windowDefinitionKey, WindowDefinition.FORWARD.name()));
        } catch (IllegalArgumentException iae) {
            m_windowDefinition = WindowDefinition.FORWARD;
        }

        try {
            setEventStepSize(settings.getInt(m_evenStepSizeKey, 1));
        } catch (InvalidSettingsException ise) {
            m_eventStepSize = 1;
        }

        try {
            setEventWindowSize(settings.getInt(m_eventWindowSizeKey, 1));
        } catch (InvalidSettingsException e) {
            m_eventWindowSize = 1;
        }

        m_timeStepSize = settings.getString(m_timeStepSizeKey, null);
        m_timeWindowSize = settings.getString(m_timeWindowSizeKey, null);

        m_timeStepUnit = Unit.getUnit(settings.getString(m_timeStepUnitKey, null));
        m_timeWindowUnit = Unit.getUnit(settings.getString(m_timeWindowUnitKey, null));

        setLimitWindow(settings.getBoolean(m_limitWindowKey, false));
        setUseSpecifiedStartTime(settings.getBoolean(m_useSpecifiedStartTimeKey, false));
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "";
    }

    /**
     * Sets the duration of the starting interval.
     *
     * @param duration of the starting interval.
     */
    public void setTimeStepSize(final String duration) {
        m_timeStepSize = duration;
    }

    /**
     * @return time-step size
     */
    public String getTimeStepSize() {
        return m_timeStepSize;
    }

    /**
     * Set the window duration.
     *
     * @param duration of window.
     */
    public void setTimeWindowSize(final String duration) {
        m_timeWindowSize = duration;
    }

    /**
     * @return duration of the window
     */
    public String getTimeWindowSize() {
        return m_timeWindowSize;
    }

    /**
     * Sets of the window shall be limited to the table.
     *
     * @param selected {@code true} if the window shall be limited, {@code false} otherwise.
     */
    public void setLimitWindow(final boolean selected) {
        m_limitWindow = selected;
    }

    /**
     * @return {@code true} if window shall be limited to the table
     */
    public boolean getLimitWindow() {
        return m_limitWindow;
    }

    /**
     * @param useSpecifiedTime {@code true} if the specified start time shall be used, {@code false} otherwise.
     */
    public void setUseSpecifiedStartTime(final boolean useSpecifiedTime) {
        this.m_useSpecifiedStartTime = useSpecifiedTime;

    }

    /**
     * @return {@code true} if specified start time shall be used, {@code false} otherwise.
     */
    public boolean useSpecifiedStartTime() {
        return m_useSpecifiedStartTime;
    }

    /**
     * @param windowUnit unit of the window size.
     */
    public void setTimeWindowUnit(final Unit windowUnit) {
        m_timeWindowUnit = windowUnit;
    }

    /**
     * @param stepUnit unit of the step size.
     */
    public void setTimeStepUnit(final Unit stepUnit) {
        m_timeStepUnit = stepUnit;
    }

    /**
     * @return unit of the step size.
     */
    public Unit getTimeStepUnit() {
        return m_timeStepUnit;
    }

    /**
     * @return unit of the window size.
     */
    public Unit getTimeWindowUnit() {
        return m_timeWindowUnit;
    }
}
