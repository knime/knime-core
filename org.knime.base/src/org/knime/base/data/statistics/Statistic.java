package org.knime.base.data.statistics;

import static org.knime.core.node.util.CheckUtils.checkArgument;
import static org.knime.core.node.util.CheckUtils.checkArgumentNotNull;
import static org.knime.core.node.util.CheckUtils.checkNotNull;
import static org.knime.core.node.util.CheckUtils.checkSetting;
import static org.knime.core.node.util.CheckUtils.checkSettingNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.ConvenienceMethods;

/**
 *
 * Base class for statistics used in the {@link StatisticCalculator}. A statistic here is a simple function which maps
 * a set of DataCells (given by a column) to something arbitrary. The set of columns is defined in the constructor. An
 * instance is intended to be used exactly once in combination with the {@link StatisticCalculator}. The statistics
 * resources, like result arrays, should be allocated during the {@link #init(DataTableSpec, int)} method, as it
 * delivers the amount of columns, which depends not only on the constructor argument but also on the default columns
 * which are not explicitly given in the constructor. If the statistic needs the total amount of rows the
 * {@link #beforeEvaluation(int)} hook can be used. Afterwards the statistic is called with each row -
 * {@link #consumeRow(DataRow)} - the implementation finds the configured indices for computation the
 * {@link #getIndices()} method. {@link #afterEvaluation()} is called at the end of the data table and is intended to
 * provide warning messages.
 *
 *
 * @author Marcel Hanser
 * @since 2.11
 */
public abstract class Statistic {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(Statistic.class);

    private String[] m_columns;

    private int[] m_indices;

    private Map<String, Integer> m_stringToIndexMap;

    private final Class<? extends DataValue>[] m_clazz;

    private State m_state = State.CREATED;

    /**
     * @param clazz DataValue implementations for which this statistic is defined
     * @param columns the columns for which this statistic is defined. If it is empty the default columns given in the
     *            {@link StatisticCalculator#StatisticCalculator(DataTableSpec, String[], Statistic...)} which are
     *            compatible to the clazz attribute are used
     */
    @SafeVarargs
    public Statistic(final Class<? extends DataValue>[] clazz, final String... columns) {
        checkArgument(!ArrayUtils.contains(clazz, null), "Value classes cannot contain null values.");
        m_clazz = checkNotNull(clazz);
        m_columns = checkNotNull(columns);
    }

    /**
     *
     * @param clazz DataValue implementation for which this statistic is defined
     * @param columns the columns for which this statistic is defined
     */
    @SuppressWarnings("unchecked")
    public Statistic(final Class<? extends DataValue> clazz, final String... columns) {
        this(new Class[]{clazz}, columns);
    }

    /**
     * @param columnName the column name to check
     * @return <code>true</code> if the statistic may contain results for this column
     */
    public final boolean containsColumn(final String columnName) {
        checkState(State.INITIALIZED);
        return m_stringToIndexMap.containsKey(columnName);
    }

    /**
     * @param spec the datatable spec of the data table to compute stats on.
     * @param amountOfColumns the amount of columns, of the set columns or of the default columns.
     */
    protected abstract void init(final DataTableSpec spec, final int amountOfColumns);

    /**
     * @param amountOfRows the amount of data points
     * @deprecated use {@link #beforeEvaluation(long)} instead
     */
    @Deprecated
    protected void beforeEvaluation(final int amountOfRows) {

    }

    /**
     * @param amountOfRows the amount of data points
     * @since 3.0
     */
    protected void beforeEvaluation(final long amountOfRows) {
        beforeEvaluation(ConvenienceMethods.checkTableSize(amountOfRows));
    }

    /**
     * Called for each row in the data table. Implementations should loop over the {@link #getIndices()} array, to
     * receive the DataCells of interest. Be aware that the data cell could also be the missing cell. An example is
     * shown below.
     *
     * <pre>
     * protected void consumeRow(final DataRow dataRow) {
     *     for (int i : getIndices()) {
     *         DataCell cell = dataRow.getCell(i);
     *         //... do computation
     *     }
     * }
     * </pre>
     *
     * @param dataRow the data points
     */
    protected abstract void consumeRow(DataRow dataRow);

    /**
     * @return a warning string or <code>null</code>
     */
    protected String afterEvaluation() {
        return null;
    }

    /**
     * @param columnName the column name
     * @return the
     */
    protected int getIndexForColumn(final String columnName) {
        checkState(State.INITIALIZED);
        Integer index = m_stringToIndexMap.get(columnName);
        return index == null ? -1 : index;
    }

    /**
     * @return a copy of the columns for which this statistic is defined
     */
    protected final String[] getColumns() {
        checkState(State.INITIALIZED);
        return m_columns;
    }

    /**
     * @return the indices for which this statistic is defined
     */
    protected final int[] getIndices() {
        checkState(State.INITIALIZED);
        return m_indices;
    }

    /**
     * Convenience method for subclasses, which returns the index of the column and checks if the column name was
     * configured.
     *
     * @param columnName the column
     * @return the index of the column
     */
    protected int assertIndexForColumn(final String columnName) {
        checkState(State.EVALUATED);
        return checkArgumentNotNull(m_stringToIndexMap.get(columnName),
            "Column: '%s' was not included in the statistic calculation: '%s'", columnName, getClass().getSimpleName());
    }

    /**
     * @param spec the data table spec
     * @param defaultColumns the default columns
     * @throws InvalidSettingsException if the column settings are invalid
     */
    final void init(final DataTableSpec spec, final String... defaultColumns) throws InvalidSettingsException {
        String[] columns = m_columns;
        checkState(State.CREATED);
        checkSetting(!ArrayUtils.contains(columns, null), "Columns cannot contain null values.");
        checkSetting(!ArrayUtils.contains(defaultColumns, null), "Default columns cannot contain null values.");

        // add the default columns
        if (!ArrayUtils.isEmpty(columns)) {
            for (String col : columns) {
                DataColumnSpec columnSpec = spec.getColumnSpec(col);
                checkSettingNotNull(columnSpec, "Column '%s' not contained in input table.", col);
                checkSetting(isCompatibleWithAny(spec, m_clazz), "Column '%s' not compatible with any of '%s'.", col,
                    Arrays.toString(getSimpleNames(m_clazz)));
            }
            m_columns = columns;
        } else {
            m_columns = findAllColumnsCompatibleWithAny(//
                FilterColumnTable.createFilterTableSpec(spec, defaultColumns), m_clazz);
        }

        checkSetting(ArrayUtils.isNotEmpty(m_columns), "No Columns set or no compatible column exist in input table.");
        m_indices = findIndices(spec, m_columns);
        m_stringToIndexMap = new HashMap<String, Integer>();

        m_state = State.INITIALIZED;
        int index = 0;
        for (String col : m_columns) {
            m_stringToIndexMap.put(col, index++);
        }
        init(spec, m_columns.length);
    }

    /**
     * Called after the evaluation.
     *
     * @return a warning string or <code>null</code>
     */
    final String finish() {
        m_state = State.EVALUATED;
        return afterEvaluation();
    }

    private void checkState(final State currState) {
        if (!m_state.equals(currState)) {
            String msg = String.format("Inproper usage: State is %s but should be %s - ", m_state, currState);
            LOGGER.coding(msg);
            throw new IllegalStateException(msg);
        }
    }

    @SafeVarargs
    private static String[] findAllColumnsCompatibleWithAny(final DataTableSpec spec,
        final Class<? extends DataValue>... clazzes) {

        List<String> toReturn = new ArrayList<>();
        for (DataColumnSpec cSpec : spec) {
            for (Class<? extends DataValue> clazz : clazzes) {
                if (cSpec.getType().isCompatible(clazz)) {
                    toReturn.add(cSpec.getName());
                    break;
                }
            }
        }
        return toReturn.toArray(new String[toReturn.size()]);
    }

    @SafeVarargs
    private static boolean isCompatibleWithAny(final DataTableSpec spec, final Class<? extends DataValue>... clazzes) {
        for (DataColumnSpec cSpec : spec) {
            for (Class<? extends DataValue> clazz : clazzes) {
                if (cSpec.getType().isCompatible(clazz)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param clazz
     * @return
     */
    @SafeVarargs
    private static String[] getSimpleNames(final Class<? extends DataValue>... clazzes) {
        List<String> toReturn = new ArrayList<>();
        for (Class<? extends DataValue> clazz : clazzes) {
            toReturn.add(clazz.getSimpleName());
        }
        return toReturn.toArray(new String[toReturn.size()]);
    }

    private static int[] findIndices(final DataTableSpec spec, final String[] columns) {
        int[] toReturn = new int[columns.length];
        int i = 0;
        for (String col : columns) {
            toReturn[i++] = spec.findColumnIndex(col);
        }
        return toReturn;
    }

    private enum State {
        CREATED, INITIALIZED, EVALUATED;
    }
}