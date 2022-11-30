<!-- eslint-disable max-lines -->
<script>
import Vue from 'vue';
import { JsonDataService, SelectionService } from '@knime/ui-extension-service';
import { TableUI } from '@knime/knime-ui-table';
import { createDefaultFilterConfig, arrayEquals, isImage } from '@/utils/tableViewUtils';
import throttle from 'raf-throttle';
import { MIN_COLUMN_SIZE, SPECIAL_COLUMNS_SIZE, DATA_COLUMNS_MARGIN } from '@knime/knime-ui-table/util/constants';

const INDEX_SYMBOL = Symbol('Index');
const ROW_KEY_SYMBOL = Symbol('RowID');
// -1 is the backend representation (columnName) for sorting the table by rowKeys
const ROW_KEYS_SORT_COL_NAME = '-1';
const MIN_SCOPE_SIZE = 200;
const MIN_BUFFER_SIZE = 50;

export default {
    components: {
        TableUI
    },
    inject: ['getKnimeService'],
    data() {
        return {
            dataLoaded: false,
            currentIndex: 0,
            currentPage: 1,
            currentScopeStartIndex: null,
            currentScopeEndIndex: null,
            lastRequestScopeStartIndex: null,
            columnSortIndex: null,
            columnSortDirection: null,
            columnSortColumnName: null,
            currentSelection: null,
            currentBottomSelection: null,
            totalSelected: 0,
            table: {},
            dataTypes: {},
            columnDomainValues: {},
            currentSelectedRowKeys: new Set(),
            totalRowCount: 0,
            currentRowCount: 0,
            settings: {},
            displayedColumns: [],
            jsonDataService: null,
            selectionService: null,
            searchTerm: '',
            columnFilters: [],
            columnIndexMap: null,
            baseUrl: null,
            clientWidth: 0,
            columnSizeOverrides: {},
            scopeSize: MIN_SCOPE_SIZE,
            bufferSize: MIN_BUFFER_SIZE,
            numRowsAbove: 0,
            numRowsBelow: 0,
            maxNumRows: 200000,
            bottomRows: []
        };
    },
    computed: {
        knimeService() {
            return this.getKnimeService();
        },
        specContainsImages() {
            return this.table.columnContentTypes.some(contentType => isImage(contentType));
        },
        dataConfig() {
            const { showRowKeys, showRowIndices, compactMode } = this.settings;
            let columnConfigs = [];
            if (showRowIndices) {
                columnConfigs.push(this.createColumnConfig({ index: 0, columnName: 'Index', isSortable: false }));
            }
            if (showRowKeys) {
                columnConfigs.push(this.createColumnConfig({ index: 1, columnName: 'RowID', isSortable: true }));
            }
            this.displayedColumns.forEach((columnName, index) => {
                // + 2: offset for the index and rowKey, because the first column
                // (index 0) always contains the indices and the second one the row keys
                const { showColumnDataType } = this.settings;
                const columnInformation = {
                    index: index + 2,
                    columnName,
                    contentType: this.table.columnContentTypes?.[index],
                    ...showColumnDataType && {
                        columnTypeName: this.dataTypes[this.table.columnDataTypeIds?.[index]]?.name
                    },
                    isSortable: true
                };
                columnConfigs.push(this.createColumnConfig(columnInformation));
            });
            this.updateColumnIndexMap(columnConfigs);
            return {
                columnConfigs,
                rowConfig: { ...this.specContainsImages && { rowHeight: 80 }, compactMode }
            };
        },
        tableConfig() {
            const { enableSortingByHeader, enableGlobalSearch, enableColumnSearch,
                publishSelection, subscribeToSelection, pageSize, enablePagination } = this.settings;
            return {
                subMenuItems: false,
                showSelection: publishSelection || subscribeToSelection,
                showColumnFilters: enableColumnSearch,
                pageConfig: {
                    currentSize: this.currentRowCount,
                    tableSize: this.totalRowCount,
                    pageSize: enablePagination ? pageSize : this.currentRowCount,
                    currentPage: this.currentPage,
                    columnCount: this.displayedColumns.length
                },
                enableVirtualScrolling: true,
                fitToContainer: true,
                ...enableSortingByHeader && {
                    sortConfig: {
                        sortColumn: this.columnSortIndex,
                        sortDirection: this.columnSortDirection
                    }
                },
                ...enableGlobalSearch && {
                    searchConfig: {
                        searchQuery: ''
                    }
                }
            };
        },
        numberOfDisplayedIdColumns() {
            let offset = this.settings.showRowKeys ? 1 : 0;
            offset += this.settings.showRowIndices ? 1 : 0;
            return offset;
        },
        numberOfDisplayedColumns() {
            return this.displayedColumns.length + this.numberOfDisplayedIdColumns;
        },
        numberOfUsedColumns() {
            // The columns sent to the TableUI. The rowIndex and rowKey are included but might not be displayed.
            return this.displayedColumns.length + 2;
        },
        columnSizes() {
            const nColumns = this.numberOfDisplayedColumns;
            if (nColumns < 1) {
                return [];
            }

            const specialColumnsSizeTotal = (this.settings.enableColumnSearch ? SPECIAL_COLUMNS_SIZE : 0) +
                (this.settings.publishSelection || this.settings.subscribeToSelection ? SPECIAL_COLUMNS_SIZE : 0);
            const dataColumnsSizeTotal = this.clientWidth - specialColumnsSizeTotal - nColumns * DATA_COLUMNS_MARGIN;
            const defaultColumnSize = Math.max(MIN_COLUMN_SIZE, dataColumnsSizeTotal / nColumns);

            const currentColumnSizes = this.displayedColumns.reduce((columnSizes, columnName) => {
                columnSizes.push(this.columnSizeOverrides[columnName] || defaultColumnSize);
                return columnSizes;
            }, [this.columnSizeOverrides[INDEX_SYMBOL] || defaultColumnSize,
                this.columnSizeOverrides[ROW_KEY_SYMBOL] || defaultColumnSize]);
            const lastColumnMinSize = this.lastColumnMinSize(dataColumnsSizeTotal, currentColumnSizes);
            currentColumnSizes[currentColumnSizes.length - 1] = Math.max(lastColumnMinSize,
                currentColumnSizes[currentColumnSizes.length - 1]);
            return currentColumnSizes;
        },
        rowData() {
            return this.table.rows.map((row, index) => [index + this.numRowsAbove, ...row]);
        },
        bottomRowData() {
            if (typeof this.bottomRows === 'undefined') {
                return [];
            }
            const bottomRowsLength = this.bottomRows.length;
            const bottomRowsEnd = this.currentPageEnd - this.numRowsBelow;
            return this.bottomRows.map((row, index) => [index + bottomRowsEnd - bottomRowsLength, ...row]);
        },
        columnFilterValues() {
            const columnFilterValues = [];
            this.columnFilters.forEach((filter, index) => {
                // filter out empty dummy filter of rowIndices
                if (index === 0) {
                    return;
                }
                if (typeof filter.value === 'string') {
                    columnFilterValues.push([filter.value]);
                } else {
                    columnFilterValues.push(filter.value);
                }
            });
            return columnFilterValues;
        },
        colFilterActive() {
            return this.columnFilterValues.some(val => val.length && val[0] !== '');
        },
        colSortActive() {
            return this.columnSortColumnName !== null && this.columnSortIndex !== null;
        },
        showTitle() {
            return this.settings.showTitle;
        },
        useLazyLoading() {
            return !this.settings.enablePagination;
        },
        currentPageStart() {
            return this.settings.enablePagination ? this.settings.pageSize * (this.currentPage - 1) : 0;
        },
        currentPageEnd() {
            return this.settings.enablePagination
                ? Math.min(this.currentRowCount, this.settings.pageSize * this.currentPage)
                : this.currentRowCount;
        },
        currentNumRowsOnPage() {
            return this.currentPageEnd - this.currentPageStart;
        },
        bottomRowsMode() {
            return this.currentNumRowsOnPage > this.maxNumRows;
        },
        numRowsBottom() {
            return this.bottomRowsMode ? 1000 : 0;
        },
        numRowsTop() {
            // minus 1 due to the additional "…" row
            return this.bottomRowsMode ? this.maxNumRows - this.numRowsBottom - 1 : this.currentNumRowsOnPage;
        },
        numRowsTotal() {
            return this.bottomRowsMode ? this.maxNumRows : this.currentNumRowsOnPage;
        }
    },
    async mounted() {
        const clientWidth = this.$el.getBoundingClientRect().width;
        // clientWidth can be 0, e.g., if table is not visible (yet)
        if (clientWidth) {
            this.clientWidth = clientWidth;
            window.addEventListener('resize', this.onResize);
        } else {
            this.observeTableIntersection();
        }

        this.jsonDataService = new JsonDataService(this.knimeService);
        this.jsonDataService.addOnDataChangeCallback(this.onViewSettingsChange.bind(this));
        const initialData = await this.jsonDataService.initialData();
        this.selectionService = new SelectionService(this.knimeService);
        this.baseUrl = this.knimeService?.extensionConfig?.resourceInfo?.baseUrl;

        if (initialData) {
            const { table, dataTypes, columnDomainValues, settings } = initialData;
            this.displayedColumns = table.displayedColumns;
            this.dataTypes = dataTypes;
            this.columnDomainValues = columnDomainValues;
            this.totalRowCount = table.rowCount;
            this.currentRowCount = table.rowCount;
            this.settings = settings;
            if (this.useLazyLoading) {
                await this.initializeLazyLoading();
            } else {
                this.table = table;
            }
            await this.handleInitialSelection();
            const { publishSelection, subscribeToSelection } = settings;
            this.selectionService.onInit(this.onSelectionChange, publishSelection, subscribeToSelection);
            this.dataLoaded = true;
            this.columnFilters = this.getDefaultFilterConfigs(this.displayedColumns);
        }
    },
    beforeDestroy() {
        window.removeEventListener('resize', this.onResize);
    },
    methods: {

        lastColumnMinSize(dataColumnsSizeTotal, currentColumnSizes) {
            return dataColumnsSizeTotal -
                (this.settings.showRowIndices ? currentColumnSizes[0] : 0) -
                (this.settings.showRowKeys ? currentColumnSizes[1] : 0) -
                currentColumnSizes.slice(2, currentColumnSizes.length - 1).reduce((sum, size) => sum + size, 0);
        },

        async initializeLazyLoading(params) {
            const { updateDisplayedColumns = false, updateTotalSelected = true } = params || {};
            const numRows = Math.min(this.scopeSize, this.currentRowCount);
            this.currentScopeStartIndex = 0;
            this.currentScopeEndIndex = Math.min(this.scopeSize, this.currentRowCount);
            await this.updateData({
                lazyLoad: { loadFromIndex: 0, numRows, newScopeStart: 0 },
                updateDisplayedColumns,
                updateTotalSelected
            });
        },

        getLazyLoadParamsForCurrentScope() {
            const numRows = this.currentScopeEndIndex - this.currentScopeStartIndex;
            return {
                loadFromIndex: this.currentScopeStartIndex,
                newScopeStart: this.currentScopeStartIndex,
                numRows
            };
        },

        onScroll({ direction, startIndex, endIndex }) {
            if (!this.useLazyLoading) {
                return;
            }
            const prevScopeStart = this.currentScopeStartIndex;
            const prevScopeEnd = this.currentScopeEndIndex;
            const prevScopeSize = this.scopeSize;
            this.scopeSize = this.computeScopeSize(startIndex, endIndex);
            /** we only force an update on a scope size change if it is significant
             * (since startIndex and endIndex do not have a fixed distance even if the
             * height of the scroller is not changed)
             */
            const scopeChangeThreshold = 10;
            const scopeSizeChanged = Math.abs(prevScopeSize - this.scopeSize) > scopeChangeThreshold;
            let bufferStart, bufferEnd, newScopeStart, loadFromIndex, numRows;
            if (direction > 0) {
                if (prevScopeEnd - endIndex > this.bufferSize && !scopeSizeChanged) {
                    return;
                }
                // keep bufferSize elements above the current first visible element or keep all previous rows if the
                // update is due to a change in scope size
                bufferStart = scopeSizeChanged ? prevScopeStart : Math.max(startIndex - this.bufferSize, 0);
                // keep the already loaded elements below the current last visible.
                bufferEnd = Math.max(bufferStart, prevScopeEnd);
                // The next scope consist of numRows newly loaded rows and the buffer.
                // the size of the next scope should be this.scopeSize again (or less at the bottom of the table).
                numRows = Math.min(this.scopeSize - (bufferEnd - bufferStart), this.numRowsTotal - bufferEnd);
                newScopeStart = bufferStart;
                loadFromIndex = bufferEnd;
            } else {
                if (startIndex - prevScopeStart > this.bufferSize && !scopeSizeChanged) {
                    return;
                }
                // keep bufferSize elements below the current last visible or keep all previous rows if the
                // update is due to a change in scope size
                bufferEnd = scopeSizeChanged ? prevScopeEnd : Math.min(endIndex + this.bufferSize, this.numRowsTotal);
                // keep already loaded elements above the current first visible.
                bufferStart = Math.min(bufferEnd, prevScopeStart);
                // The next scope consist of numRows newly loaded rows and the buffer.
                // the size of the next scope should be this.scopeSize again (or less at the top of the table).
                numRows = Math.min(bufferStart, this.scopeSize - (bufferEnd - bufferStart));
                newScopeStart = loadFromIndex = bufferStart - numRows;
            }
            if (numRows > 0) {
                this.updateData({
                    lazyLoad: {
                        loadFromIndex,
                        numRows,
                        bufferStart,
                        bufferEnd,
                        direction,
                        newScopeStart
                    }
                });
            }
        },

        computeScopeSize(startIndex, endIndex) {
            return Math.max(endIndex - startIndex + 2 * this.bufferSize, MIN_SCOPE_SIZE);
        },

        // eslint-disable-next-line complexity
        async updateData(params) {
            const { lazyLoad, updateTotalSelected = false, updateDisplayedColumns = false,
                updateColumnContentTypes = false } = params;
            const displayedColumns = this.getColumnsForRequest(updateDisplayedColumns);

            let loadFromIndex, numRows;
            if (lazyLoad) {
                this.lastRequestScopeStartIndex = lazyLoad.newScopeStart;
                ({ loadFromIndex, numRows } = lazyLoad);
            } else {
                loadFromIndex = this.currentIndex;
                numRows = this.currentNumRowsOnPage;
            }
            const loadToIndex = loadFromIndex + numRows;

            const { topLoadFromIndex, topNumRows, bottomLoadFromIndex, bottomNumRows } = this.settings.enablePagination
                ? this.getTopAndBottomIndicesOnPagination(loadFromIndex, numRows)
                : {
                    ...this.getTopIndices(loadFromIndex, loadToIndex),
                    ...this.getBottomIndices(loadFromIndex, loadToIndex)
                };

            const fetchTopTable = topNumRows !== 0 || bottomNumRows === 0;
            const fetchBottomTable = bottomNumRows !== 0;

            const topTablePromise = fetchTopTable
                ? this.requestTable(topLoadFromIndex, topNumRows, displayedColumns,
                    updateDisplayedColumns, updateTotalSelected, this.settings.enablePagination)
                : null;

            let bottomTablePromise = fetchBottomTable
                ? this.requestTable(bottomLoadFromIndex, bottomNumRows, displayedColumns,
                    updateDisplayedColumns, updateTotalSelected, this.settings.enablePagination)
                : null;
            const topTable = fetchTopTable ? await topTablePromise : null;
            const bottomTable = fetchBottomTable ? await bottomTablePromise : null;
            const getFromTopOrBottom = (key) => topTable ? topTable[key] : bottomTable[key];
            if (updateDisplayedColumns) {
                this.columnFilters = this.getDefaultFilterConfigs(getFromTopOrBottom('displayedColumns'));
                this.displayedColumns = getFromTopOrBottom('displayedColumns');
            }
            if (updateTotalSelected) {
                if (this.columnSortColumnName || this.searchTerm || this.colFilterActive) {
                    this.totalSelected = getFromTopOrBottom('totalSelected');
                } else {
                    this.totalSelected = this.currentSelectedRowKeys.size;
                }
            }
            if (updateColumnContentTypes) {
                this.table.columnContentTypes = getFromTopOrBottom('columnContentTypes');
            }
            
            if (lazyLoad) {
                const { newScopeStart, direction, bufferStart = 0, bufferEnd = bufferStart } = lazyLoad;
                if (this.lastRequestScopeStartIndex !== newScopeStart) {
                    return;
                }
                const topPreviousDataLength = this.table?.rows?.length || 0;
                const rows = this.getCombinedTopRows(
                    { topTable, bufferStart, bufferEnd, direction, topPreviousDataLength }
                );
                if (typeof this.table.rows === 'undefined') {
                    this.table = { ...topTable, rows };
                } else {
                    this.table.rows = rows;
                    this.table.rowCount = getFromTopOrBottom('rowCount');
                }
                this.bottomRows = this.getCombinedBottomRows(
                    { bottomTable, bufferStart, bufferEnd, direction, topPreviousDataLength }
                );
                this.currentScopeStartIndex = newScopeStart;
                this.currentScopeEndIndex = newScopeStart + (bufferEnd - bufferStart) + numRows;
            } else {
                this.table = topTable;
                this.bottomRows = bottomTable ? bottomTable.rows : [];
            }
            this.currentRowCount = this.table.rowCount;
            this.transformSelection();
                
            this.numRowsAbove = lazyLoad ? this.currentScopeStartIndex : 0;
            this.numRowsBelow = lazyLoad
                ? this.numRowsTop + (this.bottomRowsMode && this.numRowsBottom + 1) - this.currentScopeEndIndex
                : 0;
        },
        getTopAndBottomIndicesOnPagination(loadFromIndex, numRows) {
            const currentPageEnd = loadFromIndex + numRows;
            const topLoadFromIndex = loadFromIndex;
            const topNumRows = this.numRowsTop;
            const bottomLoadFromIndex = currentPageEnd - this.numRowsBottom;
            const bottomNumRows = this.numRowsBottom;
            return { topLoadFromIndex, topNumRows, bottomLoadFromIndex, bottomNumRows };
        },
        getTopIndices(loadFromIndex, loadToIndex) {
            const topLoadFromIndex = Math.min(loadFromIndex, this.numRowsTop);
            const topLoadToIndex = Math.min(loadToIndex, this.numRowsTop);
            const topNumRows = topLoadToIndex - topLoadFromIndex;
            return { topLoadFromIndex, topNumRows };
        },
        getBottomIndices(loadFromIndex, loadToIndex) {
            const bottomLoadFromIndex = this.currentRowCount - Math.min(
                this.numRowsBottom, this.numRowsTotal - loadFromIndex
            );
            const bottomLoadToIndex = this.currentRowCount - Math.min(
                this.numRowsBottom, this.numRowsTotal - loadToIndex
            );
            const bottomNumRows = bottomLoadToIndex - bottomLoadFromIndex;
            return { bottomLoadFromIndex, bottomNumRows };
        },
        // eslint-disable-next-line max-params
        requestTable(startIndex, numRows, displayedColumns, updateDisplayedColumns, updateTotalSelected,
            clearImageDataCache) {
            // if columnSortColumnName is present a sorting is active
            if (this.columnSortColumnName || this.searchTerm || this.colFilterActive) {
                return this.requestFilteredAndSortedTable(startIndex, numRows, displayedColumns,
                    updateDisplayedColumns, updateTotalSelected, clearImageDataCache);
            } else {
                return this.requestUnfilteredAndUnsortedTable(startIndex, numRows, displayedColumns,
                    updateDisplayedColumns, clearImageDataCache);
            }
        },

        // eslint-disable-next-line max-params
        requestFilteredAndSortedTable(startIndex, numRows, displayedColumns, updateDisplayedColumns,
            updateTotalSelected, clearImageDataCache) {
            const columnSortIsAscending = this.columnSortDirection === 1;
            return this.requestNewData('getFilteredAndSortedTable',
                [
                    displayedColumns,
                    Math.min(this.totalRowCount - 1, Math.max(0, startIndex)),
                    numRows,
                    this.columnSortColumnName,
                    columnSortIsAscending,
                    this.searchTerm,
                    updateDisplayedColumns ? null : this.columnFilterValues,
                    this.settings.showRowKeys,
                    null,
                    updateDisplayedColumns,
                    updateTotalSelected,
                    clearImageDataCache
                ]);
        },

        // eslint-disable-next-line max-params
        requestUnfilteredAndUnsortedTable(startIndex, numRows, displayedColumns, updateDisplayedColumns,
            clearImageDataCache) {
            return this.requestNewData('getTable',
                [displayedColumns, startIndex, numRows, null, updateDisplayedColumns, clearImageDataCache]);
        },

        getColumnsForRequest(updateDisplayedColumns) {
            return updateDisplayedColumns ? this.settings.displayedColumns : this.displayedColumns;
        },

        requestNewData(method, options) {
            return this.jsonDataService.data({ method, options });
        },

        getCombinedTopRows({ topTable, bufferStart, bufferEnd, direction, topPreviousDataLength }) {
            const previousStartIndex = this.currentScopeStartIndex;
            const topBufferStart = Math.min(bufferStart - previousStartIndex, topPreviousDataLength);
            const topBufferEnd = Math.min(bufferEnd - previousStartIndex, topPreviousDataLength);
            return this.combineWithPrevious({
                newRows: topTable?.rows,
                bufferStart: topBufferStart,
                bufferEnd: topBufferEnd,
                direction
            });
        },
        getCombinedBottomRows({ bottomTable, bufferStart, bufferEnd, direction, topPreviousDataLength }) {
            // plus 1 because of the additional "…" row
            const previousBottomStartIndex = Math.max(this.numRowsTop + 1, this.currentScopeStartIndex);
            const bottomBufferStart = Math.max(bufferStart - previousBottomStartIndex, 0);
            const bottomBufferEnd = Math.max(bufferEnd - previousBottomStartIndex, 0);
            return this.combineWithPrevious({
                newRows: bottomTable?.rows,
                bufferStart: bottomBufferStart,
                bufferEnd: bottomBufferEnd,
                direction,
                bottom: true
            });
        },
        combineWithPrevious({ newRows, bufferStart, bufferEnd, direction, bottom = false }) {
            const rows = newRows || [];
            if (bufferStart === bufferEnd) {
                return rows;
            }
            const previousRows = bottom ? this.bottomRows : this.table.rows;
            const buffer = previousRows?.slice(bufferStart, bufferEnd) || [];
            if (direction > 0) {
                return [...buffer, ...rows];
            } else {
                return [...rows, ...buffer];
            }
        },

        refreshTable(params) {
            let { updateDisplayedColumns = false, resetPage = false, updateTotalSelected = false } = params || {};
            const tableUI = this.$refs.tableUI;
            if (tableUI) {
                tableUI.refreshScroller();
            }
            if (resetPage) {
                this.currentPage = 1;
                this.currentIndex = 0;
            }
            if (this.useLazyLoading) {
                this.initializeLazyLoading({ updateDisplayedColumns, updateTotalSelected });
            } else {
                this.updateData({ updateDisplayedColumns, updateTotalSelected });
            }
        },

        onPageChange(pageDirection) {
            const { pageSize } = this.settings;
            this.currentPage += pageDirection;
            this.currentIndex += pageDirection * pageSize;
            this.refreshTable();
        },

        onViewSettingsChange(event) {
            const newSettings = event.data.data.view;
            const enablePaginationChanged = newSettings.enablePagination !== this.settings.enablePagination;
            const displayedColumnsChanged =
                !arrayEquals(newSettings.displayedColumns, this.settings.displayedColumns);
            const showRowKeysChanged = newSettings.showRowKeys !== this.settings.showRowKeys;
            const showRowIndicesChanged = newSettings.showRowIndices !== this.settings.showRowIndices;
            const pageSizeChanged = newSettings.pageSize !== this.settings.pageSize;
            const compactModeChangeInducesRefresh = this.useLazyLoading &&
                (newSettings.compactMode !== this.settings.compactMode);

            this.settings = newSettings;

            const numberOfDisplayedColsChanged = displayedColumnsChanged || showRowKeysChanged || showRowIndicesChanged;
            let sortingParamsReseted = false;
            if (this.colSortActive && numberOfDisplayedColsChanged) {
                sortingParamsReseted = this.updateSortingParams(newSettings, displayedColumnsChanged,
                    showRowKeysChanged, showRowIndicesChanged);
            }
            if (compactModeChangeInducesRefresh || sortingParamsReseted) {
                this.refreshTable();
            } else if (displayedColumnsChanged) {
                this.refreshTable({ updateDisplayedColumns: true, updateTotalSelected: true });
            } else if (pageSizeChanged || enablePaginationChanged) {
                this.refreshTable({ resetPage: true });
            }
            this.selectionService.onSettingsChange(() => Array.from(this.currentSelectedRowKeys), this.clearSelection,
                newSettings.publishSelection, newSettings.subscribeToSelection);
        },
        onColumnSort(newColumnSortIndex) {
            const mappedDisplayedColumns = [null, ROW_KEYS_SORT_COL_NAME, ...this.displayedColumns];
            // if columnSortIndex equals newColumnSortIndex sorting is ascending as default is descending
            const ascendingSort = this.columnSortIndex === newColumnSortIndex && this.columnSortDirection < 0;
            this.columnSortDirection = ascendingSort ? 1 : -1;
            this.currentPage = 1;
            this.currentIndex = 0;
            this.columnSortIndex = newColumnSortIndex;

            this.columnSortColumnName = mappedDisplayedColumns[this.columnIndexMap.get(this.columnSortIndex)];
            this.refreshTable();
        },
        async onSelectionChange(rawEvent) {
            const { selection, mode } = rawEvent;
            if (mode === 'REPLACE') {
                this.currentSelectedRowKeys = new Set(selection);
            } else {
                const addOrDelete = mode === 'ADD' ? 'add' : 'delete';
                this.updateCurrentSelectedRowKeys(addOrDelete, selection);
            }
            this.transformSelection();
            this.totalSelected = await this.requestTotalSelected();
        },
        requestTotalSelected() {
            if (this.searchTerm || this.colFilterActive) {
                return this.requestNewData('getTotalSelected');
            } else {
                return this.currentSelectedRowKeys.size;
            }
        },
        onRowSelect(selected, rowInd, _groupInd, isTop) {
            const rowKey = isTop ? this.table.rows[rowInd][0] : this.bottomRows[rowInd][0];
            this.totalSelected += selected ? 1 : -1;
            this.updateSelection(selected, [rowKey]);
        },
        async onSelectAll(selected) {
            const filterActive = this.currentRowCount !== this.totalRowCount;
            if (selected) {
                const currentRowKeys = await this.requestNewData('getCurrentRowKeys', []);
                if (filterActive) {
                    this.updateCurrentSelectedRowKeys('add', currentRowKeys);
                } else {
                    this.currentSelectedRowKeys = new Set(currentRowKeys);
                }
                this.selectionService.publishOnSelectionChange('add', currentRowKeys);
            } else if (filterActive) {
                const currentRowKeys = await this.requestNewData('getCurrentRowKeys', []);
                this.updateCurrentSelectedRowKeys('delete', currentRowKeys);
                this.selectionService.publishOnSelectionChange('remove', currentRowKeys);
            } else {
                this.currentSelectedRowKeys = new Set();
                this.selectionService.publishOnSelectionChange('replace', []);
            }
            this.transformSelection();
            this.totalSelected = selected ? this.currentRowCount : 0;
        },
        onSearch(input) {
            this.searchTerm = input;
            this.refreshTable({ resetPage: true, updateTotalSelected: true });
        },
        onColumnFilter(colInd, value) {
            this.columnFilters[this.columnIndexMap.get(colInd)].value = value;
            this.refreshTable({ resetPage: true, updateTotalSelected: true });
        },
        onClearFilter() {
            this.columnFilters = this.getDefaultFilterConfigs(this.displayedColumns);
            this.refreshTable({ resetPage: true, updateTotalSelected: true });
        },
        onColumnResize(columnIndex, newColumnSize) {
            const colName = this.dataConfig.columnConfigs[columnIndex].header;
            if (columnIndex < this.numberOfDisplayedIdColumns) {
                if (colName === 'Index') {
                    Vue.set(this.columnSizeOverrides, INDEX_SYMBOL, newColumnSize);
                } else {
                    Vue.set(this.columnSizeOverrides, ROW_KEY_SYMBOL, newColumnSize);
                }
            } else {
                Vue.set(this.columnSizeOverrides, colName, newColumnSize);
            }
        },
        observeTableIntersection() {
            new IntersectionObserver((entries, observer) => {
                entries.forEach((entry) => {
                    if (entry.target === this.$el && entry.boundingClientRect.width) {
                        this.clientWidth = entry.boundingClientRect.width;
                        // observer is either removed here or on garbage collection
                        observer.unobserve(entry.target);
                        window.addEventListener('resize', this.onResize);
                    }
                });
            }).observe(this.$el);
        },
        onResize: throttle(function () {
            /* eslint-disable no-invalid-this */
            const updatedClientWidth = this.$el.getBoundingClientRect().width;
            if (updatedClientWidth) {
                // also update all overridden column widths according to the relative change in client width
                const ratio = updatedClientWidth / this.clientWidth;
                Object.keys(this.columnSizeOverrides).forEach(key => {
                    this.columnSizeOverrides[key] *= ratio;
                });
                Object.getOwnPropertySymbols(this.columnSizeOverrides).forEach(symbol => {
                    this.columnSizeOverrides[symbol] *= ratio;
                });
                this.clientWidth = updatedClientWidth;
            } else {
                this.observeTableIntersection();
                window.removeEventListener('resize', this.onResize);
            }
            /* eslint-enable no-invalid-this */
        }),
        updateSelection(selected, rowKeys) {
            this.selectionService.publishOnSelectionChange(selected ? 'add' : 'remove', rowKeys);
            this.updateCurrentSelectedRowKeys(selected ? 'add' : 'delete', rowKeys);
            this.transformSelection();
        },
        updateCurrentSelectedRowKeys(addOrDelete, selectedRowKeys) {
            selectedRowKeys.forEach(selectedRowKey => this.currentSelectedRowKeys[addOrDelete](selectedRowKey));
        },
        transformSelection() {
            if (typeof this.table.rows === 'undefined') {
                return;
            }
            const rowKeysTop = this.table.rows.map(row => row[0]).filter(x => typeof x !== 'undefined');
            const rowKeysBottom = this.bottomRows.map(row => row[0]);
            this.currentSelection = rowKeysTop.map(rowKey => this.currentSelectedRowKeys.has(rowKey));
            this.currentBottomSelection = rowKeysBottom.map(rowKey => this.currentSelectedRowKeys.has(rowKey));
        },
        clearSelection() {
            this.currentSelectedRowKeys = new Set();
            this.totalSelected = 0;
            this.transformSelection();
        },
        async handleInitialSelection() {
            if (this.settings.subscribeToSelection) {
                const initialSelection = await this.selectionService.initialSelection();
                this.currentSelectedRowKeys = new Set(initialSelection);
                this.totalSelected = initialSelection.length;
                this.transformSelection();
            } else {
                this.clearSelection();
            }
        },
        createColumnConfig(columnInformation) {
            const { index, columnName, columnTypeName, contentType, isSortable } = columnInformation;
            return {
                key: index,
                header: columnName,
                subHeader: columnTypeName,
                hasSlotContent: isImage(contentType),
                size: this.columnSizes[index],
                filterConfig: this.columnFilters[index],
                formatter: val => val,
                isSortable
            };
        },
        getDefaultFilterConfigs(displayedColumns) {
            // default filter config for rowKey and empty config for rowIndex
            const filterConfigs = [
                {
                    is: '',
                    value: ''
                },
                createDefaultFilterConfig(false, null)
            ];
            displayedColumns.forEach(col => {
                const domainValues = this.columnDomainValues[col];
                const domainValuesMapped = domainValues ? domainValues.map(val => ({ id: val, text: val })) : null;
                filterConfigs.push(createDefaultFilterConfig(domainValues, domainValuesMapped));
            });
            return filterConfigs;
        },
        // only call the method when (displayedColumns XOR showRowKeys XOR showRowKeys) changed and a sorting is active
        updateSortingParams(newSettings, displayedColumnsChanged, showRowKeysChanged, showRowIndicesChanged) {
            const { showRowKeys, showRowIndices, displayedColumns } = newSettings;
            if (displayedColumnsChanged) {
                // sort on column (not rowKey column) and add/remove (different) column (not rowKey/rowIndex column)
                if (this.columnSortColumnName !== ROW_KEYS_SORT_COL_NAME) {
                    const newColIndexOfSortCol = displayedColumns.indexOf(this.columnSortColumnName);
                    if (newColIndexOfSortCol === -1) {
                        this.resetSorting();
                        return true;
                    } else {
                        this.columnSortIndex = newColIndexOfSortCol + this.numberOfDisplayedIdColumns;
                    }
                }
                // no change when sorting the rowKey column and adding/removing columns behind the rowKey column
                // current sorting is on rowKey column which is removed
            } else if (this.columnSortColumnName === ROW_KEYS_SORT_COL_NAME && !showRowKeys) {
                this.resetSorting();
                return true;
                // rowKey or rowIndex column is added
            } else if ((showRowKeys && showRowKeysChanged) || (showRowIndices && showRowIndicesChanged)) {
                this.columnSortIndex += 1;
                // rowKey or rowIndex column is removed
            } else {
                this.columnSortIndex -= 1;
            }
            return false;
        },
        updateColumnIndexMap(columnConfigs) {
            this.columnIndexMap = new Map(columnConfigs.map((config, index) => [index, config.key]));
        },
        getImageUrl(data, index) {
            // the columnConfigs contain at index 0 rowIndices, at index 1 rowKeys, and at index 2+ the data
            // the rowData consists of [rowIndices?, rowkeys, ...data] (rowIndices if showRowIndices)
            // we need to map from the columnConfig data indices to the rowData data indices
            return this.$store.getters['api/uiExtResourceLocation']({
                resourceInfo: {
                    baseUrl: this.baseUrl,
                    path: data.data.row[index - (this.numberOfUsedColumns - this.numberOfDisplayedColumns)]
                }
            });
        },
        resetSorting() {
            this.columnSortColumnName = null;
            this.columnSortIndex = null;
            this.columnSortDirection = null;
            this.currentPage = 1;
            this.currentIndex = 0;
        }
    }
};
</script>

<template>
  <div class="table-view-wrapper">
    <h4
      v-if="showTitle"
      class="table-title"
    >
      {{ settings.title }}
    </h4>
    <TableUI
      v-if="dataLoaded && numberOfDisplayedColumns > 0"
      ref="tableUI"
      :data="[rowData]"
      :bottom-data="bottomRowData"
      :current-selection="[currentSelection]"
      :current-bottom-selection="currentBottomSelection"
      :total-selected="totalSelected"
      :data-config="dataConfig"
      :table-config="tableConfig"
      :num-rows-above="numRowsAbove"
      :num-rows-below="numRowsBelow"
      @pageChange="onPageChange"
      @columnSort="onColumnSort"
      @rowSelect="onRowSelect"
      @selectAll="onSelectAll"
      @search="onSearch"
      @columnFilter="onColumnFilter"
      @clearFilter="onClearFilter"
      @columnResize="onColumnResize"
      @lazyload="onScroll"
    >
      <template
        v-for="index in numberOfUsedColumns"
        #[`cellContent-${index}`]="data"
      >
        <img
          :key="index"
          loading="lazy"
          :src="getImageUrl(data, index)"
          alt=""
        >
      </template>
    </TableUI>
    <div
      v-else-if="dataLoaded"
      class="no-columns"
    >
      <h4>
        No data to display
      </h4>
    </div>
  </div>
</template>

<style lang="postcss" scoped>
.table-view-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;

  & >>> .table-header {
    background-color: var(--knime-porcelain);
  }

  & >>> .row {
    border-bottom: 1px solid var(--knime-porcelain);
    align-content: center;

    & img {
      object-fit: contain;
      max-width: 100%;
      max-height: 100%;
      vertical-align: middle;
    }
  }
}

.table-title {
  margin: 0;
  padding: 15px 0, 5px 5px;
  color: rgb(70 70 70);
  font-size: 20px;
}

.no-columns {
  height: 100%;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;

  & h4 {
    color: rgb(70 70 70);
    font-size: 16px;
  }
}
</style>
