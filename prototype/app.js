/* ============================================================
   Conditional Alarms – App Logic
   ============================================================
   Implements every use case from the diagram:
     Alarm Management: Browse, Search, Create, Edit, Enable/Disable
     AI Features:      Create Alarm via AI Prompt
     Alarm Setup:      Title, Add/Modify/Remove Condition, Boolean
                       Operator, Numerical Value, Readout, Ring,
                       Trigger Once
     Condition Selection: Browse Categories, Select Condition,
                          Create/Modify/Delete Custom Condition
   ============================================================ */

// ─── Data ────────────────────────────────────────────────────
const CATEGORIES = [
  {
    name: 'Weather',
    icon: 'cloud',
    conditions: [
      { title: 'Temperature above', hasNum: true, unit: '°F' },
      { title: 'Temperature below', hasNum: true, unit: '°F' },
      { title: 'Rain expected', hasNum: false },
      { title: 'Snow expected', hasNum: false },
      { title: 'Wind speed above', hasNum: true, unit: 'mph' },
      { title: 'Humidity above', hasNum: true, unit: '%' },
    ],
  },
  {
    name: 'Device Attributes',
    icon: 'smartphone',
    conditions: [
      { title: 'Battery below', hasNum: true, unit: '%' },
      { title: 'Battery above', hasNum: true, unit: '%' },
      { title: 'Connected to WiFi', hasNum: false },
      { title: 'Bluetooth connected', hasNum: false },
      { title: 'Charging', hasNum: false },
    ],
  },
  {
    name: 'Time / Date',
    icon: 'schedule',
    conditions: [
      { title: 'Time is', hasNum: false, placeholder: 'HH:MM' },
      { title: 'Day of week is', hasNum: false },
      { title: 'Date is', hasNum: false },
      { title: 'Minutes from now', hasNum: true, unit: 'min' },
    ],
  },
  {
    name: 'Location',
    icon: 'location_on',
    conditions: [
      { title: 'Arrive at location', hasNum: false },
      { title: 'Leave location', hasNum: false },
      { title: 'Within radius of', hasNum: true, unit: 'mi' },
    ],
  },
  {
    name: 'Recurring Schedule',
    icon: 'event_repeat',
    conditions: [
      { title: 'Every X hours', hasNum: true, unit: 'hrs' },
      { title: 'Every X days', hasNum: true, unit: 'days' },
      { title: 'Every X weeks', hasNum: true, unit: 'weeks' },
      { title: 'X times per day', hasNum: true, unit: 'times' },
      { title: 'X times per week', hasNum: true, unit: 'times' },
    ],
  },
  {
    name: 'Custom',
    icon: 'tune',
    conditions: [], // user-created conditions stored here
  },
];

// Persistent alarm list
let alarms = JSON.parse(localStorage.getItem('alarms') || '[]');

// Current editing state
let editState = {
  mode: null,   // 'create' | 'edit'
  index: null,  // alarm index when editing
  title: '',
  conditions: [],  // [{ title, hasNum, unit, value, custom }]
  operators: [],   // ['AND' | 'OR']  (length = conditions.length - 1)
  readout: false,
  ring: false,
  triggerOnce: false,
};

// Track which condition block we're modifying (for modify flow)
let modifyCondIndex = null;

// Track which custom condition we're managing
let manageCustomIndex = null;

// Track which condition's numerical value we're editing
let numvalCondIndex = null;

// ─── Helpers ─────────────────────────────────────────────────
const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => document.querySelectorAll(sel);

function saveAlarms() {
  localStorage.setItem('alarms', JSON.stringify(alarms));
}

function showPage(id) {
  $$('.page').forEach((p) => p.classList.remove('active'));
  $(`#${id}`).classList.add('active');
}

function showPopup(id) {
  $(`#${id}`).classList.remove('hidden');
}

function hidePopup(id) {
  $(`#${id}`).classList.add('hidden');
}

// ─── Home Page ───────────────────────────────────────────────
function renderAlarmList(filter = '') {
  const list = $('#alarm-list');
  const lowerFilter = filter.toLowerCase();

  const filtered = alarms.map((a, i) => ({ ...a, _i: i })).filter((a) => {
    if (!lowerFilter) return true;
    if (a.title.toLowerCase().includes(lowerFilter)) return true;
    return a.conditions.some((c) => c.title.toLowerCase().includes(lowerFilter));
  });

  if (filtered.length === 0) {
    list.innerHTML = `
      <div class="empty-state">
        <span class="material-icons-round empty-icon">${filter ? 'search_off' : 'alarm_off'}</span>
        <p>${filter ? 'No matching alarms' : 'No alarms yet'}</p>
        <p class="sub">${filter ? 'Try a different search' : 'Tap <strong>+</strong> to create one'}</p>
      </div>`;
    return;
  }

  list.innerHTML = filtered
    .map((a) => {
      const condText = a.conditions
        .map((c) => c.title + (c.hasNum && c.value != null ? ` ${c.value} ${c.unit || ''}` : ''))
        .join(` ${a.operators[0] || 'AND'} `);
      const badges = [];
      if (a.readout) badges.push('<span class="badge">Readout</span>');
      if (a.ring) badges.push('<span class="badge">Ring</span>');
      if (a.triggerOnce) badges.push('<span class="badge muted">Once</span>');
      return `
        <div class="alarm-card ${a.enabled ? '' : 'disabled'} ${selectMode ? 'selectable' : ''} ${selectMode && selectedIndices.has(a._i) ? 'selected' : ''}" data-index="${a._i}">
          <div class="alarm-card-body">
            <div class="alarm-card-title">${esc(a.title)}</div>
            <div class="alarm-card-conditions">${esc(condText) || '<em>No conditions</em>'}</div>
            ${badges.length ? `<div class="alarm-card-badges">${badges.join('')}</div>` : ''}
          </div>
          <div class="alarm-card-toggle">
            <label class="switch" onclick="event.stopPropagation()">
              <input type="checkbox" ${a.enabled ? 'checked' : ''} data-toggle="${a._i}" />
              <span class="slider"></span>
            </label>
          </div>
          <div class="select-check">
            <span class="material-icons-round">check</span>
          </div>
        </div>`;
    })
    .join('');

  // Enable/Disable toggle
  list.querySelectorAll('[data-toggle]').forEach((inp) => {
    inp.addEventListener('change', (e) => {
      const idx = parseInt(e.target.dataset.toggle);
      alarms[idx].enabled = e.target.checked;
      saveAlarms();
      renderAlarmList($('#search-input').value);
    });
  });

  // Edit alarm on card tap (or toggle selection in select mode)
  list.querySelectorAll('.alarm-card').forEach((card) => {
    card.addEventListener('click', (e) => {
      // Don't handle clicks from toggle switch
      if (e.target.closest('.switch')) return;
      const idx = parseInt(card.dataset.index);
      if (selectMode) {
        toggleSelection(idx);
      } else {
        openEditAlarm(idx);
      }
    });
    // Start drag-to-select on pointerdown in select mode
    card.addEventListener('pointerdown', (e) => {
      if (selectMode && !e.target.closest('.switch')) {
        isDragging = true;
      }
    });
  });
}

function esc(str) {
  const d = document.createElement('div');
  d.textContent = str;
  return d.innerHTML;
}

// ─── Search ──────────────────────────────────────────────────
$('#btn-search').addEventListener('click', () => {
  const bar = $('#search-bar');
  bar.classList.remove('hidden');
  $('#search-input').focus();
});
$('#btn-search-close').addEventListener('click', () => {
  $('#search-bar').classList.add('hidden');
  $('#search-input').value = '';
  renderAlarmList();
});
$('#search-input').addEventListener('input', (e) => {
  renderAlarmList(e.target.value);
});

// ─── Create Alarm ────────────────────────────────────────────
$('#btn-create').addEventListener('click', () => {
  openCreateAlarm();
});

function openCreateAlarm() {
  editState = {
    mode: 'create',
    index: null,
    title: '',
    conditions: [],
    operators: [],
    readout: false,
    ring: false,
    triggerOnce: false,
  };
  $('#setup-page-title').textContent = 'Create Alarm';
  $('#btn-delete-alarm').classList.add('hidden');
  populateSetupPage();
  showPage('page-setup');
}

// ─── Edit Alarm ──────────────────────────────────────────────
function openEditAlarm(idx) {
  const a = alarms[idx];
  editState = {
    mode: 'edit',
    index: idx,
    title: a.title,
    conditions: a.conditions.map((c) => ({ ...c })),
    operators: [...a.operators],
    readout: a.readout,
    ring: a.ring,
    triggerOnce: a.triggerOnce,
  };
  $('#setup-page-title').textContent = 'Edit Alarm';
  $('#btn-delete-alarm').classList.remove('hidden');
  populateSetupPage();
  showPage('page-setup');
}

// ─── Setup Page ──────────────────────────────────────────────
function populateSetupPage() {
  $('#alarm-title-input').value = editState.title;
  $('#opt-readout').checked = editState.readout;
  $('#opt-ring').checked = editState.ring;
  $('#opt-trigger-once').checked = editState.triggerOnce;
  renderConditions();
}

function renderConditions() {
  const container = $('#conditions-container');
  container.innerHTML = '';

  editState.conditions.forEach((cond, i) => {
    // Boolean operator chip before condition (if not first)
    if (i > 0) {
      const op = editState.operators[i - 1] || 'AND';
      const chip = document.createElement('button');
      chip.className = 'bool-operator-chip';
      chip.textContent = op;
      chip.dataset.opIndex = i - 1;
      chip.addEventListener('click', () => {
        openBoolPopup(i - 1);
      });
      container.appendChild(chip);
    }

    // Condition block
    const block = document.createElement('div');
    block.className = 'condition-block';
    block.dataset.condIndex = i;

    let inner = `<span class="condition-text">${esc(cond.title)}`;
    if (cond.hasNum) {
      const val = cond.value != null ? cond.value : '—';
      inner += `</span><button class="condition-numval" data-numidx="${i}">${val} ${esc(cond.unit || '')}</button>`;
    } else {
      inner += `</span>`;
    }
    inner += `<button class="condition-remove" data-remidx="${i}"><span class="material-icons-round">close</span></button>`;

    block.innerHTML = inner;

    // Modify condition (tap on text area to browse for replacement)
    block.querySelector('.condition-text').addEventListener('click', () => {
      modifyCondIndex = i;
      openAddConditionPage();
    });

    // Numerical value button
    const numBtn = block.querySelector('.condition-numval');
    if (numBtn) {
      numBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        openNumvalPopup(i);
      });
    }

    // Remove condition
    block.querySelector('.condition-remove').addEventListener('click', (e) => {
      e.stopPropagation();
      removeCondition(i);
    });

    container.appendChild(block);
  });
}

// ─── Add Condition ───────────────────────────────────────────
$('#btn-add-condition').addEventListener('click', () => {
  modifyCondIndex = null;
  openAddConditionPage();
});

function openAddConditionPage() {
  renderCategories();
  // Reset to category view
  $('#category-list').classList.remove('hidden');
  $('#condition-items').classList.add('hidden');
  showPage('page-add-condition');
}

function renderCategories() {
  const list = $('#category-list');
  list.innerHTML = CATEGORIES.map(
    (cat, ci) => `
    <button class="category-card" data-cat="${ci}">
      <span class="cat-icon"><span class="material-icons-round">${cat.icon}</span></span>
      ${esc(cat.name)}
      ${cat.name === 'Custom' && cat.conditions.length ? `<span class="badge muted">${cat.conditions.length}</span>` : ''}
      <span class="material-icons-round chevron">chevron_right</span>
    </button>`
  ).join('');

  list.querySelectorAll('.category-card').forEach((card) => {
    card.addEventListener('click', () => {
      const ci = parseInt(card.dataset.cat);
      openCategoryItems(ci);
    });
  });
}

function openCategoryItems(catIndex) {
  const cat = CATEGORIES[catIndex];
  $('#current-category-name').textContent = cat.name;
  const list = $('#condition-items-list');

  list.innerHTML = cat.conditions
    .map((cond, ci) => {
      const isCustom = cat.name === 'Custom';
      return `
        <div class="condition-item-btn ${isCustom ? 'custom-item' : ''}" data-cat="${catIndex}" data-ci="${ci}">
          <span class="material-icons-round">${isCustom ? 'tune' : 'check_circle_outline'}</span>
          <span>${esc(cond.title)}${cond.hasNum ? ` (${esc(cond.unit || 'val')})` : ''}</span>
          ${isCustom ? `
            <span class="condition-item-actions">
              <button class="icon-btn small btn-manage-custom" data-cat="${catIndex}" data-ci="${ci}" aria-label="Manage">
                <span class="material-icons-round">more_vert</span>
              </button>
            </span>` : ''}
        </div>`;
    })
    .join('');

  // Select condition
  list.querySelectorAll('.condition-item-btn').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      // Don't trigger if managing custom
      if (e.target.closest('.btn-manage-custom')) return;
      const ci = parseInt(btn.dataset.ci);
      const cIdx = parseInt(btn.dataset.cat);
      selectCondition(CATEGORIES[cIdx].conditions[ci]);
    });
  });

  // Manage custom condition (modify / delete)
  list.querySelectorAll('.btn-manage-custom').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const ci = parseInt(btn.dataset.ci);
      manageCustomIndex = ci;
      $('#manage-custom-title').textContent = CATEGORIES[catIndex].conditions[ci].title;
      showPopup('popup-manage-custom');
    });
  });

  $('#category-list').classList.add('hidden');
  $('#condition-items').classList.remove('hidden');
}

// Back to categories
$('#btn-cond-cat-back').addEventListener('click', () => {
  $('#category-list').classList.remove('hidden');
  $('#condition-items').classList.add('hidden');
});

function selectCondition(cond) {
  const newCond = { ...cond, value: cond.hasNum ? null : undefined };

  if (modifyCondIndex != null) {
    // Modify existing condition — replace it
    editState.conditions[modifyCondIndex] = newCond;
  } else {
    // Add new condition
    if (editState.conditions.length > 0) {
      editState.operators.push('AND');
    }
    editState.conditions.push(newCond);
  }

  modifyCondIndex = null;
  renderConditions();
  showPage('page-setup');
}

// ─── Remove Condition ────────────────────────────────────────
function removeCondition(idx) {
  editState.conditions.splice(idx, 1);
  if (idx === 0 && editState.operators.length > 0) {
    editState.operators.splice(0, 1);
  } else if (idx > 0) {
    editState.operators.splice(idx - 1, 1);
  }
  renderConditions();
}

// ─── Boolean Operator Popup ──────────────────────────────────
let boolOpIndex = null;

function openBoolPopup(opIdx) {
  boolOpIndex = opIdx;
  showPopup('popup-bool');
}

$$('#popup-bool .popup-option').forEach((btn) => {
  btn.addEventListener('click', () => {
    editState.operators[boolOpIndex] = btn.dataset.op;
    hidePopup('popup-bool');
    renderConditions();
  });
});

// Close bool popup on overlay tap
$('#popup-bool').addEventListener('click', (e) => {
  if (e.target === e.currentTarget) hidePopup('popup-bool');
});

// ─── Numerical Value Popup ───────────────────────────────────
function openNumvalPopup(condIdx) {
  numvalCondIndex = condIdx;
  const cond = editState.conditions[condIdx];
  $('#numval-label').textContent = `${cond.title} (${cond.unit || 'value'})`;
  $('#numval-input').value = cond.value != null ? cond.value : '';
  showPopup('popup-numval');
  setTimeout(() => $('#numval-input').focus(), 100);
}

$('#btn-numval-save').addEventListener('click', () => {
  const val = parseFloat($('#numval-input').value);
  if (!isNaN(val)) {
    editState.conditions[numvalCondIndex].value = val;
  }
  hidePopup('popup-numval');
  renderConditions();
});

$('#btn-numval-close').addEventListener('click', () => hidePopup('popup-numval'));
$('#popup-numval').addEventListener('click', (e) => {
  if (e.target === e.currentTarget) hidePopup('popup-numval');
});

// ─── Save Alarm ──────────────────────────────────────────────
$('#btn-setup-save').addEventListener('click', () => {
  editState.title = $('#alarm-title-input').value.trim() || 'Untitled Alarm';
  editState.readout = $('#opt-readout').checked;
  editState.ring = $('#opt-ring').checked;
  editState.triggerOnce = $('#opt-trigger-once').checked;

  const alarm = {
    title: editState.title,
    conditions: editState.conditions,
    operators: editState.operators,
    readout: editState.readout,
    ring: editState.ring,
    triggerOnce: editState.triggerOnce,
    enabled: true,
  };

  if (editState.mode === 'create') {
    alarms.push(alarm);
  } else {
    alarm.enabled = alarms[editState.index].enabled;
    alarms[editState.index] = alarm;
  }

  saveAlarms();
  renderAlarmList();
  showPage('page-home');
});

// Delete alarm (with confirmation)
let pendingDeleteAction = null;

$('#btn-delete-alarm').addEventListener('click', () => {
  if (editState.mode === 'edit' && editState.index != null) {
    const name = alarms[editState.index]?.title || 'this alarm';
    $('#confirm-delete-title').textContent = 'Delete alarm?';
    $('#confirm-delete-msg').textContent = `"${name}" will be permanently deleted.`;
    pendingDeleteAction = () => {
      alarms.splice(editState.index, 1);
      saveAlarms();
      renderAlarmList();
      showPage('page-home');
    };
    showPopup('popup-confirm-delete');
  }
});

$('#btn-confirm-delete-ok').addEventListener('click', () => {
  if (pendingDeleteAction) pendingDeleteAction();
  pendingDeleteAction = null;
  hidePopup('popup-confirm-delete');
});
$('#btn-confirm-delete-cancel').addEventListener('click', () => {
  pendingDeleteAction = null;
  hidePopup('popup-confirm-delete');
});
$('#popup-confirm-delete').addEventListener('click', (e) => {
  if (e.target === e.currentTarget) {
    pendingDeleteAction = null;
    hidePopup('popup-confirm-delete');
  }
});

// Back from setup
$('#btn-setup-back').addEventListener('click', () => {
  showPage('page-home');
});

// Back from add-condition
$('#btn-cond-back').addEventListener('click', () => {
  modifyCondIndex = null;
  showPage('page-setup');
});

// ─── AI Prompt ───────────────────────────────────────────────
$('#btn-ai-home').addEventListener('click', () => {
  // Open setup page first, then AI popup
  openCreateAlarm();
  setTimeout(() => showPopup('popup-ai'), 200);
});

$('#btn-ai-close').addEventListener('click', () => hidePopup('popup-ai'));
$('#popup-ai').addEventListener('click', (e) => {
  if (e.target === e.currentTarget) hidePopup('popup-ai');
});

$('#btn-ai-submit').addEventListener('click', () => {
  const prompt = $('#ai-prompt-input').value.trim();
  if (!prompt) return;

  // Simulate AI-generated alarm from the prompt
  const generated = simulateAIAlarm(prompt);
  editState.title = generated.title;
  editState.conditions = generated.conditions;
  editState.operators = generated.operators;
  editState.readout = generated.readout;
  editState.ring = generated.ring;
  editState.triggerOnce = generated.triggerOnce;

  populateSetupPage();
  $('#ai-prompt-input').value = '';
  hidePopup('popup-ai');
});

function simulateAIAlarm(prompt) {
  // Simple keyword-based simulation for the prototype
  const lower = prompt.toLowerCase();
  const conditions = [];
  const operators = [];

  if (lower.includes('rain')) {
    conditions.push({ title: 'Rain expected', hasNum: false });
  }
  if (lower.includes('cold') || lower.includes('freez')) {
    conditions.push({ title: 'Temperature below', hasNum: true, unit: '°F', value: 32 });
  }
  if (lower.includes('hot') || lower.includes('heat')) {
    conditions.push({ title: 'Temperature above', hasNum: true, unit: '°F', value: 90 });
  }
  if (lower.includes('battery') || lower.includes('charge')) {
    conditions.push({ title: 'Battery below', hasNum: true, unit: '%', value: 20 });
  }
  if (lower.includes('morning')) {
    conditions.push({ title: 'Time is', hasNum: false });
  }
  if (lower.includes('location') || lower.includes('arrive') || lower.includes('home') || lower.includes('work')) {
    conditions.push({ title: 'Arrive at location', hasNum: false });
  }
  if (lower.includes('every') && lower.includes('hour')) {
    conditions.push({ title: 'Every X hours', hasNum: true, unit: 'hrs', value: 1 });
  }
  if (lower.includes('wind')) {
    conditions.push({ title: 'Wind speed above', hasNum: true, unit: 'mph', value: 25 });
  }

  // Fallback if nothing matched
  if (conditions.length === 0) {
    conditions.push({ title: 'Time is', hasNum: false });
  }

  // Add operators between conditions
  for (let i = 1; i < conditions.length; i++) {
    operators.push('AND');
  }

  return {
    title: prompt.length > 40 ? prompt.substring(0, 40) + '…' : prompt,
    conditions,
    operators,
    readout: lower.includes('read') || lower.includes('speak') || lower.includes('say'),
    ring: lower.includes('ring') || lower.includes('alarm') || lower.includes('loud'),
    triggerOnce: lower.includes('once') || lower.includes('one time'),
  };
}

// ─── Custom Conditions ───────────────────────────────────────
const customCategoryIndex = CATEGORIES.findIndex((c) => c.name === 'Custom');

// Load saved custom conditions
const savedCustom = JSON.parse(localStorage.getItem('customConditions') || '[]');
CATEGORIES[customCategoryIndex].conditions = savedCustom;

function saveCustomConditions() {
  localStorage.setItem('customConditions', JSON.stringify(CATEGORIES[customCategoryIndex].conditions));
}

// Create custom condition
$('#btn-create-custom-cond').addEventListener('click', () => {
  $('#custom-cond-popup-title').textContent = 'Create Custom Condition';
  $('#custom-cond-title').value = '';
  $('#custom-cond-stmt').value = '';
  manageCustomIndex = null;
  showPopup('popup-custom-cond');
  setTimeout(() => $('#custom-cond-title').focus(), 100);
});

$('#btn-custom-cond-close').addEventListener('click', () => hidePopup('popup-custom-cond'));
$('#popup-custom-cond').addEventListener('click', (e) => {
  if (e.target === e.currentTarget) hidePopup('popup-custom-cond');
});

$('#btn-custom-cond-save').addEventListener('click', () => {
  const title = $('#custom-cond-title').value.trim();
  const stmt = $('#custom-cond-stmt').value.trim();
  if (!title) return;

  const cond = { title: title + (stmt ? `: ${stmt}` : ''), hasNum: false, custom: true };

  if (manageCustomIndex != null) {
    // Modify existing
    CATEGORIES[customCategoryIndex].conditions[manageCustomIndex] = cond;
  } else {
    // Create new
    CATEGORIES[customCategoryIndex].conditions.push(cond);
  }

  saveCustomConditions();
  hidePopup('popup-custom-cond');

  // Refresh the category items if we're viewing Custom
  if (!$('#condition-items').classList.contains('hidden')) {
    openCategoryItems(customCategoryIndex);
  }
});

// Manage custom (modify / delete)
$('#btn-modify-custom').addEventListener('click', () => {
  hidePopup('popup-manage-custom');
  const cond = CATEGORIES[customCategoryIndex].conditions[manageCustomIndex];
  const parts = cond.title.split(': ');
  $('#custom-cond-popup-title').textContent = 'Modify Custom Condition';
  $('#custom-cond-title').value = parts[0] || '';
  $('#custom-cond-stmt').value = parts.slice(1).join(': ') || '';
  showPopup('popup-custom-cond');
});

$('#btn-delete-custom').addEventListener('click', () => {
  CATEGORIES[customCategoryIndex].conditions.splice(manageCustomIndex, 1);
  saveCustomConditions();
  hidePopup('popup-manage-custom');
  // Refresh
  if (!$('#condition-items').classList.contains('hidden')) {
    openCategoryItems(customCategoryIndex);
  }
});

$('#btn-manage-custom-cancel').addEventListener('click', () => hidePopup('popup-manage-custom'));
$('#popup-manage-custom').addEventListener('click', (e) => {
  if (e.target === e.currentTarget) hidePopup('popup-manage-custom');
});

// ─── Alarm Triggered Demo ────────────────────────────────────
function simulateTrigger(alarm, idx) {
  let msg = '';
  let icon = 'notifications_active';

  if (alarm.ring && alarm.readout) {
    icon = 'alarm';
    msg = '🔔 Ringing… (after dismiss, readout will play)';
  } else if (alarm.ring) {
    icon = 'alarm';
    msg = '🔔 Alarm ringing until dismissed';
  } else if (alarm.readout) {
    icon = 'record_voice_over';
    msg = '🗣️ Reading title aloud until dismissed';
  } else {
    icon = 'notifications';
    msg = '📌 ' + alarm.title;
  }

  $('#triggered-icon').textContent = icon;
  $('#triggered-title').textContent = alarm.title;
  $('#triggered-msg').textContent = msg;
  showPopup('popup-triggered');

  if (alarm.triggerOnce) {
    alarms[idx].enabled = false;
    saveAlarms();
  }
}

$('#btn-dismiss-triggered').addEventListener('click', () => {
  hidePopup('popup-triggered');
  renderAlarmList($('#search-input').value);
});
$('#popup-triggered').addEventListener('click', (e) => {
  if (e.target === e.currentTarget) {
    hidePopup('popup-triggered');
    renderAlarmList($('#search-input').value);
  }
});

// ─── Select Mode (Browse Page) ──────────────────────────────
let selectMode = false;
let selectedIndices = new Set();
let isDragging = false;

function enterSelectMode(initialIndex) {
  selectMode = true;
  selectedIndices.clear();
  if (initialIndex != null) selectedIndices.add(initialIndex);
  $('#home-top-bar').classList.add('hidden');
  $('#search-bar').classList.add('hidden');
  $('#select-top-bar').classList.remove('hidden');
  updateSelectUI();
}

function exitSelectMode() {
  selectMode = false;
  selectedIndices.clear();
  isDragging = false;
  $('#select-top-bar').classList.add('hidden');
  $('#home-top-bar').classList.remove('hidden');
  renderAlarmList($('#search-input').value);
}

function updateSelectUI() {
  const count = selectedIndices.size;
  $('#select-count').textContent = `${count} selected`;

  // Update visual selection on cards
  document.querySelectorAll('#alarm-list .alarm-card').forEach((card) => {
    const idx = parseInt(card.dataset.index);
    card.classList.add('selectable');
    if (selectedIndices.has(idx)) {
      card.classList.add('selected');
    } else {
      card.classList.remove('selected');
    }
  });
}

function toggleSelection(idx) {
  if (selectedIndices.has(idx)) {
    selectedIndices.delete(idx);
  } else {
    selectedIndices.add(idx);
  }
  updateSelectUI();
}

// Exit select mode
$('#btn-select-close').addEventListener('click', exitSelectMode);

// Bulk enable
$('#btn-select-on').addEventListener('click', () => {
  selectedIndices.forEach((idx) => {
    if (alarms[idx]) alarms[idx].enabled = true;
  });
  saveAlarms();
  exitSelectMode();
});

// Bulk disable
$('#btn-select-off').addEventListener('click', () => {
  selectedIndices.forEach((idx) => {
    if (alarms[idx]) alarms[idx].enabled = false;
  });
  saveAlarms();
  exitSelectMode();
});

// Bulk delete (with confirmation)
$('#btn-select-delete').addEventListener('click', () => {
  const count = selectedIndices.size;
  if (count === 0) return;
  $('#confirm-delete-title').textContent = `Delete ${count} alarm${count > 1 ? 's' : ''}?`;
  $('#confirm-delete-msg').textContent = `${count} alarm${count > 1 ? 's' : ''} will be permanently deleted.`;
  pendingDeleteAction = () => {
    // Delete in reverse index order to avoid index shifting
    const sorted = [...selectedIndices].sort((a, b) => b - a);
    sorted.forEach((idx) => alarms.splice(idx, 1));
    saveAlarms();
    exitSelectMode();
  };
  showPopup('popup-confirm-delete');
});

// Long-press to enter select mode (replaces old trigger simulation on long-press)
let longPressTimer = null;
let longPressCard = null;
let dragSelecting = true; // true = drag adds, false = drag removes

document.addEventListener('pointerdown', (e) => {
  const card = e.target.closest('.alarm-card');
  if (!card) return;
  // Don't start long-press from toggle switch
  if (e.target.closest('.switch')) return;
  longPressCard = card;

  // In select mode, determine drag direction based on starting card
  if (selectMode) {
    const idx = parseInt(card.dataset.index);
    dragSelecting = !selectedIndices.has(idx);
  }

  longPressTimer = setTimeout(() => {
    const idx = parseInt(card.dataset.index);
    if (!selectMode) {
      enterSelectMode(idx);
      dragSelecting = true;
      // Re-render cards in select mode
      renderAlarmList($('#search-input').value);
      // Need to re-apply select state after re-render
      setTimeout(() => updateSelectUI(), 0);
    }
    longPressCard = null;
  }, 500);
});

document.addEventListener('pointermove', (e) => {
  // If not in select mode yet, cancel long press on move
  if (!selectMode && longPressTimer) {
    const card = e.target.closest('.alarm-card');
    if (card !== longPressCard) {
      clearTimeout(longPressTimer);
      longPressCard = null;
    }
  }

  // Drag-to-select/deselect in select mode
  if (selectMode && isDragging) {
    const el = document.elementFromPoint(e.clientX, e.clientY);
    const card = el?.closest('.alarm-card');
    if (card) {
      const idx = parseInt(card.dataset.index);
      const has = selectedIndices.has(idx);
      if (dragSelecting && !has) {
        selectedIndices.add(idx);
        updateSelectUI();
      } else if (!dragSelecting && has) {
        selectedIndices.delete(idx);
        updateSelectUI();
      }
    }
  }
});

document.addEventListener('pointerup', (e) => {
  clearTimeout(longPressTimer);
  isDragging = false;
  longPressCard = null;
});
document.addEventListener('pointercancel', () => {
  clearTimeout(longPressTimer);
  isDragging = false;
  longPressCard = null;
});

// ─── Init ────────────────────────────────────────────────────
renderAlarmList();
