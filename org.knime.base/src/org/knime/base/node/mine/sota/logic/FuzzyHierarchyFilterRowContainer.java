/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * 
 * History
 *   Jan 19, 2006 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.logic;

import java.util.ArrayList;
import java.util.Set;

import org.knime.base.node.util.DataArray;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.FuzzyIntervalValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowIterator;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class FuzzyHierarchyFilterRowContainer implements DataArray {
    private int m_hierarchyLevel;

    private DataArray m_rc;

    private int m_levelColumn;

    private int m_noFuzzyColumns;

    private int m_maxLevel;

    private ArrayList<FuzzyHierarchyFilterMetaInformation> m_meta;

    /**
     * Creates an instance of FuzzyHierarchyFilterRowContainer with given
     * DataArray and hierarchy level.
     * 
     * @param rc DataArray to set
     * @param hierarchiyLevel hierarchy level to set
     * @throws IllegalStateException if hierarchy level of hierarchical fuzzy
     *             data is less than 1
     */
    public FuzzyHierarchyFilterRowContainer(final DataArray rc,
            final int hierarchiyLevel) throws IllegalStateException {
        m_rc = rc;
        m_hierarchyLevel = hierarchiyLevel;
        m_noFuzzyColumns = 0;

        // Find the column which stores the level information
        for (int i = 0; i < m_rc.getDataTableSpec().getNumColumns(); i++) {
            DataType type = m_rc.getDataTableSpec().getColumnSpec(i).getType();

            if (SotaUtil.isIntType(type)) {
                m_levelColumn = i;
            } else if (SotaUtil.isFuzzyIntervalType(type)) {
                m_noFuzzyColumns++;
            }
        }

        m_meta = new ArrayList<FuzzyHierarchyFilterMetaInformation>();

        // Find meta information and store it
        m_maxLevel = 0;

        for (int i = 0; i < m_rc.size(); i++) {
            int level = ((IntValue)m_rc.getRow(i).getCell(m_levelColumn))
                    .getIntValue();

            if (level < 1) {
                throw new IllegalStateException("A hierarchiy level less than"
                        + " 1 is not allowed !");
            }

            if (m_maxLevel < level) {
                m_maxLevel = level;
            }

            // check if new level is reached
            boolean newLevel = false;
            if (m_meta.size() > level) {
                if (m_meta.get(level).getLevel() == 0) {
                    newLevel = true;
                }

            } else {
                newLevel = true;
            }

            // if new level is reached
            if (newLevel) {
                FuzzyHierarchyFilterMetaInformation meta 
                    = new FuzzyHierarchyFilterMetaInformation();
                meta.setLevel(level);
                meta.setSize(1);

                // Save min max values
                for (int h = 0; h < m_rc.getDataTableSpec().getNumColumns(); 
                    h++) {
                    DataType type = m_rc.getDataTableSpec().getColumnSpec(h)
                            .getType();

                    if (SotaUtil.isFuzzyIntervalType(type)) {
                        meta.setMinValueAtIndex(i, h);
                        meta.setMaxValueAtIndex(i, h);
                    }
                }

                // fill up array list if npthing stored at prior levels
                if (level > m_meta.size()) {
                    for (int j = 0; j <= level; j++) {
                        if (j < m_meta.size()) {
                            if (m_meta.get(j) == null) {
                                m_meta
                                .add(new FuzzyHierarchyFilterMetaInformation());
                            }
                        } else {
                            m_meta
                            .add(new FuzzyHierarchyFilterMetaInformation());
                        }
                    }
                }
                m_meta.set(level, meta);

            } else {
                m_meta.get(level).addToSize(1);

                for (int h = 0; h < m_rc.getDataTableSpec().getNumColumns(); 
                    h++) {
                    DataType type = m_rc.getDataTableSpec().getColumnSpec(h)
                            .getType();

                    if (SotaUtil.isFuzzyIntervalType(type)) {

                        double min = SotaFuzzyMath
                                .getCenterOfCoreRegion((FuzzyIntervalValue)m_rc
                                        .getRow(
                                                m_meta.get(level)
                                                        .getMinAtIndex(h))
                                        .getCell(h));

                        double max = SotaFuzzyMath
                                .getCenterOfCoreRegion((FuzzyIntervalValue)m_rc
                                        .getRow(
                                                m_meta.get(level)
                                                        .getMinAtIndex(h))
                                        .getCell(h));

                        double newVal = SotaFuzzyMath
                                .getCenterOfCoreRegion((FuzzyIntervalValue)m_rc
                                        .getRow(i).getCell(h));

                        if (newVal < min) {
                            m_meta.get(level).setMinValueAtIndex(i, h);
                        }
                        if (newVal > max) {
                            m_meta.get(level).setMaxValueAtIndex(i, h);
                        }
                    }
                }
            }
        }
    }

    /**
     * @return the hierarchyLevel
     */
    public int getHierarchyLevel() {
        return m_hierarchyLevel;
    }

    /**
     * @param level the hierarchyLevel to set
     */
    public void setHierarchyLevel(final int level) {
        if (level > m_maxLevel) {
            m_hierarchyLevel = m_maxLevel;
        } else {
            m_hierarchyLevel = level;
        }
    }

    /**
     * @return the maxLevel
     */
    public int getMaxLevel() {
        return m_maxLevel;
    }

    /**
     * {@inheritDoc}
     */
    public DataRow getRow(final int idx) {
        if (idx >= m_meta.get(m_hierarchyLevel).getSize()) {
            return null;
        }

        int index = 0;
        for (int i = 0; i < m_rc.size(); i++) {
            DataRow row = m_rc.getRow(i);
            if (((IntValue)row.getCell(m_levelColumn)).getIntValue() 
                    == m_hierarchyLevel) {
                if (index == idx) {
                    return row;
                } else {
                    index++;
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Set<DataCell> getValues(final int colIdx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public DataCell getMinValue(final int colIdx) {
        DataCell cell = null;
        int index = 0;

        index = m_meta.get(m_hierarchyLevel).getMinAtIndex(colIdx);
        cell = m_rc.getRow(index).getCell(colIdx);

        return cell;
    }

    /**
     * {@inheritDoc}
     */
    public DataCell getMaxValue(final int colIdx) {
        DataCell cell = null;
        int index = 0;

        index = m_meta.get(m_hierarchyLevel).getMaxAtIndex(colIdx);
        cell = m_rc.getRow(index).getCell(colIdx);

        return cell;
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return m_meta.get(m_hierarchyLevel).getSize();
    }

    /**
     * {@inheritDoc}
     */
    public int getFirstRowNumber() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public RowIterator iterator() {
        return m_rc.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        return m_rc.getDataTableSpec();
    }
}
