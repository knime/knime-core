/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   15.03.2016 (adrian): created
 */
package org.knime.base.node.meta.feature.selection;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;

import javax.swing.JComponent;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.util.Pair;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
public class FeatureSelectionModel implements PortObject, PortObjectSpec {

    /**
     * The {@link PortType} of {@link FeatureSelectionModel}
     */
    public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(FeatureSelectionModel.class);

    private final Collection<Pair<Double, Collection<Integer>>> m_featureLevels =
        new ArrayList<Pair<Double, Collection<Integer>>>();

    private final AbstractColumnHandler m_columnHandler;

    private boolean m_isMinimize;

    private String m_scoreName;

    /**
     * Creates a new model.
     * @param columnHandler the ColumnHandler that holds the information about the features and constant columns
     */
    public FeatureSelectionModel(final AbstractColumnHandler columnHandler) {
        m_columnHandler = columnHandler;
        createUnmodList();
    }

    private Collection<Pair<Double, Collection<String>>> createUnmodList() {
        final List<Pair<Double, Collection<String>>> list = new ArrayList<>(m_featureLevels.size());
        for (final Pair<Double, Collection<Integer>> featureLevel : m_featureLevels) {
            list.add(new Pair<>(featureLevel.getFirst(), m_columnHandler.getColumnNamesFor(featureLevel.getSecond())));
        }

        return Collections.unmodifiableCollection(list);
    }

    /**
     * @param scoreName the name of the score variable
     */
    public void setScoreName(final String scoreName) {
        m_scoreName = scoreName;
    }

    /**
     * Adds a new feature level.
     *
     * @param error the resulting error rate
     * @param includedColumns a list with the included column names
     */
    public void addFeatureLevel(final double error, final Collection<Integer> includedColumns) {
        m_featureLevels.add(new Pair<Double, Collection<Integer>>(error,
            Collections.unmodifiableCollection(new ArrayList<Integer>(includedColumns))));
    }

    /**
     * Returns an unmodifieable collection of the stored feature levels. Each entry if a pair with the error rate as
     * first part and a list of all columns that were included in the iteration as second part.
     *
     * @return a collection with pairs
     */
    public Collection<Pair<Double, Collection<String>>> featureLevels() {
        return createUnmodList();
    }

    /**
     * Checks if the input table contains all columns specified in the selected feature level and provides a warning message if any columns are missing.
     * It also provides an outSpec if only some of the columns are missing.
     *
     * @param settings the settings of the FeatureSelectionFilter node
     * @param inSpec {@link DataTableSpec} of the input table
     * @return a Pair containing a possible warning message and the outSpec
     * @throws InvalidSettingsException thrown if no columns of the feature level are contained in the input table
     */
    public Pair<String, DataTableSpec> getTableSpecAndWarning(final FeatureSelectionFilterSettings settings, final DataTableSpec inSpec) throws InvalidSettingsException {
        final boolean includeStaticCols = settings.includeConstantColumns();
        final Collection<Integer> includedFeatures = getIncludedFeatures(settings);
        return m_columnHandler.getOutSpecAndWarning(includedFeatures, inSpec, includeStaticCols);
    }

    private Collection<Integer> getIncludedFeatures(final FeatureSelectionFilterSettings settings) {
        // get included features
        Collection<Integer> includedFeatures = new ArrayList<>();
        if (settings.thresholdMode()) {
            // find minimal set for given threshold
            Pair<Double, Collection<Integer>> p = findMinimalSet(settings.errorThreshold());
            if (p != null) {
                includedFeatures.addAll(p.getSecond());
            }
        } else {
            // find features with specified length
            includedFeatures.addAll(getLevelWithLength(settings.nrOfFeatures()).getSecond());
        }
        return includedFeatures;
    }

    /**
     * Uses the ColumnHandler to create the output table containing the columns selected in the node dialog.
     *
     * @param settings
     * @param inTable
     * @param exec
     * @return a table containing the features that were selected in the node dialog
     * @throws CanceledExecutionException
     */
    public BufferedDataTable createTable(final FeatureSelectionFilterSettings settings, final BufferedDataTable inTable,
        final ExecutionContext exec) throws CanceledExecutionException {
        // get included features
        final Collection<Integer> includedFeatures = getIncludedFeatures(settings);
        final BufferedDataTable[] table = m_columnHandler.getTables(exec, new BufferedDataTable[]{inTable},
            includedFeatures, settings.includeConstantColumns());

        return table[0];
    }

    private Pair<Double, Collection<Integer>> getLevelWithLength(final int nrFeatures) {
        Optional<Pair<Double, Collection<Integer>>> o =
            m_featureLevels.stream().filter(p -> p.getSecond().size() == nrFeatures).findFirst();
        if (o.isPresent()) {
            return o.get();
        } else {
            throw new IllegalStateException(
                "There exists no feature level with the specified length \"" + nrFeatures + "\".");
        }
    }

    /**
     * Returns the names of the features contained in the smallest feature set that is better than <b>threshold</b>.
     * What is considered as better depends on whether the score variable should be minimized or not (e.g. in case
     * of accuracy the minimal set that has a accuracy larger than <b>threshold</b> is returned while for error, the
     * minimal set with an error lower than <b>threshold</b> is returned).
     *
     * @param threshold the threshold to use
     * @return the minimal set with a better score than <b>threshold</b>
     */
    public Collection<String> getNamesOfMinimialSet(final double threshold) {
        final Pair<Double, Collection<Integer>> minimalSet = findMinimalSet(threshold);
        return m_columnHandler.getColumnNamesFor(minimalSet.getSecond());
    }

    private Pair<Double, Collection<Integer>> findMinimalSet(final double threshold) {
        Pair<Double, Collection<Integer>> minimalSet = null;
        int minimalSetSize = Integer.MAX_VALUE;
        for (final Pair<Double, Collection<Integer>> level : m_featureLevels) {
            if (comp(level.getFirst(), threshold) && level.getSecond().size() < minimalSetSize) {
                minimalSet = level;
                minimalSetSize = level.getSecond().size();
            }
        }

        return minimalSet;
    }

    private final boolean comp(final double value, final double threshold) {
        return m_isMinimize ? value <= threshold : value >= threshold;
    }

    /**
     * Returns the target column's name.
     *
     * @return the name
     */
    public String[] getConstantColumns() {
        final List<String> constantCols = m_columnHandler.getConstantColumns();
        return constantCols.toArray(new String[constantCols.size()]);
    }

    /**
     * @return true if the score should be minimized
     */
    public boolean isMinimize() {
        return m_isMinimize;
    }

    /**
     * @return the name of the variable containing the score
     */
    public String getScoreName() {
        return m_scoreName;
    }

    /**
     * @param isMinimize whether the score should be minimized or not
     */
    public void setIsMinimize(final boolean isMinimize) {
        m_isMinimize = isMinimize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObjectSpec getSpec() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        return m_featureLevels.size() + " different features";
    }

    /**
     * @noreference This class is not intended to be referenced by clients.
     * @since 3.1.2
     */

    public static final class NewSpecSerializer extends PortObjectSpecSerializer<FeatureSelectionModel> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void savePortObjectSpec(final FeatureSelectionModel model, final PortObjectSpecZipOutputStream out)
            throws IOException {
            out.putNextEntry(new ZipEntry("spec.file"));
            final DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(out));
            model.m_columnHandler.save(outStream);
            outStream.writeBoolean(model.isMinimize());
            outStream.writeUTF(model.m_scoreName);
            outStream.writeInt(model.m_featureLevels.size());
            for (final Pair<Double, Collection<Integer>> level : model.m_featureLevels) {
                outStream.writeDouble(level.getFirst());
                outStream.writeInt(level.getSecond().size());
                for (final Integer feature : level.getSecond()) {
                    outStream.writeInt(feature);
                }
            }
            outStream.flush();
            outStream.close();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public FeatureSelectionModel loadPortObjectSpec(final PortObjectSpecZipInputStream in) throws IOException {
            in.getNextEntry();
            final DataInputStream inStream = new DataInputStream(new BufferedInputStream(in));
            final AbstractColumnHandler colHandler = AbstractColumnHandler.loadColumnHandler(inStream);
            final FeatureSelectionModel model = new FeatureSelectionModel(colHandler);
            model.setIsMinimize(inStream.readBoolean());
            model.setScoreName(inStream.readUTF());
            final int numFeatureLevels = inStream.readInt();
            for (int i = 0; i < numFeatureLevels; i++) {
                double score = inStream.readDouble();
                int length = inStream.readInt();
                List<Integer> list = new ArrayList<>(length);
                for (int j = 0; j < length; j++) {
                    list.add(inStream.readInt());
                }
                model.addFeatureLevel(score, Collections.unmodifiableCollection(list));
            }
            inStream.close();
            return model;
        }

    }


    /**
     * @noreference This class is not intended to be referenced by clients.
     * @since 3.0
     */
    public static final class ModelSerializer extends PortObjectSerializer<FeatureSelectionModel> {
        /**
         * {@inheritDoc}
         */
        @Override
        public FeatureSelectionModel loadPortObject(final PortObjectZipInputStream in, final PortObjectSpec spec,
            final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
            return (FeatureSelectionModel)spec;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void savePortObject(final FeatureSelectionModel portObject, final PortObjectZipOutputStream out,
            final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        return null;
    }
}
