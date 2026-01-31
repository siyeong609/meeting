/**
 * 모달 알림 (성공/오류/경고)
 */
function showModal(message, type) {
  type = type || 'success'; // success, error, warning

  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';

  const iconMap = {
    success: '✓',
    error: '✕',
    warning: '!',
    info: 'i'
  };

  const titleMap = {
    success: '성공',
    error: '오류',
    warning: '경고',
    info: '알림'
  };

  overlay.innerHTML = `
    <div class="modal">
      <div class="modal-header ${type}">
        <div class="modal-icon ${type}">${iconMap[type] || iconMap.info}</div>
        <div class="modal-title">${titleMap[type] || titleMap.info}</div>
        <button class="modal-close" onclick="this.closest('.modal-overlay').remove()">&times;</button>
      </div>
      <div class="modal-body">${escapeHtml(message)}</div>
      <div class="modal-footer">
        <button class="btn btn-primary" onclick="this.closest('.modal-overlay').remove()">확인</button>
      </div>
    </div>
  `;

  document.body.appendChild(overlay);

  setTimeout(function() {
    overlay.classList.add('show');
  }, 10);

  overlay.addEventListener('click', function(e) {
    if (e.target === overlay) {
      overlay.remove();
    }
  });
}

function escapeHtml(text) {
  if (typeof text !== 'string') return '';
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}