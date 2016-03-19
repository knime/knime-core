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
 *   Mar 19, 2016 (Berthold): created
 */
package org.knime.base.node.preproc.vector.expand;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.vector.doublevector.DoubleVectorValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortType;

/**
 * Abstract base model implementation for a node which extracts a given subset of elements of
 * a string or double vector to individual string/double columns. Derived classes either
 * generate their own random sampling scheme or read it from a predecessor.
 *
 * @author M. Berthold
 * @since 3.2
 */
public abstract class BaseExpandVectorNodeModel extends NodeModel {

    /**
     * @param inPortTypes
     * @param outPortTypes
     */
    protected BaseExpandVectorNodeModel(final PortType[] inPortTypes, final PortType[] outPortTypes) {
        super(inPortTypes, outPortTypes);
    }

    // members holding the sampling scheme:
    protected int[] m_sampledIndices = null;
    protected enum VType { String, Double };
    protected VType m_vectorType = null;

    protected int m_sourceColumnIndex = -1;

    /* static factory methods for the SettingsModels used here and in the NodeDialog. */
    /**
     * @return the settings model used to store the source column name.
     */
    static public SettingsModelString createVectorColSelectSettingsModel() {
        return new SettingsModelString("SelectedColumn", null);
    }
    protected final SettingsModelString m_vectorColumn = createVectorColSelectSettingsModel();

    static public SettingsModelBoolean createRemoveSourceColSettingModel() {
        return new SettingsModelBoolean("Remove Source", true);
    }
    protected final SettingsModelBoolean m_removeSourceCol = createRemoveSourceColSettingModel();

    static public SettingsModelBoolean createExpandColumnsSettingModel() {
        return new SettingsModelBoolean("ExpandToColumns", false);
    }
    protected final SettingsModelBoolean m_expandToColumns = createExpandColumnsSettingModel();

    protected void checkBaseSettings(final DataTableSpec spec) throws InvalidSettingsException {
        assert m_vectorColumn.getStringValue() != null;
        // selected column should exist in input table
        if (!spec.containsName(m_vectorColumn.getStringValue())) {
            throw new InvalidSettingsException("Selected column '"
                    + m_vectorColumn.getStringValue() + "' does not exist in input table!");
        }
        m_sourceColumnIndex = spec.findColumnIndex(m_vectorColumn.getStringValue());
        // and it should be of type double or string vector
        if (spec.getColumnSpec(m_vectorColumn.getStringValue()).getType().isCompatible(DoubleVectorValue.class)) {
            m_vectorType = VType.Double;
        } else if (spec.getColumnSpec(m_vectorColumn.getStringValue()).getType().
                isCompatible(DoubleVectorValue.class)) {
            m_vectorType = VType.String;
        } else {
            throw new InvalidSettingsException("Selected column '"
                    + m_vectorColumn.getStringValue() + "' does not contain double or string vectors!");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_vectorColumn.saveSettingsTo(settings);
        m_removeSourceCol.saveSettingsTo(settings);
        m_expandToColumns.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_vectorColumn.validateSettings(settings);
        m_removeSourceCol.validateSettings(settings);
        m_expandToColumns.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_vectorColumn.loadSettingsFrom(settings);
        m_removeSourceCol.loadSettingsFrom(settings);
        m_expandToColumns.loadSettingsFrom(settings);
    }
}
