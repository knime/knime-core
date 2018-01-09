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
 *   27.10.2015 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.model;

import java.io.IOException;
import java.util.zip.ZipEntry;

import javax.swing.JComponent;

import org.knime.base.node.mine.treeensemble2.model.pmml.RegressionTreeModelPMMLTranslator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.AbstractPortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class RegressionTreeModelPortObject extends AbstractPortObject {
    public static final class Serializer extends AbstractPortObjectSerializer<RegressionTreeModelPortObject> {}

    public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(RegressionTreeModelPortObject.class);

    public static final String SUMMARY = "Regression Tree Model";

    private RegressionTreeModelPortObjectSpec m_spec;

    private RegressionTreeModel m_treeModel;

    public RegressionTreeModelPortObject(final RegressionTreeModel treeModel, final RegressionTreeModelPortObjectSpec spec) {
        if (treeModel == null || spec == null) {
            throw new NullPointerException("Null Arguments are not allowed.");
        }
        m_spec = spec;
        m_treeModel = treeModel;
    }

    /*
     * Framework constructor for serialization
     */
    public RegressionTreeModelPortObject() {
        // not to be used by node
    }

    public PMMLPortObject createDecisionTreePMMLPortObject() {
        final RegressionTreeModel model = getModel();
        DataTableSpec attributeLearnSpec = model.getLearnAttributeSpec(m_spec.getLearnTableSpec());
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
        final TreeModelRegression tree = model.getTreeModel();
        portObject.addModelTranslater(new RegressionTreeModelPMMLTranslator(tree, model.getMetaData(), m_spec.getLearnTableSpec()));
        return portObject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        StringBuilder b = new StringBuilder();
        b.append("Classifiers on ");
        b.append(m_treeModel.getMetaData().getNrAttributes());
        b.append(" attributes");
        return b.toString();
    }


    public RegressionTreeModel getModel() {
        return m_treeModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RegressionTreeModelPortObjectSpec getSpec() {
        return m_spec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final PortObjectZipOutputStream out, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        out.putNextEntry(new ZipEntry("treeensemble.bin"));
        m_treeModel.save(out, exec);
        out.closeEntry();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final PortObjectZipInputStream in, final PortObjectSpec spec, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        in.getNextEntry();
        final TreeBuildingInterner treeBuildingInterner = new TreeBuildingInterner();
        m_treeModel = RegressionTreeModel.load(in, exec, treeBuildingInterner);
        in.closeEntry();
        m_spec = (RegressionTreeModelPortObjectSpec)spec;

    }

}
