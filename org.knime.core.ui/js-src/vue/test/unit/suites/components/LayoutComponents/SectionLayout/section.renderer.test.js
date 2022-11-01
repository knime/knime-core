import { vanillaRenderers } from '@jsonforms/vue2-vanilla';
import { fallbackRenderers, defaultRenderers } from '@/components/renderers';
import { determineRenderer } from '~/test/unit/suites/utils/rendererTestUtils';

const renderers = [...vanillaRenderers, ...fallbackRenderers, ...defaultRenderers];

describe('Section', () => {
    const schema = {};

    it('Empty SectionLayout', () => {
        const uiSchema = {
            type: 'Section',
            scope: '#/properties/test'
        };

        expect(determineRenderer(uiSchema, schema, renderers)).toBe('SectionLayout');
    });
});
