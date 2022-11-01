import { vanillaRenderers } from '@jsonforms/vue2-vanilla';
import { fallbackRenderers, defaultRenderers } from '@/components/renderers';
import { determineRenderer } from '~/test/unit/suites/utils/rendererTestUtils';

const renderers = [...vanillaRenderers, ...fallbackRenderers, ...defaultRenderers];

describe('ColumnFilter', () => {
    const schema = {
        type: 'object',
        properties: {
            columnFilter: {
                anyOf: [
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

    it('ColumnFilter with options', () => {
        const uiSchema = {
            type: 'Control',
            scope: '#/properties/columnFilter',
            options: {
                format: 'columnFilter'
            }
        };
        
        expect(determineRenderer(uiSchema, schema, renderers)).toBe('ColumnFilter');
    });

    it('ColumnFilter without options uses anyOf fallback', () => {
        const uiSchema = {
            type: 'Control',
            scope: '#/properties/columnFilter'
        };

        expect(determineRenderer(uiSchema, schema, renderers)).toBe('TwinListInput');
    });
});
