package org.knime.base.node.preproc.pmml.binner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.dmg.pmml40.DATATYPE;
import org.dmg.pmml40.DerivedFieldDocument.DerivedField;
import org.dmg.pmml40.DiscretizeBinDocument.DiscretizeBin;
import org.dmg.pmml40.DiscretizeDocument.Discretize;
import org.dmg.pmml40.IntervalDocument.Interval;
import org.dmg.pmml40.IntervalDocument.Interval.Closure;
import org.dmg.pmml40.LocalTransformationsDocument.LocalTransformations;
import org.dmg.pmml40.OPTYPE;
import org.dmg.pmml40.TransformationDictionaryDocument.TransformationDictionary;
import org.knime.base.node.preproc.pmml.binner.BinnerColumnFactory.Bin;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocTranslator;

/**
 *
 */
public class PMMLBinningTranslator implements PMMLPreprocTranslator {
    private static final String SUMMARY = "summary";

    /** Selected columns for binning. */
    private final Map<String, Bin[]> m_columnToBins;

    private final Map<String, String> m_columnToAppend;

    private DerivedFieldMapper m_mapper;




    /**
     * Creates a new empty translator to be initialized by one of the
     * initializeFrom methods.
     */
    public PMMLBinningTranslator() {
        super();
        m_columnToBins = new TreeMap<String, BinnerColumnFactory.Bin[]>();
        m_columnToAppend = new TreeMap<String, String>();
    }

    /**
     * Creates an initialized translator for export to PMML.
     *
     * @param columnToBins the colums to bin
     * @param columnToAppended mapping of existing column names to binned
     *      column names that are appended
     * @param mapper mapping data column names to PMML derived field names and
     *      vice versa
     */
    public PMMLBinningTranslator(final Map<String, Bin[]> columnToBins,
            final Map<String, String> columnToAppended,
            final DerivedFieldMapper mapper) {
        super();
        m_columnToBins = columnToBins;
        m_columnToAppend = columnToAppended;
        m_mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransformationDictionary exportToTransDict() {
        TransformationDictionary dictionary =
                TransformationDictionary.Factory.newInstance();
        dictionary.setDerivedFieldArray(createDerivedFields());
        return dictionary;
    }

    private DerivedField[] createDerivedFields() {
        int num = m_columnToBins.size();
        DerivedField[] derivedFields = new DerivedField[num];

        int i = 0;
        for (Map.Entry<String, Bin[]> entry : m_columnToBins.entrySet()) {
            Bin[] bins = entry.getValue();
            DerivedField df = DerivedField.Factory.newInstance();
            String name = entry.getKey();

            /* The field name must be retrieved before creating a new derived
             * name for this derived field as the map only contains the
             * current mapping. */
            String fieldName = m_mapper.getDerivedFieldName(name);
            Discretize dis = df.addNewDiscretize();
            dis.setField(fieldName);

            String derivedName = m_columnToAppend.get(name);
            if (derivedName != null) {
                df.setName(derivedName);
            } else {
                df.setName(m_mapper.createDerivedFieldName(name));
                df.setDisplayName(name);
            }
            df.setOptype(OPTYPE.CATEGORICAL);
            df.setDataType(DATATYPE.STRING);
            for (int j = 0; j < bins.length; j++) {
                NumericBin knimeBin = (NumericBin)bins[j];
                boolean leftOpen = knimeBin.isLeftOpen();
                boolean rightOpen = knimeBin.isRightOpen();
                double leftValue = knimeBin.getLeftValue();
                double rightValue = knimeBin.getRightValue();
                DiscretizeBin pmmlBin = dis.addNewDiscretizeBin();
                pmmlBin.setBinValue(knimeBin.getBinName());
                Interval interval = pmmlBin.addNewInterval();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalTransformations exportToLocalTrans() {
        LocalTransformations localtrans =
                LocalTransformations.Factory.newInstance();
        localtrans.setDerivedFieldArray(createDerivedFields());
        return localtrans;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> initializeFrom(final DerivedField[] derivedFields) {
        m_mapper = new DerivedFieldMapper(derivedFields);
        List<Integer> consumed = new ArrayList(derivedFields.length);
        for (int i = 0; i < derivedFields.length; i++) {
            DerivedField df = derivedFields[i];

            if (!df.isSetDiscretize()) {
                // only reading discretize entries other entries are skipped
                continue;
            }
            consumed.add(i);
            Discretize discretize = df.getDiscretize();
            DiscretizeBin[] pmmlBins = discretize.getDiscretizeBinArray();
            NumericBin[] knimeBins = new NumericBin[pmmlBins.length];

            for (int j = 0; j < pmmlBins.length; j++) {
                DiscretizeBin bin = pmmlBins[j];
                String binName = bin.getBinValue();
                Interval interval = bin.getInterval();

                double leftValue = interval.getLeftMargin();
                double rightValue = interval.getRightMargin();

                Closure.Enum closure = interval.getClosure();
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
                knimeBins[j] =
                        new NumericBin(binName, leftOpen, leftValue, rightOpen,
                                rightValue);
            }

            /** This field contains the name of the column in KNIME that
             * corresponds to the derived field in PMML. This is necessary if
             * derived fields are defined on other derived fields and the
             * columns in KNIME are replaced with the preprocessed values.
             * In this case KNIME has to know the original names (e.g. A) while
             * PMML references to A', A'' etc. */
            String displayName = df.getDisplayName();
            if (displayName != null) {
                m_columnToBins.put(displayName, knimeBins);
                m_columnToAppend.put(displayName, null);
            } else if (df.getName() != null) {
                String field = m_mapper.getColumnName(discretize.getField());
                m_columnToBins.put(field, knimeBins);
                m_columnToAppend.put(field, df.getName());
            }
        }
        return consumed;
    }

}
