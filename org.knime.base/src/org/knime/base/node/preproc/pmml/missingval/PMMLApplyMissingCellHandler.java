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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   15.01.2015 (Alexander): created
 */
package org.knime.base.node.preproc.pmml.missingval;

import java.text.ParseException;

import org.dmg.pmml.DATATYPE;
import org.dmg.pmml.DerivedFieldDocument.DerivedField;
import org.knime.base.data.statistics.Statistic;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * A missing cell handler that is initialized from a PMML document instead of a factory.
 * Only works with the data types boolean, int, long, double and date time.
 *
 * @author Alexander Fillbrunn
 * @since 3.5
 * @noreference This class is not intended to be referenced by clients.
 */
public class PMMLApplyMissingCellHandler extends DefaultMissingCellHandler {

    private DerivedField m_derivedField;

    private String m_value;

    private DataType m_dataType;

    /**
     * @param col the column for which this handler is created
     * @param df the derived field that has the information for the missing value replacement
     * @throws InvalidSettingsException if the PMML structure cannot be interpreted
     */
    public PMMLApplyMissingCellHandler(final DataColumnSpec col, final DerivedField df)
                                                        throws InvalidSettingsException {
        super(col);
        m_derivedField = df;
        try {
            m_value = df.getApply().getConstantList().get(0).getStringValue();
        } catch (NullPointerException e) {
            throw new InvalidSettingsException("The derived field for column " + col.getName()
                                               + " is malformed for missing value replacement", e);
        }

        DataType type = getKnimeDataTypeForPMML(df.getDataType());
        m_dataType = DataType.getCommonSuperType(type, col.getType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Statistic getStatistic() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getCell(final RowKey key, final DataColumnWindow window) {
        try {
            if (m_dataType.equals(BooleanCell.TYPE)) {
                return BooleanCell.get(Boolean.parseBoolean(m_value));
            } else if (m_dataType.equals(IntCell.TYPE)) {
                return new IntCell(Integer.parseInt(m_value));
            } else if (m_dataType.equals(LongCell.TYPE)) {
                return new LongCell(Long.parseLong(m_value));
            } else if (m_derivedField.getDataType() == DATATYPE.DOUBLE) {
                return new DoubleCell(Double.parseDouble(m_value));
            } else if (m_derivedField.getDataType() == DATATYPE.DATE_TIME) {
                return DateAndTimeCell.fromString(m_value);
            }
        } catch (NumberFormatException | ParseException e) {
            return new MissingCell("Could not parse PMML value");
        }
        return new StringCell(m_value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DerivedField getPMMLDerivedField() {
        return m_derivedField;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataType getOutputDataType() {
        return m_dataType;
    }

    /**
     * Maps the columns data type to a PMML data type.
     * @return The PMML data type for this handler's column
     */
    private DataType getKnimeDataTypeForPMML(final DATATYPE.Enum dt) {
        if (dt.equals(DATATYPE.DOUBLE)) {
            return DoubleCell.TYPE;
        } else if (dt.equals(DATATYPE.BOOLEAN)) {
            return BooleanCell.TYPE;
        } else if (dt.equals(DATATYPE.INTEGER)) {
            return IntCell.TYPE;
        } else if (dt.equals(DATATYPE.DATE_TIME)) {
            return DateAndTimeCell.TYPE;
        } else  {
            return StringCell.TYPE;
        }
    }
}
