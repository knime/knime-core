/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */

package org.knime.base.data.aggregation.collection;

import java.awt.Component;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;


/**
 * Collection operator that creates a new List by appending the elements of all lists of the group.
 * @author Tobias Koetter, University of Konstanz
 * @since 2.11
 */
public class AppendElementOperator extends AggregationOperator {

    private final Collection<DataCell> m_vals = new LinkedList<>();

    private DialogComponentBoolean m_filterMissingDC;

    private final SettingsModelBoolean m_filterMissing = new SettingsModelBoolean("ignoreMissing", true);

    /**Constructor for class AndElementOperator.
     * @param operatorData the operator data
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    protected AppendElementOperator(final OperatorData operatorData,
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        super(operatorData, globalSettings, opColSettings);
    }

    /**Constructor for class AndElementOperator.
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public AppendElementOperator(final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        this(new OperatorData("Append elements", true, false, CollectionDataValue.class, true), globalSettings,
                opColSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        return new AppendElementOperator(getOperatorData(), globalSettings, opColSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        return ListCell.getCollectionType(origType.getCollectionElementType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        if (cell instanceof CollectionDataValue) {
            //missing cells are skipped
            final CollectionDataValue collectionCell = (CollectionDataValue)cell;
            for (final DataCell dataCell : collectionCell) {
                //add the element cell if it is not missing or if missing cells should be also added
                if (!dataCell.isMissing() || !m_filterMissing.getBooleanValue()) {
                    m_vals.add(dataCell);
                }
            }
        }
        return false;
    }

    /**
     * @return the values that have been collected so far as
     * an unmodifiable collection
     */
    protected Collection<DataCell> getValues() {
        return Collections.unmodifiableCollection(m_vals);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        return CollectionCellFactory.createListCell(m_vals);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        m_vals.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasOptionalSettings() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
            throws NotConfigurableException {
        getDC().loadSettingsFrom(settings, new DataTableSpec[] {spec});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValidatedSettings(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_filterMissing.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_filterMissing.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getSettingsPanel() {
        return getDC().getComponentPanel();
    }

    /**
     * @return
     */
    private DialogComponent getDC() {
        if (m_filterMissingDC == null) {
            m_filterMissingDC = new DialogComponentBoolean(m_filterMissing, "Filter missing collection elements");
            m_filterMissingDC.setToolTipText("Tick this option to filter missing collection elements");
        }
        return m_filterMissingDC;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Creates a ListCell that contains all collection elements per group."
            + " The elements of each collection are appended at the end of the new ListCell in the order they occur.";
    }
}
