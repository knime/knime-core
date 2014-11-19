/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   20.08.2014 (koetter): created
 */
package org.knime.core.node.port.database.aggregation;

import java.awt.Component;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialog;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * Methods are not thread safe since they might have internal state.
 *
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @since 2.11
 */
public interface AggregationFunction {

    /**
     * The unique identifier of the function that is used for registration and
     * identification of the aggregation method. The id is an internal
     * used variable that is not displayed to the user.
     *
     * @return the unique id of this function
     */
    String getId();

    /**
     * @return the label that is displayed to the user
     */
    String getLabel();

    /**
     * @param type the {@link DataType} check for compatibility
     * @return <code>true</code> if the aggregation method is compatible
     */
    boolean isCompatible(DataType type);

    /**
     * @return the description for this function
     */
    String getDescription();

    /**
     * This method indicates if the operator requires additional settings.
     * If that is the case the operator should return <code>true</code>. It must
     * also override the corresponding methods (#getSettingsPanel(DataTableSpec),
     * #validateSettings(NodeSettingsRO), #loadValidatedSettings(NodeSettingsRO),
     * #saveSettingsTo(NodeSettingsWO) and #resetSettings()).
     * @return <code>true</code> if the operator requires additional
     * settings
     * @see #getSettingsPanel()
     * @see #validateSettings(NodeSettingsRO)
     * @see #loadValidatedSettings(NodeSettingsRO)
     * @see #loadSettingsFrom(NodeSettingsRO, DataTableSpec)
     * @see #saveSettingsTo(NodeSettingsWO)
     */
    boolean hasOptionalSettings();

    /**
     * Returns the optional {@link Component} that allows the user to adjust
     * all {@link DBAggregationFunction} specific settings. Methods that do need
     * additional settings must override this method in order to return
     * their settings panel.
     * @return the Component that contains all necessary settings
     */
    Component getSettingsPanel();

    /**
     * This method is called in the {@link NodeModel} to load the settings
     * that have been saved ({@link #saveSettingsTo(NodeSettingsWO)}) by the
     * {@link DBAggregationFunction} used in the in the additional settings panel
     * ({@link #getSettingsPanel()}) into the {@link DBAggregationFunction}
     * that is actually used in the {@link NodeModel}. Each operator gets its
     * own {@link NodeSettingsRO} object to ensure the uniqueness of the
     * settings keys.
     *
     * @param settings {@link NodeSettingsRO} that contains the optional
     * settings
     * @throws InvalidSettingsException if a property is not available
     */
    void loadValidatedSettings(final NodeSettingsRO settings)
    throws InvalidSettingsException;

    /**
     * This method is called prior the {@link #getSettingsPanel()} method
     * is called and the dialog is opened. This method should be used
     * to load all settings form the provided settings object and to adjust
     * the corresponding dialog components accordingly. Each operator gets
     * its own {@link NodeSettingsRO} object to ensure the uniqueness of the
     * settings keys.
     *
     * @param settings {@link NodeSettingsRO} that contains the optional
     * settings
     * @param spec the {@link DataTableSpec} of the input table
     * @throws NotConfigurableException  if the dialog cannot be opened because
     * of invalid settings or if any preconditions are not fulfilled, e.g.
     * no nominal column in input table, etc.
     */
    void loadSettingsFrom(final NodeSettingsRO settings,
                 final DataTableSpec spec) throws NotConfigurableException;

    /**
     * This method is called from the {@link NodeDialog} and {@link NodeModel} in
     * order to save the additional settings. It is also called prior the
     * {@link #getSettingsPanel()} method in order to save the current settings.
     * The saved settings are used to initialize the settings panel by calling
     * {@link #loadSettingsFrom(NodeSettingsRO, DataTableSpec)} prior
     * calling {@link #getSettingsPanel()} and to restore the previous (default) setting
     * by calling the {@link #loadValidatedSettings(NodeSettingsRO)}
     * when the user closes the dialog in any other way then by clicking on the OK button!
     * Each operator gets its own {@link NodeSettingsRO} object to ensure
     * the uniqueness of the settings keys.
     *
     * @param settings the {@link NodeSettingsWO} to save the optional settings
     */
    void saveSettingsTo(final NodeSettingsWO settings);

    /**
     * This method is called from the {@link NodeDialog} when the user closes
     * the additional settings dialog by clicking on the OK button.
     * It is also called from the {@link NodeModel} prior the settings are loaded
     * calling the {@link #loadValidatedSettings(NodeSettingsRO)}.
     * Each operator gets its own {@link NodeSettingsRO} object to ensure the
     * uniqueness of the settings keys.
     * This function has been replaced by the {@link #validate()} method.
     *
     * @param settings the {@link NodeSettingsRO} that contains the optional
     * settings to validate
     * @throws InvalidSettingsException if the settings are invalid
     * @see #validate()
     */
    void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Validates the internal state (e.g. settings) of the function.
     * This method is typically called in the node dialog in order to validate the settings of each
     * {@link AggregationFunction} before saving it.
     * @throws InvalidSettingsException if the internal state is invalid
     */
    void validate() throws InvalidSettingsException;

    /**
     * This method is called from {@link NodeModel} in the <code>configure()</code> in
     * order to let the operator configure and to check that the aggregation operator
     * can work with the available input table.
     * @param spec the {@link DataTableSpec} of the input table
     *
     * @throws InvalidSettingsException if the settings are invalid
     * @see #loadValidatedSettings(NodeSettingsRO)
     */
    void configure(final DataTableSpec spec)
        throws InvalidSettingsException;
}
