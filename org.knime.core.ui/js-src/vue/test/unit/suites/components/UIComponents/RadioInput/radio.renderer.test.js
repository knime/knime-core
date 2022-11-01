import { vanillaRenderers } from '@jsonforms/vue2-vanilla';
import { fallbackRenderers, defaultRenderers } from '@/components/renderers';
import { determineRenderer } from '~/test/unit/suites/utils/rendererTestUtils';

const renderers = [...vanillaRenderers, ...fallbackRenderers, ...defaultRenderers];

describe('RadioInput', () => {
    const schema = {
        type: 'object',
        properties: {
            radio: {
                oneOf: [
                    {
                        const: '1',
                        title: 'One'
                    },
                    {
                        const: '2',
                        title: 'Two'
                    }
                ]
            }
        }
    };

    it('RadioInput config error', () => {
        const uiSchema = {
            type: 'Control',
            scope: '#/properties/radio',
            options: {
                format: 'integer'
            }
        };

        expect(determineRenderer(uiSchema, schema, renderers)).toBe('DropdownInput');
    });

    it('RadioInput with options', () => {
        const uiSchema = {
            type: 'Control',
            scope: '#/properties/radio',
            options: {
                format: 'radio'
            }
        };
        
        expect(determineRenderer(uiSchema, schema, renderers)).toBe('RadioInput');
    });

    it('RadioInput without options', () => {
        const uiSchema = {
            type: 'Control',
            scope: '#/properties/radio'
        };

        expect(determineRenderer(uiSchema, schema, renderers)).toBe('DropdownInput');
    });
});
