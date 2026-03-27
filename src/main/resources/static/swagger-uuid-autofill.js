(function () {
  'use strict';

  function extractUuidFromJwt(token) {
    try {
      const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(atob(base64)).sub || null;
    } catch (e) {
      return null;
    }
  }

  function getBearerToken() {
    try {
      return window.ui.getState().getIn(['auth', 'authorized', 'bearerAuth', 'value']);
    } catch (e) {
      return null;
    }
  }

  // React 제어 input에 값 세팅: nativeSetter로 값 쓴 뒤 React가 감지하도록 이벤트 발생
  function setReactInputValue(input, value) {
    const nativeSetter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
    nativeSetter.call(input, value);
    input.dispatchEvent(new Event('input', { bubbles: true }));
    input.dispatchEvent(new Event('change', { bubbles: true }));
  }

  function fillUuidInputs() {
    const token = getBearerToken();
    if (!token) return;

    const uuid = extractUuidFromJwt(token);
    if (!uuid) return;

    // Swagger UI는 path parameter 이름을 input의 placeholder로 사용함
    document.querySelectorAll('input[placeholder="uuid"]').forEach(function (input) {
      if (input.value && input.value !== 'uuid') return; // 이미 값 있으면 스킵
      setReactInputValue(input, uuid);
    });
  }

  // MutationObserver: DOM 변경(패널 열기 등) 감지 후 약간의 딜레이로 React 렌더 완료 대기
  function startObserver() {
    const target = document.getElementById('swagger-ui') || document.body;
    const observer = new MutationObserver(function () {
      setTimeout(fillUuidInputs, 150);
    });
    observer.observe(target, { childList: true, subtree: true });
    // 최초 로드 시 한 번 실행
    fillUuidInputs();
  }

  window.addEventListener('load', function () {
    // Swagger UI 초기화 대기
    setTimeout(startObserver, 2000);
  });
})();
