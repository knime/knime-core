import { mountJsonFormsComponent, initializesJsonFormsLayout } from '~/test/unit/suites/utils/jsonFormsTestUtils';
import SectionLayout from '@/components/LayoutComponents/SectionLayout.vue';

describe('SectionLayout.vue', () => {
    const defaultPropsData = {
        layout: {
            cells: [],
            data: {
                view: {
                    xAxisLabel: 'xAxisLabel'
                }
            },
            path: '',
            schema: {
                properties: {
                    xAxisLabel: {
                        type: 'string',
                        title: 'X Axis Label'
                    }
                }
            },
            uischema: {
                type: 'Section',
                label: 'Interactivity',
                elements: [
                    {
                        type: 'Control',
                        scope: '#/properties/view/properties/xAxisLabel'
                    }
                ]
            }
        }
    };

    let wrapper;

    beforeEach(async () => {
        wrapper = await mountJsonFormsComponent(SectionLayout, defaultPropsData);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders', () => {
        expect(wrapper.getComponent(SectionLayout).exists()).toBe(true);
    });
    
    it('initializes jsonforms', () => {
        initializesJsonFormsLayout(wrapper);
    });
});
