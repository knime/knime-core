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
 *   14.01.2015 (tibuch): created
 */
package org.knime.base.collection.list.split;

import java.util.Arrays;
import java.util.Iterator;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableDomainCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DomainCreatorColumnSelection;
import org.knime.core.data.NominalValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.BlobSupportDataCellIterator;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.container.BlobWrapperDataCell;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.ExecutionMonitor;

/**
 * CellFactory being used to split the column.
 *
 * @author Tim-Oliver Buchholz, KNIME.com, Zurich, Switzerland
 * @since 2.12
 */
class SplitCellFactory implements CellFactory {
    private final DataColumnSpec[] m_colSpecs;
    private final DataType[] m_commonTypes;
    private final int m_colIndex;
    private String m_warnMessage;
    private final DataTableDomainCreator m_domainCreator;

    /** Create new cell factory.
     * @param colIndex Index of collection column
     * @param colSpecs The column specs of the new columns.
     */
    public SplitCellFactory(final int colIndex, final DataColumnSpec[] colSpecs) {
        m_commonTypes = new DataType[colSpecs.length];
        m_colSpecs = colSpecs;
        m_colIndex = colIndex;

        DataTableSpec dummySpec = new DataTableSpec(colSpecs);
        m_domainCreator = new DataTableDomainCreator(dummySpec, new DomainCreatorColumnSelection() {
            @Override
            public boolean dropDomain(final DataColumnSpec colSpec) {
                return false;
            }

            @Override
            public boolean createDomain(final DataColumnSpec colSpec) {
                return true;
            }
        }, new DomainCreatorColumnSelection() {
            @Override
            public boolean dropDomain(final DataColumnSpec colSpec) {
                return false;
            }

            @Override
            public boolean createDomain(final DataColumnSpec colSpec) {
                return true;
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public DataColumnSpec[] getColumnSpecs() {
        return m_colSpecs;
    }

    /** {@inheritDoc} */
    @Override
    public DataCell[] getCells(final DataRow row) {
        DataCell inCell = row.getCell(m_colIndex);
        DataCell[] result = new DataCell[m_colSpecs.length];
        Arrays.fill(result, DataType.getMissingCell());
        if (inCell.isMissing()) {
            if (m_warnMessage == null) {
                m_warnMessage = "Some rows contain missing values";
            }
            return result;
        }
        CollectionDataValue v = (CollectionDataValue)inCell;
        Iterator<DataCell> it = v.iterator();
        for (int i = 0; i < m_colSpecs.length && it.hasNext(); i++) {
            DataCell next;
            DataType type;
            if (it instanceof BlobSupportDataCellIterator) {
                next =
                    ((BlobSupportDataCellIterator)it).nextWithBlobSupport();
                if (next instanceof BlobWrapperDataCell) {
                    // try to not access the cell (will get deserialized)
                    BlobWrapperDataCell bw = (BlobWrapperDataCell)next;
                    type = DataType.getType(bw.getBlobClass());
                } else {
                    type = next.getType();
                }
            } else {
                next = it.next();
                type = next.getType();
            }
            if (m_commonTypes[i] == null) {
                m_commonTypes[i] = type;
            } else {
                m_commonTypes[i] =
                    DataType.getCommonSuperType(m_commonTypes[i], type);
            }
            result[i] = next;
        }
        if (it.hasNext()) {
            m_warnMessage = "At least one row had more elements than "
                + "specified; row was truncated.";
        }
        m_domainCreator.updateDomain(new DefaultRow(row.getKey(), result));
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress(curRowNr / (double)rowCount, "Split row "
                + curRowNr + " (\"" + lastKey + "\")");
    }

    /** @return the commonTypes */
    public DataType[] getCommonTypes() {
        return m_commonTypes;
    }

    /**
     * Returns the domains that were computed while processing all rows.
     *
     * @return an array with domain
     */
    public DataColumnDomain[] getDomains() {
        DataColumnDomain[] domains = new DataColumnDomain[m_colSpecs.length];

        int i = 0;
        for (DataColumnSpec cs : m_domainCreator.createSpec()) {
            DataColumnDomainCreator crea = new DataColumnDomainCreator(cs.getDomain());
            if (!m_commonTypes[i].isCompatible(BoundedValue.class)) {
                crea.setLowerBound(null);
                crea.setUpperBound(null);
            }
            if (!m_commonTypes[i].isCompatible(NominalValue.class)) {
                crea.setValues(null);
            }

            domains[i] = crea.createDomain();
            i++;
        }

        return domains;
    }

    /** @return the warnMessage or null */
    public String getWarnMessage() {
        return m_warnMessage;
    }

}


