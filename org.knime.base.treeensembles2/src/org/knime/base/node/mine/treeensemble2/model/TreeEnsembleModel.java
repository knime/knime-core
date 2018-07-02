/*
 * ------------------------------------------------------------------------
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
import java.util.Arrays;

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
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class TreeEnsembleModel extends AbstractTreeEnsembleModel {

    /**
     * Tracks the version of the tree ensemble.
     * The version is only updated when some changes are made to the tree ensembles.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public enum Version {

        /**
         * Version 2.6.0.
         * First public release.
         */
        V260(20121019),

        /**
         * Version 2.10.0.
         * Optionally omit target distribution to save memory.
         */
        V2100(20140201),

        /**
         * Version 3.2.0.
         * Release of gradient boosted trees.
         */
        V320(20160114),

        /**
         * Version 3.6.0.
         * Fix a serialization bug that only appears if a categorical column has many (>128) possible values.
         */
        V360(20180702);

        private int m_versionNumber;

        Version(final int versionNumber) {
            m_versionNumber = versionNumber;
        }

        public int getVersionNumber() {
            return m_versionNumber;
        }

        /**
         * Checks if this version is as new or newer as the <b>other</b> version.
         *
         * @param other the version to compare to
         * @return true if this version is as recent as other or newer
         */
        public boolean sameOrNewer(final Version other) {
            return m_versionNumber >= other.m_versionNumber;
        }

        public static Version getVersion(final int versionNumber) throws IOException {
            return Arrays.stream(values()).filter(v -> versionNumber == v.m_versionNumber).findFirst()
                    .orElseThrow(() -> new IOException("Tree Ensemble version " + versionNumber + " not supported"));
        }

    }

    private final AbstractTreeModel[] m_models;

    /**
     * For classification models if each tree node/leaf contains an array with the target class distribution. It
     * consumes a lot of memory and is only relevant for the view or when exporting individual trees to PMML. Only
     * useful when tree type is classification.
     */
    private final boolean m_containsClassDistribution;

    /**
     * @param models
     */
    public TreeEnsembleModel(final TreeEnsembleLearnerConfiguration configuration, final TreeMetaData metaData,
        final AbstractTreeModel[] models, final TreeType treeType) {
        this(metaData, models, treeType, configuration.isSaveTargetDistributionInNodes());
    }

    /**
     * @param models
     */
    public TreeEnsembleModel(final TreeMetaData metaData, final AbstractTreeModel[] models, final TreeType treeType,
        final boolean containsClassDistribution) {
        super(metaData, treeType);
        m_models = models;
        m_containsClassDistribution = containsClassDistribution;
    }

    /**
     *
     * @return true if the trees in the forest contain class distributions
     */
    public boolean containsClassDistribution() {
        return m_containsClassDistribution;
    }

    /**
     * @return the models
     */
    public AbstractTreeModel<?> getTreeModel(final int index) {
        return m_models[index];
    }

    /**
     * Retrieves the tree at <b>index</b> and casts it to a classification tree.
     *
     * @param index of the classification tree to retrieve
     * @return the model at <b>index</b>
     */
    public TreeModelClassification getTreeModelClassification(final int index) {
        return (TreeModelClassification)m_models[index];
    }

    /**
     * Retrieves the tree at <b>index</b> and casts it to a regression tree.
     *
     * @param index of the regression tree to retrieve
     * @return the models
     */
    public TreeModelRegression getTreeModelRegression(final int index) {
        return (TreeModelRegression)m_models[index];
    }

    /**
     * @return the number of models
     */
    public int getNrModels() {
        return m_models.length;
    }



    public DecisionTree createDecisionTree(final int modelIndex, final DataTable sampleForHiliting) {
        final DecisionTree result;
        final TreeMetaData metaData = getMetaData();
        if (metaData.isRegression()) {
            TreeModelRegression treeModel = getTreeModelRegression(modelIndex);
            result = treeModel.createDecisionTree(metaData);
        } else {
            TreeModelClassification treeModel = getTreeModelClassification(modelIndex);
            result = treeModel.createDecisionTree(metaData);
        }
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
     * @param out the stream to write to
     * @throws IOException if IO problems occur
     */
    public void save(final OutputStream out) throws IOException {
        // wrapping the (zip) output stream with a buffered stream reduces
        // the write operation from, e.g. 63s to 8s
        DataOutputStream dataOutput = new DataOutputStream(new BufferedOutputStream(out));
        // previous version numbers:
        // 20121019 - first public release
        // 20140201 - omit target distribution in each tree node
        // 20160114 - first version of gradient boosting
        dataOutput.writeInt(Version.V360.getVersionNumber()); // version number
        if (this instanceof GradientBoostedTreesModel) {
            dataOutput.writeByte('t');
        } else if (this instanceof MultiClassGradientBoostedTreesModel) {
            dataOutput.writeByte('m');
        } else if (this instanceof GradientBoostingModel) {
            dataOutput.writeByte('g');
        } else {
            dataOutput.writeByte('r');
        }
        getType().save(dataOutput);
        getMetaData().save(dataOutput);
        dataOutput.writeInt(m_models.length);
        dataOutput.writeBoolean(m_containsClassDistribution);
        for (int i = 0; i < m_models.length; i++) {
            AbstractTreeModel singleModel = m_models[i];
            try {
                singleModel.save(dataOutput);
            } catch (IOException ioe) {
                throw new IOException("Can't save tree model " + (i + 1) + "/" + m_models.length, ioe);
            }
            dataOutput.writeByte((byte)0);
        }
        saveData(dataOutput);
        dataOutput.flush();
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
        save(out);
    }

    /**
     * A subclass has to override this method to save additional data
     *
     * @param dataOutput
     * @throws IOException
     */
    protected void saveData(final DataOutputStream dataOutput) throws IOException {
        // no more data to save
    }

    public static TreeEnsembleModel load(final InputStream in) throws IOException {
        // wrapping the argument (zip input) stream in a buffered stream
        // reduces read operation from, e.g. 42s to 2s
        TreeModelDataInputStream input =
            new TreeModelDataInputStream(new BufferedInputStream(new NonClosableInputStream(in)));
        int version = input.readInt();
        if (version > Version.V360.getVersionNumber()) {
            throw new IOException("Tree Ensemble version " + version + " not supported");
        }
        byte ensembleType;
        if (version >= Version.V320.getVersionNumber()) {
            ensembleType = input.readByte();
        } else {
            ensembleType = 'r';
        }
        input.setVersion(Version.getVersion(version));
        TreeType type = TreeType.load(input);
        TreeMetaData metaData = TreeMetaData.load(input);
        int nrModels = input.readInt();
        boolean containsClassDistribution;
        if (version == Version.V260.getVersionNumber()) {
            containsClassDistribution = true;
        } else {
            containsClassDistribution = input.readBoolean();
        }
        input.setContainsClassDistribution(containsClassDistribution);
        AbstractTreeModel[] models = new AbstractTreeModel[nrModels];
        boolean isRegression = metaData.isRegression();
        if (ensembleType != 'r') {
            isRegression = true;
        }
        final TreeBuildingInterner treeBuildingInterner = new TreeBuildingInterner();
        for (int i = 0; i < nrModels; i++) {
            AbstractTreeModel singleModel;
            try {
                singleModel = isRegression ? TreeModelRegression.load(input, metaData, treeBuildingInterner)
                    : TreeModelClassification.load(input, metaData, treeBuildingInterner);
                if (input.readByte() != 0) {
                    throw new IOException("Model not terminated by 0 byte");
                }
            } catch (IOException e) {
                throw new IOException("Can't read tree model " + (i + 1) + "/" + nrModels + ": " + e.getMessage(), e);
            }
            models[i] = singleModel;
        }
        TreeEnsembleModel result;
        switch (ensembleType) {
            case 'r':
                result = new TreeEnsembleModel(metaData, models, type, containsClassDistribution);
                break;
            case 'g':
                result = new GradientBoostingModel(metaData, models, type, containsClassDistribution);
                break;
            case 't':
                result = new GradientBoostedTreesModel(metaData, models, type, containsClassDistribution);
                break;
            case 'm':
                result = new MultiClassGradientBoostedTreesModel(metaData, models, type, containsClassDistribution);
                break;
            default:
                throw new IllegalStateException("Unknown ensemble type: '" + (char)ensembleType + "'");
        }
        result.loadData(input);
        input.close(); // does not close the method argument stream!!
        return result;
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
    public static TreeEnsembleModel load(final InputStream in, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        return load(in);
    }

    /**
     * A subclass has to override this method to load any additional data
     *
     * @param input
     * @throws IOException
     */
    protected void loadData(final TreeModelDataInputStream input) throws IOException {
        // no more data to load
    }

}
