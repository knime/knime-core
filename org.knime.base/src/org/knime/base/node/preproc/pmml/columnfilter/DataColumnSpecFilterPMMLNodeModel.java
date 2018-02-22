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
package org.knime.base.node.preproc.pmml.columnfilter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.dmg.pmml.AggregateDocument.Aggregate;
import org.dmg.pmml.ApplyDocument.Apply;
import org.dmg.pmml.DataDictionaryDocument.DataDictionary;
import org.dmg.pmml.DataFieldDocument.DataField;
import org.dmg.pmml.DerivedFieldDocument.DerivedField;
import org.dmg.pmml.DiscretizeDocument.Discretize;
import org.dmg.pmml.FieldColumnPairDocument.FieldColumnPair;
import org.dmg.pmml.FieldRefDocument.FieldRef;
import org.dmg.pmml.MapValuesDocument.MapValues;
import org.dmg.pmml.MiningFieldDocument.MiningField;
import org.dmg.pmml.MiningSchemaDocument.MiningSchema;
import org.dmg.pmml.NormContinuousDocument.NormContinuous;
import org.dmg.pmml.NormDiscreteDocument.NormDiscrete;
import org.dmg.pmml.PMMLDocument;
import org.dmg.pmml.PMMLDocument.PMML;
import org.knime.base.node.preproc.filter.column.DataColumnSpecFilterNodeModel;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.util.AutocloseableSupplier;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLModelWrapper;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortObjectOutput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.w3c.dom.Document;

/**
 * The model for the column filter which extracts certain columns from the input
 * {@link org.knime.core.data.DataTable} using a list of columns to
 * exclude.
 *
 * @author Thomas Gabriel, KNIME.com AG, Zurich
 * @since 2.8
 */
public class DataColumnSpecFilterPMMLNodeModel extends DataColumnSpecFilterNodeModel {

    /** The default constructor for the <code>DataColumnSpecFilterPMMLNodeModel</code> class. */
    public DataColumnSpecFilterPMMLNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE, PMMLPortObject.TYPE},
              new PortType[]{BufferedDataTable.TYPE, PMMLPortObject.TYPE});
    }

     /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] data, final ExecutionContext exec) throws Exception {
        final BufferedDataTable inTable = (BufferedDataTable) data[0];
        final BufferedDataTable outTable = super.execute(new BufferedDataTable[]{inTable}, exec)[0];
        final FilterResult res = getFilterResult(inTable.getSpec());
        final PMMLPortObject pmmlOut = createPMMLOut((PMMLPortObject)data[1], outTable.getSpec(), res);
        return new PortObject[]{outTable, pmmlOut};
    }

    /**
     * Excludes a number of columns from the input spec and generates a new output spec.
     *
     * @param inSpecs the input table spec
     * @return outSpecs the output table spec with some excluded columns
     *
     * @throws InvalidSettingsException if the selected column is not available
     *             in the table spec.
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inSpec = (DataTableSpec) inSpecs[0];
        final DataTableSpec outSpec = super.configure(new DataTableSpec[]{inSpec})[0];
        final FilterResult res = getFilterResult(inSpec);
        final PMMLPortObjectSpec pmmlSpec = createPMMLSpec((PMMLPortObjectSpec) inSpecs[1], inSpec, res);
        return new PortObjectSpec[]{outSpec, pmmlSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo, final PortObjectSpec[] inSpecs)
        throws InvalidSettingsException {
        final ColumnRearranger corea = createColumnRearranger((DataTableSpec)inSpecs[0]);
        final StreamableFunction fct = corea.createStreamableFunction();
        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec) throws Exception {

                //execute streamable in- and outport (0)
                fct.init(exec);
                fct.runFinal(inputs, outputs, exec);
                fct.finish();

                DataTableSpec outSpec = corea.createSpec();

                //pmml ports
                PMMLPortObject pmmlIn = (PMMLPortObject) ((PortObjectInput) inputs[1]).getPortObject();
                final PMMLPortObject pmmlOut = createPMMLOut(pmmlIn, outSpec, getFilterResult((DataTableSpec) inSpecs[0]));
                ((PortObjectOutput) outputs[1]).setPortObject(pmmlOut);

            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE, InputPortRole.NONDISTRIBUTED_NONSTREAMABLE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED, OutputPortRole.NONDISTRIBUTED};
    }



 /*----------------------------------------------------------------------
  * Methods for PMML functionality
  ------------------------------------------------------------------------*/

    private boolean isExcluded(final String colName, final FilterResult res) {
        for (String name : res.getExcludes()) {
            if (name.equals(colName)) {
                return true;
            }
        }
        return false;
    }

    private PMMLPortObject createPMMLOut(final PMMLPortObject pmmlIn, final DataTableSpec outSpec, final FilterResult res) throws XmlException {
        StringBuffer warningBuffer = new StringBuffer();
        if (pmmlIn == null) {
            return new PMMLPortObject(createPMMLSpec(null, outSpec, res));
        } else {
            PMMLDocument pmmldoc;

            try (AutocloseableSupplier<Document> supplier = pmmlIn.getPMMLValue().getDocumentSupplier()) {
                pmmldoc = PMMLDocument.Factory.parse(supplier.get());
            }

            // Inspect models to check if they use any excluded columns
            List<PMMLModelWrapper> models = PMMLModelWrapper.getModelListFromPMMLDocument(pmmldoc);
            for (PMMLModelWrapper model : models) {
                MiningSchema ms = model.getMiningSchema();
                for (MiningField mf : ms.getMiningFieldList()) {
                    if (isExcluded(mf.getName(), res)) {
                        if (warningBuffer.length() != 0) {
                            warningBuffer.append("\n");
                        }
                        warningBuffer.append(model.getModelType().name() + " uses excluded column " + mf.getName());
                    }
                }
            }

            ArrayList<String> warningFields = new ArrayList<String>();
            PMML pmml = pmmldoc.getPMML();

            // Now check the transformations if they exist
            if (pmml.getTransformationDictionary() != null) {
                for (DerivedField df : pmml.getTransformationDictionary().getDerivedFieldList()) {
                    FieldRef fr = df.getFieldRef();
                    if (fr != null && isExcluded(fr.getField(), res)) {
                        warningFields.add(fr.getField());
                    }
                    Aggregate a = df.getAggregate();
                    if (a != null && isExcluded(a.getField(), res)) {
                        warningFields.add(a.getField());
                    }
                    Apply ap = df.getApply();
                    if (ap != null) {
                        for (FieldRef fieldRef : ap.getFieldRefList()) {
                            if (isExcluded(fieldRef.getField(), res)) {
                                warningFields.add(fieldRef.getField());
                                break;
                            }
                        }
                    }
                    Discretize d = df.getDiscretize();
                    if (d != null && isExcluded(d.getField(), res)) {
                        warningFields.add(d.getField());
                    }
                    MapValues mv = df.getMapValues();
                    if (mv != null) {
                        for (FieldColumnPair fcp : mv.getFieldColumnPairList()) {
                            if (isExcluded(fcp.getField(), res)) {
                                warningFields.add(fcp.getField());
                            }
                        }
                    }
                    NormContinuous nc = df.getNormContinuous();
                    if (nc != null && isExcluded(nc.getField(), res)) {
                        warningFields.add(nc.getField());
                    }
                    NormDiscrete nd = df.getNormDiscrete();
                    if (nd != null && isExcluded(nd.getField(), res)) {
                        warningFields.add(nd.getField());
                    }
                }
            }
            DataDictionary dict = pmml.getDataDictionary();
            List<DataField> fields = dict.getDataFieldList();
            // Apply filter to spec
            int numFields = 0;
            for (int i = fields.size() - 1; i >= 0; i--) {
                if (isExcluded(fields.get(i).getName(), res)) {
                    dict.removeDataField(i);
                } else {
                    numFields++;
                }
            }
            dict.setNumberOfFields(new BigInteger(Integer.toString(numFields)));
            pmml.setDataDictionary(dict);
            pmmldoc.setPMML(pmml);
            // generate warnings and set as warning message
            for (String s : warningFields) {
                if (warningBuffer.length() != 0) {
                    warningBuffer.append("\n");
                }
                warningBuffer.append("Transformation dictionary uses excluded column " + s);
            }
            if (warningBuffer.length() > 0) {
                setWarningMessage(warningBuffer.toString().trim());
            }
            PMMLPortObject outport = null;
            try {
                outport = new PMMLPortObject(createPMMLSpec(pmmlIn.getSpec(), outSpec, res), pmmldoc);
            } catch (IllegalArgumentException e) {
                if (res.getIncludes().length == 0) {
                    throw new IllegalArgumentException("Excluding all columns produces invalid PMML", e);
                } else {
                    throw e;
                }
            }
            return outport;
        }
    }

    private PMMLPortObjectSpec createPMMLSpec(final PMMLPortObjectSpec pmmlIn, final DataTableSpec spec,
                                              final FilterResult res) {
        PMMLPortObjectSpecCreator creator = new PMMLPortObjectSpecCreator(pmmlIn, spec);

        if (pmmlIn != null) {
            List<String> learningCols = new ArrayList<String>();
            for (DataColumnSpec s : pmmlIn.getLearningCols()) {
                if (!isExcluded(s.getName(), res)) {
                    learningCols.add(s.getName());
                }
            }
            creator.setLearningColsNames(learningCols);

            List<String> targetCols = new ArrayList<String>();
            for (DataColumnSpec s : pmmlIn.getTargetCols()) {
                if (!isExcluded(s.getName(), res)) {
                    targetCols.add(s.getName());
                }
            }
            creator.setTargetColsNames(targetCols);

            List<String> preprocCols = new ArrayList<String>();
            for (DataColumnSpec s : pmmlIn.getPreprocessingCols()) {
                if (!isExcluded(s.getName(), res)) {
                    preprocCols.add(s.getName());
                }
            }
            creator.setPreprocColNames(preprocCols);
        }
        return creator.createSpec();
    }

}

