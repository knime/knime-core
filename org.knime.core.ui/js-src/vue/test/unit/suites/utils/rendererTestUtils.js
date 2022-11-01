/* Utility function to mimic the behaviour of JSONForms determining a renderer component out of a set of
 * renderers, given a certain schema and uischema.
 * The functions below have mainly been copied directly from JSONForms v. 2.5.2 and adapted slightly to be usable
 * for tests. */
import { vanillaRenderers } from '@jsonforms/vue2-vanilla';
import { fallbackRenderers, defaultRenderers } from '@/components/renderers';

const maxBy = require('lodash/maxBy');
const _interopDefaultLegacy = (e) => e && typeof e === 'object' && 'default' in e ? e : { default: e };
const maxByDefault = _interopDefaultLegacy(maxBy);

const renderers = [...vanillaRenderers, ...fallbackRenderers, ...defaultRenderers];

const findRenderer = (uiSchema, schema) => {
    const renderer = maxByDefault.default(renderers, (r) => r.tester(uiSchema, schema));

    if (renderer === undefined || renderer.tester(uiSchema, schema) === -1) {
        return {};
    } else {
        return renderer.renderer;
    }
};

export const determineRenderer = (uiSchema, schema) => {
    const renderer = findRenderer(uiSchema, schema);
    return renderer.name;
};
