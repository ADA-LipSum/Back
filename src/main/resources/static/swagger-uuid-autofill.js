(function () {
  'use strict';

  // JWT payload에서 sub(uuid) 추출
  function extractUuidFromJwt(token) {
    try {
      const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(atob(base64));
      return payload.sub || null;
    } catch (e) {
      return null;
    }
  }

  // Swagger UI Redux store에서 bearerAuth 토큰 가져오기
  function getBearerToken() {
    try {
      return window.ui.getState().getIn(['auth', 'authorized', 'bearerAuth', 'value']);
    } catch (e) {
      return null;
    }
  }

  // React가 관리하는 input에 값 세팅 (nativeInputValueSetter 필요)
  function setReactInputValue(input, value) {
    const nativeSetter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
    nativeSetter.call(input, value);
    input.dispatchEvent(new Event('input', { bubbles: true }));
    input.dispatchEvent(new Event('change', { bubbles: true }));
  }

  // 파라미터 이름이 "uuid"인 입력 필드를 찾아 자동 채우기
  function fillUuidInputs() {
    const token = getBearerToken();
    if (!token) return;

    const uuid = extractUuidFromJwt(token);
    if (!uuid) return;

    // Swagger UI는 파라미터명을 .parameter__name 에 렌더링함
    document.querySelectorAll('.parameter__name').forEach(function (nameEl) {
      const name = nameEl.textContent.replace('*', '').trim();
      if (name !== 'uuid') return;

      // 같은 row(tr) 또는 부모 컨테이너에서 input 찾기
      const container = nameEl.closest('tr') || nameEl.closest('.parameter-item') || nameEl.parentElement;
      if (!container) return;

      const input = container.querySelector('input');
      if (input && !input.value) {
        setReactInputValue(input, uuid);
      }
    });
  }

  // Swagger UI가 완전히 로드된 후 MutationObserver 등록
  window.addEventListener('load', function () {
    setTimeout(function () {
      const target = document.getElementById('swagger-ui') || document.body;
      const observer = new MutationObserver(function () {
        fillUuidInputs();
      });
      observer.observe(target, { childList: true, subtree: true });
    }, 2000);
  });
})();
