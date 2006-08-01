/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultCellIterator;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;


/**
 * This class implements the DDA-algorithm published by <i>Berthold&Huber</i>
 * which iteratively introduces new basisfunctions and/or shrinks already
 * existing ones of conflicting classes during the trainings algorithm.
 * <p>
 * The learning algorithm itself is based on two distinct phases. During the
 * training phase, missclassified pattern either prompt the spontaneous creation
 * of new basisfunctions units (commitment) or the adjustment of conflicting
 * Basisfunction radii (shrinking of Basisfunctions belonging to incorrect
 * classes). To commit a new prototype, none of existing Basisfunctions of the
 * correct class has an activation above a certain threshold and, after
 * shrinking, no Basisfunction of a conflicting class is allowed to have an
 * activation above.
 * <p>
 * This underlying {@link DataTable} contains just one column derived from
 * {@link BasisFunctionLearnerRow}.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class BasisFunctionLearnerTable implements DataTable {
    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(BasisFunctionLearnerTable.class);

    /**
     * Array holding all <code>BasisFunctionLearnerRow</code> objects in an
     * <code>ArrayList</code> which can be referenced by its
     * <code>RowKey</code> info as key values.
     */
    private final Map<DataCell, List<BasisFunctionLearnerRow>> m_bfs;

    /** The underlying factory used to generate new basisfunctions. */
    private final transient BasisFunctionFactory m_factory;

    /** The basisfunction key prefix. */
    private static final String RULE_PREFIX = "Rule_";

    /** Missing replacement function. */
    private final MissingValueReplacementFunction m_missing;

    /** Count number of loops overall input pattern. */
    private int m_cycles = 0;

    /** Counts number of pattern used for training. */
    private int m_numPattern = 0;

    /**
     * Creates a new basis function learner and starts the training algorithm.
     * The given data (only double columns) is used for training. Its assigned
     * class label is used to determine the class info for each row.
     * Furthermore, we provide a name for the new model column. The factory is
     * used to automatically generate new prototypes of a certain basisfuntion
     * type.
     * 
     * @param data the trainings data from which are all {@link DoubleCell}
     *            columns are used for training and the last the specified
     *            <code>target</code> column for classification
     * @param factory the factory used to generate
     *            {@link BasisFunctionLearnerRow}s
     * @param missing the missing values replacement function
     * @param shrinkAfterCommit if <code>true</code> do it
     * @param exec the execution monitor
     * @throws CanceledExecutionException always tested when a new run over data
     *             is started.
     */
    public BasisFunctionLearnerTable(final DataTable data,
            final BasisFunctionFactory factory,
            final MissingValueReplacementFunction missing,
            final boolean shrinkAfterCommit, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        this(data, factory, missing, shrinkAfterCommit, new int[]{1}, exec);
    }

    /**
     * Creates a new basisfunction learner and starts the training algorithm.
     * The given data (only double columns) is used for training. Its assigned
     * class label is used to determine the class info for each row.
     * Furthermore, we provide a name for the new model column. The factory is
     * used to automatically generate new prototypes of a certain basisfuntion
     * type.
     * 
     * @param data The trainings data from which are all {@link DoubleCell}
     *            columns are used for training and the last the specified
     *            <code>target</code> column for classification.
     * @param factory the factory used to generate
     *            {@link BasisFunctionLearnerRow}s
     * @param missing the missing values replacement function
     * @param shrinkAfterCommit if <code>true</code> do it
     * @param startRuleCount at this point
     * @param exec the execution monitor
     * @throws CanceledExecutionException always tested when a new run over data
     *             is started
     */
    public BasisFunctionLearnerTable(final DataTable data,
            final BasisFunctionFactory factory,
            final MissingValueReplacementFunction missing,
            final boolean shrinkAfterCommit, final int[] startRuleCount,
            final ExecutionMonitor exec) throws CanceledExecutionException {
        // keep bfs by class label
        m_bfs = new LinkedHashMap<DataCell, List<BasisFunctionLearnerRow>>();
        // keep factory
        m_factory = factory;
        // keeps missing repl. function
        m_missing = missing;

        // index of the class info column which is the last one here
        int classColumn = data.getDataTableSpec().getNumColumns() - 1;
        // init array if column indices without last class column
        int[] columns = new int[classColumn];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = i;
        }

        // true if shrink or a new prototype was created otherwise false
        boolean goon = false;
        LOGGER.info("Start Learning... #rules [#epoch]");
        do { // overall input pattern ... while (goon == true)
            exec.checkCanceled();
            String progMsg = "Learning... #rules=" + getNumBasisFunctions()
                    + " at #epoch=" + (m_cycles + 1);
            exec.setProgress(Math.min(0.90, ((m_cycles + 1) * 10) / 100.0),
                    progMsg);
            goon = false; // reset flag for a new run

            /* --- R E S E T --- */

            // reset all bfs internaly
            for (BasisFunctionIterator it = getBasisFunctionIterator(); it
                    .hasNext();) {
                exec.checkCanceled();
                it.nextBasisFunction().resetIntern();
            }
            // reset trainings pattern counter
            m_numPattern = 0;

            // overall trainings rows
            for (RowIterator rowIt = data.iterator(); rowIt.hasNext(); m_numPattern++) {
                exec.checkCanceled();
                // current data row
                final DataRow oRow = rowIt.next();
                progMsg = "Learning... #rules=" + getNumBasisFunctions()
                        + " at #epoch=" + (m_cycles + 1);
                exec.setMessage(progMsg + " \"" + oRow.getKey().getId() + "\"");
                final FilteredClassRow row = new FilteredClassRow(oRow, missing);
                // get current class label of current bf
                final DataCell classInfo = row.getClassInfo();

                /* --- C O V E R S --- */

                // find best covering bf of correct class, if exist
                BasisFunctionLearnerRow bestBF = null;
                // overall bfs within the model
                for (BasisFunctionIterator it = getBasisFunctionIterator(); it
                        .hasNext();) {
                    // get current bf
                    final BasisFunctionLearnerRow currentBF = it
                            .nextBasisFunction();
                    // check if class indices match
                    if (currentBF.getClassLabel().equals(classInfo)) {
                        // if pattern covered
                        if (currentBF.covers(row)) {
                            // null?; first one
                            if (bestBF == null) {
                                // init with first one
                                bestBF = currentBF; // first one that covers
                            } else if (currentBF.compareCoverage(bestBF, row)) {
                                // otherwise compare coverage with best one
                                assert (bestBF != currentBF);
                                bestBF = currentBF;
                            }
                        }
                    } else { // skip current class
                        it.skipClass();
                    }
                }

                // we didn't find any covering prototype
                if (bestBF == null) {

                    /* --- C O M M I T --- */

                    String bfRowPrefix = RULE_PREFIX + (startRuleCount[0]++);
                    // (level >= 0 ? RULE_PREFIX + (level + 1) + "_" +
                    // (bfKEY++):
                    // new bf with initial vector and data key
                    BasisFunctionLearnerRow newBF = factory.commit(new RowKey(
                            bfRowPrefix), classInfo, row);
                    // add new prototype to the collection
                    addBasisFunction(newBF);

                    /* --- S H R I N K --- */

                    // --- shrink new bf to all other conflicting ones
                    // pro: increases the number trainings epochs
                    // con: decreases final number of bf
                    if (shrinkAfterCommit) {
                        // overall basefunctions within the model
                        for (BasisFunctionIterator it = getBasisFunctionIterator(); it
                                .hasNext();) {
                            BasisFunctionLearnerRow bf = it.nextBasisFunction();
                            // if class indices don't match
                            if (!bf.getClassLabel().equals(classInfo)) {
                                // shrinks new bf on current bf, true if changed
                                newBF.shrink(bf.getAnchor());
                            } else { // skip bfs of current class
                                it.skipClass();
                            }
                        }
                    }
                    // after commit all other conflicting bfs have to be
                    // adjusted in a new run overall input pattern
                    goon = true;
                } else {
                    /* --- C O V E R --- */

                    // best pattern covers current input pattern
                    bestBF.coverIntern(row);
                }

                /* --- S H R I N K --- */

                // overall basefunctions of conflicing classes
                for (BasisFunctionIterator it = getBasisFunctionIterator(); it
                        .hasNext();) {
                    // get current basisfunction
                    final BasisFunctionLearnerRow bf = it.nextBasisFunction();
                    // if class indices don't match
                    if (!bf.getClassLabel().equals(classInfo)) {
                        // shrink the bf on the current input pattern
                        goon |= bf.shrink(row); // true if changed
                    } else {
                        // skip current class
                        it.skipClass();
                    }
                }
            }
            // increase loop counter
            m_cycles++;
            LOGGER.debug(getNumBasisFunctions() + " [" + m_cycles + "]");
        } while (goon); // true, if commit or shrink was performed
        /* --- P R U N E --- */
        this.prune(0, m_cycles, m_numPattern); // prune bfs with zero coverage
        /* -- E X P L A I N S --- */
        this.explain(data);
    }

    /**
     * Assignes all explained examples to to basis functions.
     * 
     * @param data the data to explain
     */
    public final void explain(final DataTable data) {
        // overall training rows
        for (RowIterator rowIt = data.iterator(); rowIt.hasNext();) {
            final FilteredClassRow row = new FilteredClassRow(rowIt.next(),
                    m_missing);
            // overall Basisfunctions in the model
            for (BasisFunctionIterator it = getBasisFunctionIterator(); it
                    .hasNext();) {
                // get current Basisfunction
                BasisFunctionLearnerRow bf = it.nextBasisFunction();
                // if row is explained
                if (bf.explains(row)) {
                    // keep key
                    bf.addCovered(row.getKey().getId(), row.getClassInfo());
                }
            }
        }
    }

    /**
     * Inner class to separate an data input row into a new row which are the
     * first n-1 double cells and returns the class label.
     */
    final class FilteredClassRow implements DataRow {
        private final DataCell[] m_data;

        private final DataCell m_class;

        private final RowKey m_key;

        /**
         * @param row the row to filter in data and class label
         * @param missing the missing value replacement function
         */
        FilteredClassRow(final DataRow row,
                final MissingValueReplacementFunction missing) {
            m_key = row.getKey();
            m_data = new DataCell[row.getNumCells() - 1]; // exclude class
            for (int i = 0; i < m_data.length; i++) {
                m_data[i] = row.getCell(i);
            }
            m_class = row.getCell(row.getNumCells() - 1); // last is class
            // replace missing values
            DataCell[] newCells = new DataCell[m_data.length];
            for (int i = 0; i < newCells.length; i++) {
                if (m_data[i].isMissing()) {
                    newCells[i] = missing.getMissing(this, i,
                            BasisFunctionLearnerTable.this);
                } else {
                    newCells[i] = m_data[i];
                }
            }
            // copy everything back to the data array
            for (int i = 0; i < m_data.length; i++) {
                m_data[i] = newCells[i];
            }
        }

        /** @see org.knime.core.data.DataRow#getNumCells() */
        public int getNumCells() {
            return m_data.length;
        }

        /** @see org.knime.core.data.DataRow#getKey() */
        public RowKey getKey() {
            return m_key;
        }

        /** @see org.knime.core.data.DataRow#getCell(int) */
        public DataCell getCell(final int index) {
            assert (index >= 0 && index < getNumCells());
            return m_data[index];
        }

        /** @return The last column of the internal row. */
        DataCell getClassInfo() {
            return m_class;
        }

        /**
         * @see java.lang.Iterable#iterator()
         */
        public Iterator<DataCell> iterator() {
            return new DefaultCellIterator(this);
        }
    } // FilteredClassRow

    /**
     * Keeps the first available
     * {@link org.knime.core.data.property.ColorAttr} for given class info
     * {@link DataCell}.
     */
    /**
     * Prunes basis functions which are below the given threshold <code>t</code>.
     * 
     * @param t the threshold below all basisfunction are removed from this
     *            model.
     * @param cycles Print info of m_cycles
     * @param numPattern print info of number pattern
     */
    private void prune(final int t, final int cycles, final int numPattern) {
        // keeps number of Basisfunctions to determine number of prunded ones
        int oldNumBFs = getNumBasisFunctions();
        // list of Basisfunctions to remove from the model
        ArrayList<BasisFunctionLearnerRow> removeBFs = new ArrayList<BasisFunctionLearnerRow>();
        // overall Basisfunctions in the model
        for (BasisFunctionIterator it = getBasisFunctionIterator(); it
                .hasNext();) {
            // get current Basisfunction
            BasisFunctionLearnerRow bf = it.nextBasisFunction();
            // if no pattern is covered
            if (bf.getPredictorRow().getNumAllCoveredPattern() <= t) {
                // add Basisfunction to the list of remove ones.
                removeBFs.add(bf);
            }
        }
        // overall Basisfunctions to remove from the list
        for (BasisFunctionLearnerRow remRow : removeBFs) {
            // remove Basisfunction i
            removeBasisFunction(remRow);
        }
        // print model info
        LOGGER.debug("#rules  =" + getNumBasisFunctions());
        LOGGER.debug("#pruned =" + (oldNumBFs - getNumBasisFunctions()));
        LOGGER.debug("#cycles =" + cycles);
        LOGGER.debug("#pattern=" + numPattern);
    }

    /**
     * @return the model's basisfunction factory
     */
    BasisFunctionFactory getFactory() {
        return m_factory;
    }

    /**
     * Adds the given basis function to the list using its nominal value for
     * class assignment.
     * 
     * @param bf the basis function to add
     * 
     * @see #removeBasisFunction(BasisFunctionLearnerRow)
     */
    public void addBasisFunction(final BasisFunctionLearnerRow bf) {
        // get the bf's class info
        DataCell classInfo = bf.getClassLabel();
        // get list of Basisfunctions for current class
        List<BasisFunctionLearnerRow> list = m_bfs.get(classInfo);
        // if not available
        if (list == null) {
            // create and add new one
            list = new ArrayList<BasisFunctionLearnerRow>();
            // put current bf to the array list
            if (m_bfs.put(classInfo, list) != null) {
                assert (false);
            }
            assert (m_bfs.containsKey(classInfo) && m_bfs.containsValue(list));
        }

        // add new list for bf
        list.add(bf);
        assert (list.contains(bf));
    }

    /**
     * Removes the given basisfunction from the model and updates all internal
     * members.
     * 
     * @param bf the basis function to remove.
     * 
     * @see #addBasisFunction(BasisFunctionLearnerRow)
     */
    public void removeBasisFunction(final BasisFunctionLearnerRow bf) {
        // get Basisfunction's nomina class info
        DataCell classInfo = bf.getClassLabel();
        // get array list for current class
        List list = m_bfs.get(classInfo);
        // if not available
        if (list == null) {
            assert (false);
        }

        // remove bf from its list
        list.remove(bf);
        // if list empty remove it
        if (list.isEmpty()) {
            m_bfs.remove(classInfo);
        }
    }

    /**
     * @return number of trained basis functions
     */
    public int getRowCount() {
        return getNumBasisFunctions();
    }

    /**
     * Returns the overall number of Basisfunction in this model.
     * 
     * @return the number of basis functions
     * 
     * @see #getNumBasisFunctions(DataCell)
     */
    public final int getNumBasisFunctions() {
        int n = 0;
        for (DataCell c : m_bfs.keySet()) {
            List<BasisFunctionLearnerRow> l = m_bfs.get(c);
            n += l.size();
        }
        return n;
    }

    /**
     * @return the number of different classes
     */
    protected int getNumClasses() {
        return m_bfs.size();
    }

    /**
     * Returns the number of basis functions for the given class.
     * 
     * @param classInfo the class to get the number of basisfunctions for
     * @return the number of basisfunctions for the given class or 0 if no
     *         Basisfunction is available for the given class or the class is
     *         not in the list
     * @throws NullPointerException if the class label is <code>null</code>
     * 
     * @see #getNumBasisFunctions()
     */
    final int getNumBasisFunctions(final DataCell classInfo) {
        if (classInfo == null) {
            throw new NullPointerException();
        }
        List<BasisFunctionLearnerRow> o = m_bfs.get(classInfo);
        if (o == null) {
            return 0;
        }
        return o.size();
    }

    /**
     * @see org.knime.core.data.DataTable#iterator()
     */
    public RowIterator iterator() {
        return getBasisFunctionIterator();
    }

    /**
     * @see org.knime.core.data.DataTable#getDataTableSpec()
     */
    public DataTableSpec getDataTableSpec() {
        return getFactory().getModelSpec();
    }

    /**
     * Returns the map of basis functions list for each class. The key is the
     * {@link DataCell} info and the value a list of basisfunctions.
     * 
     * @return the key to list of basis functions' map
     */
    final Map<DataCell, List<BasisFunctionLearnerRow>> getBasisFunctions() {
        return m_bfs;
    }

    /**
     * @return an array holding the number of basis functions for each class
     */
    final int[] getClassDistribution() {
        int[] ret = new int[m_bfs.size()];
        int idx = 0;
        for (Iterator<DataCell> it = m_bfs.keySet().iterator(); it.hasNext(); idx++) {
            ret[idx] = getNumBasisFunctions(it.next());
            assert (ret[idx] > 0);
        }
        return ret;
    }

    /**
     * @return a new iterator to get all basis functions from this model
     */
    public final BasisFunctionIterator getBasisFunctionIterator() {
        return new BasisFunctionIterator(this);
    }

    /**
     * Adds info about this object to the given stream.
     * 
     * @param out the stream to add info to
     * @throws NullPointerException if the given stream is <code>null</code>
     */
    public void print(final PrintStream out) {
        StringBuffer buf = new StringBuffer();
        this.write(buf, true);
        out.println(buf.toString().getBytes());
    }

    /**
     * Saves the results of the training to the given object as string key-value
     * pairs.
     * 
     * @param pp the object to write result strings to
     */
    public void saveInfos(final ModelContentWO pp) {
        ModelContentWO statisticsContent = pp.addModelContent("learner_info");
        statisticsContent.addString("Number of rules learned: ", ""
                + getNumBasisFunctions());
        statisticsContent.addString("Number of epochs: ", "" + m_cycles);
        statisticsContent.addString("Number of training instances: ", ""
                + m_numPattern);
        // basisfunctions per class
        statisticsContent.addString("Number of classes: ", ""
                + m_bfs.keySet().size());

        ModelContentWO classContent = pp.addModelContent("class_info");
        for (DataCell classInfo : m_bfs.keySet()) {
            classContent.addString(classInfo.toString() + ": ", ""
                    + getNumBasisFunctions(classInfo));
        }
        // save model spec
        DataTableSpec modelSpec = m_factory.getModelSpec();
        ModelContentWO specContent = pp.addModelContent("column_info");
        specContent.addString("Number of columns: ", ""
                + modelSpec.getNumColumns());
        for (int i = 0; i < modelSpec.getNumColumns(); i++) {
            DataColumnSpec spec = modelSpec.getColumnSpec(i);
            specContent.addString(spec.getName() + ": ", spec.getType()
                    .toString());
        }
    }

    /**
     * Write this model into the given string buffer.
     * 
     * @param buf the buffer to write into
     * @param full write full description including the entire model
     */
    public void write(final StringBuffer buf, final boolean full) {
        // print number of basisfunctions
        buf.append("###\n");
        buf.append(getNumBasisFunctions() + " rules learned\n");
        buf.append("  cycles =" + m_cycles + "\n");
        buf.append("  classes=" + m_bfs.keySet().size() + "\n");
        buf.append("  pattern=" + m_numPattern + "\n");
        buf.append("###\n");
        // basisfunctions per class
        for (DataCell classInfo : m_bfs.keySet()) {
            buf.append(getNumBasisFunctions(classInfo) + " for "
                    + classInfo.toString() + "\n");
        }
        buf.append("###\n");
        // print column names
        DataTableSpec spec = m_factory.getModelSpec();
        for (DataColumnSpec cspec : spec) {
            buf.append(cspec.getName() + " ");
        }
        buf.append("\n###\n");
        if (full) {
            // print info about alle Basisfunctions
            for (BasisFunctionIterator it = getBasisFunctionIterator(); it
                    .hasNext();) {
                // get current Basisfunction
                BasisFunctionLearnerRow bf = it.nextBasisFunction();
                // add Basisfunction specific info
                buf.append(bf.getKey().getId() + ": " + bf.toString() + "\n");
            }
        }
    }

    /**
     * @return the hilite mapper which maps rules to covered instances
     */
    public DefaultHiLiteMapper getHiLiteMapper() {
        Map<DataCell, Set<DataCell>> map = new LinkedHashMap<DataCell, Set<DataCell>>();
        for (BasisFunctionIterator i = getBasisFunctionIterator(); i.hasNext();) {
            BasisFunctionLearnerRow bf = i.nextBasisFunction();
            map.put(bf.getKey().getId(), bf.getAllCoveredPattern());
        }
        return new DefaultHiLiteMapper(map);
    }

    /** NodeSettings key for the missing replacement value. */
    public static final String MISSING = "missing";

    /**
     * A list of possible missing value replacements. In addition, the so called
     * BEST GUESS and INCORP methods can be applied.
     */
    public static final MissingValueReplacementFunction[] MISSINGS = {
            new IncorpMissingValueReplacementFunction(),
            new BestGuessMissingValueReplacementFunction(),
            new MeanMissingValueReplacementFunction(),
            new MinimumMissingValueReplacementFunction(),
            new MaximumMissingValueReplacementFunction(),
            new ZeroMissingValueReplacementFunction(),
            new OneMissingValueReplacementFunction()};

    /**
     * General missing values replacement interface.
     */
    public interface MissingValueReplacementFunction {
        /**
         * This function returns the missing replacement value for a given
         * value.
         * 
         * @param row the row te replace the missing value in
         * @param col the column index
         * @param model this basis function model
         * @return the missing replacement value
         */
        DataCell getMissing(final FilteredClassRow row, final int col,
                final BasisFunctionLearnerTable model);
    }
}

/**
 * Makes use the missing value by using it inside the model. The missing value
 * will be replaced as soon as real value(s) are available from the trinaings
 * data.
 */
final class IncorpMissingValueReplacementFunction implements
        BasisFunctionLearnerTable.MissingValueReplacementFunction {
    /**
     * @see #getMissing(
     *      BasisFunctionLearnerTable.FilteredClassRow,int,BasisFunctionLearnerTable)
     */
    public DataCell getMissing(
            final BasisFunctionLearnerTable.FilteredClassRow row,
            final int col, final BasisFunctionLearnerTable model) {
        return row.getCell(col);
    }

    /**
     * @return <i>Incorp</i>
     */
    @Override
    public String toString() {
        return "Incorp";
    }
}

/**
 * "Best Guess" replacement which searches for the best value in the model or
 * just takes the mean if not available.
 */
final class BestGuessMissingValueReplacementFunction implements
        BasisFunctionLearnerTable.MissingValueReplacementFunction {
    /**
     * @see #getMissing(
     *      BasisFunctionLearnerTable.FilteredClassRow,int,BasisFunctionLearnerTable)
     */
    public DataCell getMissing(
            final BasisFunctionLearnerTable.FilteredClassRow row,
            final int col, final BasisFunctionLearnerTable model) {
        BasisFunctionLearnerRow best = null;
        for (BasisFunctionIterator i = model.getBasisFunctionIterator(); i
                .hasNext();) {
            BasisFunctionLearnerRow bf = i.nextBasisFunction();
            // check if class indices match
            if (bf.getClassLabel().equals(row.getClassInfo())) {
                // if pattern covered
                if (bf.covers(row)) {
                    // null?; first one
                    if (best == null) {
                        // init with first one
                        best = bf; // first one that covers
                    } else if (bf.compareCoverage(best, row)) {
                        // otherwise compare coverage with best one
                        assert (best != bf);
                        best = bf;
                    }
                }
            } else { // skip current class
                i.skipClass();
            }
        }
        if (best == null) {
            double min = model.getFactory().getMins()[col];
            double max = model.getFactory().getMaxs()[col];
            // no bf yet available, use mean of column
            return new DoubleCell((max - min) / 2);
        } else {
            // request missing replacement "best guess" at the best bf
            return new DoubleCell(best.getMissingValue(col).getDoubleValue());
        }
    }

    /**
     * @return <i>Best Guess</i>
     */
    @Override
    public String toString() {
        return "Best Guess";
    }
}

/**
 * Minimum replacement.
 */
final class MinimumMissingValueReplacementFunction implements
        BasisFunctionLearnerTable.MissingValueReplacementFunction {
    /**
     * @see #getMissing(
     *      BasisFunctionLearnerTable.FilteredClassRow,int,BasisFunctionLearnerTable)
     */
    public DataCell getMissing(
            final BasisFunctionLearnerTable.FilteredClassRow row,
            final int col, final BasisFunctionLearnerTable model) {
        return new DoubleCell(model.getFactory().getMins()[col]);
    }

    /**
     * @return <i>Min</i>
     */
    @Override
    public String toString() {
        return "Min";
    }
}

/**
 * Maximum replacement.
 */
final class MaximumMissingValueReplacementFunction implements
        BasisFunctionLearnerTable.MissingValueReplacementFunction {
    /**
     * @see #getMissing(
     *      BasisFunctionLearnerTable.FilteredClassRow,int,BasisFunctionLearnerTable)
     */
    public DataCell getMissing(
            final BasisFunctionLearnerTable.FilteredClassRow row,
            final int col, final BasisFunctionLearnerTable model) {
        return new DoubleCell(model.getFactory().getMaxs()[col]);
    }

    /**
     * @return <i>Max</i>
     */
    @Override
    public String toString() {
        return "Max";
    }
}

/**
 * Mean replacement.
 */
final class MeanMissingValueReplacementFunction implements
        BasisFunctionLearnerTable.MissingValueReplacementFunction {
    /**
     * @see #getMissing(
     *      BasisFunctionLearnerTable.FilteredClassRow,int,BasisFunctionLearnerTable)
     */
    public DataCell getMissing(
            final BasisFunctionLearnerTable.FilteredClassRow row,
            final int col, final BasisFunctionLearnerTable model) {
        double min = model.getFactory().getMins()[col];
        double max = model.getFactory().getMaxs()[col];
        return new DoubleCell((max + min) / 2);
    }

    /**
     * @return <i>Mean</i>
     */
    @Override
    public String toString() {
        return "Mean";
    }
}

/**
 * Zero replacement.
 */
final class ZeroMissingValueReplacementFunction implements
        BasisFunctionLearnerTable.MissingValueReplacementFunction {

    /** static zero replacement value. */
    static final DataCell ZERO = new DoubleCell(0.0);

    /**
     * @see #getMissing(
     *      BasisFunctionLearnerTable.FilteredClassRow,int,BasisFunctionLearnerTable)
     */
    public DataCell getMissing(
            final BasisFunctionLearnerTable.FilteredClassRow row,
            final int col, final BasisFunctionLearnerTable model) {
        return ZERO;
    }

    /**
     * @return <i>Zero</i>
     */
    @Override
    public String toString() {
        return "Zero";
    }
}

/**
 * One replacement.
 */
final class OneMissingValueReplacementFunction implements
        BasisFunctionLearnerTable.MissingValueReplacementFunction {

    /** static one replacement value. */
    static final DataCell ONE = new DoubleCell(1.0);

    /**
     * @see #getMissing(
     *      BasisFunctionLearnerTable.FilteredClassRow,int,BasisFunctionLearnerTable)
     */
    public DataCell getMissing(
            final BasisFunctionLearnerTable.FilteredClassRow row,
            final int col, final BasisFunctionLearnerTable model) {
        return ONE;
    }

    /**
     * @return <i>One</i>
     */
    @Override
    public String toString() {
        return "One";
    }
}
