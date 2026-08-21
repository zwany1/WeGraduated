/** 返回上一页, 无浏览历史时回首页 */
export function backOrHome(router) {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/home')
  }
}
