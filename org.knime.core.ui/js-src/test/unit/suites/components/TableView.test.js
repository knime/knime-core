/* eslint-disable max-nested-callbacks */
/* eslint-disable max-lines */
import TableView from '@/components/TableView';
import { JsonDataService, SelectionService } from '@knime/ui-extension-service';
import { mount, createLocalVue } from '@vue/test-utils';
import { TableUI } from '@knime/knime-ui-table';
import { asyncMountTableView, changeViewSetting } from '../utils/tableViewTestUtils';
import Vuex from 'vuex';
import { MIN_COLUMN_SIZE, SPECIAL_COLUMNS_SIZE, DATA_COLUMNS_MARGIN } from '@knime/knime-ui-table/util/constants';

const localVue = createLocalVue();
localVue.use(Vuex);

jest.mock('raf-throttle', () => function (func) {
    return function (...args) {
        // eslint-disable-next-line no-invalid-this
        return func.apply(this, args);
    };
});

describe('TableView.vue', () => {
    const emptyColumnFilterValues = [[''], [], [], [''], ['']];

    let context,
        initialDataMock,
        dataRequestResult,
        getData;

    const rowCount = 4;

    beforeEach(() => {
        initialDataMock = {
            table: {
                rows: [['row1', 'entry1col1', 'entry1col2', '1', 'view_x_y/datacell/hash1.png'],
                    ['row2', 'entry2col1', 'entry2col2', '2', 'view_x_y/datacell/hash2.png'],
                    ['row3', 'entry3col1', 'entry3col2', '3', 'view_x_y/datacell/hash3.png'],
                    ['row4', 'entry4col1', 'entry4col2', '4', 'view_x_y/datacell/hash4.png']],
                columnContentTypes: ['txt', 'txt', 'txt', 'img_path'],
                columnDataTypeIds: ['datatype1', 'datatype1', 'datatype2', 'datatype3'],
                rowCount,
                displayedColumns: ['col1', 'col2', 'col3', 'col4'],
                rowKeys: ['row1', 'row2', 'row3', 'row4']
            },
            dataTypes: {
                datatype1: {
                    name: 'col1And2TypeName'
                },
                datatype2: {
                    name: 'col3TypeName'
                },
                datatype3: {
                    name: 'col4TypeName'
                }
            },
            columnDomainValues: {
                col1: ['entry1col1', 'entry2col1', 'entry3col1', 'entry4col1'],
                col2: ['entry1col2', 'entry2col2', 'entry3col2', 'entry4col2'],
                col3: null,
                col4: null
            },
            currentIndex: 0,
            currentPage: 1,
            currentSelection: [[false, false, false, false]],
            settings: {
                pageSize: 2,
                displayedColumns: ['col1', 'col2', 'col3', 'col4'],
                showRowKeys: false,
                showColumnDataType: false,
                showRowIndices: false,
                showTitle: true,
                enableColumnSearch: true,
                enableGlobalSearch: true,
                enablePagination: true,
                enableSortingByHeader: true,
                publishSelection: true,
                subscribeToSelection: true,
                compactMode: false
            }
        };

        // eslint-disable-next-line no-magic-numbers
        dataRequestResult = { ...initialDataMock.table, rows: initialDataMock.table.rows.slice(1, 3) };

        getData = jest.fn();
        JsonDataService.mockImplementation(() => ({
            initialData: jest.fn().mockResolvedValue(initialDataMock),
            data: getData.mockImplementation((obj) => Promise.resolve(dataRequestResult)),
            addOnDataChangeCallback: jest.fn(),
            knimeService: {}
        }));

        SelectionService.mockImplementation(() => ({
            add: jest.fn(),
            remove: jest.fn(),
            replace: jest.fn(),
            addOnSelectionChangeCallback: jest.fn(),
            initialSelection: jest.fn().mockResolvedValue([]),
            onInit: jest.fn(),
            publishOnSelectionChange: jest.fn(),
            onSettingsChange: jest.fn()
        }));

        const store = new Vuex.Store({
            modules: {
                api: {
                    getters: {
                        uiExtResourceLocation: () => ({ resourceInfo }) => resourceInfo.baseUrl + resourceInfo.path
                    },
                    namespaced: true
                }
            }
        });

        context = {
            provide: {
                getKnimeService: () => ({ extensionConfig: { resourceInfo: { baseUrl: 'http://localhost:8080/base.url/' } } })
            },
            store,
            localVue
        };
    });

    window.IntersectionObserver = jest.fn(() => ({
        observe: () => null,
        unobserve: () => null,
        disconnect: () => null
    }));

    // eslint-disable-next-line func-style
    async function getTableProps(wrapper) {
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick();
        await wrapper.vm.$nextTick(); // need four ticks for accessing child props
        return wrapper.vm.$children[0].$props;
    }

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('initialization and data fetching', () => {
        it('does not render the TableUI when no initialData is given', async () => {
            initialDataMock = null;
            let wrapper = await mount(TableView, context);
            expect(wrapper.findComponent(TableUI).exists()).toBeFalsy();
        });

        it('fetches initialData', async () => {
            let wrapper = await mount(TableView, context);
            await wrapper.vm.$nextTick();
            const dataSpy = jest.spyOn(wrapper.vm.jsonDataService, 'initialData');
            expect(dataSpy).toHaveBeenCalled();
        });

        it('requests new data and updates table view', async () => {
            let wrapper = await mount(TableView, context);
            await wrapper.vm.$nextTick();

            const dataSpy = jest.spyOn(wrapper.vm.jsonDataService, 'data');
            await wrapper.vm.requestNewData('getTable', [['col1', 'col2'], 0, rowCount]); // eslint-disable-line no-magic-numbers
            expect(dataSpy).toBeCalledWith({ method: 'getTable', options: [['col1', 'col2'], 0, rowCount] }); // eslint-disable-line no-magic-numbers
        });

        it('does not render the TableUI when no columns are to be displayed', async () => {
            initialDataMock.settings = {
                ...initialDataMock.settings,
                displayedColumns: [],
                showRowIndices: false,
                showRowKeys: false
            };
            initialDataMock.table.displayedColumns = [];
            let wrapper = await mount(TableView, context);
            await wrapper.vm.$nextTick();
            await wrapper.vm.$nextTick();
            await wrapper.vm.$nextTick();
            await wrapper.vm.$nextTick();
            expect(wrapper.findComponent(TableUI).exists()).toBeFalsy();
            expect(wrapper.find('.no-columns').exists()).toBeTruthy();
        });

        it('renders the TableUI and passes the correct props', async () => {
            initialDataMock.settings.publishSelection = true;
            initialDataMock.settings.subscribeToSelection = true;

            let wrapper = await mount(TableView, context);
            const { data, currentSelection, totalSelected, dataConfig, tableConfig } = await getTableProps(wrapper);

            expect(wrapper.getComponent(TableUI).exists()).toBe(true);
            expect(data).toEqual([initialDataMock.table.rows.map((row, index) => [index, ...row])]);
            expect(currentSelection).toEqual(Array(1).fill(Array(initialDataMock.settings.pageSize).fill(false)));
            expect(totalSelected).toStrictEqual(0);
            expect(tableConfig).toMatchObject({
                subMenuItems: false,
                pageConfig: {
                    currentSize: 4,
                    tableSize: 4,
                    pageSize: 2,
                    currentPage: 1
                },
                sortConfig: {
                    sortColumn: null,
                    sortDirection: null
                }
            });
            const expectedColumnSize = MIN_COLUMN_SIZE;
            expect(dataConfig).toMatchObject({
                columnConfigs: [
                    { key: 2, header: 'col1', size: expectedColumnSize, hasSlotContent: false },
                    { key: 3, header: 'col2', size: expectedColumnSize, hasSlotContent: false },
                    { key: 4, header: 'col3', size: expectedColumnSize, hasSlotContent: false },
                    { key: 5, header: 'col4', size: expectedColumnSize, hasSlotContent: true }
                ],
                rowConfig: {
                    rowHeight: 80,
                    compactMode: false
                }
            });

            expect(wrapper.getComponent(TableUI).exists()).toBe(true);
            expect(tableConfig).toMatchObject({
                showSelection: true
            });
        });

        it('passes the correct dataConfig when showing rowkeys', async () => {
            initialDataMock.settings.showRowKeys = true;
            
            const expectedColumnSize = MIN_COLUMN_SIZE;
            let wrapper = await mount(TableView, context);
            const { dataConfig } = await getTableProps(wrapper);

            expect(wrapper.getComponent(TableUI).exists()).toBe(true);
            expect(dataConfig).toMatchObject({
                columnConfigs: [
                    { key: 1, header: 'Row Key', size: expectedColumnSize },
                    { key: 2, header: 'col1', size: expectedColumnSize },
                    { key: 3, header: 'col2', size: expectedColumnSize },
                    { key: 4, header: 'col3', size: expectedColumnSize },
                    { key: 5, header: 'col4', size: expectedColumnSize }
                ]
            });
        });

        it('passes the correct dataConfig when showing content types', async () => {
            initialDataMock.settings.showColumnDataType = true;
            
            const expectedColumnSize = MIN_COLUMN_SIZE;
            let wrapper = await mount(TableView, context);
            const { dataConfig } = await getTableProps(wrapper);

            const newColumnConfig = [
                {
                    key: 2,
                    header: 'col1',
                    subHeader: 'col1And2TypeName',
                    size: expectedColumnSize,
                    hasSlotContent: false
                },
                {
                    key: 3,
                    header: 'col2',
                    subHeader: 'col1And2TypeName',
                    size: expectedColumnSize,
                    hasSlotContent: false
                },
                { key: 4, header: 'col3', subHeader: 'col3TypeName', size: expectedColumnSize, hasSlotContent: false },
                { key: 5, header: 'col4', subHeader: 'col4TypeName', size: expectedColumnSize, hasSlotContent: true }
            ];

            expect(wrapper.getComponent(TableUI).exists()).toBe(true);
            expect(dataConfig).toMatchObject({
                columnConfigs: newColumnConfig
            });
        });

        it('passes the correct tableConfig when publishSelection and subscribeToSelection are disabled', async () => {
            initialDataMock.settings.publishSelection = false;
            initialDataMock.settings.subscribeToSelection = false;

            let wrapper = await mount(TableView, context);
            const { tableConfig } = await getTableProps(wrapper);

            expect(wrapper.getComponent(TableUI).exists()).toBe(true);
            expect(tableConfig).toMatchObject({
                showSelection: false
            });
        });

        describe('lazyloading and scrolling', () => {
            let wrapper, updateDataSpy;
    
            beforeEach(async () => {
                initialDataMock.settings.enablePagination = false;
                ({ wrapper, updateDataSpy } = await asyncMountTableView(context));
            });

            describe('initialization', () => {
                it('requests initial data for lazy loading for small rowCount', async () => {
                    wrapper.vm.refreshTable();
                    await wrapper.vm.$nextTick();
                    expect(wrapper.vm.currentScopeStartIndex).toBe(0);
                    expect(wrapper.vm.currentScopeEndIndex).toBe(rowCount);
                    expect(updateDataSpy).toHaveBeenCalledWith(expect.objectContaining(
                        { lazyLoad: { loadFromIndex: 0, newScopeStart: 0, numRows: rowCount }, requestRowKeys: true }
                    ));
                    expect(wrapper.vm.currentRowKeys).toStrictEqual(dataRequestResult.rowKeys);
                });

                
                it('requests pageSize rows as initial data for lazy loading', async () => {
                    wrapper.vm.rowCount = 300;
                    wrapper.vm.refreshTable();
                    await wrapper.vm.$nextTick();
                    expect(wrapper.vm.currentScopeStartIndex).toBe(0);
                    expect(wrapper.vm.currentScopeEndIndex).toBe(wrapper.vm.scopeSize);
                    expect(updateDataSpy).toHaveBeenCalledWith(expect.objectContaining(
                        { lazyLoad: { loadFromIndex: 0, newScopeStart: 0, numRows: wrapper.vm.scopeSize } }
                    ));
                });
            });
            
            describe('on data update', () => {
                beforeEach(() => {
                    expect.extend({
                        arrayStartingWith(received, params) {
                            const message = `Expected ${received} to be an array starting with the entries ${params}`;
                            if (params.length > received.length) {
                                return { pass: false, message };
                            }
                            for (let i = 0; i < params.length; i++) {
                                expect(params[i]).toStrictEqual(received[i]);
                            }
                            return { pass: true };
                        }
                    });
                });

                it('requests window of data and fills the rest with dummy rows', async () => {
                    const loadFromIndex = 1;
                    const numRows = 2;
                    wrapper.vm.updateData({ lazyLoad: {
                        loadFromIndex,
                        newScopeStart: loadFromIndex,
                        numRows
                    } });
                    await wrapper.vm.$nextTick();
                    expect(wrapper.vm.table.rows.length).toBe(rowCount);
                    // eslint-disable-next-line no-magic-numbers
                    expect(getData).toHaveBeenNthCalledWith(2, expect.objectContaining({
                        options: expect.arrayStartingWith(
                            [wrapper.vm.settings.displayedColumns, loadFromIndex, numRows]
                        )
                    }));
                    expect(wrapper.vm.table.rows).toStrictEqual([
                        [],
                        dataRequestResult.rows[0],
                        dataRequestResult.rows[1],
                        []
                    ]);
                });
    
                it('appends buffer from previously fetched rows', async () => {
                    wrapper.vm.table.rows = [['previousRow1'], ['previousRow2'], ['previousRow3'], ['previousRow4']];
                    wrapper.vm.updateData({ lazyLoad: {
                        direction: 1,
                        bufferStart: 0,
                        bufferEnd: 1,
                        loadFromIndex: 1,
                        newScopeStart: 0,
                        numRows: 2
                    } });
                    await wrapper.vm.$nextTick();
                    expect(wrapper.vm.table.rows.length).toBe(rowCount);
                    expect(wrapper.vm.table.rows).toStrictEqual([
                        ['previousRow1'],
                        dataRequestResult.rows[0],
                        dataRequestResult.rows[1],
                        []
                    ]);
                });
            });
    
            describe('on scroll event', () => {
                describe('downwards scroll', () => {
                    it('handles update on downwards scroll', () => {
                        wrapper.vm.currentScopeStartIndex = 0;
                        wrapper.vm.currentScopeEndIndex = 2;
                        wrapper.vm.onScroll({ direction: 1, startIndex: 1, endIndex: 2 }); // eslint-disable-line no-magic-numbers
                        expect(updateDataSpy).toHaveBeenCalledWith(expect.objectContaining(
                            { lazyLoad: {
                                direction: 1,
                                loadFromIndex: 2,
                                newScopeStart: 0,
                                bufferStart: 0,
                                bufferEnd: 2,
                                numRows: 2
                            } }
                        ));
                    });
                
    
                    it('does not update data if the distance to the end of the previous window exceeds the buffer size',
                        () => {
                            wrapper.vm.currentScopeStartIndex = 0;
                            wrapper.vm.currentScopeEndIndex = 200;
                            wrapper.vm.rowCount = 1000;
                            wrapper.vm.onScroll({ direction: 1, startIndex: 130, endIndex: 140 }); // eslint-disable-line no-magic-numbers
                            expect(updateDataSpy).toHaveBeenCalledTimes(0);
                        });
    
                    it('keeps a buffer of buffer size in the opposite direction and adjusts the number of loaded rows',
                        () => {
                            wrapper.vm.currentScopeStartIndex = 200;
                            wrapper.vm.currentScopeEndIndex = 400;
                            wrapper.vm.rowCount = 1000;
                            wrapper.vm.onScroll({ direction: 1, startIndex: 440, endIndex: 480 }); // eslint-disable-line no-magic-numbers
                            expect(updateDataSpy).toHaveBeenCalledWith(expect.objectContaining(
                                { lazyLoad: {
                                    direction: 1,
                                    loadFromIndex: 400,
                                    newScopeStart: 390,
                                    bufferStart: 390,
                                    bufferEnd: 400,
                                    numRows: 190
                                } }
                            ));
                            // eslint-disable-next-line no-magic-numbers
                            expect(wrapper.vm.currentScopeStartIndex).toBe(390);
                            // eslint-disable-next-line no-magic-numbers
                            expect(wrapper.vm.currentScopeEndIndex).toBe(590);
                        });
    
                    it('does not update data on upwards scroll on the top of the table', () => {
                        wrapper.vm.currentScopeStartIndex = 1;
                        wrapper.vm.currentScopeEndIndex = 4;
                        wrapper.vm.onScroll({ direction: 1, startIndex: 1, endIndex: 2 }); // eslint-disable-line no-magic-numbers
                        expect(updateDataSpy).toHaveBeenCalledTimes(0);
                    });
                });
    
                describe('upwards scroll', () => {
                    it('handles update on upwards scroll', () => {
                        wrapper.vm.currentScopeStartIndex = 1;
                        wrapper.vm.currentScopeEndIndex = 4;
                        wrapper.vm.onScroll({ direction: -1, startIndex: 1, endIndex: 2 }); // eslint-disable-line no-magic-numbers
                        expect(updateDataSpy).toHaveBeenCalledWith(expect.objectContaining(
                            { lazyLoad: {
                                direction: -1,
                                loadFromIndex: 0,
                                newScopeStart: 0,
                                bufferStart: 1,
                                bufferEnd: 4,
                                numRows: 1
                            } }
                        ));
                    });
    
                    it('does not update data if the distance to the start of the previous window exceeds the ' +
                    'buffer size',
                    () => {
                        wrapper.vm.currentScopeStartIndex = 0;
                        wrapper.vm.currentScopeEndIndex = 200;
                        wrapper.vm.rowCount = 1000;
                        wrapper.vm.onScroll({ direction: -1, startIndex: 60, endIndex: 70 }); // eslint-disable-line no-magic-numbers
                        expect(updateDataSpy).toHaveBeenCalledTimes(0);
                    });
    
                    it('keeps a buffer of buffer size in the opposite direction and adjusts the number of loaded rows',
                        () => {
                            wrapper.vm.currentScopeStartIndex = 200;
                            wrapper.vm.currentScopeEndIndex = 400;
                            wrapper.vm.rowCount = 1000;
                            wrapper.vm.onScroll({ direction: -1, startIndex: 160, endIndex: 170 }); // eslint-disable-line no-magic-numbers
                            expect(updateDataSpy).toHaveBeenCalledWith(expect.objectContaining(
                                { lazyLoad: {
                                    direction: -1,
                                    loadFromIndex: 20,
                                    newScopeStart: 20,
                                    bufferStart: 200,
                                    bufferEnd: 220,
                                    numRows: 180
                                } }
                            ));
                            // eslint-disable-next-line no-magic-numbers
                            expect(wrapper.vm.currentScopeStartIndex).toBe(20);
                            // eslint-disable-next-line no-magic-numbers
                            expect(wrapper.vm.currentScopeEndIndex).toBe(220);
                        });
        
                    it('does not update data on downwards scroll on the bottom of the table', () => {
                        wrapper.vm.currentScopeStartIndex = 0;
                        wrapper.vm.currentScopeEndIndex = 2;
                        wrapper.vm.onScroll({ direction: -1, startIndex: 1, endIndex: 2 }); // eslint-disable-line no-magic-numbers
                        expect(updateDataSpy).toHaveBeenCalledTimes(0);
                    });
                });
            });
        });
    });

    describe('settings change event', () => {
        let wrapper, updateDataSpy, refreshTableSpy, dataSpy;

        beforeEach(async () => {
            ({ wrapper, updateDataSpy, refreshTableSpy, dataSpy } = await asyncMountTableView(context));
        });

        test.each([
            ['displayedColumns', ['col3']],
            ['pageSize', 3], // eslint-disable-line no-magic-numbers
            ['enablePagination', false]
        ])('view setting %p change causes table to be refreshed',
            (settingsKey, newValue) => {
                const settings = JSON.parse(JSON.stringify(wrapper.vm.$data.settings));
                settings[settingsKey] = newValue;
                wrapper.vm.onViewSettingsChange({
                    data: { data: { view: settings } }
                });
                expect(refreshTableSpy).toHaveBeenCalled();
                expect(wrapper.vm.$data.settings[settingsKey]).toBe(newValue);
            });

        test.each([
            ['showRowKeys', true],
            ['showColumnDataType', true],
            ['showRowIndices', true],
            ['enableSortingByHeader', false],
            ['enableColumnSearch', false],
            ['enableGlobalSearch', false],
            ['showTitle', false],
            ['compactMode', true]
        ])('view setting %p change causes data NOT to be requested',
            (settingsKey, newValue) => {
                const settings = JSON.parse(JSON.stringify(wrapper.vm.$data.settings));
                settings[settingsKey] = newValue;
                wrapper.vm.onViewSettingsChange({
                    data: { data: { view: settings } }
                });
                expect(updateDataSpy).not.toHaveBeenCalled();
                expect(wrapper.vm.$data.settings[settingsKey]).toBe(newValue);
            });

        it('view setting "compactMode" change causes table to be refreshed if useLazyLoading is true', async () => {
            const viewSettings = { ...initialDataMock.settings,
                enablePagination: false };
            wrapper.vm.onViewSettingsChange({
                data: { data: { view: viewSettings } }
            });
            await wrapper.vm.$nextTick();
            jest.clearAllMocks();

            const settings = JSON.parse(JSON.stringify(wrapper.vm.$data.settings));
            settings.compactMode = true;
            wrapper.vm.onViewSettingsChange({
                data: { data: { view: settings } }
            });

            expect(refreshTableSpy).toHaveBeenCalled();
        });

        it('hides title', async () => {
            expect(wrapper.find('.table-title').exists()).toBeTruthy();
            const settings = JSON.parse(JSON.stringify(wrapper.vm.$data.settings));
            settings.showTitle = false;
            wrapper.vm.onViewSettingsChange({
                data: { data: { view: settings } }
            });
            await wrapper.vm.$nextTick();
            await wrapper.vm.$nextTick();
            expect(wrapper.find('.table-title').exists()).toBeFalsy();
        });

        it('updates displayed columns on displayed columns change', () => {
            const settings = JSON.parse(JSON.stringify(wrapper.vm.$data.settings));
            settings.displayedColumns.push('missingCol');
            wrapper.vm.onViewSettingsChange({
                data: { data: { view: settings } }
            });
            expect(dataSpy).toHaveBeenCalledWith({
                method: 'getTable',
                options: [settings.displayedColumns, 0, 2, null, true, false]
            });
            expect(wrapper.vm.displayedColumns).toStrictEqual(initialDataMock.table.displayedColumns);
        });
        
        describe('sort parameter update', () => {
            it('updates the sort parameters when a sorting is active and columns are changed',
                () => {
                    wrapper.vm.onColumnSort(2);
                    const updateSortingParamsSpy = jest.spyOn(wrapper.vm, 'updateSortingParams');
                    const settings = JSON.parse(JSON.stringify(wrapper.vm.$data.settings));
                    settings.displayedColumns = ['col2', 'col3', 'col4'];
                    wrapper.vm.onViewSettingsChange({
                        data: { data: { view: settings } }
                    });
                    expect(updateSortingParamsSpy).toHaveBeenCalled();
                    expect(wrapper.vm.columnSortIndex).toEqual(1); // eslint-disable-line no-magic-numbers
                });
        
            it('does not update the sort parameters when no sorting is active', async () => {
                const updateSortingParamsSpy = jest.spyOn(wrapper.vm, 'updateSortingParams');
                await changeViewSetting(wrapper, 'displayedColumns', ['col2', 'col3', 'col4']);
                expect(updateSortingParamsSpy).not.toHaveBeenCalled();
            });

            it('resets the sort parameters when the sorted column gets removed', async () => {
                const resetSortingSpy = jest.spyOn(wrapper.vm, 'resetSorting');
                wrapper.vm.onColumnSort(2);
                await changeViewSetting(wrapper, 'displayedColumns', ['col1', 'col2', 'col4']);
                expect(resetSortingSpy).toHaveBeenCalled();
            });

            it('resets the sort parameters when the sorted row key column gets removed', async () => {
                const resetSortingSpy = jest.spyOn(wrapper.vm, 'resetSorting');
                await changeViewSetting(wrapper, 'showRowKeys', true);
                await wrapper.vm.onColumnSort(0);
                await changeViewSetting(wrapper, 'showRowKeys', false);

                expect(resetSortingSpy).toHaveBeenCalled();
                expect(wrapper.vm.columnSortColumnName).toEqual(null);
                expect(wrapper.vm.columnSortIndex).toEqual(null);
                expect(wrapper.vm.columnSortDirection).toEqual(null);
                expect(wrapper.vm.currentPage).toEqual(1);
                expect(wrapper.vm.currentIndex).toEqual(0);
            });

            test.each([
                ['showRowKeys', 2, 3], // eslint-disable-line no-magic-numbers
                ['showRowIndices', 2, 3] // eslint-disable-line no-magic-numbers
            ])('enabling viewSetting %p when sorting is active leads to incrementation of sortColIndex from %p to %p',
                async (settingsKey, colSortIndex, newColSortIndex) => {
                    wrapper.vm.onColumnSort(colSortIndex);
                    const updateSortingParamsSpy = jest.spyOn(wrapper.vm, 'updateSortingParams');
                    await changeViewSetting(wrapper, settingsKey, true);

                    expect(wrapper.vm.columnSortIndex).toEqual(newColSortIndex);
                    expect(updateSortingParamsSpy).toHaveBeenCalled();
                });

            test.each([
                ['showRowKeys', 4, 3], // eslint-disable-line no-magic-numbers
                ['showRowIndices', 4, 3] // eslint-disable-line no-magic-numbers
            ])('disabling viewSetting %p when sorting is active leads to decrementation of sortColIndex from %p to %p',
                async (settingsKey, colSortIndex, newColSortIndex) => {
                    await changeViewSetting(wrapper, settingsKey, true);

                    wrapper.vm.onColumnSort(colSortIndex);
                    const updateSortingParamsSpy = jest.spyOn(wrapper.vm, 'updateSortingParams');
                    await changeViewSetting(wrapper, settingsKey, false);

                    expect(wrapper.vm.columnSortIndex).toEqual(newColSortIndex);
                    expect(updateSortingParamsSpy).toHaveBeenCalled();
                });
        });
    });

    describe('sorting and pagination', () => {
        let wrapper, dataSpy;

        beforeEach(async () => {
            ({ wrapper, dataSpy } = await asyncMountTableView(context));
        });

        it('sets the correct parameters on next page and requests new data with updated parameters',
            () => {
                wrapper.vm.onPageChange(1);
                expect(dataSpy).toBeCalledWith({
                    method: 'getTable',
                    options: [initialDataMock.table.displayedColumns, 2, 2, null, false, false] // eslint-disable-line no-magic-numbers
                });
                expect(wrapper.vm.currentPage).toStrictEqual(2);
            });

        it('sets the correct parameters on prev page and requests new data with updated parameters', () => {
            wrapper.vm.onPageChange(1);
            wrapper.vm.onPageChange(-1);

            expect(dataSpy).toHaveBeenNthCalledWith(2, {
                method: 'getTable',
                options: [initialDataMock.table.displayedColumns, 0, 2, null, false, false] // eslint-disable-line no-magic-numbers
            });
            expect(wrapper.vm.currentPage).toStrictEqual(1);
        });

        it('disables sorting', async () => {
            initialDataMock.settings.enableSortingByHeader = false;
            const { wrapper } = await asyncMountTableView(context);

            const columnHeaders = wrapper.findAll('.column-header');
            columnHeaders.wrappers.forEach(colHeaderWrapper => {
                expect(colHeaderWrapper.element.classList.contains('sortable')).toBe(false);
            });
            const { tableConfig } = wrapper.vm.$children[0].$props;
            expect(tableConfig).toMatchObject({
                subMenuItems: false,
                pageConfig: {
                    currentSize: rowCount,
                    tableSize: rowCount,
                    pageSize: 2,
                    currentPage: 1
                }
            });
        });

        it('sorts descending on any new column and requests new data with updated parameters', () => {
            wrapper.vm.onColumnSort(0);
            expect(dataSpy).toBeCalledWith({
                method: 'getFilteredAndSortedTable',
                options: [initialDataMock.table.displayedColumns,
                    0, 2, 'col1', false, '', emptyColumnFilterValues, false, null, false, true] // eslint-disable-line no-magic-numbers
            });
            expect(wrapper.vm.currentPage).toStrictEqual(1);
        });

        it('sorts in the different direction when sorting the same column again', () => {
            wrapper.vm.onColumnSort(0);

            const dataSpyDesc = jest.spyOn(wrapper.vm.jsonDataService, 'data');
            wrapper.vm.onColumnSort(0);
            expect(dataSpyDesc).toBeCalledWith({
                method: 'getFilteredAndSortedTable',
                options: [initialDataMock.table.displayedColumns,
                    0, 2, 'col1', true, '', emptyColumnFilterValues, false, null, false, true] // eslint-disable-line no-magic-numbers
            });
            expect(wrapper.vm.currentPage).toStrictEqual(1);

            const dataSpyAsc = jest.spyOn(wrapper.vm.jsonDataService, 'data');
            wrapper.vm.onColumnSort(0);
            expect(dataSpyAsc).toBeCalledWith({
                method: 'getFilteredAndSortedTable',
                options: [initialDataMock.table.displayedColumns,
                    0, 2, 'col1', false, '', emptyColumnFilterValues, false, null, false, true] // eslint-disable-line no-magic-numbers
            });
        });

        it('requests new sorted data with updated parameters on changing page after sorting', () => {
            wrapper.vm.onColumnSort(0);
            wrapper.vm.onPageChange(1);

            expect(dataSpy).toHaveBeenNthCalledWith(2, {
                method: 'getFilteredAndSortedTable',
                options: [initialDataMock.table.displayedColumns,
                    2, 2, 'col1', false, '', emptyColumnFilterValues, false, null, false, false] // eslint-disable-line no-magic-numbers
            });
            expect(wrapper.vm.currentPage).toStrictEqual(2);
        });

        it('passes the correct parameters when sorting by rowKeys', async () => {
            initialDataMock.settings.showRowKeys = true;
            const { wrapper, dataSpy } = await asyncMountTableView(context);

            wrapper.vm.onColumnSort(0);

            expect(dataSpy).toHaveBeenCalledWith({
                method: 'getFilteredAndSortedTable',
                options: [initialDataMock.table.displayedColumns, 0, 2, '-1', false, '', emptyColumnFilterValues,
                    true, null, false, true] // eslint-disable-line no-magic-numbers
            });
        });
    });

    describe('selection', () => {
        it('resets the selection on clearSelection', async () => {
            const wrapper = await mount(TableView, context);
            await wrapper.vm.$nextTick();

            wrapper.vm.currentSelection = [false, false, true, false];
            expect(wrapper.vm.currentSelection).toEqual([false, false, true, false]);

            wrapper.vm.clearSelection();
            expect(wrapper.vm.currentSelection).toEqual([false, false, false, false]);
            expect(wrapper.vm.currentSelectedRowKeys).toEqual(new Set());
            expect(wrapper.vm.totalSelected).toEqual(0);
        });

        it('calls selectionService.onInit with correct parameters when mounting the view', async () => {
            const { wrapper } = await asyncMountTableView(context);
            const selectionServiceOnInitSpy = jest.spyOn(wrapper.vm.selectionService, 'onInit');

            const { publishSelection, subscribeToSelection } = initialDataMock.settings;
            expect(selectionServiceOnInitSpy).toHaveBeenCalledWith(expect.any(Function), publishSelection,
                subscribeToSelection);
        });

        describe('emit selection', () => {
            let wrapper, rowSelectSpy, selectAllSpy, publishOnSelectionChangeSpy;

            beforeEach(async () => {
                wrapper = await mount(TableView, context);
                rowSelectSpy = jest.spyOn(wrapper.vm, 'onRowSelect');
                selectAllSpy = jest.spyOn(wrapper.vm, 'onSelectAll');
                await wrapper.vm.$nextTick();
                await wrapper.vm.$nextTick();
                await wrapper.vm.$nextTick(); // needed three times
                publishOnSelectionChangeSpy = jest.spyOn(wrapper.vm.selectionService, 'publishOnSelectionChange');
            });

            it('calls the selection service and updates local selection on select single row',
                () => {
                    wrapper.vm.settings.publishSelection = true;

                    const tableRows = wrapper.findAll('table tbody .row');
                    const checkboxInput = tableRows.wrappers[1].find('input[type="checkbox"]');

                    // select row
                    checkboxInput.setChecked();
                    expect(rowSelectSpy).toHaveBeenCalledWith(true, 1, 0);

                    expect(publishOnSelectionChangeSpy).toHaveBeenCalledWith('add', ['row2']);
                    expect(wrapper.vm.currentSelection).toEqual([false, true]);
                    expect(wrapper.vm.totalSelected).toEqual(1);

                    // unselect row
                    checkboxInput.setChecked(false);
                    expect(rowSelectSpy).toHaveBeenCalledWith(false, 1, 0);
                    expect(publishOnSelectionChangeSpy).toHaveBeenCalledWith('remove', ['row2']);
                    expect(wrapper.vm.currentSelection).toEqual([false, false]);
                    expect(wrapper.vm.totalSelected).toEqual(0);
                });

            it('calls the selection service and updates local selection on selectAll rows',
                async () => {
                    wrapper.vm.settings.publishSelection = true;

                    const tableRows = wrapper.findAll('table .table-header');
                    const checkboxInput = tableRows.wrappers[0].find('input[type="checkbox"]');

                    // select row
                    checkboxInput.setChecked();
                    expect(selectAllSpy).toHaveBeenCalledWith(true);
                    expect(publishOnSelectionChangeSpy).toHaveBeenCalledWith(
                        'add', ['row1', 'row2']
                    );
                    expect(wrapper.vm.currentSelection).toEqual([true, true]);
                    expect(wrapper.vm.totalSelected).toEqual(initialDataMock.settings.pageSize);

                    await wrapper.vm.$nextTick();

                    // unselect row
                    checkboxInput.setChecked(false);
                    expect(selectAllSpy).toHaveBeenNthCalledWith(2, false);
                    expect(publishOnSelectionChangeSpy).toHaveBeenCalledWith(
                        'remove', ['row1', 'row2']
                    );
                    expect(wrapper.vm.currentSelection).toEqual([false, false]);
                    expect(wrapper.vm.totalSelected).toEqual(0);
                });
        });

        describe('receive selection', () => {
            let wrapper, rowKey1, rowKey2;

            beforeEach(async () => {
                wrapper = await mount(TableView, context);
                rowKey1 = initialDataMock.table.rows[0][0];
                rowKey2 = initialDataMock.table.rows[1][0];
            });

            it('updates the local selection on addSelection', async () => {
                wrapper.vm.onSelectionChange({ mode: 'ADD', selection: [rowKey2] });

                await wrapper.vm.$nextTick();

                expect(wrapper.vm.currentSelection).toEqual([false, true]);
                expect(wrapper.vm.totalSelected).toEqual(1);
                expect(wrapper.vm.currentSelectedRowKeys).toEqual(new Set([rowKey2]));
            });

            it('updates the local selection on removeSelection', () => {
                wrapper.vm.onSelectionChange({ mode: 'ADD', selection: [rowKey1, rowKey2] });
                wrapper.vm.onSelectionChange({ mode: 'REMOVE', selection: [rowKey2] });

                expect(wrapper.vm.currentSelection).toEqual([true, false]);
                expect(wrapper.vm.totalSelected).toEqual(1);
                expect(wrapper.vm.currentSelectedRowKeys).toEqual(new Set([rowKey1]));
            });

            it('updates the local selection on replace with subscribe to selection', () => {
                wrapper.vm.onSelectionChange({ mode: 'ADD', selection: [rowKey1] });
                wrapper.vm.onSelectionChange({ mode: 'REPLACE', selection: [rowKey2] });

                expect(wrapper.vm.currentSelection).toEqual([false, true]);
                expect(wrapper.vm.totalSelected).toEqual(1);
                expect(wrapper.vm.currentSelectedRowKeys).toEqual(new Set([rowKey2]));
            });

            it('calls selectionService.onSettingsChange with the correct parameters on settings change', () => {
                const selectionServiceOnSettingsChangeSpy = jest.spyOn(wrapper.vm.selectionService, 'onSettingsChange');
                const publishSelection = initialDataMock.settings.publishSelection;
                let subscribeToSelection = false;
                let viewSettings = { ...initialDataMock.settings, subscribeToSelection };
                wrapper.vm.onViewSettingsChange({
                    data: { data: { view: viewSettings } }
                });
                expect(selectionServiceOnSettingsChangeSpy).toHaveBeenCalledWith(expect.any(Function),
                    expect.any(Function), publishSelection, subscribeToSelection);
            });
        });
    });

    describe('global and column search', () => {
        let wrapper, dataSpy;

        beforeEach(async () => {
            wrapper = await mount(TableView, context);
            dataSpy = jest.spyOn(wrapper.vm.jsonDataService, 'data');
            await wrapper.vm.$nextTick();
            await wrapper.vm.$nextTick();
            await wrapper.vm.$nextTick(); // needed thrice
        });

        it('requests new data on column search', () => {
            const columnSearchTerm = 'entry1col1';
            wrapper.vm.onColumnFilter(0, columnSearchTerm);
            const columnFilterValues = [[''], [columnSearchTerm], [], [''], ['']];
            expect(dataSpy).toBeCalledWith({
                method: 'getFilteredAndSortedTable',
                options: [initialDataMock.table.displayedColumns,
                    0,
                    2, // eslint-disable-line no-magic-numbers
                    null,
                    false,
                    '',
                    columnFilterValues,
                    initialDataMock.settings.showRowKeys,
                    null,
                    false,
                    true]
            });
        });

        it('requests new data on global search', () => {
            const globalSearchTerm = 'entry1';
            wrapper.vm.onSearch(globalSearchTerm);
            expect(dataSpy).toBeCalledWith({
                method: 'getFilteredAndSortedTable',
                options: [initialDataMock.table.displayedColumns,
                    0,
                    2, // eslint-disable-line no-magic-numbers
                    null,
                    false,
                    globalSearchTerm,
                    emptyColumnFilterValues,
                    initialDataMock.settings.showRowKeys,
                    null,
                    false,
                    true]
            });
        });

        it('clears column filters', () => {
            const columnSearchTerm = 'entry1col1';
            wrapper.vm.onColumnFilter(0, columnSearchTerm);
            wrapper.vm.onClearFilter();
            expect(dataSpy).toHaveBeenNthCalledWith(2, {
                method: 'getTable',
                options: [initialDataMock.table.displayedColumns, 0, 2, null, false, true] // eslint-disable-line no-magic-numbers
            });
        });

        it('clears column filters on displayed columns change', async () => {
            const columnSearchTerm = 'entry1col1';
            wrapper.vm.onColumnFilter(0, columnSearchTerm);
            const settings = JSON.parse(JSON.stringify(wrapper.vm.$data.settings));
            settings.displayedColumns.push('missingCol');
            wrapper.vm.onViewSettingsChange({
                data: { data: { view: settings } }
            });
            expect(dataSpy).toHaveBeenNthCalledWith(2, {
                method: 'getFilteredAndSortedTable',
                options: [settings.displayedColumns,
                    0,
                    2, // eslint-disable-line no-magic-numbers
                    null,
                    false,
                    '',
                    null,
                    initialDataMock.settings.showRowKeys,
                    null,
                    true,
                    true]
            });
            await wrapper.vm.$nextTick();
            expect(wrapper.vm.columnFilters).toStrictEqual(
                wrapper.vm.getDefaultFilterConfigs(initialDataMock.table.displayedColumns)
            );
        });
    });

    describe('image rendering', () => {
        it('creates the correct source urls', async () => {
            const { getImageUrlSpy } = await asyncMountTableView(context);

            const imageIndex = 5;

            expect(getImageUrlSpy).toHaveBeenNthCalledWith(1, {
                data: {
                    row: ['entry1col1', 'entry1col2', '1', 'view_x_y/datacell/hash1.png'],
                    ind: 3,
                    key: 5,
                    rowInd: 0,
                    colInd: 3
                }
            }, imageIndex);
            expect(getImageUrlSpy).toHaveNthReturnedWith(1, 'http://localhost:8080/base.url/view_x_y/datacell/hash1.png');

            // eslint-disable-next-line no-magic-numbers
            expect(getImageUrlSpy).toHaveBeenNthCalledWith(3, {
                data: {
                    row: ['entry3col1', 'entry3col2', '3', 'view_x_y/datacell/hash3.png'],
                    ind: 3,
                    key: 5,
                    rowInd: 2,
                    colInd: 3
                }
            }, imageIndex);
            // eslint-disable-next-line no-magic-numbers
            expect(getImageUrlSpy).toHaveNthReturnedWith(3, 'http://localhost:8080/base.url/view_x_y/datacell/hash3.png');
        });

        it('uses the correct image source url for image slots', async () => {
            const { wrapper } = await asyncMountTableView(context);
            await wrapper.vm.$nextTick();
            const tableRows = wrapper.vm.$refs.tableUI.$refs.dynamicScroller[0].$refs.scroller.$children
                .map(item => item.$children[0]).slice(1);

            expect(tableRows[0].$refs.dataCell[3].innerHTML).toContain('src="http://localhost:8080/base.url/view_x_y/datacell/hash1.png"');
            expect(tableRows[1].$refs.dataCell[3].innerHTML).toContain('src="http://localhost:8080/base.url/view_x_y/datacell/hash2.png"');
            expect(tableRows[2].$refs.dataCell[3].innerHTML).toContain('src="http://localhost:8080/base.url/view_x_y/datacell/hash3.png"');
        });
    });

    describe('column resizing', () => {
        // TODO UIEXT-525 lets rethink these tests
        const getWrapperAndDefaultSizes = async (clientWidth, enableColumnSearch, publish, subscribe) => {
            let wrapper = await mount(TableView, context);
            await wrapper.vm.$nextTick();
            await wrapper.vm.$nextTick();
            await wrapper.vm.$nextTick(); // need three ticks for accessing child props
            wrapper.vm.settings.enableColumnSearch = enableColumnSearch;
            wrapper.vm.settings.publishSelection = publish;
            wrapper.vm.settings.subscribeToSelection = subscribe;
            wrapper.vm.settings.showRowKeys = true;
            wrapper.vm.settings.showRowIndices = true;

            wrapper.vm.clientWidth = clientWidth;
            
            const nColumns = wrapper.vm.numberOfDisplayedColumns;
            const specialColumnsSizeTotal = (enableColumnSearch ? SPECIAL_COLUMNS_SIZE : 0) +
                (publish || subscribe ? SPECIAL_COLUMNS_SIZE : 0);
            const dataColumnsSizeTotal = clientWidth - specialColumnsSizeTotal - nColumns * DATA_COLUMNS_MARGIN;
            const defaultColumnSize = Math.max(MIN_COLUMN_SIZE, dataColumnsSizeTotal / nColumns);

            const defaultColumnSizes = Array(nColumns).fill(defaultColumnSize);
            const lastColumnMinSize = dataColumnsSizeTotal -
                defaultColumnSizes.slice(0, nColumns - 1).reduce((sum, size) => sum + size, 0);
            defaultColumnSizes[nColumns - 1] = Math.max(lastColumnMinSize, defaultColumnSizes[nColumns - 1]);
            return { wrapper, defaultColumnSizes };
        };

        const clientWidth = 500;
        
        it('provides minimum column sizes for small client widths', async () => {
            const { wrapper, defaultColumnSizes } = await getWrapperAndDefaultSizes(0, false);
            expect(wrapper.vm.columnSizes).toStrictEqual(defaultColumnSizes);
        });

        it('provides expected column sizes for regular client widths', async () => {
            const { wrapper, defaultColumnSizes } = await getWrapperAndDefaultSizes(clientWidth, false, false, false);
            expect(wrapper.vm.columnSizes).toStrictEqual(defaultColumnSizes);
        });

        it('provides expected column sizes for regular client widths when column search is enabled', async () => {
            const { wrapper, defaultColumnSizes } = await getWrapperAndDefaultSizes(clientWidth, true, false, false);
            expect(wrapper.vm.columnSizes).toStrictEqual(defaultColumnSizes);
        });

        it('provides expected column sizes for regular client widths when publish is enabled', async () => {
            const { wrapper, defaultColumnSizes } = await getWrapperAndDefaultSizes(clientWidth, false, true, false);
            expect(wrapper.vm.columnSizes).toStrictEqual(defaultColumnSizes);
        });

        it('provides expected column sizes for regular client widths when subscribe is enabled', async () => {
            const { wrapper, defaultColumnSizes } = await getWrapperAndDefaultSizes(clientWidth, false, true, false);
            expect(wrapper.vm.columnSizes).toStrictEqual(defaultColumnSizes);
        });

        it('provides expected column sizes for regular client widths when publish and subscribe enabled', async () => {
            const { wrapper, defaultColumnSizes } = await getWrapperAndDefaultSizes(clientWidth, false, true, true);
            expect(wrapper.vm.columnSizes).toStrictEqual(defaultColumnSizes);
        });

        it('provides expected column sizes for empty tables', async () => {
            let wrapper = await mount(TableView, context);
            wrapper.vm.settings.showRowKeys = false;
            wrapper.vm.settings.showRowIndices = false;
            wrapper.vm.settings.displayedColumns = [];
            expect(wrapper.vm.columnSizes).toStrictEqual([]);
        });

        it('correctly overrides column sizes', async () => {
            const { wrapper, defaultColumnSizes } = await getWrapperAndDefaultSizes(clientWidth, true);
            wrapper.vm.onColumnResize(0, 1);
            wrapper.vm.onColumnResize(1, 2);
            wrapper.vm.onColumnResize(2, 1);
            defaultColumnSizes[defaultColumnSizes.length - 1] +=
                defaultColumnSizes[0] + defaultColumnSizes[1] + defaultColumnSizes[2] - 1 - 2 - 1;
            defaultColumnSizes[0] = 1;
            defaultColumnSizes[1] = 2;
            defaultColumnSizes[2] = 1;
            expect(wrapper.vm.columnSizes).toStrictEqual(defaultColumnSizes);
        });

        it('adds / removes intersection observer / resize listener and updates client width accordingly', async () => {
            const observe = jest.fn();
            const unobserve = jest.fn();
            window.IntersectionObserver = jest.fn(() => ({
                observe,
                unobserve
            }));
            const registerSpy = jest.spyOn(window, 'addEventListener');
            const removeSpy = jest.spyOn(window, 'removeEventListener');
            
            const wrapper = await mount(TableView, context);
            expect(wrapper.vm.clientWidth).toBe(0);
            expect(window.IntersectionObserver).toHaveBeenCalledTimes(1);
            expect(observe).toHaveBeenCalledTimes(1);
            expect(observe).toHaveBeenCalledWith(wrapper.vm.$el);

            let clientWidth = 100;
            const mockedEntries = [{
                target: null
            }, {
                target: wrapper.vm.$el,
                boundingClientRect: { width: 0 }
            }, {
                target: wrapper.vm.$el,
                boundingClientRect: { width: clientWidth }
            }];
            const [callback] = window.IntersectionObserver.mock.calls[0];
            callback(mockedEntries, window.IntersectionObserver.mock.results[0].value);
            expect(wrapper.vm.clientWidth).toBe(clientWidth);
            expect(unobserve).toHaveBeenCalledTimes(1);
            expect(unobserve).toHaveBeenCalledWith(wrapper.vm.$el);
            expect(registerSpy).toHaveBeenCalledTimes(1);
            expect(registerSpy).toHaveBeenCalledWith('resize', wrapper.vm.onResize);

            wrapper.vm.$el.getBoundingClientRect = function () {
                return { width: 0 };
            };
            window.dispatchEvent(new Event('resize'));
            expect(wrapper.vm.clientWidth).toBe(clientWidth);
            expect(removeSpy).toHaveBeenCalledTimes(1);
            expect(removeSpy).toHaveBeenCalledWith('resize', wrapper.vm.onResize);
            expect(window.IntersectionObserver).toHaveBeenCalledTimes(2);
            expect(observe).toHaveBeenCalledTimes(2);
            expect(observe).toHaveBeenLastCalledWith(wrapper.vm.$el);

            callback(mockedEntries, window.IntersectionObserver.mock.results[0].value);
            expect(wrapper.vm.clientWidth).toBe(clientWidth);
            expect(unobserve).toHaveBeenCalledTimes(2);
            expect(unobserve).toHaveBeenLastCalledWith(wrapper.vm.$el);
            expect(registerSpy).toHaveBeenCalledTimes(2);
            expect(registerSpy).toHaveBeenLastCalledWith('resize', wrapper.vm.onResize);

            clientWidth = 200;
            wrapper.vm.$el.getBoundingClientRect = function () {
                return { width: clientWidth };
            };
            window.dispatchEvent(new Event('resize'));
            expect(wrapper.vm.clientWidth).toBe(clientWidth);

            wrapper.destroy();
            expect(removeSpy).toHaveBeenCalledTimes(2);
            expect(removeSpy).toHaveBeenLastCalledWith('resize', wrapper.vm.onResize);
        });
    });
});
