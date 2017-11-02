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
 * Created on 03.11.2014 by Alexander
 */
package org.knime.base.node.mine.knn.pmml;

import java.math.BigInteger;
import java.util.LinkedHashMap;

import org.apache.xmlbeans.SchemaType;
import org.dmg.pmml.COMPAREFUNCTION;
import org.dmg.pmml.ComparisonMeasureDocument.ComparisonMeasure;
import org.dmg.pmml.InlineTableDocument.InlineTable;
import org.dmg.pmml.InstanceFieldDocument.InstanceField;
import org.dmg.pmml.InstanceFieldsDocument.InstanceFields;
import org.dmg.pmml.KNNInputDocument.KNNInput;
import org.dmg.pmml.KNNInputsDocument.KNNInputs;
import org.dmg.pmml.NearestNeighborModelDocument.NearestNeighborModel;
import org.dmg.pmml.PMMLDocument;
import org.dmg.pmml.PMMLDocument.PMML;
import org.dmg.pmml.RowDocument.Row;
import org.dmg.pmml.TrainingInstancesDocument.TrainingInstances;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.port.pmml.PMMLMiningSchemaTranslator;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLTranslator;
import org.knime.core.pmml.PMMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Alexander
 */
public class PMMLKNNTranslator implements PMMLTranslator {

    private DataTable m_table;
    private int m_maxRecords;
    private int m_numNeighbors;
    private String[] m_includes;
    /**
     * Constructor for PMMLKNNTranslator.
     * @param table the table with the data for the knn model
     * @param maxRecords the maximum number of records added to the PMML
     * @param numNeighbors the number of neighbors to take into account for classification of new records
     * @param includes the included columns
     */
    public PMMLKNNTranslator(final DataTable table, final int maxRecords, final int numNeighbors,
                             final String[] includes) {
        m_table = table;
        m_maxRecords = maxRecords;
        m_numNeighbors = numNeighbors;
        m_includes = includes;
    }

    /**
     * Constructor for PMMLKNNTranslator.
     * @param table the table with the data for the knn model
     * @param includes the included columns
     */
    public PMMLKNNTranslator(final DataTable table, final String[] includes) {
        this(table, 0, 3, includes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeFrom(final PMMLDocument pmmlDoc) {
        // TODO Read from inline table?
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SchemaType exportTo(final PMMLDocument pmmlDoc, final PMMLPortObjectSpec spec) {
        LinkedHashMap<Integer, String> columnNames = new LinkedHashMap<Integer, String>();
        DataTableSpec tSpec = m_table.getDataTableSpec();

        // Find learning columns and store them in the map for later
        for (String lc : m_includes) {
            columnNames.put(tSpec.findColumnIndex(lc), "col" + columnNames.size());
        }

        // Create initial XML elements
        PMML pmml = pmmlDoc.getPMML();
        NearestNeighborModel knn = pmml.addNewNearestNeighborModel();
        PMMLMiningSchemaTranslator.writeMiningSchema(spec, knn);
        knn.setAlgorithmName("K-Nearest Neighbors");
        knn.setFunctionName(org.dmg.pmml.MININGFUNCTION.CLASSIFICATION);
        knn.setNumberOfNeighbors(BigInteger.valueOf(m_numNeighbors));
        // Only euclidean is supported so far
        ComparisonMeasure cm = knn.addNewComparisonMeasure();
        cm.addNewEuclidean();

        // KNNInputs is a list of the fields used for determining the distance
        KNNInputs inputs = knn.addNewKNNInputs();
        for (int i : columnNames.keySet()) {
            KNNInput input = inputs.addNewKNNInput();
            String col = tSpec.getColumnSpec(i).getName();
            input.setField(col);
            input.setCompareFunction(COMPAREFUNCTION.ABS_DIFF);
        }

        TrainingInstances ti = knn.addNewTrainingInstances();

        // Here we create a mapping from column name to name of the XML element for the column's values
        InstanceFields instanceFields = ti.addNewInstanceFields();
        for (int i : columnNames.keySet()) {
            InstanceField instanceField = instanceFields.addNewInstanceField();
            String col = tSpec.getColumnSpec(i).getName();
            instanceField.setField(col);
            instanceField.setColumn(columnNames.get(i));
        }

        int targetIdx = tSpec.findColumnIndex(spec.getTargetFields().get(0));
        InstanceField target = instanceFields.addNewInstanceField();
        target.setField(spec.getTargetFields().get(0));
        target.setColumn("target");

        // The inline table holds the actual data.
        // We use the map we created in the beginning to determine the element xml-element-names
        InlineTable it = ti.addNewInlineTable();
        Document doc = it.getDomNode().getOwnerDocument();
        int counter = 0;
        for (DataRow row : m_table) {
            // Stop if we have reached the maximum number of records
            if (m_maxRecords > -1 && ++counter > m_maxRecords) {
                break;
            }
            Row inlineRow = it.addNewRow();
            Element rowNode = (Element)inlineRow.getDomNode();

            for (int col : columnNames.keySet()) {
                Element field = doc.createElementNS(PMMLUtils.getPMMLCurrentVersionNamespace(), columnNames.get(col));
                field.appendChild(doc.createTextNode(row.getCell(col).toString()));
                rowNode.appendChild(field);
            }

            Element targetField = doc.createElementNS(PMMLUtils.getPMMLCurrentVersionNamespace(), "target");
            targetField.appendChild(doc.createTextNode(row.getCell(targetIdx).toString()));
            rowNode.appendChild(targetField);
        }
        return NearestNeighborModel.type;
    }
}
