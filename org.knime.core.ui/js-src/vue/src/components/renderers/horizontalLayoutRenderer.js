import { rankWith, uiTypeIs } from '@jsonforms/core';
import HorizontalLayout from '@/components/LayoutComponents/HorizontalLayout.vue';
import { priorityRanks } from '@/constants';

export const horizontalLayoutTester = uiTypeIs('HorizontalLayout');

export const horizontalLayoutRenderer = {
    renderer: HorizontalLayout,
    tester: rankWith(priorityRanks.default, horizontalLayoutTester)
};
