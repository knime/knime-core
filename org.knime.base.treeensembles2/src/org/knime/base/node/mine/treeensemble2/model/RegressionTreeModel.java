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
 *   Jan 20, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.knime.base.node.mine.decisiontree2.model.DecisionTree;
import org.knime.base.node.mine.treeensemble2.data.TreeMetaData;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
public final class RegressionTreeModel extends AbstractTreeEnsembleModel {

    private final TreeModelRegression m_model;

    /**
     * @param models
     */
    public RegressionTreeModel(final TreeEnsembleLearnerConfiguration configuration, final TreeMetaData metaData,
        final TreeModelRegression model, final TreeType treeType) {
        this(metaData, model, treeType);
    }

    /**
     * @param models
     */
    private RegressionTreeModel(final TreeMetaData metaData, final TreeModelRegression model, final TreeType treeType) {
        super(metaData, treeType);
        m_model = model;
    }

    /**
     * @return the models
     */
    public TreeModelRegression getTreeModel() {
        return m_model;
    }

    /**
     * @return the models
     */
    public TreeModelRegression getTreeModelRegression() {
        return m_model;
    }


    public DecisionTree createDecisionTree(final DataTable sampleForHiliting) {
        final DecisionTree result;
        TreeModelRegression treeModel = getTreeModelRegression();
        result = treeModel.createDecisionTree(getMetaData());

        if (sampleForHiliting != null) {
            final DataTableSpec dataSpec = sampleForHiliting.getDataTableSpec();
            final DataTableSpec spec = getLearnAttributeSpec(dataSpec);
            for (DataRow r : sampleForHiliting) {
                try {
                    DataRow fullAttributeRow = createLearnAttributeRow(r, spec);
                    result.addCoveredPattern(fullAttributeRow, spec);
                } catch (Exception e) {
                    // dunno what to do with that
                    NodeLogger.getLogger(getClass()).error("Error updating hilite info in tree view", e);
                    break;
                }
            }
        }
        return result;
    }



    /**
     * Saves ensemble to target in binary format, output is NOT closed afterwards.
     *
     * @param out ...
     * @param exec ...
     * @throws IOException ...
     * @throws CanceledExecutionException ...
     */
    public void save(final OutputStream out, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // wrapping the (zip) output stream with a buffered stream reduces
        // the write operation from, e.g. 63s to 8s
        DataOutputStream dataOutput = new DataOutputStream(new BufferedOutputStream(out));
        // previous version numbers:
        // 20121019 - first public release
        // 20140201 - omit target distribution in each tree node
        dataOutput.writeInt(20140201); // version number
        getType().save(dataOutput);
        getMetaData().save(dataOutput);
        try {
            m_model.save(dataOutput);
        } catch (IOException ioe) {
            throw new IOException("Can't save tree model.", ioe);
        }
        dataOutput.writeByte((byte)0);

        dataOutput.flush();
    }

    /**
     * Loads and returns new ensemble model, input is NOT closed afterwards.
     *
     * @param in ...
     * @param exec ...
     * @return ...
     * @throws IOException ...
     * @throws CanceledExecutionException ...
     */
    public static RegressionTreeModel load(final InputStream in, final ExecutionMonitor exec, final TreeBuildingInterner treeBuildingInterner)
        throws IOException, CanceledExecutionException {
        // wrapping the argument (zip input) stream in a buffered stream
        // reduces read operation from, e.g. 42s to 2s
        TreeModelDataInputStream input =
            new TreeModelDataInputStream(new BufferedInputStream(new NonClosableInputStream(in)));
        int version = input.readInt();
        if (version > 20140201) {
            throw new IOException("Tree Ensemble version " + version + " not supported");
        }
        TreeType type = TreeType.load(input);
        TreeMetaData metaData = TreeMetaData.load(input);
        boolean isRegression = metaData.isRegression();
        TreeModelRegression model;
        try {
            model = TreeModelRegression.load(input, metaData, treeBuildingInterner);
            if (input.readByte() != 0) {
                throw new IOException("Model not terminated by 0 byte");
            }
        } catch (IOException e) {
            throw new IOException("Can't read tree model. " + e.getMessage(), e);
        }
        input.close(); // does not close the method argument stream!!
        return new RegressionTreeModel(metaData, model, type);
    }

}
