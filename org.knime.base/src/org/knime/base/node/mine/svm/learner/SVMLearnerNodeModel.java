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
 * ---------------------------------------------------------------------
 *
 * History
 *   27.09.2007 (cebron): created
 */
package org.knime.base.node.mine.svm.learner;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.knime.base.node.mine.svm.PMMLSVMHandler;
import org.knime.base.node.mine.svm.Svm;
import org.knime.base.node.mine.svm.kernel.Kernel;
import org.knime.base.node.mine.svm.kernel.KernelFactory;
import org.knime.base.node.mine.svm.kernel.KernelFactory.KernelType;
import org.knime.base.node.mine.svm.util.BinarySvmRunnable;
import org.knime.base.node.mine.svm.util.DoubleVector;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.util.KNIMETimer;
import org.knime.core.util.ThreadPool;

/**
 *
 * @author cebron, University of Konstanz
 */
public class SVMLearnerNodeModel extends NodeModel {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(SVMLearnerNodeModel.class);

    /**
     * Key to store the parameter c in the NodeSettings.
     */
    public static final String CFG_PARAMC = "c_parameter";

    /**
     * Key to store the class column in the NodeSettings.
     */
    public static final String CFG_CLASSCOL = "classcol";

    /**
     * Key to store kernel parameters in the NodeSettings ATTENTION: this key
     * name is used together with an index. So the i'th parameter will be in
     * KEY_KERNELPARAM + i.toString()
     */
    public static final String CFG_KERNELPARAM = "kernel_param";

    /**
     * Key to store the kernel type in the NodeSettings.
     */
    public static final String CFG_KERNELTYPE = "kernel_type";

    /** Keys under which to save the parameters. */
    public static final String KEY_CATEG_COUNT = "Category count";

    /** key to save the DataTableSpec .*/
    public static final String KEY_SPEC = "DataTableSpec";

    /** Key to save the DataTableSpec .*/
    public static final String KEY_CLASSCOL = "classcol";

    /** Default c parameter. */
    public static final double DEFAULT_PARAMC = 1.0;

    /*
     * The c parameter value.
     */
    private final SettingsModelDouble m_paramC =
            new SettingsModelDouble(CFG_PARAMC, DEFAULT_PARAMC);

    /*
     * Class column
     */
    private final SettingsModelString m_classcol =
            new SettingsModelString(CFG_CLASSCOL, "");

    /*
     * The chosen kernel
     */
    private KernelType m_kernelType = KernelFactory.getDefaultKernelType();

    private final HashMap<KernelType, Vector<SettingsModelDouble>> m_kernelParameters;

    /*
     * For each category, a BinarySvm that splits the category from the others.
     */
    private Svm[] m_svms;

    /*
     * String containing info about the trained SVM's
     */
    private String m_svmInfo = "";

    /**
     * creates the kernel parameter SettingsModels.
     *
     * @return HashMap containing the kernel and its assigned SettingsModels.
     */
    static LinkedHashMap<KernelType, Vector<SettingsModelDouble>>
    createKernelParams() {
        LinkedHashMap<KernelType, Vector<SettingsModelDouble>>
        kernelParameters =
                new LinkedHashMap<KernelType, Vector<SettingsModelDouble>>();
        for (KernelType kerneltype : KernelType.values()) {
            Kernel kernel = KernelFactory.getKernel(kerneltype);
            Vector<SettingsModelDouble> settings =
                    new Vector<SettingsModelDouble>();
            for (int i = 0; i < kernel.getNumberParameters(); i++) {
                settings.add(new SettingsModelDouble(CFG_KERNELPARAM + "_"
                        + kernel.getParameterName(i), kernel
                        .getDefaultParameter(i)));
            }
            kernelParameters.put(kerneltype, settings);
        }
        return kernelParameters;
    }

    /**
     *
     */
    public SVMLearnerNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE},
                new PortType[]{PMMLPortObject.TYPE});
        m_kernelParameters = createKernelParams();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec inSpec = (DataTableSpec)inSpecs[0];
        LearnColumnsAndColumnRearrangerTuple tuple =
            createTrainTableColumnRearranger(inSpec);
        DataTableSpec trainSpec = tuple.getTrainingRearranger().createSpec();
        PMMLPortObjectSpecCreator pmmlcreate =
                new PMMLPortObjectSpecCreator(trainSpec);
        pmmlcreate.setTargetCol(tuple.getTargetColumn());
        pmmlcreate.setLearningCols(tuple.getLearningColumns());
        return new PortObjectSpec[]{pmmlcreate.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable inTable = (BufferedDataTable)inData[0];
        LearnColumnsAndColumnRearrangerTuple tuple =
            createTrainTableColumnRearranger(inTable.getDataTableSpec());
        // no progress needed as constant operation (column removal only)
        BufferedDataTable trainTable = exec.createColumnRearrangeTable(inTable,
                tuple.getTrainingRearranger(), exec.createSubProgress(0.0));
        DataTableSpec trainSpec = trainTable.getDataTableSpec();
        int classpos = trainSpec.findColumnIndex(m_classcol.getStringValue());

        // convert input data
        ArrayList<DoubleVector> inputData = new ArrayList<DoubleVector>();
        List<String> categories = new ArrayList<String>();
        StringValue classvalue = null;
        for (DataRow row : trainTable) {
            exec.checkCanceled();
            ArrayList<Double> values = new ArrayList<Double>();
            boolean add = true;
            for (int i = 0; i < row.getNumCells(); i++) {
                if (row.getCell(i).isMissing()) {
                    add = false;
                    break;
                }
                if (i != classpos) {
                    DoubleValue cell = (DoubleValue)row.getCell(i);
                    values.add(cell.getDoubleValue());
                } else {
                    classvalue = (StringValue)row.getCell(classpos);
                    if (!categories.contains(classvalue.getStringValue())) {
                        categories.add(classvalue.getStringValue());
                    }
                }
            }
            if (add) {
                inputData.add(new DoubleVector(row.getKey(), values, classvalue
                        .getStringValue()));
            }
        }
        if (categories.isEmpty()) {
            throw new Exception("No categories found to train SVM. "
                    + "Possibly an empty input table was provided.");
        }
        DoubleVector[] inputDataArr = new DoubleVector[inputData.size()];
        inputDataArr = inputData.toArray(inputDataArr);
        Kernel kernel = KernelFactory.getKernel(m_kernelType);
        Vector<SettingsModelDouble> kernelparams =
                m_kernelParameters.get(m_kernelType);
        for (int i = 0; i < kernel.getNumberParameters(); ++i) {
            kernel.setParameter(i, kernelparams.get(i).getDoubleValue());
        }

        m_svms = new Svm[categories.size()];
        exec.setMessage("Training SVM");
        final BinarySvmRunnable[] bst =
                new BinarySvmRunnable[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            bst[i] =
                    new BinarySvmRunnable(inputDataArr, categories.get(i),
                            kernel, m_paramC.getDoubleValue(),
                            exec.createSubProgress((1.0 / categories.size())));

        }
        ThreadPool pool = KNIMEConstants.GLOBAL_THREAD_POOL;
        final Future<?>[] fut = new Future<?>[bst.length];
        KNIMETimer timer = KNIMETimer.getInstance();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    exec.checkCanceled();
                } catch (final CanceledExecutionException ce) {
                    for (int i = 0; i < fut.length; i++) {
                        if (fut[i] != null) {
                            fut[i].cancel(true);
                        }
                    }
                    super.cancel();
                }

            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, 3000);
        for (int i = 0; i < bst.length; i++) {
            fut[i] = pool.enqueue(bst[i]);
        }

        try {
            pool.runInvisible(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    for (int i = 0; i < fut.length; ++i) {
                        fut[i].get();
                        bst[i].ok();
                        m_svms[i] = bst[i].getSvm();
                    }
                    return null;
                }
            });
        } catch (Exception ex) {
            exec.checkCanceled();
            Throwable t = ex;
            if (ex instanceof ExecutionException) {
                t = ex.getCause();
            }
            if (t instanceof Exception) {
                throw (Exception) t;
            } else {
                throw new Exception(t);
            }
        } finally {
            for (int i = 0; i < fut.length; i++) {
                fut[i].cancel(true);
            }
            timerTask.cancel();
        }

        PMMLPortObjectSpecCreator pmmlcreate =
            new PMMLPortObjectSpecCreator(trainSpec);
        pmmlcreate.setLearningCols(trainSpec);
        pmmlcreate.setTargetCol(trainSpec.getColumnSpec(m_classcol
                .getStringValue()));
        PMMLPortObjectSpec pmmlspec = pmmlcreate.createSpec();
        PMMLPortObject pmml =
            new PMMLPortObject(pmmlspec, new PMMLSVMHandler(categories,
                    Arrays.asList(m_svms), kernel));
        return new PortObject[]{pmml};
    }

    private LearnColumnsAndColumnRearrangerTuple
        createTrainTableColumnRearranger(final DataTableSpec spec)
        throws InvalidSettingsException {
        if (spec.getNumColumns() == 0) {
            throw new InvalidSettingsException("No columns in input table");
        }
        String classCol = m_classcol.getStringValue();
        if (classCol == null || classCol.length() == 0) {
            throw new InvalidSettingsException("Class column not set");
        }
        DataColumnSpec targetColumn = null;
        ArrayList<DataColumnSpec> learningColumns =
            new ArrayList<DataColumnSpec>();
        ArrayList<String> rejectedColumns = new ArrayList<String>();
        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec colspec = spec.getColumnSpec(i);
            if (colspec.getName().equals(classCol)) {
                if (!colspec.getType().isCompatible(StringValue.class)) {
                    throw new InvalidSettingsException("Target column "
                            + colspec.getName() + " must be nominal.");
                }
                targetColumn = colspec;
            } else {
                if (colspec.getType().isCompatible(DoubleValue.class)) {
                    learningColumns.add(colspec);
                } else {
                    rejectedColumns.add(colspec.getName());
                }
            }
        }
        if (targetColumn == null) {
            throw new InvalidSettingsException("Target column \""
                    + m_classcol.getStringValue() + "\" not found"
                    + " in DataTableSpec.");
        }
        if (learningColumns.isEmpty()) {
            throw new InvalidSettingsException("Input DataTable does"
                    + " not contain one single valid column.");
        }
        int rejectedColumnsSize = rejectedColumns.size();
        if (rejectedColumnsSize > 0) {
            List<String> shortList = rejectedColumns;
            // do not list 1000+ columns in a user warning message
            int maxLength = 4;
            if (rejectedColumnsSize >= maxLength) {
                shortList = new ArrayList<String>(
                        rejectedColumns.subList(0, maxLength));
                shortList.add("... (remainder truncated)"); // now 4 elements
            }
            setWarningMessage("Rejecting " + rejectedColumnsSize + " column(s)"
                    + " due to incompatible type: " + shortList);
        }

        String[] validColumns = new String[learningColumns.size() + 1];
        for (int i = 0; i < learningColumns.size(); i++) {
            validColumns[i] = learningColumns.get(i).getName();
        }
        validColumns[validColumns.length - 1] = targetColumn.getName();
        ColumnRearranger result = new ColumnRearranger(spec);
        result.keepOnly(validColumns);
        return new LearnColumnsAndColumnRearrangerTuple(
                result, learningColumns, targetColumn);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File f = new File(nodeInternDir, "SVM_Internals");
        FileReader in = new FileReader(f);
        StringBuffer sb = new StringBuffer();
        char c;
        while (in.ready()) {
            c = (char)in.read();
            sb.append(c);
        }
        m_svmInfo = sb.toString();
        in.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_paramC.loadSettingsFrom(settings);
        m_classcol.loadSettingsFrom(settings);
        if (settings.containsKey(CFG_KERNELTYPE)) {
            m_kernelType =
                    KernelType.valueOf(settings.getString(CFG_KERNELTYPE));
        }
        for (Map.Entry<KernelType, Vector<SettingsModelDouble>>
        entry : m_kernelParameters
                .entrySet()) {
            Vector<SettingsModelDouble> kernelsettings = entry.getValue();
            for (SettingsModelDouble smd : kernelsettings) {
               try {
                    smd.loadSettingsFrom(settings);
                } catch (InvalidSettingsException ise) {
                    // it's not bad if a parameter is missing. This may be
                    // an old version, but inform the user.
                    LOGGER.warn("Did not find " + smd.toString() + " in the"
                            + " NodeSettings. Using default value instead.");
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_svms = null;
        m_svmInfo = "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File f = new File(nodeInternDir, "SVM_Internals");
        FileWriter out = new FileWriter(f);
        out.write(m_svmInfo);
        out.close();
    }

    /**
     * @return a string containing all SVM infos in HTML for the view.
     */
    String getSVMInfos() {
        if (m_svmInfo.length() > 0) {
            return m_svmInfo;
        }
        StringBuffer sb = new StringBuffer();
        if (m_svms != null) {
            sb.append("<html>\n");
            sb.append("<body>\n");
            for (int i = 0; i < m_svms.length; i++) {
                if (m_svms[i] != null) {
                    sb.append("<h1> SVM " + i + " Class: "
                            + m_svms[i].getPositive() + "</h1>");
                    sb.append("<b> Support Vectors: </b><br>");
                    DoubleVector[] supvecs = m_svms[i].getSupportVectors();
                    for (DoubleVector vec : supvecs) {
                        for (int s = 0; s < vec.getNumberValues(); s++) {
                            sb.append(vec.getValue(s) + ", ");
                        }
                        sb.append(vec.getClassValue() + "<br>");
                    }
                }
            }
            sb.append("</body>\n");
            sb.append("</html>\n");
        }
        m_svmInfo = sb.toString();
        return m_svmInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_KERNELTYPE, m_kernelType.toString());
        m_paramC.saveSettingsTo(settings);
        m_classcol.saveSettingsTo(settings);
        for (Map.Entry<KernelType, Vector<SettingsModelDouble>>
        entry : m_kernelParameters
                .entrySet()) {
            Vector<SettingsModelDouble> kernelsettings = entry.getValue();
            for (SettingsModelDouble smd : kernelsettings) {
                smd.saveSettingsTo(settings);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        if (settings.containsKey(CFG_KERNELTYPE)) {
            String tmpKernel = settings.getString(CFG_KERNELTYPE);
            boolean found = false;
            for (String kernel : KernelFactory.getKernelNames()) {
                if (tmpKernel.equals(kernel)) {
                    found = true;
                }
            }
            if (!found) {
                throw new InvalidSettingsException("Unknown kernel type: "
                        + tmpKernel);
            }

        }

        for (Map.Entry<KernelType, Vector<SettingsModelDouble>> entry
            : m_kernelParameters.entrySet()) {
            Vector<SettingsModelDouble> kernelsettings = entry.getValue();
            for (SettingsModelDouble smd : kernelsettings) {
                try {
                    smd.validateSettings(settings);
                } catch (InvalidSettingsException ise) {
                    // it's not bad if a parameter is missing. This may be
                    // an old version, but inform the user.
                    LOGGER.warn("Did not find " + smd.toString() + " in the"
                            + " NodeSettings. Using default value instead.");
                }
            }
        }

        m_paramC.validateSettings(settings);
        m_classcol.validateSettings(settings);
    }

    private static final class LearnColumnsAndColumnRearrangerTuple {

        private final ColumnRearranger m_trainingRearranger;
        private final List<DataColumnSpec> m_learningColumns;
        private final DataColumnSpec m_targetColumn;

        /** Create tuple of column rearranger for training table and
         * corresponding learning and target columns.
         * @param trainingRearranger The training table column rearranger
         * @param learningColumns The list of learning columns.
         * @param targetColumn The target column. */
        LearnColumnsAndColumnRearrangerTuple(
                final ColumnRearranger trainingRearranger,
                final List<DataColumnSpec> learningColumns,
                final DataColumnSpec targetColumn) {
            m_trainingRearranger = trainingRearranger;
            m_learningColumns = learningColumns;
            m_targetColumn = targetColumn;
        }

        /** @return the trainingRearranger */
        ColumnRearranger getTrainingRearranger() {
            return m_trainingRearranger;
        }

        /** @return the learningColumns */
        List<DataColumnSpec> getLearningColumns() {
            return m_learningColumns;
        }

        /** @return the targetColumn */
        DataColumnSpec getTargetColumn() {
            return m_targetColumn;
        }
    }

}
