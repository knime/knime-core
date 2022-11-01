import { rankWith, isAnyOfControl } from '@jsonforms/core';
import TwinlistInput from '@/components/UIComponents/TwinlistInput.vue';
import { priorityRanks, inputFormats } from '@/constants';

export const twinlistTester = (uischema, schema) => {
    const isAnyOf = isAnyOfControl(uischema, schema);
    return isAnyOf && uischema.options?.format === inputFormats.anyOfTwinList;
};

export const twinlistRenderer = {
    renderer: TwinlistInput,
    tester: rankWith(priorityRanks.default, twinlistTester)
};
