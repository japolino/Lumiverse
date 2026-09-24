(() => {
  // A native button invokes this function; web content receives no native bridge.
  const direction = __DIRECTION__;
  const editable = document.activeElement?.closest('input,textarea,select,[contenteditable="true"],[role="textbox"]');
  if (editable || document.querySelector('[role="dialog"],[aria-modal="true"]')) return false;
  const chat = Array.from(document.querySelectorAll('[data-chat-scroll="true"]'))
    .find(el => el.getClientRects().length && el.clientHeight > 0);
  if (!chat) return false;
  const height = Math.min(chat.clientHeight, window.visualViewport?.height ?? chat.clientHeight);
  const distance = direction * Math.max(1, height * 0.85);
  // Tell the existing chat wheel handler to cancel forced auto-follow first.
  chat.dispatchEvent(new WheelEvent('wheel', { deltaY: distance, bubbles: true }));
  chat.scrollBy({ top: distance, behavior: 'instant' });
  return true;
})();
