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
 *
 * History
 *   Jan 1, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.model;

import java.io.IOException;
import java.util.zip.ZipEntry;

import javax.swing.JComponent;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.AbstractPortObject;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class TreeEnsembleModelPortObject extends AbstractPortObject {
    public static final class Serializer extends AbstractPortObjectSerializer<TreeEnsembleModelPortObject> {}

    public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(TreeEnsembleModelPortObject.class);

    private TreeEnsembleModelPortObjectSpec m_spec;

    private TreeEnsembleModel m_ensembleModel;

    /**
     * @param models
     */
    public TreeEnsembleModelPortObject(final TreeEnsembleModelPortObjectSpec spec, final TreeEnsembleModel ensembleModel) {
        m_spec = spec;
        m_ensembleModel = ensembleModel;
    }

    /** Framework constructor, not to be used by node code. */
    public TreeEnsembleModelPortObject() {
        // no op, load method to be called by framework
    }

    /** @return the ensembleModel */
    public TreeEnsembleModel getEnsembleModel() {
        return m_ensembleModel;
    }

    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        StringBuilder b = new StringBuilder();
        b.append(m_ensembleModel.getNrModels());
        b.append(" classifiers on ");
        b.append(m_ensembleModel.getMetaData().getNrAttributes());
        b.append(" attributes");
        return b.toString();
    }

    /** {@inheritDoc} */
    @Override
    public TreeEnsembleModelPortObjectSpec getSpec() {
        return m_spec;
    }

    public PMMLPortObject createDecisionTreePMMLPortObject(final int modelIndex) {
        DataTableSpec attributeLearnSpec = m_ensembleModel.getLearnAttributeSpec(m_spec.getLearnTableSpec());
        DataColumnSpec targetSpec = m_spec.getTargetColumn();
        PMMLPortObjectSpecCreator pmmlSpecCreator =
            new PMMLPortObjectSpecCreator(new DataTableSpec(attributeLearnSpec, new DataTableSpec(targetSpec)));

        try {
            pmmlSpecCreator.setLearningCols(attributeLearnSpec);
        } catch (InvalidSettingsException e) {
            // this exception is not actually thrown in the code
            // (as of KNIME v2.5.1)
            throw new IllegalStateException(e);
        }
        pmmlSpecCreator.setTargetCol(targetSpec);
        PMMLPortObjectSpec pmmlSpec = pmmlSpecCreator.createSpec();
        PMMLPortObject portObject = new PMMLPortObject(pmmlSpec);
        TreeModelClassification model = m_ensembleModel.getTreeModelClassification(modelIndex);
        portObject.addModelTranslater(new TreeModelPMMLTranslator(model));
        return portObject;
    }

    /** {@inheritDoc} */
    @Override
    protected void save(final PortObjectZipOutputStream out, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        out.putNextEntry(new ZipEntry("treeensemble.bin"));
        m_ensembleModel.save(out, exec);
        out.closeEntry();
    }

    /** {@inheritDoc} */
    @Override
    protected void load(final PortObjectZipInputStream in, final PortObjectSpec spec, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        in.getNextEntry();
        m_ensembleModel = TreeEnsembleModel.load(in, exec);
        in.closeEntry();
        m_spec = (TreeEnsembleModelPortObjectSpec)spec;
    }

    /** {@inheritDoc} */
    @Override
    public JComponent[] getViews() {
        // TODO Auto-generated method stub
        return null;
    }
}
