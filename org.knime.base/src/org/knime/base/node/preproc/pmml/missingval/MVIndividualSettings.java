/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   16.12.2014 (Alexander): created
 */
package org.knime.base.node.preproc.pmml.missingval;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Holds information necessary for initializing a missing value handler for one column or one data type.
 *
 * @author Alexander Fillbrunn
 * @since 3.5
 * @noreference This class is not intended to be referenced by clients.
 */
public class MVIndividualSettings {

    private static final String FACTORY_ID_CFG = "factoryID";

    private static final String SETTINGS_CFG = "settings";

    private MissingCellHandlerFactory m_factory;

    private NodeSettings m_settings;

    private final MissingCellHandlerFactoryManager m_handlerFactoryManager;

    /**
     * Creates a new instance of MVColumnSettings.
     * @param factory the factory for the missing cell handler
     * @param handlerFactoryManager manager keeping the missing value handler factories
     */
    public MVIndividualSettings(final MissingCellHandlerFactory factory,
        final MissingCellHandlerFactoryManager handlerFactoryManager) {

        if (factory == null) {
            throw new NullPointerException("The facory must not be null");
        }

        if (handlerFactoryManager == null) {
            throw new NullPointerException("Factory manager required");
        }

        m_settings = new NodeSettings("");
        m_factory = factory;
        m_handlerFactoryManager = handlerFactoryManager;
    }

    /**
     * Creates a new instance of MVColumnSettings using a do nothing handler.
     * @param handlerFactoryManager manager keeping the missing value handler factories
     */
    public MVIndividualSettings(final MissingCellHandlerFactoryManager handlerFactoryManager) {
        this(handlerFactoryManager.getDoNothingHandlerFactory(), handlerFactoryManager);
    }

    /**
     * Returns the node settings used to store user settings for the selected factory.
     * @return the node settings
     */
    public NodeSettings getSettings() {
        return m_settings;
    }

    /**
     * Returns the factory selected by the user.
     * @return the factory
     */
    public MissingCellHandlerFactory getFactory() {
        return m_factory;
    }

    /**
     * Loads settings for a column.
     * @param settings the settings
     * @param repair if true, missing factories are replaced by the do nothing factory, else an exception is thrown
     * @return if repair is false, any warning message that occurred, else null
     * @throws InvalidSettingsException when the settings are not properly structured
     *          or repair is false and a factory is missing
     */
    public String loadSettings(final NodeSettingsRO settings, final boolean repair) throws InvalidSettingsException {
        String factID = settings.getString(FACTORY_ID_CFG);
        String warning = null;
        m_factory = m_handlerFactoryManager.getFactoryByID(factID);
        if (m_factory == null) {
            warning = "The factory " + factID + " is not a registered extension but is chosen in the settings.";
            if (!repair) {
                throw new InvalidSettingsException(warning);
            }
            m_factory = m_handlerFactoryManager.getDoNothingHandlerFactory();
        }
        if (settings.containsKey(SETTINGS_CFG)) {
            m_settings = new NodeSettings("");
            settings.getNodeSettings(SETTINGS_CFG).copyTo(this.m_settings);
        }
        return warning;
    }

    /**
     * Saves settings for a column.
     * @param settings the settings to write to
     */
    protected void saveSettings(final NodeSettingsWO settings) {
        settings.addString(FACTORY_ID_CFG, m_factory.getID());
        NodeSettingsWO s = settings.addNodeSettings(SETTINGS_CFG);
        m_settings.copyTo(s);
    }
}
