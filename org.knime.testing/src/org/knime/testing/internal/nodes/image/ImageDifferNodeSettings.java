/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 * ---------------------------------------------------------------------
 *
 * History
 *   16.08.2013 (thor): created
 */
package org.knime.testing.internal.nodes.image;

import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.testing.core.DifferenceChecker;
import org.knime.testing.core.DifferenceCheckerFactory;
import org.knime.testing.internal.diffcheckers.CheckerUtil;

/**
 * Settings for the image difference checker.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
public class ImageDifferNodeSettings {
    private NodeSettings m_checkerConfig = new NodeSettings("internals");

    private String m_checkerFactoryClass;

    /**
     * Returns the class name of the checker factory.
     *
     * @return a class name
     */
    public String checkerFactoryClassName() {
        return m_checkerFactoryClass;
    }

    /**
     * Returns the configured checker factory. If not factory is configured, <code>null</code> is returned.
     *
     * @return a checker factory or <code>null</code>
     */
    public DifferenceCheckerFactory<? extends DataValue> checkerFactory() {
        if (m_checkerFactoryClass == null) {
            return null;
        } else {
            return CheckerUtil.instance.getFactory(m_checkerFactoryClass);
        }
    }

    /**
     * Sets the checker factory.
     *
     * @param checkerFactory a checker factory, must not be <code>null</code>
     */
    public void checkerFactory(final DifferenceCheckerFactory<? extends DataValue> checkerFactory) {
        if (checkerFactory == null) {
            throw new IllegalArgumentException("Checker factory must not be null");
        }
        m_checkerFactoryClass = checkerFactory.getClass().getName();
    }

    /**
     * Returns the internal configuration for the checker. If not internals are available <code>null</code> is returned.
     *
     * @return internal settings or <code>null</code>
     */
    public NodeSettings checkerConfiguration() {
        return m_checkerConfig;
    }


    /**
     * Returns a clean internal configuration for the checker.
     *
     * @return internal settings
     */
    public NodeSettings newCheckerConfiguration() {
        m_checkerConfig = new NodeSettings("internals");
        return m_checkerConfig;
    }


    /**
     * Creates a configured checker. If no checker factory is registered <code>null</code> is returned.
     *
     * @return a checker or <code>null</code>
     * @throws InvalidSettingsException if the internal settings for the checker cannot be loaded
     */
    public DifferenceChecker<? extends DataValue> createChecker() throws InvalidSettingsException {
        if (m_checkerFactoryClass == null) {
            return null;
        } else {
            DifferenceChecker<? extends DataValue> checker =
                    CheckerUtil.instance.getFactory(m_checkerFactoryClass).newChecker();
            checker.loadSettings(m_checkerConfig);
            return checker;
        }
    }

    /**
     * Loads the settings from the given settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if settings are missing or invalid
     */
    public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_checkerFactoryClass = settings.getString("checkerFactory");
        m_checkerConfig = new NodeSettings("internals");
        settings.getNodeSettings("internals").copyTo(m_checkerConfig);
    }

    /**
     * Loads the settings from the given settings object, using default values for missing settings. By default the
     * {@link org.knime.testing.internal.diffcheckers.EqualityChecker} is used.
     *
     * @param settings a node settings object
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_checkerFactoryClass = settings.getString("checkerFactory", null);
        m_checkerConfig = new NodeSettings("internals");
        try {
            settings.getNodeSettings("internals").copyTo(m_checkerConfig);
        } catch (InvalidSettingsException ex) {
            // ignore it
        }
    }

    /**
     * Saves the settings into the given settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("checkerFactory", m_checkerFactoryClass);
        settings.addNodeSettings(m_checkerConfig);
    }
}
