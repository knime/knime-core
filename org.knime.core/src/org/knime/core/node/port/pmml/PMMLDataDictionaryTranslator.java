/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * ------------------------------------------------------------------------
  *
  * History
  *   May 18, 2011 (morent): created
  */

package org.knime.core.node.port.pmml;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.xmlbeans.SchemaType;
import org.dmg.pmml40.DATATYPE;
import org.dmg.pmml40.DataDictionaryDocument.DataDictionary;
import org.dmg.pmml40.DataFieldDocument.DataField;
import org.dmg.pmml40.DerivedFieldDocument.DerivedField;
import org.dmg.pmml40.IntervalDocument.Interval;
import org.dmg.pmml40.OPTYPE;
import org.dmg.pmml40.PMMLDocument;
import org.dmg.pmml40.ValueDocument.Value;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class PMMLDataDictionaryTranslator implements PMMLTranslator {
    private DataTableSpec m_spec = null;
    private final List<String> m_activeDerivedFields;
    private final List<String> m_dictFields;

    /**
     * Creates an empty PMML dictionary translator.
     */
    public PMMLDataDictionaryTranslator() {
        m_activeDerivedFields = new ArrayList<String>();
        m_dictFields = new ArrayList<String>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeFrom(final PMMLDocument pmmlDoc) {
        List<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>();
       addColSpecsForDataFields(pmmlDoc, colSpecs);
       addOrUpdateColSpecsForDerivedFields(pmmlDoc, colSpecs);
       m_spec = new DataTableSpec(colSpecs.toArray(new DataColumnSpec[0]));
    }


    /**
     * @param pmmlDoc the PMML document to analyze
     * @param colSpecs the list to add the data column specs to
     */
    private void addColSpecsForDataFields(final PMMLDocument pmmlDoc,
            final List<DataColumnSpec> colSpecs) {
        DataDictionary dict = pmmlDoc.getPMML().getDataDictionary();
           for (DataField dataField : dict.getDataFieldArray()) {
               String name = dataField.getName();
               DataType dataType = getKNIMEDataType(dataField.getDataType());
               DataColumnSpecCreator specCreator = new DataColumnSpecCreator(
                       name, dataType);
               DataColumnDomain domain = null;
               if (dataType.isCompatible(NominalValue.class)) {
                   Value[] valueArray = dataField.getValueArray();
                   DataCell[] cells;
                   if (DataType.getType(StringCell.class).equals(dataType)) {
                       if (dataField.getIntervalArray().length > 0) {
                           throw new IllegalArgumentException(
                                   "Intervals cannot be defined for Strings.");
                       }
                       cells = new StringCell[valueArray.length];
                       if (valueArray != null && valueArray.length > 0) {
                           for (int j = 0; j < cells.length; j++) {
                               cells[j] = new StringCell(
                                       valueArray[j].getValue());
                           }
                       }
                       domain = new DataColumnDomainCreator(cells)
                               .createDomain();
                   }
                } else if (dataType.isCompatible(DoubleValue.class)) {
                    Double leftMargin = null;
                    Double rightMargin = null;
                    Interval[] intervalArray = dataField.getIntervalArray();
                    if (intervalArray != null && intervalArray.length > 0) {
                       Interval interval = dataField.getIntervalArray(0);
                       leftMargin = interval.getLeftMargin();
                       rightMargin = interval.getRightMargin();
                   } else if (dataField.getValueArray() != null
                           && dataField.getValueArray().length > 0) {
                       // try to derive the bounds from the values
                       Value[] valueArray = dataField.getValueArray();
                       List<Double> values = new ArrayList<Double>();
                       for (int j = 0; j < valueArray.length; j++) {
                           String value = "";
                           try {
                               value = valueArray[j].getValue();
                            values.add(Double.parseDouble(value));
                            } catch (Exception e) {
                                throw new IllegalArgumentException(
                                        "Skipping domain calculation. "
                                        + "Value \"" + value
                                        + "\" cannot be cast to double.");
                            }
                       }
                       leftMargin = Collections.min(values);
                       rightMargin = Collections.max(values);
                   }
                    if (leftMargin != null && rightMargin != null) {
                        // set the bounds of the domain if available
                        DataCell lowerBound = null;
                        DataCell upperBound = null;
                        if (DataType.getType(IntCell.class).equals(dataType)) {
                            lowerBound = new IntCell(leftMargin.intValue());
                            upperBound = new IntCell(rightMargin.intValue());
                        } else if (DataType.getType(DoubleCell.class).equals(
                                dataType)) {
                            lowerBound = new DoubleCell(leftMargin);
                            upperBound = new DoubleCell(rightMargin);
                        }
                        domain = new DataColumnDomainCreator(lowerBound,
                                upperBound).createDomain();
                    } else {
                        domain = new DataColumnDomainCreator().createDomain();
                    }

                }
               specCreator.setDomain(domain);
               colSpecs.add(specCreator.createSpec());
               m_dictFields.add(name);
           }
    }

    /**
     * @param pmmlDoc the PMML document to analyze
     * @param colSpecs the list to add the data column specs to
     */
    private void addOrUpdateColSpecsForDerivedFields(final PMMLDocument pmmlDoc,
            final List<DataColumnSpec> colSpecs) {
        DerivedField[] derivedFields = DerivedFieldMapper.getDerivedFields(
                pmmlDoc.getPMML());
        DerivedFieldMapper mapper = new DerivedFieldMapper(derivedFields);
        Set<String> mappedColumnNames = mapper.getDerivedNames();
        for (DerivedField df : derivedFields) {
            String name = df.getName();
            DataType dataType = getKNIMEDataType(df.getDataType());
            if (!mappedColumnNames.contains(name)) {
                /* It is a "real" column - not one that is mapping to another
                 * column. Hence add a data column spec. */
                DataColumnSpecCreator specCreator = new DataColumnSpecCreator(
                        name, dataType);
                DataColumnSpec newSpec = specCreator.createSpec();
                colSpecs.add(newSpec);
                m_activeDerivedFields.add(name);
            } else {
                /* Update the data type of the referenced data column spec
                 * by replacing it with a spec of the new type. */
                String colName = mapper.getColumnName(name);
                DataColumnSpecCreator specCreator = new DataColumnSpecCreator(
                        colName, dataType);
                DataColumnSpec newSpec = specCreator.createSpec();
                for (int i = 0; i < colSpecs.size(); i++) {
                    if (colSpecs.get(i).getName().equals(colName)) {
                        colSpecs.remove(i);
                        colSpecs.add(i, newSpec);
                        break;
                    }
                }
                m_activeDerivedFields.add(colName);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SchemaType exportTo(final PMMLDocument pmmlDoc,
            final PMMLPortObjectSpec spec) {
        return exportTo(pmmlDoc, spec.getDataTableSpec());
    }

    /**
     * Adds a data dictionary to the PMML document based on the
     * {@link DataTableSpec}.
     *
     * @param pmmlDoc the PMML document to export to
     * @param dts the data table spec
     * @return the schema type of the exported schema if applicable, otherwise
     *         null
     * @see #exportTo(PMMLDocument, PMMLPortObjectSpec)
     */
    public SchemaType exportTo(final PMMLDocument pmmlDoc,
            final DataTableSpec dts) {
        DataDictionary dict = DataDictionary.Factory.newInstance();
        dict.setNumberOfFields(BigInteger.valueOf(dts.getNumColumns()));
        DataField dataField;
        for (DataColumnSpec colSpec : dts) {
            dataField = dict.addNewDataField();
            dataField.setName(colSpec.getName());
            DataType dataType = colSpec.getType();
            dataField.setOptype(getOptype(dataType));
            dataField.setDataType(getPMMLDataType(dataType));

            // Value
            if (colSpec.getType().isCompatible(NominalValue.class)
                    && colSpec.getDomain().hasValues()) {
                for (DataCell possVal : colSpec.getDomain().getValues()) {
                    Value value = dataField.addNewValue();
                    value.setValue(possVal.toString());
                }
            } else if (colSpec.getType().isCompatible(DoubleValue.class)
                    && colSpec.getDomain().hasBounds()) {
                Interval interval = dataField.addNewInterval();
                interval.setClosure(Interval.Closure.CLOSED_CLOSED);
                interval.setLeftMargin(((DoubleValue)colSpec.getDomain()
                        .getLowerBound()).getDoubleValue());
                interval.setRightMargin(((DoubleValue)colSpec.getDomain()
                        .getUpperBound()).getDoubleValue());
            }
        }
        pmmlDoc.getPMML().setDataDictionary(dict);
        return null; //no schematype available yet
    }

    /**
     * @param dataType the data type to get the PMML optype type for
     * @return the PMML data type for the {@link DataColumnSpec}
     */
     public static OPTYPE.Enum getOptype(final DataType dataType) {
        if (dataType.isCompatible(DoubleValue.class)) {
            return OPTYPE.CONTINUOUS;
        }
        return OPTYPE.CATEGORICAL;
    }

     /**
      *
      * @param pmmlType the PMML data type
      * @return the corresponding {@link DataType} in KNIME
      */
     public static DataType getKNIMEDataType(final DATATYPE.Enum pmmlType) {
         if (DATATYPE.INTEGER == pmmlType) {
             return DataType.getType(IntCell.class);
         } else if (DATATYPE.FLOAT == pmmlType) {
             return DataType.getType(DoubleCell.class);
         } else if (DATATYPE.DOUBLE == pmmlType) {
             return DataType.getType(DoubleCell.class);
         } else if (DATATYPE.STRING == pmmlType) {
             return DataType.getType(StringCell.class);
         } else if (DATATYPE.BOOLEAN == pmmlType) {
             return DataType.getType(BooleanCell.class);
//         TODO: Add date and time support
//         } else if (DATATYPE.XXX == pmmlType) {
//             return DataType.getType(XXX.class);
         } else {
           // handle it as string
             return DataType.getType(StringCell.class);
         }
     }

     /**
     *
     * @param dataType the KNIME data type to get the PMML data type
     *      attribute for
     * @return the PMML data type for the {@link DataType}
     */
    public static DATATYPE.Enum getPMMLDataType(
            final DataType dataType) {
        DATATYPE.Enum pmmlDataType;
        if (dataType.isCompatible(BooleanValue.class)) {
            pmmlDataType = DATATYPE.BOOLEAN;
        } else if (dataType.isCompatible(IntValue.class)) {
            pmmlDataType = DATATYPE.INTEGER;
        } else if (dataType.isCompatible(DoubleValue.class)) {
            pmmlDataType = DATATYPE.DOUBLE;
        } else {
            // handle everything else as String to stay backward compatible
            pmmlDataType = DATATYPE.STRING;
//            throw new IllegalArgumentException("Type " + dataType
//                    + " is not supported"
//                    + " by PMML. Allowed types are only all "
//                    + "double-compatible and all nominal value "
//                    + "compatible types.");
        }
        return pmmlDataType;
    }

    /**
     * @return the data table spec or null if the
     *      {@link #initializeFrom(PMMLDocument)} has not been invoked before
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * @return the activeDerivedFields
     */
    public List<String> getActiveDerivedFields() {
        return m_activeDerivedFields;
    }

    /**
     * @return the dictFields
     */
    public List<String> getDictionaryFields() {
        return m_dictFields;
    }

}
