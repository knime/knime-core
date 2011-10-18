/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   09.09.2011 (hofer): created
 */
package org.knime.base.node.preproc.colconvert.categorytonumber;

import java.util.Map;

import org.dmg.pmml40.DATATYPE;
import org.dmg.pmml40.OPTYPE;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.node.port.pmml.PMMLDataDictionaryTranslator;

/**
 * Container class to hold information about PMML MapValues Objects.
 *
 * @author Heiko Hofer
 */
public abstract class MapValuesConfiguration {
    private final String m_inColumn;
    private final String m_outColumn;
    private final Map<DataCell, ? extends DataCell> m_map;
    private final DataCell m_defaultValue;
    private final DataCell m_mapMissingTo;


    /**
     * @param inColumn input column
     * @param outColumn output column
     * @param map map of DataCells from input column to output column
     */
    public MapValuesConfiguration(final String inColumn, final String outColumn,
            final Map<DataCell, ? extends DataCell> map) {
        this(inColumn, outColumn, map, DataType.getMissingCell(),
                DataType.getMissingCell());
    }

    /**
     * @param inColumn input column
     * @param outColumn output column
     * @param map map of DataCells from input column to output column
     * @param defaultValue value used when element in the input is not found in
     *        the map
     * @param mapMissingTo value used when element in the input is a missing
     *        cell
     */
    public MapValuesConfiguration(final String inColumn, final String outColumn,
            final Map<DataCell, ? extends DataCell> map,
            final DataCell defaultValue,
            final DataCell mapMissingTo) {
        super();
        m_inColumn = inColumn;
        m_outColumn = outColumn;
        m_map = map;
        m_defaultValue = defaultValue;
        m_mapMissingTo = mapMissingTo;
    }

    /**
     * Get descriptive summary.
     * @return descriptive summary
     */
    public abstract String getSummary();

    /**
     * The data type of the output column. See
     * {@link PMMLDataDictionaryTranslator} to get the PMML data types from
     * KNIME data types.
     * @return data type of the output column
     */
    public abstract DATATYPE.Enum getOutDataType();

    /**
     * Get the input column.
     * @return the input column
     */
    public String getInColumn() {
        return m_inColumn;
    }

    /**
     * Get the output column.
     * @return the output column
     */
    public String getOutColumn() {
        return m_outColumn;
    }

    /**
     * The PMML optype of the derived field. The is typically categorical for
     * a discrete value mapping. It can also be continuous if the value of
     * the out column should be interpreted as continuous values.
     * @return the PMML optype of the derived field
     */
    public OPTYPE.Enum getOpType() {
        return OPTYPE.CATEGORICAL;
    }

    /**
     * Get the value used when element in the input is not found in the map.
     * @return value used when element in the input is not found in the map
     */
    public DataCell getDefaultValue() {
        return m_defaultValue;
    }

    /**
     * Get the value used when element in the input is a missing cell.
     * @return value used when element in the input is a missing cell
     */
    public DataCell getMapMissingTo() {
        return m_mapMissingTo;
    }

    /**
     * Get the mapping.
     * @return the mapping.
     */
    public Map<DataCell, ? extends DataCell> getEntries() {
        return m_map;
    }

}
