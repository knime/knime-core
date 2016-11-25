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
 *   Nov 23, 2016 (simon): created
 */
package org.knime.base.node.preproc.filter.definition.merger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.viewproperty.FilterDefinitionHandlerPortObject;

/**
 * The node model of the node which merges filter definitions.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
public class FilterDefinitionMergerNodeModel extends NodeModel {

    /**
     * One mandatory and two optional {@link FilterDefinitionHandlerPortObject} as input, one as {@link FilterDefinitionHandlerPortObject} output.
     */
    protected FilterDefinitionMergerNodeModel() {
        super(
            new PortType[]{FilterDefinitionHandlerPortObject.TYPE, FilterDefinitionHandlerPortObject.TYPE_OPTIONAL,
                FilterDefinitionHandlerPortObject.TYPE_OPTIONAL},
            new PortType[]{FilterDefinitionHandlerPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec modelSpec = new DataTableSpec();
        for (final DataTableSpec inSpec : inSpecs) {
            if (inSpec != null) {
                modelSpec = mergeFilters(modelSpec, inSpec);
            }
        }
        return new DataTableSpec[]{modelSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        DataTableSpec modelSpec = new DataTableSpec();
        for (final PortObject inObject : inObjects) {
            if (inObject != null) {
            modelSpec = mergeFilters(modelSpec, (DataTableSpec)inObject.getSpec());
            }
        }
        return new PortObject[]{new FilterDefinitionHandlerPortObject(modelSpec, "")};
    }

    /**
     * Merges two specs. If both have columns with same names, the column spec of the first spec is taken.
     *
     * @param spec1 first spec
     * @param spec2 second spec
     * @return merged spec
     */
    private DataTableSpec mergeFilters(final DataTableSpec spec1, final DataTableSpec spec2) {
        List<DataColumnSpec> allColSpecs = new ArrayList<>();
        List<String> allColNames = new ArrayList<>();
        for (final DataColumnSpec colSpec : spec1){
            if (colSpec.getFilterHandler().isPresent()){
                allColSpecs.add(colSpec);
                allColNames.add(colSpec.getName());
            }
        }
        for (final DataColumnSpec colSpec : spec2){
            if (colSpec.getFilterHandler().isPresent() && !allColNames.contains(colSpec.getName())){
                allColSpecs.add(colSpec);
            }
        }
        return new DataTableSpec(allColSpecs.toArray(new DataColumnSpec[allColSpecs.size()]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // nothing to save
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // nothing to validate
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // nothing to load
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

}
