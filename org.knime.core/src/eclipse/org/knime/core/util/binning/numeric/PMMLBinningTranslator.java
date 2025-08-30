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
 */
package org.knime.core.util.binning.numeric;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.dmg.pmml.DATATYPE;
import org.dmg.pmml.DerivedFieldDocument.DerivedField;
import org.dmg.pmml.DiscretizeBinDocument.DiscretizeBin;
import org.dmg.pmml.DiscretizeDocument.Discretize;
import org.dmg.pmml.IntervalDocument.Interval;
import org.dmg.pmml.IntervalDocument.Interval.Closure;
import org.dmg.pmml.LocalTransformationsDocument.LocalTransformations;
import org.dmg.pmml.OPTYPE;
import org.dmg.pmml.TransformationDictionaryDocument.TransformationDictionary;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocTranslator;

/**
 * Implementation of {@link PMMLPreprocTranslator} for binning.
 *
 * @author Mor Kalla
 * @since 3.6
 *
 */
public class PMMLBinningTranslator implements PMMLPreprocTranslator {

    private final Map<String, Bin[]> m_columnToBins;

    private final Map<String, String> m_columnToAppend;

    private DerivedFieldMapper m_mapper;

    /**
     * Constructs a new empty translator to be initialized by one of the initializeFrom methods.
     */
    public PMMLBinningTranslator() {
        super();
        m_columnToBins = new TreeMap<>();
        m_columnToAppend = new TreeMap<>();
    }

    /**
     * Constructs an initialized translator for export to PMML.
     *
     * @param columnToBins the columns to bin
     * @param columnToAppended mapping of existing column names to binned column names that are appended
     * @param mapper mapping data column names to PMML derived field names and vice versa
     */
    public PMMLBinningTranslator(final Map<String, Bin[]> columnToBins, final Map<String, String> columnToAppended,
        final DerivedFieldMapper mapper) {
        super();
        m_columnToBins = columnToBins;
        m_columnToAppend = columnToAppended;
        m_mapper = mapper;
    }

    @Override
    public TransformationDictionary exportToTransDict() {
        final TransformationDictionary dictionary = TransformationDictionary.Factory.newInstance();
        dictionary.setDerivedFieldArray(createDerivedFields());
        return dictionary;
    }

    private DerivedField[] createDerivedFields() {
        final int num = m_columnToBins.size();
        final DerivedField[] derivedFields = new DerivedField[num];

        int i = 0;
        for (Map.Entry<String, Bin[]> entry : m_columnToBins.entrySet()) {
            final Bin[] bins = entry.getValue();
            final DerivedField df = DerivedField.Factory.newInstance();
            final String name = entry.getKey();

            /* The field name must be retrieved before creating a new derived
             * name for this derived field as the map only contains the
             * current mapping. */
            final String fieldName = m_mapper.getDerivedFieldName(name);
            final Discretize dis = df.addNewDiscretize();
            dis.setField(fieldName);

            final String derivedName = m_columnToAppend.get(name);
            if (derivedName != null) {
                df.setName(derivedName);
            } else {
                df.setName(m_mapper.createDerivedFieldName(name));
                df.setDisplayName(name);
            }
            df.setOptype(OPTYPE.CATEGORICAL);
            df.setDataType(DATATYPE.STRING);
            for (Bin bin : bins) {
                final NumericBin knimeBin = (NumericBin)bin;
                final boolean leftOpen = knimeBin.isLeftOpen();
                final boolean rightOpen = knimeBin.isRightOpen();
                final double leftValue = knimeBin.getLeftValue();
                final double rightValue = knimeBin.getRightValue();
                final DiscretizeBin pmmlBin = dis.addNewDiscretizeBin();
                pmmlBin.setBinValue(knimeBin.getBinName());
                final Interval interval = pmmlBin.addNewInterval();
                if (!Double.isInfinite(leftValue)) {
                    interval.setLeftMargin(leftValue);
                }
                if (!Double.isInfinite(rightValue)) {
                    interval.setRightMargin(rightValue);
                }
                if (leftOpen && rightOpen) {
                    interval.setClosure(Closure.OPEN_OPEN);
                } else if (leftOpen && !rightOpen) {
                    interval.setClosure(Closure.OPEN_CLOSED);
                } else if (!leftOpen && rightOpen) {
                    interval.setClosure(Closure.CLOSED_OPEN);
                } else if (!leftOpen && !rightOpen) {
                    interval.setClosure(Closure.CLOSED_CLOSED);
                }
            }
            derivedFields[i++] = df;
        }
        return derivedFields;
    }

    @Override
    public LocalTransformations exportToLocalTrans() {
        final LocalTransformations localtrans = LocalTransformations.Factory.newInstance();
        localtrans.setDerivedFieldArray(createDerivedFields());
        return localtrans;
    }

    @Override
    public List<Integer> initializeFrom(final DerivedField[] derivedFields) {
        m_mapper = new DerivedFieldMapper(derivedFields);
        final List<Integer> consumed = new ArrayList<>(derivedFields.length);
        for (int i = 0; i < derivedFields.length; i++) {
            final DerivedField df = derivedFields[i];

            if (!df.isSetDiscretize()) {
                // only reading discretize entries other entries are skipped
                continue;
            }
            consumed.add(i);
            final Discretize discretize = df.getDiscretize();
            @SuppressWarnings("deprecation")
            final DiscretizeBin[] pmmlBins = discretize.getDiscretizeBinArray();
            final NumericBin[] knimeBins = new NumericBin[pmmlBins.length];

            for (int j = 0; j < pmmlBins.length; j++) {
                final DiscretizeBin bin = pmmlBins[j];
                final String binName = bin.getBinValue();
                final Interval interval = bin.getInterval();

                final double leftValue = interval.getLeftMargin();
                final double rightValue = interval.getRightMargin();

                final Closure.Enum closure = interval.getClosure();
                boolean leftOpen = true;
                boolean rightOpen = true;
                if (Closure.OPEN_CLOSED == closure) {
                    rightOpen = false;
                } else if (Closure.CLOSED_OPEN == closure) {
                    leftOpen = false;
                } else if (Closure.CLOSED_CLOSED == closure) {
                    leftOpen = false;
                    rightOpen = false;
                }
                knimeBins[j] = new NumericBin(binName, leftOpen, leftValue, rightOpen, rightValue);
            }

            /**
             * This field contains the name of the column in KNIME that corresponds to the derived field in PMML. This
             * is necessary if derived fields are defined on other derived fields and the columns in KNIME are replaced
             * with the preprocessed values. In this case KNIME has to know the original names (e.g. A) while PMML
             * references to A', A'' etc.
             */
            final String displayName = df.getDisplayName();
            if (displayName != null) {
                m_columnToBins.put(displayName, knimeBins);
                m_columnToAppend.put(displayName, null);
            } else if (df.getName() != null) {
                final String field = m_mapper.getColumnName(discretize.getField());
                m_columnToBins.put(field, knimeBins);
                m_columnToAppend.put(field, df.getName());
            }
        }
        return consumed;
    }

}
