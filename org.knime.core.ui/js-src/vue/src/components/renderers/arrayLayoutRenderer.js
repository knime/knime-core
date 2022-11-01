import { isObjectArray, rankWith } from '@jsonforms/core';
import ArrayLayout from '@/components/LayoutComponents/ArrayLayout.vue';
import { priorityRanks } from '@/constants';

export const arrayLayoutTester = isObjectArray;
export const arrayLayoutRenderer = {
    renderer: ArrayLayout,
    tester: rankWith(priorityRanks.default, arrayLayoutTester)
};
