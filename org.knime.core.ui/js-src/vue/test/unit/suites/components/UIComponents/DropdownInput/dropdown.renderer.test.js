import { vanillaRenderers } from '@jsonforms/vue2-vanilla';
import { fallbackRenderers, defaultRenderers } from '@/components/renderers';
import { determineRenderer } from '~/test/unit/suites/utils/rendererTestUtils';

const renderers = [...vanillaRenderers, ...fallbackRenderers, ...defaultRenderers];

describe('Dropdown', () => {
    const schema = {
        type: 'object',
        properties: {
            dropdown: {
                oneOf: [
                    {
                        const: '1',
                        title: 'One'
                    },
                    {
                        const: '2',
                        title: 'Two'
                    },
                    {
                        const: '3',
                        title: 'Three'
                    }
                ]
            }
        }
    };

    it('renders DropdownInput', () => {
        const uiSchema = {
            type: 'Control',
            scope: '#/properties/dropdown',
            options: {
                format: 'dropDown'
            }
        };
        
        expect(determineRenderer(uiSchema, schema, renderers)).toBe('DropdownInput');
    });
});
