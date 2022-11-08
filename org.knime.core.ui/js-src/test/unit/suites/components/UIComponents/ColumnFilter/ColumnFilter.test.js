import { mountJsonFormsComponent, initializesJsonFormsControl } from '~/test/unit/suites/utils/jsonFormsTestUtils';
import ColumnFilter from '@/components/UIComponents/ColumnFilter.vue';
import TwinlistInput from '@/components/UIComponents/TwinlistInput.vue';


describe('ColumnFilter.vue', () => {
    const defaultPropsData = {
        path: '',
        control: {
            schema: {
                anyOf: [{
                    const: 'test_1',
                    title: 'test_1'
                },
                {
                    const: 'test_2',
                    title: 'test_2'
                },
                {
                    const: 'test_3',
                    title: 'test_3'
                }]
            },
            uischema: {
                type: 'Control',
                scope: '#/properties/xAxis',
                options: {
                    format: 'columnFilter'
                }
            }
        }
    };

    let wrapper;

    beforeEach(async () => {
        wrapper = await mountJsonFormsComponent(ColumnFilter, defaultPropsData);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders', () => {
        expect(wrapper.getComponent(ColumnFilter).exists()).toBe(true);
        expect(wrapper.getComponent(TwinlistInput).exists()).toBe(true);
    });

    it('passes default props', () => {
        const twinListProps = wrapper.getComponent(TwinlistInput).props();
        expect(twinListProps.twinlistLabelLeft).toBe('Excludes');
        expect(twinListProps.twinlistLabelRight).toBe('Includes');
    });

    it('initializes jsonforms on pass-through component', () => {
        initializesJsonFormsControl(wrapper.getComponent(TwinlistInput));
    });
});
