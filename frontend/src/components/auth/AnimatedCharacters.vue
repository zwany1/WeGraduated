<script>
import { h, ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'

// ===== Pupil (照抄 careercompass animated-characters.tsx) =====
const Pupil = {
  props: {
    size: { type: Number, default: 12 },
    maxDistance: { type: Number, default: 5 },
    pupilColor: { type: String, default: 'black' },
    forceLookX: { type: Number, default: null },
    forceLookY: { type: Number, default: null }
  },
  setup(props) {
    const pupilRef = ref(null)
    const mouse = reactive({ x: 0, y: 0 })
    const onMove = (e) => { mouse.x = e.clientX; mouse.y = e.clientY }
    onMounted(() => window.addEventListener('mousemove', onMove))
    onBeforeUnmount(() => window.removeEventListener('mousemove', onMove))
    const pos = computed(() => {
      if (props.forceLookX != null && props.forceLookY != null) {
        return { x: props.forceLookX, y: props.forceLookY }
      }
      const el = pupilRef.value
      if (!el) return { x: 0, y: 0 }
      const r = el.getBoundingClientRect()
      const cx = r.left + r.width / 2
      const cy = r.top + r.height / 2
      const dx = mouse.x - cx
      const dy = mouse.y - cy
      const dist = Math.min(Math.sqrt(dx * dx + dy * dy), props.maxDistance)
      const angle = Math.atan2(dy, dx)
      return { x: Math.cos(angle) * dist, y: Math.sin(angle) * dist }
    })
    return () => h('div', {
      ref: pupilRef,
      class: 'rounded-full',
      style: {
        width: `${props.size}px`, height: `${props.size}px`,
        backgroundColor: props.pupilColor,
        transform: `translate(${pos.value.x}px, ${pos.value.y}px)`,
        transition: 'transform 0.1s ease-out'
      }
    })
  }
}

// ===== EyeBall (照抄 careercompass) =====
const EyeBall = {
  props: {
    size: { type: Number, default: 48 },
    pupilSize: { type: Number, default: 16 },
    maxDistance: { type: Number, default: 10 },
    eyeColor: { type: String, default: 'white' },
    pupilColor: { type: String, default: 'black' },
    isBlinking: { type: Boolean, default: false },
    forceLookX: { type: Number, default: null },
    forceLookY: { type: Number, default: null }
  },
  setup(props) {
    const eyeRef = ref(null)
    const mouse = reactive({ x: 0, y: 0 })
    const onMove = (e) => { mouse.x = e.clientX; mouse.y = e.clientY }
    onMounted(() => window.addEventListener('mousemove', onMove))
    onBeforeUnmount(() => window.removeEventListener('mousemove', onMove))
    const pos = computed(() => {
      if (props.forceLookX != null && props.forceLookY != null) {
        return { x: props.forceLookX, y: props.forceLookY }
      }
      const el = eyeRef.value
      if (!el) return { x: 0, y: 0 }
      const r = el.getBoundingClientRect()
      const cx = r.left + r.width / 2
      const cy = r.top + r.height / 2
      const dx = mouse.x - cx
      const dy = mouse.y - cy
      const dist = Math.min(Math.sqrt(dx * dx + dy * dy), props.maxDistance)
      const angle = Math.atan2(dy, dx)
      return { x: Math.cos(angle) * dist, y: Math.sin(angle) * dist }
    })
    return () => h('div', {
      ref: eyeRef,
      class: 'rounded-full flex items-center justify-center transition-all duration-150',
      style: {
        width: `${props.size}px`,
        height: props.isBlinking ? '2px' : `${props.size}px`,
        backgroundColor: props.eyeColor,
        overflow: 'hidden'
      }
    }, !props.isBlinking ? [h('div', {
      class: 'rounded-full',
      style: {
        width: `${props.pupilSize}px`, height: `${props.pupilSize}px`,
        backgroundColor: props.pupilColor,
        transform: `translate(${pos.value.x}px, ${pos.value.y}px)`,
        transition: 'transform 0.1s ease-out'
      }
    })] : [])
  }
}

// ===== AnimatedCharacters (照抄 careercompass) =====
export default {
  name: 'AnimatedCharacters',
  props: {
    isTyping: { type: Boolean, default: false },
    showPassword: { type: Boolean, default: false },
    passwordLength: { type: Number, default: 0 }
  },
  setup(props) {
    const mouse = reactive({ x: 0, y: 0 })
    const isPurpleBlinking = ref(false)
    const isBlackBlinking = ref(false)
    const isLookingAtEachOther = ref(false)
    const isPurplePeeking = ref(false)
    const purpleRef = ref(null)
    const blackRef = ref(null)
    const yellowRef = ref(null)
    const orangeRef = ref(null)

    const onMove = (e) => { mouse.x = e.clientX; mouse.y = e.clientY }
    onMounted(() => window.addEventListener('mousemove', onMove))
    onBeforeUnmount(() => window.removeEventListener('mousemove', onMove))

    onMounted(() => {
      const schedule = () => setTimeout(() => {
        isPurpleBlinking.value = true
        setTimeout(() => { isPurpleBlinking.value = false; schedule() }, 150)
      }, Math.random() * 4000 + 3000)
      schedule()
    })
    onMounted(() => {
      const schedule = () => setTimeout(() => {
        isBlackBlinking.value = true
        setTimeout(() => { isBlackBlinking.value = false; schedule() }, 150)
      }, Math.random() * 4000 + 3000)
      schedule()
    })

    let lookTimer = null
    watch(() => props.isTyping, (v) => {
      if (v) {
        isLookingAtEachOther.value = true
        clearTimeout(lookTimer)
        lookTimer = setTimeout(() => { isLookingAtEachOther.value = false }, 800)
      } else {
        isLookingAtEachOther.value = false
      }
    })

    let peekTimer = null
    watch([() => props.passwordLength, () => props.showPassword, isPurplePeeking], () => {
      if (props.passwordLength > 0 && props.showPassword) {
        clearTimeout(peekTimer)
        peekTimer = setTimeout(() => {
          isPurplePeeking.value = true
          setTimeout(() => { isPurplePeeking.value = false }, 800)
        }, Math.random() * 3000 + 2000)
      } else {
        isPurplePeeking.value = false
        clearTimeout(peekTimer)
      }
    })

    const calcPos = (refEl) => {
      if (!refEl.value) return { faceX: 0, faceY: 0, bodySkew: 0 }
      const r = refEl.value.getBoundingClientRect()
      const cx = r.left + r.width / 2
      const cy = r.top + r.height / 3
      const dx = mouse.x - cx
      const dy = mouse.y - cy
      return {
        faceX: Math.max(-15, Math.min(15, dx / 20)),
        faceY: Math.max(-10, Math.min(10, dy / 30)),
        bodySkew: Math.max(-6, Math.min(6, -dx / 120))
      }
    }

    const purplePos = computed(() => calcPos(purpleRef))
    const blackPos = computed(() => calcPos(blackRef))
    const yellowPos = computed(() => calcPos(yellowRef))
    const orangePos = computed(() => calcPos(orangeRef))

    const isHidingPassword = computed(() => props.passwordLength > 0 && !props.showPassword)
    const pwdVisible = computed(() => props.passwordLength > 0 && props.showPassword)

    const purpleStyle = computed(() => ({
      left: '70px', width: '180px',
      height: (props.isTyping || isHidingPassword.value) ? '440px' : '400px',
      backgroundColor: '#6C3FF5', borderRadius: '10px 10px 0 0', zIndex: 1,
      transform: pwdVisible.value ? 'skewX(0deg)'
        : (props.isTyping || isHidingPassword.value)
          ? `skewX(${(purplePos.value.bodySkew || 0) - 12}deg) translateX(40px)`
          : `skewX(${purplePos.value.bodySkew || 0}deg)`,
      transformOrigin: 'bottom center'
    }))
    const purpleEyesStyle = computed(() => ({
      left: pwdVisible.value ? '20px' : isLookingAtEachOther.value ? '55px' : `${45 + purplePos.value.faceX}px`,
      top: pwdVisible.value ? '35px' : isLookingAtEachOther.value ? '65px' : `${40 + purplePos.value.faceY}px`
    }))
    const purpleLookX = computed(() => pwdVisible.value ? (isPurplePeeking.value ? 4 : -4) : isLookingAtEachOther.value ? 3 : null)
    const purpleLookY = computed(() => pwdVisible.value ? (isPurplePeeking.value ? 5 : -4) : isLookingAtEachOther.value ? 4 : null)

    const blackStyle = computed(() => ({
      left: '240px', width: '120px', height: '310px',
      backgroundColor: '#2D2D2D', borderRadius: '8px 8px 0 0', zIndex: 2,
      transform: pwdVisible.value ? 'skewX(0deg)'
        : isLookingAtEachOther.value ? `skewX(${(blackPos.value.bodySkew || 0) * 1.5 + 10}deg) translateX(20px)`
          : (props.isTyping || isHidingPassword.value) ? `skewX(${(blackPos.value.bodySkew || 0) * 1.5}deg)`
            : `skewX(${blackPos.value.bodySkew || 0}deg)`,
      transformOrigin: 'bottom center'
    }))
    const blackEyesStyle = computed(() => ({
      left: pwdVisible.value ? '10px' : isLookingAtEachOther.value ? '32px' : `${26 + blackPos.value.faceX}px`,
      top: pwdVisible.value ? '28px' : isLookingAtEachOther.value ? '12px' : `${32 + blackPos.value.faceY}px`
    }))
    const blackLookX = computed(() => pwdVisible.value ? -4 : isLookingAtEachOther.value ? 0 : null)
    const blackLookY = computed(() => pwdVisible.value ? -4 : isLookingAtEachOther.value ? -4 : null)

    const orangeStyle = computed(() => ({
      left: '0px', width: '240px', height: '200px', zIndex: 3,
      backgroundColor: '#FF9B6B', borderRadius: '120px 120px 0 0',
      transform: pwdVisible.value ? 'skewX(0deg)' : `skewX(${orangePos.value.bodySkew || 0}deg)`,
      transformOrigin: 'bottom center'
    }))
    const orangeEyesStyle = computed(() => ({
      left: pwdVisible.value ? '50px' : `${82 + (orangePos.value.faceX || 0)}px`,
      top: pwdVisible.value ? '85px' : `${90 + (orangePos.value.faceY || 0)}px`
    }))
    const orangeLookX = computed(() => pwdVisible.value ? -5 : null)
    const orangeLookY = computed(() => pwdVisible.value ? -4 : null)

    const yellowStyle = computed(() => ({
      left: '310px', width: '140px', height: '230px',
      backgroundColor: '#E8D754', borderRadius: '70px 70px 0 0', zIndex: 4,
      transform: pwdVisible.value ? 'skewX(0deg)' : `skewX(${yellowPos.value.bodySkew || 0}deg)`,
      transformOrigin: 'bottom center'
    }))
    const yellowEyesStyle = computed(() => ({
      left: pwdVisible.value ? '20px' : `${52 + (yellowPos.value.faceX || 0)}px`,
      top: pwdVisible.value ? '35px' : `${40 + (yellowPos.value.faceY || 0)}px`
    }))
    const yellowLookX = computed(() => pwdVisible.value ? -5 : null)
    const yellowLookY = computed(() => pwdVisible.value ? -4 : null)
    const yellowMouthStyle = computed(() => ({
      left: pwdVisible.value ? '10px' : `${40 + (yellowPos.value.faceX || 0)}px`,
      top: pwdVisible.value ? '88px' : `${88 + (yellowPos.value.faceY || 0)}px`
    }))

    const eyeBall = (s, ps, md, blink, lx, ly) => h(EyeBall, {
      size: s, pupilSize: ps, maxDistance: md, eyeColor: 'white', pupilColor: '#2D2D2D',
      isBlinking: blink, forceLookX: lx, forceLookY: ly
    })
    const pupil = (lx, ly) => h(Pupil, { size: 12, maxDistance: 5, pupilColor: '#2D2D2D', forceLookX: lx, forceLookY: ly })

    return () => h('div', { class: 'relative', style: { width: '550px', height: '400px' } }, [
      h('div', { ref: purpleRef, class: 'absolute bottom-0 transition-all duration-700 ease-in-out', style: purpleStyle.value }, [
        h('div', { class: 'absolute flex gap-8 transition-all duration-700 ease-in-out', style: purpleEyesStyle.value }, [
          eyeBall(18, 7, 5, isPurpleBlinking.value, purpleLookX.value, purpleLookY.value),
          eyeBall(18, 7, 5, isPurpleBlinking.value, purpleLookX.value, purpleLookY.value)
        ])
      ]),
      h('div', { ref: blackRef, class: 'absolute bottom-0 transition-all duration-700 ease-in-out', style: blackStyle.value }, [
        h('div', { class: 'absolute flex gap-6 transition-all duration-700 ease-in-out', style: blackEyesStyle.value }, [
          eyeBall(16, 6, 4, isBlackBlinking.value, blackLookX.value, blackLookY.value),
          eyeBall(16, 6, 4, isBlackBlinking.value, blackLookX.value, blackLookY.value)
        ])
      ]),
      h('div', { ref: orangeRef, class: 'absolute bottom-0 transition-all duration-700 ease-in-out', style: orangeStyle.value }, [
        h('div', { class: 'absolute flex gap-8 transition-all duration-200 ease-out', style: orangeEyesStyle.value }, [
          pupil(orangeLookX.value, orangeLookY.value),
          pupil(orangeLookX.value, orangeLookY.value)
        ])
      ]),
      h('div', { ref: yellowRef, class: 'absolute bottom-0 transition-all duration-700 ease-in-out', style: yellowStyle.value }, [
        h('div', { class: 'absolute flex gap-6 transition-all duration-200 ease-out', style: yellowEyesStyle.value }, [
          pupil(yellowLookX.value, yellowLookY.value),
          pupil(yellowLookX.value, yellowLookY.value)
        ]),
        h('div', { class: 'absolute w-20 h-[4px] bg-[#2D2D2D] rounded-full transition-all duration-200 ease-out', style: yellowMouthStyle.value })
      ])
    ])
  }
}
</script>
