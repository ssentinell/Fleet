'use strict';
const $ = id => document.getElementById(id);
let cars = [], sorted = false, histogram = null, deleteId = null, loadVersion = 0;
const dateText = value => value ? value.split('-').reverse().join('.') : '—';

async function api(url, options = {}) {
  let response;
  try { response = await fetch(url, { ...options, headers: { 'Content-Type': 'application/json', ...options.headers } }); }
  catch { throw new Error('Нет связи с сервером. Проверьте, что приложение запущено.'); }
  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    throw new Error(data.message || `Ошибка сервера (${response.status}). Повторите действие.`);
  }
  return response.status === 204 ? null : response.json();
}
function showError(id, error) { $(id).textContent = error.message; $(id).hidden = false; }
function notify(message) {
  $('toast').textContent = message; $('toast').hidden = false;
  clearTimeout(notify.timer); notify.timer = setTimeout(() => $('toast').hidden = true, 3500);
}
async function loadCars() {
  const version = ++loadVersion;
  const params = new URLSearchParams();
  for (const [name, value] of new FormData($('filters'))) if (value.trim()) params.set(name, value.trim());
  params.set('sorted', sorted); $('page-error').hidden = true;
  try {
    const data = await api('/api/cars?' + params);
    if (version !== loadVersion) return;
    cars = data.cars;
    $('total-count').textContent = data.total;
    $('visible-count').textContent = cars.length;
    $('table-info').textContent = `Показано ${cars.length} из ${data.total}`;
    renderCars();
  } catch (error) { if (version === loadVersion) showError('page-error', error); }
}
function renderCars() {
  const body = $('cars-body'); body.replaceChildren();
  if (!cars.length) {
    const row = body.insertRow(), cell = row.insertCell(); cell.colSpan = 6; cell.className = 'empty';
    cell.textContent = 'Автомобили не найдены. Измените фильтры или добавьте новую запись.'; return;
  }
  for (const car of cars) {
    const row = body.insertRow();
    for (const [index, value] of [car.id, car['марка'], car.productionYear, dateText(car.registeredOn), car.ownerName].entries()) {
      const cell = row.insertCell();
      if (index === 2) { const badge = document.createElement('span'); badge.className = 'year-badge'; badge.textContent = value; cell.append(badge); }
      else { cell.textContent = value; if (index === 1) cell.className = 'car-brand'; }
    }
    const actions = document.createElement('div'); actions.className = 'row-actions';
    for (const [text, label, handler, cls] of [
      ['✎', 'Редактировать', () => openCar(car), 'edit'], ['×', 'Удалить', () => openDelete(car), 'delete']
    ]) {
      const button = document.createElement('button'); button.type = 'button'; button.textContent = text;
      button.title = `${label}: ${car['марка']}, ID ${car.id}`; button.setAttribute('aria-label', button.title);
      button.className = cls; button.addEventListener('click', handler); actions.append(button);
    }
    row.insertCell().append(actions);
  }
}
function openCar(car = null) {
  $('car-form').reset(); $('form-error').hidden = true;
  $('dialog-title').textContent = car ? 'Редактировать автомобиль' : 'Добавить автомобиль';
  for (const name of ['id', 'марка', 'productionYear', 'registeredOn', 'ownerName'])
    $('car-form').elements[name].value = car?.[name] ?? '';
  $('car-dialog').showModal();
}
function openDelete(car) {
  deleteId = car.id; $('delete-description').textContent = `${car['марка']} · ${car.productionYear} г. · ID ${car.id}`;
  $('delete-error').hidden = true; $('delete-dialog').showModal();
}
async function loadHistogram() {
  histogram = await api('/api/cars/histogram');
  $('registration-count').textContent = histogram.total;
  $('period').textContent = `${dateText(histogram.from)} — ${dateText(histogram.to)}`;
  // Ограничения совпадают с датой сервера в часовом поясе Москвы.
  $('car-form').elements.productionYear.max = histogram.to.slice(0, 4);
  $('car-form').elements.registeredOn.max = histogram.to;
  const data = $('histogram-data'); data.replaceChildren();
  for (const day of histogram.days) { const el = document.createElement('span'); el.textContent = `${dateText(day.date)}: ${day.count}`; data.append(el); }
  drawChart();
}
function drawChart() {
  if (!histogram) return;
  const canvas = $('histogram'), rect = canvas.getBoundingClientRect(), dpr = window.devicePixelRatio || 1;
  canvas.width = rect.width * dpr; canvas.height = rect.height * dpr;
  const ctx = canvas.getContext('2d'); ctx.scale(dpr, dpr);
  const left = 27, top = 15, width = rect.width - left - 8, height = rect.height - top - 32;
  const max = Math.max(2, ...histogram.days.map(d => d.count));
  ctx.font = '10px Segoe UI, Arial'; ctx.textBaseline = 'middle';
  const tickStep = Math.max(1, Math.ceil(max / 4));
  for (let n = 0; n <= max; n += tickStep) {
    const y = top + height * (1 - n / max);
    ctx.strokeStyle = '#e8edf5'; ctx.beginPath(); ctx.moveTo(left, y); ctx.lineTo(left + width, y); ctx.stroke();
    ctx.fillStyle = '#8290a6'; ctx.textAlign = 'right'; ctx.fillText(n, left - 9, y);
  }
  const slot = width / histogram.days.length;
  histogram.days.forEach((day, index) => {
    const x = left + slot * index + slot * .19, barHeight = height * day.count / max;
    ctx.fillStyle = index === 29 ? '#193b80' : '#3d71d1';
    ctx.fillRect(x, top + height - barHeight, slot * .62, barHeight);
    if (day.count > 0) { ctx.textAlign = 'center'; ctx.fillStyle = '#395781'; ctx.fillText(day.count, x + slot * .31, top + height - barHeight - 8); }
    if ((index % (rect.width < 500 ? 7 : 4) === 0 && (rect.width >= 500 || index < 26)) || index === 29) {
      ctx.fillStyle = '#8290a6'; ctx.textAlign = 'center'; ctx.fillText(dateText(day.date).slice(0, 5), left + slot * (index + .5), top + height + 18);
    }
  });
}
$('filters').addEventListener('submit', event => { event.preventDefault(); loadCars(); });
$('filters').addEventListener('reset', () => { sorted = false; updateSort(); setTimeout(loadCars, 0); });
function updateSort() {
  $('sort').setAttribute('aria-pressed', sorted);
  $('sort-info').textContent = sorted ? 'По возрастанию: год выпуска → марка.' : 'Текущий порядок: по ID';
}
$('sort').addEventListener('click', () => { sorted = !sorted; updateSort(); loadCars(); });
$('add-car').addEventListener('click', () => openCar());
for (const id of ['close-dialog', 'cancel-dialog']) $(id).addEventListener('click', () => $('car-dialog').close());
$('cancel-delete').addEventListener('click', () => $('delete-dialog').close());
$('car-form').addEventListener('submit', async event => {
  event.preventDefault(); $('form-error').hidden = true;
  const data = Object.fromEntries(new FormData(event.currentTarget)), id = data.id; delete data.id;
  data['марка'] = data['марка'].trim(); data.ownerName = data.ownerName.trim(); data.productionYear = Number(data.productionYear);
  if (!data['марка'] || !data.ownerName) return showError('form-error', new Error('Заполните марку и ФИО владельца.'));
  if (!Number.isInteger(data.productionYear) || data.productionYear < 1886)
    return showError('form-error', new Error('Год выпуска должен быть целым числом, не раньше 1886.'));
  if (data.registeredOn.slice(0, 4) < String(data.productionYear))
    return showError('form-error', new Error('Дата постановки на учёт не может быть раньше года выпуска.'));
  if (histogram && (data.productionYear > Number(histogram.to.slice(0, 4)) || data.registeredOn > histogram.to))
    return showError('form-error', new Error('Год выпуска и дата постановки на учёт не могут быть в будущем.'));
  $('save-car').disabled = true;
  try {
    await api('/api/cars' + (id ? '/' + id : ''), { method: id ? 'PUT' : 'POST', body: JSON.stringify(data) });
    $('car-dialog').close(); notify(id ? 'Изменения сохранены' : 'Автомобиль добавлен');
    await loadCars(); await loadHistogram();
  } catch (error) { showError($('car-dialog').open ? 'form-error' : 'page-error', error); }
  finally { $('save-car').disabled = false; }
});
$('delete-form').addEventListener('submit', async event => {
  event.preventDefault(); $('confirm-delete').disabled = true;
  try { await api('/api/cars/' + deleteId, { method: 'DELETE' }); $('delete-dialog').close(); notify('Автомобиль удалён'); await loadCars(); await loadHistogram(); }
  catch (error) { showError($('delete-dialog').open ? 'delete-error' : 'page-error', error); }
  finally { $('confirm-delete').disabled = false; }
});
new ResizeObserver(drawChart).observe($('histogram').parentElement);
loadCars(); loadHistogram().catch(error => showError('page-error', error));
