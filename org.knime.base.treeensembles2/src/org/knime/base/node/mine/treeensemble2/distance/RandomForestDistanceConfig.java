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
 *   16.02.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.distance;

import java.io.IOException;
import java.util.zip.ZipEntry;

import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModel;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObject;
import org.knime.base.util.flowvariable.FlowVariableProvider;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.distance.DistanceMeasureConfig;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
public class RandomForestDistanceConfig extends DistanceMeasureConfig<RandomForestDistance>{

    private final String CFG_TABLESPEC = "learnTableSpec";
    private final String CFG_MODELCONTENT = "modelContent";

    private TreeEnsembleModel m_ensembleModel;
    private DataTableSpec m_learnTableSpec;

    /**
     * @param ensemblePO
     */
    public RandomForestDistanceConfig(final TreeEnsembleModelPortObject ensemblePO) {
        super(ensemblePO.getSpec().getLearnTableSpec().getColumnNames());
        m_ensembleModel = ensemblePO.getEnsembleModel();
        m_learnTableSpec = ensemblePO.getSpec().getLearnTableSpec();
    }

    /**
     * Framework constructor
     */
    public RandomForestDistanceConfig() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFactoryId() {
        return RandomForestDistanceFactory.ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RandomForestDistance createDistanceMeasure(final DataTableSpec spec, final FlowVariableProvider flowVariableProvider)
        throws InvalidSettingsException {
        return new RandomForestDistance(this, spec, m_ensembleModel, m_learnTableSpec);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final String prefix, final PortObjectZipOutputStream outputStream, final ExecutionMonitor exec)
        throws CanceledExecutionException, IOException {
        outputStream.putNextEntry(new ZipEntry("model.zip"));
        m_ensembleModel.save(outputStream, exec);
        outputStream.closeEntry();
        outputStream.putNextEntry(new ZipEntry("spec.zip"));
        ModelContent mc = new ModelContent(CFG_MODELCONTENT);
        m_learnTableSpec.save(mc.addModelContent(CFG_TABLESPEC));
        mc.saveToXML(outputStream);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final String prefix, final PortObjectZipInputStream inputStream, final ExecutionMonitor exec)
        throws InvalidSettingsException, CanceledExecutionException, IOException {
        ZipEntry nextEntry = inputStream.getNextEntry();
        if (new ZipEntry("model.zip").equals(nextEntry)) {
            throw new IOException("Expected model.zip entry");
        }
        m_ensembleModel = TreeEnsembleModel.load(inputStream, exec);
        inputStream.closeEntry();
        nextEntry = inputStream.getNextEntry();
        ModelContentRO mc = ModelContent.loadFromXML(inputStream);
        m_learnTableSpec = DataTableSpec.load(mc.getModelContent(CFG_TABLESPEC));
    }
}
