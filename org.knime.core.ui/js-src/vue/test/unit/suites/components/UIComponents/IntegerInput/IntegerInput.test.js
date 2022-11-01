import { mountJsonFormsComponent, initializesJsonFormsControl } from '~/test/unit/suites/utils/jsonFormsTestUtils';
import IntegerInput from '@/components/UIComponents/IntegerInput.vue';
import NumberInputBase from '@/components/UIComponents/NumberInputBase.vue';
import ErrorMessage from '@/components/UIComponents/ErrorMessage.vue';

describe('IntegerInput.vue', () => {
    const defaultPropsData = {
        path: '',
        control: {
            visible: true,
            schema: {
                properties: {
                    maxRows: {
                        type: 'integer',
                        title: 'Show tooltip'
                    }
                }
            },
            uischema: {
                type: 'Control',
                scope: '#/properties/view/properties/maxRows',
                options: {
                    format: 'integer'
                }
            }
        }
    };

    let wrapper;

    beforeEach(async () => {
        wrapper = await mountJsonFormsComponent(IntegerInput, defaultPropsData);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });
    
    it('renders', () => {
        expect(wrapper.getComponent(IntegerInput).exists()).toBe(true);
        expect(wrapper.getComponent(NumberInputBase).exists()).toBe(true);
        expect(wrapper.getComponent(IntegerInput).getComponent(ErrorMessage).exists()).toBe(true);
    });
   
    it('passes default props', () => {
        const numberInputProps = wrapper.getComponent(NumberInputBase).props();
        expect(numberInputProps.type).toBe('integer');
    });

    it('initializes jsonforms on pass-through component', () => {
        initializesJsonFormsControl(wrapper.getComponent(NumberInputBase));
    });
});
