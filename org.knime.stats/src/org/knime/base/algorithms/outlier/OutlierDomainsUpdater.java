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
 *   Feb 21, 2018 (ortmann): created
 */
package org.knime.base.algorithms.outlier;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;

/**
 * Class wrapping the functionality to update domain bounds
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
class OutlierDomainsUpdater {

    /** Map containing the min and max values for the column domains to update. */
    private final Map<String, double[]> m_domainsMap;

    public OutlierDomainsUpdater() {
        m_domainsMap = new HashMap<String, double[]>();
    }

    /**
     * Update the domain for the given columns.
     *
     * @param exec the execution context
     * @param data the data table whose domains have to be reseted
     * @return the data table after reseting the domains
     */
    public BufferedDataTable updateDomain(final ExecutionContext exec, final BufferedDataTable data) {
        DataTableSpec spec = data.getSpec();
        final DataColumnSpec[] domainSpecs = new DataColumnSpec[spec.getNumColumns()];
        for (int i = 0; i < spec.getNumColumns(); i++) {
            final DataColumnSpec columnSpec = spec.getColumnSpec(i);
            if (m_domainsMap.containsKey(columnSpec.getName())) {
                domainSpecs[i] = updateDomainSpec(columnSpec, m_domainsMap.get(columnSpec.getName()));
            } else {
                domainSpecs[i] = columnSpec;
            }
        }
        return exec.createSpecReplacerTable(data, new DataTableSpec(spec.getName(), domainSpecs));
    }

    /**
     * Updates the domain of the input spec.
     *
     * @param inSpec the spec to be updated
     * @param domainVals the min and max value of the input spec column
     * @return the updated spec
     */
    private DataColumnSpec updateDomainSpec(final DataColumnSpec inSpec, final double[] domainVals) {
        DataColumnSpecCreator specCreator = new DataColumnSpecCreator(inSpec);
        DataColumnDomainCreator domainCreator = new DataColumnDomainCreator(inSpec.getDomain());
        DataCell[] domainBounds = createBoundCells(inSpec.getType(), domainVals[0], domainVals[1]);
        domainCreator.setLowerBound(domainBounds[0]);
        domainCreator.setUpperBound(domainBounds[1]);
        specCreator.setDomain(domainCreator.createDomain());
        return specCreator.createSpec();
    }

    /**
     * Creates two data cells of the proper type holding storing the given domain.
     *
     * @param type the type of the cell to create
     * @param lowerBound the lower bound of the domain
     * @param upperBound the upper bound of the domain
     * @return cells of the proper storing the given value
     */
    private DataCell[] createBoundCells(final DataType type, final double lowerBound, final double upperBound) {
        if (type == DoubleCell.TYPE) {
            return new DataCell[]{DoubleCellFactory.create(lowerBound), DoubleCellFactory.create(upperBound)};
        }
        // for int and long type use floor of the lower bound and ceil of the upper bound
        if (type == LongCell.TYPE) {
            return new DataCell[]{LongCellFactory.create((long)Math.floor(lowerBound)),
                LongCellFactory.create((long)Math.ceil(upperBound))};
        }
        // it must be a int cell
        return new DataCell[]{IntCellFactory.create((int)Math.floor(lowerBound)),
            IntCellFactory.create((int)Math.ceil(upperBound))};
    }

    /**
     * Updates the domain for the respective column.
     *
     * @param colName the outlier column name
     * @param val the value
     */
    void updateDomain(final String colName, final double val) {
        if (!m_domainsMap.containsKey(colName)) {
            m_domainsMap.put(colName, new double[]{val, val});
        }
        final double[] domainVals = m_domainsMap.get(colName);
        domainVals[0] = Math.min(domainVals[0], val);
        domainVals[1] = Math.max(domainVals[1], val);
    }

}
