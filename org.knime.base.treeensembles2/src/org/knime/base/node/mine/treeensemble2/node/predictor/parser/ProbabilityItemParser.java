/**
 *
 */
package org.knime.base.node.mine.treeensemble2.node.predictor.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.base.node.mine.treeensemble2.node.predictor.ClassificationPrediction;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.util.UniqueNameGenerator;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class ProbabilityItemParser implements PredictionItemParser<ClassificationPrediction> {

    private final Map<String, DataCell> m_targetValueMap;

    private final String m_suffix;

    private final String m_prefix;

    private final String m_classColName;

    private final String[] m_classValues;

    /**
     * Constructor for parsers that append individual class probabilities.
     *
     * @param targetValueMap map of targetValues
     * @param prefix to prepend
     * @param suffix to append
     * @param classColName name of the class column
     * @param classValues possible class values
     */
    public ProbabilityItemParser(final Map<String, DataCell> targetValueMap, final String prefix, final String suffix,
        final String classColName, final String[] classValues) {
        m_targetValueMap = targetValueMap;
        m_prefix = prefix;
        m_suffix = suffix;
        m_classColName = classColName;
        m_classValues = classValues == null ? null : classValues.clone();
    }

    /* (non-Javadoc)
     * @see org.knime.base.node.mine.treeensemble2.node.predictor.PredictionItemParser#appendSpecs(org.knime.core.util.UniqueNameGenerator, java.util.List)
     */
    @Override
    public void appendSpecs(final UniqueNameGenerator nameGenerator, final List<DataColumnSpec> specs) {
        final String targetColName = m_classColName;
        for (String val : m_targetValueMap.keySet()) {
            String colName = m_prefix + targetColName + "=" + val + ")" + m_suffix;
            specs.add(nameGenerator.newColumn(colName, DoubleCell.TYPE));
        }

    }

    /* (non-Javadoc)
     * @see org.knime.base.node.mine.treeensemble2.node.predictor.PredictionItemParser#appendCells(java.util.List, org.knime.base.node.mine.treeensemble2.node.predictor.Prediction)
     */
    @Override
    public void appendCells(final List<DataCell> cells, final ClassificationPrediction prediction) {
        if (m_classValues == null) {
            throw new IllegalStateException("No class values available.");
        }
        int nrClasses = m_targetValueMap.size();
        // the map is necessary to ensure that the probabilities are correctly associated with the column header
           final Map<String, Double> classProbMap = new HashMap<>((int)(nrClasses * 1.5));
           for (int i = 0; i < nrClasses; i++) {
               classProbMap.put(m_classValues[i], prediction.getProbability(i));
           }
           for (final String className : m_targetValueMap.keySet()) {
               cells.add(new DoubleCell(classProbMap.get(className)));
           }
    }

}
