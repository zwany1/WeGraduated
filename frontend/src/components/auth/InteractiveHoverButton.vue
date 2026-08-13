<script>
import { h } from 'vue'

export default {
  name: 'InteractiveHoverButton',
  props: {
    type: { type: String, default: 'button' },
    text: { type: String, default: 'Button' },
    disabled: { type: Boolean, default: false }
  },
  setup(props, { attrs }) {
    return () => h('button', {
      type: props.type,
      disabled: props.disabled,
      class: ['group relative w-full cursor-pointer overflow-hidden rounded-full border bg-background px-6 py-2 text-center font-semibold', attrs.class],
      style: 'height: 48px'
    }, [
      h('span', { class: 'inline-block transition-all duration-300 group-hover:translate-x-12 group-hover:opacity-0' }, props.text),
      h('div', { class: 'absolute inset-0 z-10 flex items-center justify-center gap-2 bg-primary text-primary-foreground opacity-0 transition-all duration-300 group-hover:opacity-100 rounded-full' }, [
        h('span', {}, props.text),
        h('svg', { viewBox: '0 0 24 24', class: 'h-4 w-4', fill: 'none', stroke: 'currentColor', strokeWidth: '2', strokeLinecap: 'round', strokeLinejoin: 'round' }, [h('path', { d: 'M5 12h14M12 5l7 7-7 7' })])
      ])
    ])
  }
}
</script>
