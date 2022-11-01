import { vanillaRenderers } from '@jsonforms/vue2-vanilla';
import { fallbackRenderers, defaultRenderers } from '@/components/renderers';
import { determineRenderer } from '~/test/unit/suites/utils/rendererTestUtils';

const renderers = [...vanillaRenderers, ...fallbackRenderers, ...defaultRenderers];

describe('ColumnSelect', () => {
    const schema = {
        type: 'object',
        properties: {
            columnselect: {
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

    it('ColumnSelect with options', () => {
        const uiSchema = {
            type: 'Control',
            scope: '#/properties/columnselect',
            options: {
                format: 'columnSelection',
                showRowKeys: false,
                showNoneColumn: false
            }
        };
        
        expect(determineRenderer(uiSchema, schema, renderers)).toBe('ColumnSelect');
    });

    it('ColumnSelect without options', () => {
        const uiSchema = {
            type: 'Control',
            scope: '#/properties/columnselect'
        };

        expect(determineRenderer(uiSchema, schema, renderers)).toBe('DropdownInput');
    });
});
