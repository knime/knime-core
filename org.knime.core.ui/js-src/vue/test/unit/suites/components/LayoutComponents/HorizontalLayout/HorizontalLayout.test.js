import { mountJsonFormsComponent, initializesJsonFormsLayout } from '~/test/unit/suites/utils/jsonFormsTestUtils';
import HorizontalLayout from '@/components/LayoutComponents/HorizontalLayout';

describe('HorizontalLayout.vue', () => {
    const defaultPropsData = {
        layout: {
            cells: [],
            path: 'view.referenceLines',
            schema: {
                properties: {
                    size: {
                        type: 'integer',
                        title: 'Size'
                    }
                }
            },
            uischema: {
                type: 'HorizontalLayout',
                elements: [
                    {
                        type: 'Control',
                        scope: '#/properties/size'
                    }
                ]
            }
        }
    };

    let wrapper;

    beforeEach(async () => {
        wrapper = await mountJsonFormsComponent(HorizontalLayout, defaultPropsData);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders', () => {
        expect(wrapper.getComponent(HorizontalLayout).exists()).toBe(true);
    });
    
    it('initializes jsonforms', () => {
        initializesJsonFormsLayout(wrapper);
    });
});
