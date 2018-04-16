/**
 *
 */
package org.knime.base.node.mine.treeensemble2.node.predictor;

import java.util.HashMap;
import java.util.Map;

import org.knime.base.data.filter.column.FilterColumnRow;
import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetColumnData;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModel;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObjectSpec;
import org.knime.base.node.mine.treeensemble2.sample.row.RowSample;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.InvalidSettingsException;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <P> the type of prediction
 */
public abstract class AbstractRandomForestPredictor<P extends OutOfBagPrediction> implements Predictor<P> {

    private RowSample[] m_modelLearnRowSamples;

    private Map<RowKey, Integer> m_rowKeyToLearnIndex;

    protected final TreeEnsembleModel m_model;

    private final DataTableSpec m_learnSpec;

    private final int[] m_filterIndices;

    /**
     * @param model
     * @param modelSpec
     * @param predictSpec
     * @throws InvalidSettingsException
     */
    public AbstractRandomForestPredictor(final TreeEnsembleModel model, final TreeEnsembleModelPortObjectSpec modelSpec,
        final DataTableSpec predictSpec) throws InvalidSettingsException {
        m_model = model;
        m_filterIndices = modelSpec.calculateFilterIndices(predictSpec);
        m_learnSpec = modelSpec.getLearnTableSpec();
    }

    /**
     * @param model
     * @param modelSpec
     * @param predictSpec
     * @param modelRowSamples
     * @param targetColumnData
     * @throws InvalidSettingsException
     */
    public AbstractRandomForestPredictor(final TreeEnsembleModel model, final TreeEnsembleModelPortObjectSpec modelSpec,
        final DataTableSpec predictSpec, final RowSample[] modelRowSamples, final TreeTargetColumnData targetColumnData)
        throws InvalidSettingsException {
        this(model, modelSpec, predictSpec);
        setOutofBagFilter(modelRowSamples, targetColumnData);
    }

    /* (non-Javadoc)
     * @see org.knime.base.node.mine.treeensemble2.node.predictor.Predictor#predict(org.knime.core.data.DataRow)
     */
    @Override
    public P predict(final DataRow row) {
        FilterColumnRow filterRow = new FilterColumnRow(row, m_filterIndices);
        return predictRecord(m_model.createPredictorRecord(filterRow, m_learnSpec), row.getKey());
    }

    /**
     * @param record the record to predict
     * @param key the row key to access out of bag information
     * @return the prediction
     */
    protected abstract P predictRecord(PredictorRecord record, RowKey key);

    private void setOutofBagFilter(final RowSample[] modelRowSamples, final TreeTargetColumnData targetColumnData) {
        if (modelRowSamples == null || targetColumnData == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        final int nrRows = targetColumnData.getNrRows();
        Map<RowKey, Integer> learnItemMap = new HashMap<>((int)(nrRows / 0.75 + 1));
        for (int i = 0; i < nrRows; i++) {
            RowKey key = targetColumnData.getRowKeyFor(i);
            learnItemMap.put(key, i);
        }
        m_modelLearnRowSamples = modelRowSamples;
        m_rowKeyToLearnIndex = learnItemMap;
    }

    /**
     * @return true if <b>this<b> has an out of bag filter
     */
    protected final boolean hasOutOfBagFilter() {
        return m_modelLearnRowSamples != null;
    }

    /**
     * @param key
     * @param modelIndex
     * @return true if the row with rowkey <b>key</b> in model with index <b>modelIndex</b> is part of the training data
     *         for this model
     */
    protected final boolean isRowPartOfTrainingData(final RowKey key, final int modelIndex) {
        assert m_modelLearnRowSamples != null : "no out of bag filter set";
        Integer indexInteger = m_rowKeyToLearnIndex.get(key);
        if (indexInteger == null) {
            return false;
        }
        int index = indexInteger;
        return m_modelLearnRowSamples[modelIndex].getCountFor(index) > 0;
    }

}
