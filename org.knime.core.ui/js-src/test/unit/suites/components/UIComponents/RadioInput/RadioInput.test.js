import { mountJsonFormsComponent } from '~/test/unit/suites/utils/jsonFormsTestUtils';
import RadioInput from '@/components/UIComponents/RadioInput.vue';
import RadioInputBase from '@/components/UIComponents/RadioInputBase.vue';
import LabeledInput from '@/components/UIComponents/LabeledInput.vue';
import RadioButtons from '~/webapps-common/ui/components/forms/RadioButtons.vue';
import BaseRadioButtons from '~/webapps-common/ui/components/forms/BaseRadioButtons.vue';

describe('RadioInput.vue', () => {
    const defaultPropsData = {
        control: {
            path: 'test',
            enabled: true,
            visible: true,
            label: 'defaultLabel',
            data: 'LOG',
            schema: {
                oneOf: [
                    {
                        const: 'LOG',
                        title: 'Logarithmic'
                    },
                    {
                        const: 'VALUE',
                        title: 'Linear'
                    }
                ]
            },
            uischema: {
                type: 'Control',
                scope: '#/properties/yAxisScale',
                options: {
                    format: 'radio',
                    radioLayout: 'horizontal'
                }
            },
            rootSchema: {
                hasNodeView: true,
                flowVariablesMap: {}
            }
        }
    };

    let wrapper;
    
    beforeEach(async () => {
        wrapper = await mountJsonFormsComponent(RadioInput, defaultPropsData);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders', () => {
        expect(wrapper.getComponent(RadioInput).exists()).toBe(true);
        expect(wrapper.getComponent(RadioInputBase).exists()).toBe(true);
        expect(wrapper.findComponent(LabeledInput).exists()).toBe(true);
        expect(wrapper.findComponent(RadioButtons).exists()).toBe(true);
        expect(wrapper.findComponent(BaseRadioButtons).exists()).toBe(true);
    });

    it('sets correct type prop', () => {
        expect(wrapper.findComponent(RadioInputBase).props().type).toBe('radio');
    });
});
