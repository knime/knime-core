/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * -------------------------------------------------------------------
 */

package org.knime.base.data.aggregation;

import java.awt.Component;
import java.util.Collection;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialog;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;


/**
 * Interface that implements the main methods of an aggregation method.
 * However the main work is done by the {@link AggregationOperator} that can
 * be created using the
 * {@link #createOperator(GlobalSettings, OperatorColumnSettings)} method.
 * A new {@link AggregationOperator} should be created per column.
 * AggregationMethods are sorted first by the supported data type and then
 * by the label.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public interface AggregationMethod extends Comparable<AggregationMethod> {

    /**
     * The unique identifier of the method that is used for registration and
     * identification of the aggregation method. The id is an internal
     * used variable that is not displayed to the user.
     *
     * @return the unique id of the method that is used to register and
     * identify the method.
     */
    String getId();

    /**
     * @return the label that is displayed to the user
     */
    String getLabel();


    /**
     * @param colName the unique name of the column
     * @param origSpec the original {@link DataColumnSpec}
     * @return the new {@link DataColumnSpecCreator} for the aggregated column
     */
    DataColumnSpec createColumnSpec(String colName, DataColumnSpec origSpec);


    /**
     * @return the label of the aggregation method which is
     * used in the column name
     */
    String getColumnLabel();

    /**
     * @param origColSpec the {@link DataColumnSpec} of the column to
     * check for compatibility
     * @return <code>true</code> if the aggregation method is compatible
     */
    boolean isCompatible(DataColumnSpec origColSpec);

    /**
     * Creates a new instance of this operator and returns it.
     * A new instance must be created for each column.
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     * @return a new instance of this operator
     */
    AggregationOperator createOperator(GlobalSettings globalSettings,
            OperatorColumnSettings opColSettings);

    /**
     * @return <code>true</code> if the operator supports the alteration of
     * the missing cell option
     */
    public boolean supportsMissingValueOption();

    /**
     * @return <code>true</code> if missing cells are considered during
     * aggregation
     */
    public boolean inclMissingCells();

    /**
     * @return a description that explains the used aggregation method to
     * the user
     */
    public String getDescription();

    /**
     * @return the supported {@link DataValue} class
     */
    public Class<? extends DataValue> getSupportedType();


    /**
     * @return the user friendly label of the supported {@link DataValue} class
     */
    String getSupportedTypeLabel();

    /**
     * This method indicates if the operator requires additional settings.
     * If that is the case the operator should return <code>true</code>. It must
     * also override the corresponding methods (#getSettingsPanel(DataTableSpec),
     * #validateSettings(NodeSettingsRO), #loadValidatedSettings(NodeSettingsRO),
     * #saveSettingsTo(NodeSettingsWO) and #resetSettings()). Furthermore it
     * needs to copy all operator specific settings when creating a new instance
     * ({@link #createOperator(GlobalSettings, OperatorColumnSettings)},
     * {@link AggregationOperator#createInstance(GlobalSettings, OperatorColumnSettings)}).
     * @return <code>true</code> if the operator requires additional
     * settings
     * @since 2.7
     * @see #getSettingsPanel()
     * @see #validateSettings(NodeSettingsRO)
     * @see #loadValidatedSettings(NodeSettingsRO)
     * @see #loadSettingsFrom(NodeSettingsRO, DataTableSpec)
     * @see #saveSettingsTo(NodeSettingsWO)
     * @see #createOperator(GlobalSettings, OperatorColumnSettings)
     * @see AggregationOperator#createInstance(GlobalSettings, OperatorColumnSettings)
     */
    public boolean hasOptionalSettings();

    /**
     * Returns the optional {@link Component} that allows the user to adjust
     * all {@link AggregationMethod} specific settings. Methods that do need
     * additional settings must override this method in order to return
     * their settings panel.
     *
     * @return the Component that contains all necessary settings
     * @since 2.7
     */
    public Component getSettingsPanel();

    /**
     * This method is called in the {@link NodeModel} to load the settings
     * that have been saved ({@link #saveSettingsTo(NodeSettingsWO)}) by the
     * {@link AggregationMethod} used in the in the additional settings panel
     * ({@link #getSettingsPanel()}) into the {@link AggregationMethod}
     * that is actually used in the {@link NodeModel}. Each operator gets its
     * own {@link NodeSettingsRO} object to ensure the uniqueness of the
     * settings keys.
     *
     * @param settings {@link NodeSettingsRO} that contains the optional
     * settings
     * @throws InvalidSettingsException if a property is not available
     * @since 2.7
     */
    public void loadValidatedSettings(final NodeSettingsRO settings)
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
     * @since 2.7
     */
    public void loadSettingsFrom(final NodeSettingsRO settings,
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
     * @since 2.7
     */
    public void saveSettingsTo(final NodeSettingsWO settings);

    /**
     * This method is called from the {@link NodeDialog} when the user closes
     * the additional settings dialog by clicking on the OK button.
     * It is also called from the {@link NodeModel} prior the settings are loaded
     * calling the {@link #loadValidatedSettings(NodeSettingsRO)}.
     * Each operator gets its own {@link NodeSettingsRO} object to ensure the
     * uniqueness of the settings keys.
     *
     * @param settings the {@link NodeSettingsRO} that contains the optional
     * settings to validate
     * @throws InvalidSettingsException if the settings are invalid
     * @since 2.7
     * @see #loadValidatedSettings(NodeSettingsRO)
     */
    public void validateSettings(final NodeSettingsRO settings)
        throws InvalidSettingsException;

    /**
     * This method is called from {@link NodeModel} in the <code>configure()</code> in
     * order to let the operator configure and to check that the aggregation operator
     * can work with the available input table.
     * @param spec the {@link DataTableSpec} of the input table
     *
     * @throws InvalidSettingsException if the settings are invalid
     * @since 2.7
     * @see #loadValidatedSettings(NodeSettingsRO)
     */
    public void configure(final DataTableSpec spec)
        throws InvalidSettingsException;

    /**
     * This method should return the name of all column names this operator requires
     * since the {@link DataRow} that is passed to the
     * {@link AggregationOperator#compute(DataRow, int...)} method contains only the
     * group, aggregation and additional column names for performance reasons.
     * Thus if the operator requires additional columns during the computation
     * besides the aggregation column itself it must return the names of all
     * these additional columns here. This is most likely the case for aggregation
     * operators that have optional setting {@link #hasOptionalSettings()} which
     * allow the user to select an additional column from the input table.
     *
     * @return {@link Collection} of column names that are required by this
     * aggregation operator in addition to the column that should be aggregated
     * @since 2.7
     */
    public Collection<String> getAdditionalColumnNames();
}
