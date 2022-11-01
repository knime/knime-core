import { rankWith, isStringControl } from '@jsonforms/core';
import { priorityRanks } from '@/constants';
import TextInput from '@/components/UIComponents/TextInput.vue';

export const textTester = isStringControl;

export const textRenderer = {
    renderer: TextInput,
    tester: rankWith(priorityRanks.default, textTester)
};
