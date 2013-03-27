/*
 *
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * -------------------------------------------------------------------
 * History
 *   Apr 20, 2011 (morent): created
 */

package org.knime.core.node.port.pmml.preproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.dmg.pmml.DerivedFieldDocument.DerivedField;
import org.dmg.pmml.LocalTransformationsDocument.LocalTransformations;
import org.dmg.pmml.PMMLDocument;
import org.dmg.pmml.PMMLDocument.PMML;
import org.dmg.pmml.TransformationDictionaryDocument.TransformationDictionary;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

/**
 * Creates a map of data column names to PMML derived field names for a PMML
 * document and vice versa. This is necessary for preprocessing operations in
 * KNIME that do not offer to append an additional column but replace an
 * existing column. In PMML there is no concept of overriding a field, but there
 * is always a new name created.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class DerivedFieldMapper {
    private static final String DERIVED_APPENDIX = "*";

    private final Map<String, String> m_derivedNames;
    private final Map<String, String> m_colNames;

    private DerivedFieldMapper() {
        m_derivedNames = new TreeMap<String, String>();
        m_colNames = new TreeMap<String, String>();
    }

    /**
     * @param pmmlDoc the PMML document used for initialization
     */
    public DerivedFieldMapper(final PMMLDocument pmmlDoc) {
        this();
        init(pmmlDoc.getPMML());
    }

    /**
     * @param pmmlPort the {@link PMMLPortObject} used for initialization
     */
    public DerivedFieldMapper(final PMMLPortObject pmmlPort) {
        this();
        if (pmmlPort != null) {
            processFields(pmmlPort.getDerivedFields());
        }
    }

    /**
     * @param derivedFields the derived fields used for initialization
     */
    public DerivedFieldMapper(final DerivedField[] derivedFields) {
        this();
        processFields(derivedFields);
    }

    private void init(final PMML pmml) {
        if (pmml.getHeader() == null
                || !PMMLPortObjectSpec.KNIME.equals(pmml.getHeader()
                        .getApplication().getName())) {
            return;
        }
        // reading the data dictionary and the local transformations
        DerivedField[] derivedFields = getDerivedFields(pmml);
        processFields(derivedFields);
    }

    private void processFields(final DerivedField[] derivedFields) {
        if (derivedFields == null) {
            return;
        }
        for (DerivedField df : derivedFields) {
            if (df.getDisplayName() == null) {
                continue;
            }
            /*
             * If multiple operations are performed on the same column there
             * will be multiple fields with the same display name (column name).
             * In this case the last one is relevant because the operations are
             * inserted in order. Putting each name in and overriding previous
             * mappings does this job.
             */
            m_derivedNames.put(df.getDisplayName(), df.getName());
            m_colNames.put(df.getName(), df.getDisplayName());
        }
    }

    /**
     * @param columnName the column name to find the derived field name for
     * @return the name of the derived field for the column or the column name
     *      if no derived field exists for it
     */
    public String getDerivedFieldName(final String columnName) {
        String name = m_derivedNames.get(columnName);
        return name != null ? name : columnName;
    }


    /**
     * @param fieldName the name of the field to find the column name for
     * @return the name of the column of the field
     */
    public String getColumnName(final String fieldName) {
        String name = m_colNames.get(fieldName);
        return name != null ? name : fieldName;
    }

    /**
     * Puts a pair of column name and derived field name into both maps to allow
     * a lookup in both directions.
     *
     * @param columnName the column name
     * @param fieldName the name of the PMML field
     */
    private void put(final String columnName, final String fieldName) {
        m_colNames.put(fieldName, columnName);
        m_derivedNames.put(columnName, fieldName);
    }

    /**
     * Creates a new unique derived field name and stores the mapping.
     * @param columnName the column name to create a unique derived field name
     *      for.
     * @return the created derived field name
     */
    public String createDerivedFieldName(final String columnName) {
        String derivedName = columnName + DERIVED_APPENDIX;
        while (m_colNames.containsKey(derivedName)) {
            derivedName += DERIVED_APPENDIX;
        }
        put(columnName, derivedName);
        return derivedName;
    }

    /**
     * @return all column names that have a mapping to a derived field name
     */
    public Set<String> getDerivedNames() {
        return m_colNames.keySet();
    }

    /**
     * @return a mapping of column names to derived field names
     */
    public Map<String, String> getDerivedFieldMap() {
        return Collections.unmodifiableMap(m_derivedNames);
    }

    /**
     * @param pmml the pmml document to retrieve the derived fields from
     * @return all derived fields from the transformation dictionary as well as
     *      of all local transformation elements, or an empty array if no
     *      derived fields are defined.
     */
    public static DerivedField[] getDerivedFields(final PMML pmml) {
        List<DerivedField> derivedFields = new ArrayList<DerivedField>();
        TransformationDictionary trans = pmml.getTransformationDictionary();
        if (trans != null) {
            derivedFields.addAll(Arrays.asList(trans.getDerivedFieldArray()));
        }
        LocalTransformations localTrans = null;
        if (pmml.getAssociationModelArray().length > 0) {
            localTrans = pmml.getAssociationModelArray(0)
                .getLocalTransformations();

        } else if (pmml.getClusteringModelArray().length > 0) {
            localTrans = pmml.getClusteringModelArray(0)
                    .getLocalTransformations();
        } else if (pmml.getGeneralRegressionModelArray().length > 0) {
            localTrans = pmml.getGeneralRegressionModelArray(0)
                    .getLocalTransformations();
        } else if (pmml.getNaiveBayesModelArray().length > 0) {
            localTrans = pmml.getNaiveBayesModelArray(0)
                    .getLocalTransformations();
        } else if (pmml.getNeuralNetworkArray().length > 0) {
            localTrans = pmml.getNeuralNetworkArray(0)
                    .getLocalTransformations();
        } else if (pmml.getRegressionModelArray().length > 0) {
            localTrans = pmml.getRegressionModelArray(0)
                    .getLocalTransformations();
        } else if (pmml.getRuleSetModelArray().length > 0) {
            localTrans = pmml.getRuleSetModelArray(0)
                    .getLocalTransformations();
        } else if (pmml.getSequenceModelArray().length > 0) {
            localTrans = pmml.getSequenceModelArray(0)
                    .getLocalTransformations();
        } else if (pmml.getSupportVectorMachineModelArray().length > 0) {
            localTrans = pmml.getSupportVectorMachineModelArray(0)
                    .getLocalTransformations();
        } else if (pmml.getTextModelArray().length > 0) {
            localTrans = pmml.getTextModelArray(0)
                    .getLocalTransformations();
        } else if (pmml.getTimeSeriesModelArray().length > 0) {
            localTrans = pmml.getTimeSeriesModelArray(0)
                    .getLocalTransformations();
        } else if (pmml.getTreeModelArray().length > 0) {
            localTrans = pmml.getTreeModelArray(0)
                    .getLocalTransformations();
        }
        if (localTrans != null) {
            derivedFields.addAll(Arrays.asList(
                    localTrans.getDerivedFieldArray()));
        }

        return derivedFields.toArray(new DerivedField[0]);
    }

}
