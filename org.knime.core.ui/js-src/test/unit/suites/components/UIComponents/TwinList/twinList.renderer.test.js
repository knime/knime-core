import { vanillaRenderers } from '@jsonforms/vue2-vanilla';
import { fallbackRenderers, defaultRenderers } from '@/components/renderers';
import { determineRenderer } from '~/test/unit/suites/utils/rendererTestUtils';

const renderers = [...vanillaRenderers, ...fallbackRenderers, ...defaultRenderers];

describe('TwinlistInput', () => {
    const schema = {
        type: 'object',
        properties: {
            twinlist: {
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

    it('TwinListInput config error', () => {
        const uiSchema = {
            type: 'Control',
            scope: '#/properties/twinlist',
            options: {
                format: 'integer'
            }
        };

        expect(determineRenderer(uiSchema, schema, renderers)).toBe('TwinListInput');
    });

    it('TwinListInput with options', () => {
        const uiSchema = {
            type: 'Control',
            scope: '#/properties/twinlist',
            options: {
                format: 'twinList'
            }
        };
        
        expect(determineRenderer(uiSchema, schema, renderers)).toBe('TwinListInput');
    });

    it('TwinListInput without options uses oneOf fallback', () => {
        const uiSchema = {
            type: 'Control',
            scope: '#/properties/twinlist'
        };

        expect(determineRenderer(uiSchema, schema, renderers)).toBe('TwinListInput');
    });
});
