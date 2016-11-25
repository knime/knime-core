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
import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.base.node.mine.decisiontree2.model.DecisionTree;
import org.knime.base.node.mine.treeensemble2.data.NominalValueRepresentation;
import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.data.TreeBitColumnMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeNominalColumnMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeNumericColumnMetaData;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModel.TreeType;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.data.vector.doublevector.DoubleVectorValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
public final class RegressionTreeModel {


    private final TreeMetaData m_metaData;

    private final TreeType m_type;

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
        m_metaData = metaData;
        m_model = model;
        m_type = treeType;
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

    /**
     * @return the metaData
     */
    public TreeMetaData getMetaData() {
        return m_metaData;
    }

    /**
     * @return the type
     */
    public TreeType getType() {
        return m_type;
    }

    public DecisionTree createDecisionTree(final DataTable sampleForHiliting) {
        final DecisionTree result;
        TreeModelRegression treeModel = getTreeModelRegression();
        result = treeModel.createDecisionTree(m_metaData);

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
     * Get a table spec representing the learn attributes (not the target!). For ordinary data it is just a subset of
     * the input columns, for bit vector data it's an expanded table spec with each bit represented by a StringCell
     * column ("0" or "1"), whose name is "Bit x".
     *
     * @param learnSpec The original learn spec (which is also the return value for ordinary data)
     * @return Such a learn attribute spec.
     */
    public DataTableSpec getLearnAttributeSpec(final DataTableSpec learnSpec) {
        final TreeType type = getType();
        final int nrAttributes = getMetaData().getNrAttributes();
        switch (type) {
            case Ordinary:
                return learnSpec;
            case BitVector:
                DataColumnSpec[] colSpecs = new DataColumnSpec[nrAttributes];
                for (int i = 0; i < nrAttributes; i++) {
                    colSpecs[i] = new DataColumnSpecCreator(TreeBitColumnMetaData.getAttributeName(i), StringCell.TYPE)
                        .createSpec();
                }
                return new DataTableSpec(colSpecs);
            case ByteVector:
                DataColumnSpec[] bvColSpecs = new DataColumnSpec[nrAttributes];
                for (int i = 0; i < nrAttributes; i++) {
                    bvColSpecs[i] =
                        new DataColumnSpecCreator(TreeNumericColumnMetaData.getAttributeNameByte(i), IntCell.TYPE)
                            .createSpec();
                }
                return new DataTableSpec(bvColSpecs);
            case DoubleVector:
                DataColumnSpec[] dvColSpecs = new DataColumnSpec[nrAttributes];
                for (int i = 0; i < nrAttributes; i++) {
                    dvColSpecs[i] = new DataColumnSpecCreator(TreeNumericColumnMetaData.getAttributeName(i, "Double"), DoubleCell.TYPE).createSpec();
                }
                return new DataTableSpec(dvColSpecs);
            default:
                throw new IllegalStateException("Type unknown (not implemented): " + type);
        }
    }

    public DataRow createLearnAttributeRow(final DataRow learnRow, final DataTableSpec learnSpec) {
        final TreeType type = getType();
        final DataCell c = learnRow.getCell(0);
        final int nrAttributes = getMetaData().getNrAttributes();
        switch (type) {
            case Ordinary:
                return learnRow;
            case BitVector:
                if (c.isMissing()) {
                    return null;
                }
                BitVectorValue bv = (BitVectorValue)c;
                final long length = bv.length();
                if (length != nrAttributes) {
                    // TODO indicate error message
                    return null;
                }
                DataCell trueCell = new StringCell("1");
                DataCell falseCell = new StringCell("0");
                DataCell[] cells = new DataCell[nrAttributes];
                for (int i = 0; i < nrAttributes; i++) {
                    cells[i] = bv.get(i) ? trueCell : falseCell;
                }
                return new DefaultRow(learnRow.getKey(), cells);
            case ByteVector:
                if (c.isMissing()) {
                    return null;
                }
                ByteVectorValue byteVector = (ByteVectorValue)c;
                final long bvLength = byteVector.length();
                if (bvLength != nrAttributes) {
                    return null;
                }
                DataCell[] bvCells = new DataCell[nrAttributes];
                for (int i = 0; i < nrAttributes; i++) {
                    bvCells[i] = new IntCell(byteVector.get(i));
                }
                return new DefaultRow(learnRow.getKey(), bvCells);
            case DoubleVector:
                if (c.isMissing()) {
                    return null;
                }
                DoubleVectorValue doubleVector = (DoubleVectorValue)c;
                final int dvLenght = doubleVector.getLength();
                if (dvLenght != nrAttributes) {
                    return null;
                }
                DataCell[] dvCells = new DataCell[nrAttributes];
                for (int i = 0; i < nrAttributes; i++) {
                    dvCells[i] = new DoubleCell(doubleVector.getValue(i));
                }
                return new DefaultRow(learnRow.getKey(), dvCells);
            default:
                throw new IllegalStateException("Type unknown (not implemented): " + type);
        }
    }

    public PredictorRecord createPredictorRecord(final DataRow filterRow, final DataTableSpec learnSpec) {
        switch (m_type) {
            case Ordinary:
                return createNominalNumericPredictorRecord(filterRow, learnSpec);
            case BitVector:
                return createBitVectorPredictorRecord(filterRow);
            case ByteVector:
                return createByteVectorPredictorRecord(filterRow);
            case DoubleVector:
                return createDoubleVectorPredictorRecord(filterRow);
            default:
                throw new IllegalStateException("Unknown tree type " + "(not implemented): " + m_type);
        }
    }

    private PredictorRecord createDoubleVectorPredictorRecord(final DataRow filterRow) {
        assert filterRow.getNumCells() == 1 : "Expected one cell as double vector data";
        DataCell c = filterRow.getCell(0);
        if (c.isMissing()) {
            return null;
        }
        DoubleVectorValue dv = (DoubleVectorValue)c;
        final long length = dv.getLength();
        if (length != getMetaData().getNrAttributes()) {
            throw new IllegalArgumentException("The double-vector in " + filterRow.getKey().getString()
                + " has an invalid length (" + length + " instead of " + getMetaData().getNrAttributes() + ").");
        }
        Map<String, Object> valueMap = new LinkedHashMap<String, Object>((int)(length / 0.75 + 1.0));
        for (int i = 0; i < length; i++) {
            valueMap.put(TreeNumericColumnMetaData.getAttributeNameByte(i), Double.valueOf(dv.getValue(i)));
        }
        return new PredictorRecord(valueMap);
    }

    private PredictorRecord createByteVectorPredictorRecord(final DataRow filterRow) {
        assert filterRow.getNumCells() == 1 : "Expected one cell as byte vector data";
        DataCell c = filterRow.getCell(0);
        if (c.isMissing()) {
            return null;
        }
        ByteVectorValue bv = (ByteVectorValue)c;
        final long length = bv.length();
        if (length != getMetaData().getNrAttributes()) {
            throw new IllegalArgumentException("The byte-vector in " + filterRow.getKey().getString()
                + " has an invalid length (" + length + " instead of " + getMetaData().getNrAttributes() + ").");
        }
        Map<String, Object> valueMap = new LinkedHashMap<String, Object>((int)(length / 0.75 + 1.0));
        for (int i = 0; i < length; i++) {
            valueMap.put(TreeNumericColumnMetaData.getAttributeNameByte(i), Integer.valueOf(bv.get(i)));
        }
        return new PredictorRecord(valueMap);
    }

    private PredictorRecord createBitVectorPredictorRecord(final DataRow filterRow) {
        assert filterRow.getNumCells() == 1 : "Expected one cell as bit vector data";
        DataCell c = filterRow.getCell(0);
        if (c.isMissing()) {
            return null;
        }
        BitVectorValue bv = (BitVectorValue)c;
        final long length = bv.length();
        if (length != getMetaData().getNrAttributes()) {
            throw new IllegalArgumentException("The bit-vector in " + filterRow.getKey().getString()
                + " has an invalid length (" + length + " instead of " + getMetaData().getNrAttributes() + ").");
        }
        Map<String, Object> valueMap = new LinkedHashMap<String, Object>((int)(length / 0.75 + 1.0));
        for (int i = 0; i < length; i++) {
            valueMap.put(TreeBitColumnMetaData.getAttributeName(i), Boolean.valueOf(bv.get(i)));
        }
        return new PredictorRecord(valueMap);
    }

    private PredictorRecord createNominalNumericPredictorRecord(final DataRow filterRow,
        final DataTableSpec trainSpec) {
        final int nrCols = trainSpec.getNumColumns();
        Map<String, Object> valueMap = new LinkedHashMap<String, Object>((int)(nrCols / 0.75 + 1.0));
        for (int i = 0; i < nrCols; i++) {
            DataColumnSpec col = trainSpec.getColumnSpec(i);
            String colName = col.getName();
            DataType colType = col.getType();
            DataCell cell = filterRow.getCell(i);
            if (cell.isMissing()) {
                valueMap.put(colName, PredictorRecord.NULL);
            } else if (colType.isCompatible(NominalValue.class)) {
                final TreeNominalColumnMetaData meta = (TreeNominalColumnMetaData)m_metaData.getAttributeMetaData(i);
                final String val = ((StringCell)cell).getStringValue();
                int nomIdx = -1;
                for (final NominalValueRepresentation nomVal : meta.getValues()) {
                    if (val.equals(nomVal.getNominalValue())) {
                        nomIdx = nomVal.getAssignedInteger();
                        break;
                    }
                }
                valueMap.put(colName, nomIdx);
            } else if (colType.isCompatible(DoubleValue.class)) {
                valueMap.put(colName, ((DoubleValue)cell).getDoubleValue());
            } else {
                throw new IllegalStateException("Expected nominal or numeric column type for column \"" + colName
                    + "\" but got \"" + colType + "\"");
            }
        }
        return new PredictorRecord(valueMap);
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
        m_type.save(dataOutput);
        m_metaData.save(dataOutput);
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
