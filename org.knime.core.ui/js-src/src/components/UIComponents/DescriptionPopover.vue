<script>
import FunctionButton from '~/webapps-common/ui/components/FunctionButton.vue';
import Description from '~/webapps-common/ui/components/Description.vue';
import DescriptionIcon from '~/webapps-common/ui/assets/img/icons/circle-help.svg?inline';
import { mixin as clickaway } from 'vue-clickaway2';


export default {
    components: {
        FunctionButton,
        Description,
        DescriptionIcon
    },
    mixins: [clickaway],
    props: {
        html: {
            default: null,
            type: String
        },
        hover: {
            default: false,
            type: Boolean
        }
    },
    data() {
        return {
            orientation: 'above',
            expanded: false
        };
    },
    watch: {
        expanded(value) {
            if (value) {
                this.updateOrientation();
            }
        }
    },
    methods: {
        toggle() {
            this.expanded = !this.expanded;
        },
        close() {
            this.expanded = false;
            // emit event to notify parent to set the hover prop to false
            this.$emit('close');
        },
        closeUnlessHover() {
            if (!this.hover) {
                this.close();
            }
        },
        async updateOrientation() {
            this.orientation = 'above'; // by default, render the popover's content box above the help button
            await this.$nextTick(); // to wait until the DOM is updated
            const top = this.$refs.box.getBoundingClientRect()?.top;

            // if the content box is off-screen, render it below the help button
            // this does not work in the standalone app (since its dialog starts at some y > 0), but it works in the AP
            if (top < 0) {
                this.orientation = 'below';
            }
        }
    }
};
</script>

<template>
  <div
    v-show="expanded || hover"
    v-on-clickaway="closeUnlessHover"
    class="popover"
  >
    <!-- use mouseup instead of click as the click event fires twice on key input in Firefox-->
    <FunctionButton
      title="Click for more information"
      class="button"
      :active="expanded"
      @mouseup.native.stop="toggle"
      @keydown.native.space.stop="toggle"
      @keydown.native.esc.stop="close"
    >
      <DescriptionIcon />
    </FunctionButton>
    <div
      v-if="expanded"
      ref="box"
      :class="['box', orientation]"
    >
      <Description
        :text="html"
        render-as-html
        class="content"
      />
    </div>
  </div>
</template>

<style lang="postcss" scoped>
.popover {
  display: flex;
  justify-content: flex-end;
  pointer-events: none;
  width: 100%;
  position: absolute;
  top: 0;

  --vertical-margin: 3px; /* vertical margin between button / icon and popover box */
  --popover-oversize: 10px; /* oversize to the left and right of the content box */

  & .button {
    pointer-events: auto;
    width: var(--description-button-size);
    height: var(--description-button-size);
    padding: 0;

    & svg {
      width: var(--description-button-size);
      height: var(--description-button-size);
    }
  }

  & .box {
    z-index: 3; /* stack expanded popover on top of dialog */
    width: calc(100% + 2 * var(--popover-oversize));
    position: absolute;
    right: calc(-1 * var(--popover-oversize));
    background: var(--knime-white);
    box-shadow: 0 2px 10px 0 var(--knime-gray-dark-semi);

    &::after { /* selector for the arrow between the description button and the content box */
      position: absolute;
      right: var(--popover-oversize);
      content: "";
      border-left: calc(0.5 * var(--description-button-size)) solid transparent;
      border-right: calc(0.5 * var(--description-button-size)) solid transparent;
    }

    &.above {
      bottom: calc(100% + calc(var(--vertical-margin) + 0.5 * var(--description-button-size)));

      &::after {
        top: 100%;
        border-top: calc(0.5 * var(--description-button-size)) solid var(--knime-white);
      }
    }

    &.below {
      top: calc(100% + calc(var(--vertical-margin) + 0.5 * var(--description-button-size)));

      &::after {
        bottom: 100%;
        border-bottom: calc(0.5 * var(--description-button-size)) solid var(--knime-white);
      }
    }

    & .content {
      max-height: 300px;
      overflow: auto;
      padding: 15px;
      font-size: 13px;
      line-height: 18.78px; /* Description component line-height-to-font-size-ratio of 26/18 times font size of 13 */
      color: var(--knime-masala);
    }
  }
}
</style>
