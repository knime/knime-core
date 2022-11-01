import { mountJsonFormsComponent, initializesJsonFormsControl } from '~/test/unit/suites/utils/jsonFormsTestUtils';
import ColumnSelect from '@/components/UIComponents/ColumnSelect.vue';
import DropdownInput from '@/components/UIComponents/DropdownInput.vue';

describe('ColumnSelect.vue', () => {
    const defaultPropsData = {
        path: '',
        control: {
            label: 'Column Selection',
            schema: {
                oneOf: [
                    {
                        const: 'Universe_0_0',
                        title: 'Universe_0_0'
                    },
                    {
                        const: 'Universe_0_1',
                        title: 'Universe_0_1'
                    },
                    {
                        const: 'Universe_1_0',
                        title: 'Universe_1_0'
                    },
                    {
                        const: 'Universe_1_1',
                        title: 'Universe_1_1'
                    }
                ],
                title: 'Y Axis Column'
            },
            uischema: {
                type: 'Control',
                scope: '#/properties/view/properties/yAxisColumn',
                options: {
                    format: 'columnSelection',
                    showRowKeys: false,
                    showNoneColumn: false
                }
            }
        }
    };

    let wrapper;

    beforeEach(async () => {
        wrapper = await mountJsonFormsComponent(ColumnSelect, defaultPropsData);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders', () => {
        expect(wrapper.getComponent(ColumnSelect).exists()).toBe(true);
        expect(wrapper.getComponent(DropdownInput).exists()).toBe(true);
    });

    it('passes default props', () => {
        const dropdownProps = wrapper.getComponent(DropdownInput).props();
        expect(dropdownProps.optionsGenerator).toBe(wrapper.vm.optionsGenerator);
    });

    it('initializes jsonforms on pass-through component', () => {
        initializesJsonFormsControl(wrapper.getComponent(DropdownInput));
    });

    it('optionsGenerator correctly transforms the data', async () => {
        await wrapper.vm.$nextTick();

        expect(wrapper.getComponent(ColumnSelect).vm.optionsGenerator(defaultPropsData.control)).toEqual(
            [{
                id: 'Universe_0_0',
                text: 'Universe_0_0'
            },
            {
                id: 'Universe_0_1',
                text: 'Universe_0_1'
            },
            {
                id: 'Universe_1_0',
                text: 'Universe_1_0'
            },
            {
                id: 'Universe_1_1',
                text: 'Universe_1_1'
            }]
        );
    });

    it('optionsGenerator correctly transforms the data with none column and row keys', async () => {
        await wrapper.vm.$nextTick();
        const localDefaultPropsData = { ...defaultPropsData };
        localDefaultPropsData.control.uischema.options.showNoneColumn = true;
        localDefaultPropsData.control.uischema.options.showRowKeys = true;
        
        const tmp = wrapper.getComponent(ColumnSelect).vm.optionsGenerator(localDefaultPropsData.control);
        expect(tmp).toEqual(
            [{
                id: '<none>',
                text: 'None'
            },
            {
                id: '<row-keys>',
                text: 'Row keys'
            },
            {
                id: 'Universe_0_0',
                text: 'Universe_0_0'
            },
            {
                id: 'Universe_0_1',
                text: 'Universe_0_1'
            },
            {
                id: 'Universe_1_0',
                text: 'Universe_1_0'
            },
            {
                id: 'Universe_1_1',
                text: 'Universe_1_1'
            }]
        );
    });
});
