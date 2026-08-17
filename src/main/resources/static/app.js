'use strict';

const state = {
  lastSpeech: {voice: '', text: ''},
  remoteTts: false,
  transactions: [],
  currentAudio: null
};

const categoryLabels = {
  ALIMENTACAO: 'Alimentação',
  TRANSPORTE: 'Transporte',
  SAUDE: 'Saúde',
  LAZER: 'Lazer',
  MORADIA: 'Moradia',
  EDUCACAO: 'Educação',
  OUTROS: 'Outros'
};

function el(id){ return document.getElementById(id); }

function setBusy(buttonId, busy, busyText){
  const button = el(buttonId);
  if(!button) return;
  if(busy){
    if(!button.dataset.label) button.dataset.label = button.textContent;
    button.textContent = busyText || 'Processando…';
    button.disabled = true;
  }else{
    button.textContent = button.dataset.label || button.textContent;
    button.disabled = false;
  }
}

async function requestJson(url, options = {}, timeoutMs = 90000){
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try{
    const response = await fetch(url, {...options, signal: controller.signal});
    const text = await response.text();
    let payload = null;
    if(text){
      try{ payload = JSON.parse(text); }
      catch{ payload = text; }
    }
    if(!response.ok){
      const error = new Error(
        payload && typeof payload === 'object' && payload.message
          ? payload.message
          : `A requisição falhou (HTTP ${response.status}).`
      );
      error.status = response.status;
      error.payload = payload;
      throw error;
    }
    return payload;
  }catch(error){
    if(error && error.name === 'AbortError'){
      const timeoutError = new Error('A operação demorou mais do que o esperado. Verifique sua conexão e tente novamente.');
      timeoutError.code = 'REQUEST_TIMEOUT';
      throw timeoutError;
    }
    throw error;
  }finally{
    clearTimeout(timer);
  }
}

function clearFeedback(id){
  const target = el(id);
  if(target) target.replaceChildren();
}

function showMessage(id, type, title, message, technical){
  const target = el(id);
  if(!target) return;
  target.replaceChildren();

  const box = document.createElement('div');
  box.className = `message ${type}`;

  const strong = document.createElement('strong');
  strong.textContent = title;
  box.appendChild(strong);

  if(message){
    const p = document.createElement('p');
    p.textContent = message;
    box.appendChild(p);
  }

  if(technical){
    const details = document.createElement('details');
    const summary = document.createElement('summary');
    summary.textContent = 'Ver detalhes técnicos';
    const pre = document.createElement('pre');
    pre.textContent = technical;
    details.append(summary, pre);
    box.appendChild(details);
  }

  target.appendChild(box);
}

function showApiError(id, contextTitle, error){
  const payload = error && error.payload && typeof error.payload === 'object' ? error.payload : null;
  const message = payload?.message || error?.message || 'Não foi possível concluir a operação.';
  const technicalParts = [];

  if(payload?.code) technicalParts.push(`Código: ${payload.code}`);
  if(payload?.correlationId) technicalParts.push(`Correlação: ${payload.correlationId}`);
  if(Array.isArray(payload?.details) && payload.details.length) technicalParts.push(payload.details.join('\n'));
  if(!payload && error?.code) technicalParts.push(`Código: ${error.code}`);
  if(error?.status) technicalParts.push(`HTTP: ${error.status}`);

  showMessage(id, 'error', contextTitle, message, technicalParts.join('\n'));
}

function setProgress(step, status){
  const item = document.querySelector(`#voiceProgress li[data-step="${step}"]`);
  if(!item) return;
  item.classList.remove('active','done','skipped');
  if(status) item.classList.add(status);
  const mark = item.querySelector('.progress-mark');
  if(mark) mark.textContent = status === 'done' ? '✓' : step;
}

function resetProgress(){
  for(let i=1;i<=5;i++) setProgress(i, '');
}

function formatBRL(value){
  const number = Number(value);
  if(!Number.isFinite(number)) return 'R$ —';
  return new Intl.NumberFormat('pt-BR',{style:'currency',currency:'BRL'}).format(number);
}

function formatDate(value){
  const date = new Date(value);
  if(Number.isNaN(date.getTime())) return '';
  return new Intl.DateTimeFormat('pt-BR',{dateStyle:'short',timeStyle:'short'}).format(date);
}

function parseMoneyToCents(raw){
  let value = String(raw ?? '').trim().replace(/\s/g,'').replace(/^R\$/i,'');
  if(!value) throw new Error('Informe o valor da transação.');

  if(value.includes(',')){
    value = value.replace(/\./g,'').replace(',','.');
  }

  if(!/^\d{1,9}(\.\d{1,2})?$/.test(value)){
    throw new Error('Informe um valor válido em reais, por exemplo 12,50.');
  }

  const [whole, decimal = ''] = value.split('.');
  const cents = Number(whole) * 100 + Number((decimal + '00').slice(0,2));

  if(!Number.isSafeInteger(cents) || cents <= 0){
    throw new Error('O valor deve ser maior que zero.');
  }
  if(cents > 99999999999){
    throw new Error('O valor informado excede o limite permitido.');
  }
  return cents;
}

function renderTransactions(items){
  const list = el('transactionList');
  list.replaceChildren();

  const sorted = [...(Array.isArray(items) ? items : [])].sort((a,b) => {
    const ta = new Date(a.createdAt || 0).getTime();
    const tb = new Date(b.createdAt || 0).getTime();
    return tb - ta;
  });
  state.transactions = sorted;

  el('transactionCount').textContent = `${sorted.length} ${sorted.length === 1 ? 'item' : 'itens'}`;

  if(!sorted.length){
    const empty = document.createElement('div');
    empty.className = 'empty';
    empty.textContent = 'Nenhuma transação salva ainda. Experimente registrar um gasto por voz ou texto.';
    list.appendChild(empty);
    return;
  }

  for(const item of sorted){
    const card = document.createElement('article');
    card.className = 'transaction';

    const left = document.createElement('div');
    const title = document.createElement('h3');
    title.className = 'transaction-title';
    title.textContent = item.description || 'Sem descrição';

    const meta = document.createElement('div');
    meta.className = 'transaction-meta';
    const category = categoryLabels[item.category] || item.category || 'Sem categoria';
    const date = formatDate(item.createdAt);
    meta.textContent = date ? `${category} • ${date}` : category;

    left.append(title, meta);

    const value = document.createElement('div');
    value.className = 'transaction-value';
    value.textContent = formatBRL(item.amount);

    card.append(left, value);
    list.appendChild(card);
  }
}

async function loadTransactions(){
  setBusy('refreshButton', true, 'Atualizando…');
  clearFeedback('transactionsFeedback');
  try{
    const items = await requestJson('/api/transactions', {}, 15000);
    renderTransactions(items);
  }catch(error){
    showApiError('transactionsFeedback','Não foi possível carregar as transações',error);
  }finally{
    setBusy('refreshButton', false);
  }
}

async function loadCategories(){
  try{
    const values = await requestJson('/api/transactions/categories', {}, 10000);
    if(!Array.isArray(values) || !values.length) return;

    const select = el('category');
    const previous = select.value;
    select.replaceChildren();

    for(const value of values){
      const option = document.createElement('option');
      option.value = value;
      option.textContent = categoryLabels[value] || value;
      select.appendChild(option);
    }
    if(values.includes(previous)) select.value = previous;
  }catch(error){
    console.warn('Não foi possível atualizar as categorias; usando o fallback embutido.', error);
  }
}

async function loadSystemStatus(){
  clearFeedback('techFeedback');
  try{
    const [provider, speech] = await Promise.all([
      requestJson('/api/system/ai-provider', {}, 10000),
      requestJson('/api/ai/speech/status', {}, 10000)
    ]);

    const configured = Boolean(provider?.nvidiaConfigured);
    const status = el('appStatus');
    status.classList.toggle('ok', configured);
    status.classList.toggle('error', !configured);

    el('appStatusText').textContent = configured
      ? 'NVIDIA configurada • pronto'
      : 'NVIDIA não configurada';

    el('techProvider').textContent = provider?.provider || '—';
    el('techModel').textContent = provider?.model || '—';
    el('techAudio').textContent = provider?.transcriptionProvider || '—';
    el('techTools').textContent = provider?.toolCalling ? 'Ativo • casos de uso reais' : 'Indisponível';

    state.remoteTts = Boolean(speech?.available);
    el('techSpeech').textContent = state.remoteTts
      ? `Spring AI / ${speech.provider} → MP3 • fallback local disponível`
      : 'Web Speech API local • Spring AI TTS opcional não configurado';

    if(!configured){
      showMessage(
        'techFeedback',
        'warning',
        'Credencial NVIDIA não detectada',
        'Abra o aplicativo pelo launcher e informe sua NVIDIA API Key antes de testar os comandos.',
        ''
      );
    }
  }catch(error){
    const status = el('appStatus');
    status.classList.remove('ok');
    status.classList.add('error');
    el('appStatusText').textContent = 'Falha no diagnóstico';
    showApiError('techFeedback','Não foi possível consultar o diagnóstico',error);
  }
}

function validateAudioFile(file){
  if(!file) throw new Error('Escolha um arquivo WAV ou MP3.');
  if(file.size <= 0) throw new Error('O arquivo selecionado está vazio.');
  if(file.size > 50 * 1024 * 1024) throw new Error('O arquivo excede o limite de 50 MB.');

  const name = (file.name || '').toLowerCase();
  const type = (file.type || '').toLowerCase();
  const supported = name.endsWith('.wav') || name.endsWith('.mp3')
    || type.includes('wav') || type.includes('wave') || type.includes('mpeg') || type.includes('mp3');

  if(!supported) throw new Error('Formato não suportado. Escolha um arquivo WAV ou MP3.');
}

async function processVoice(){
  clearFeedback('voiceFeedback');
  el('voiceResult').hidden = true;
  resetProgress();

  const file = el('audio').files[0];
  try{
    validateAudioFile(file);
  }catch(error){
    showMessage('voiceFeedback','error','Não foi possível iniciar',error.message,'');
    return;
  }

  setBusy('voiceButton', true, 'Processando áudio…');
  setProgress(1,'done');

  try{
    const form = new FormData();
    form.append('file', file);

    setProgress(2,'active');
    const transcribed = await requestJson(
      '/api/ai/transcribe',
      {method:'POST',body:form},
      180000
    );
    const transcription = String(transcribed?.text || '').trim();
    if(!transcription) throw new Error('A transcrição voltou vazia.');
    setProgress(2,'done');

    setProgress(3,'active');
    const ai = await requestJson(
      '/api/ai/command',
      {
        method:'POST',
        headers:{'Content-Type':'application/json'},
        body:JSON.stringify({text:transcription})
      },
      120000
    );
    const response = String(ai?.response || '').trim();
    if(!response) throw new Error('A IA voltou sem uma resposta.');
    setProgress(3,'done');
    setProgress(4,'done');

    state.lastSpeech.voice = response;
    el('voiceTranscription').textContent = transcription;
    el('voiceResponse').textContent = response;
    el('voiceResult').hidden = false;

    await loadTransactions();

    if(el('autoSpeak').checked){
      setProgress(5,'active');
      await speakText(response, 'voiceFeedback');
      setProgress(5,'done');
    }else{
      setProgress(5,'skipped');
      showMessage(
        'voiceFeedback',
        'success',
        'Comando concluído',
        'A resposta está pronta. Use “Ouvir resposta” se quiser reproduzi-la em voz.',
        ''
      );
    }
  }catch(error){
    showApiError('voiceFeedback','Não foi possível concluir o comando por voz',error);
  }finally{
    setBusy('voiceButton', false);
  }
}

function useExample(text){
  el('command').value = text;
  el('command').focus();
}

async function sendTextCommand(){
  clearFeedback('textFeedback');
  el('textResult').hidden = true;

  const text = el('command').value.trim();
  if(!text){
    showMessage('textFeedback','error','Informe um comando','Escreva o que você quer registrar ou consultar.','');
    return;
  }

  setBusy('textButton', true, 'Consultando a IA…');
  try{
    const result = await requestJson(
      '/api/ai/command',
      {
        method:'POST',
        headers:{'Content-Type':'application/json'},
        body:JSON.stringify({text})
      },
      120000
    );

    const response = String(result?.response || '').trim();
    if(!response) throw new Error('A IA voltou sem uma resposta.');

    state.lastSpeech.text = response;
    el('textResponse').textContent = response;
    el('textResult').hidden = false;
    showMessage('textFeedback','success','Comando concluído','A IA processou a solicitação e as transações abaixo foram sincronizadas.','');
    await loadTransactions();
  }catch(error){
    showApiError('textFeedback','Não foi possível processar o comando',error);
  }finally{
    setBusy('textButton', false);
  }
}

async function createManualTransaction(){
  clearFeedback('manualFeedback');

  const description = el('description').value.trim();
  if(!description){
    showMessage('manualFeedback','error','Descrição obrigatória','Informe uma descrição para a transação.','');
    return;
  }

  let amountInCents;
  try{
    amountInCents = parseMoneyToCents(el('amount').value);
  }catch(error){
    showMessage('manualFeedback','error','Valor inválido',error.message,'');
    return;
  }

  setBusy('manualButton', true, 'Salvando…');
  try{
    const created = await requestJson(
      '/api/transactions',
      {
        method:'POST',
        headers:{'Content-Type':'application/json'},
        body:JSON.stringify({
          description,
          amountInCents,
          category:el('category').value
        })
      },
      15000
    );

    showMessage(
      'manualFeedback',
      'success',
      'Transação salva',
      `${created.description} • ${formatBRL(created.amount)} • ${categoryLabels[created.category] || created.category}`,
      ''
    );
    await loadTransactions();
  }catch(error){
    showApiError('manualFeedback','Não foi possível salvar a transação',error);
  }finally{
    setBusy('manualButton', false);
  }
}

function speechSupported(){
  return 'speechSynthesis' in window && 'SpeechSynthesisUtterance' in window;
}

function stopSpeech(){
  if(state.currentAudio){
    try{
      state.currentAudio.pause();
      state.currentAudio.src = '';
    }catch{}
    state.currentAudio = null;
  }
  if(speechSupported()) window.speechSynthesis.cancel();
}

function localSpeak(text){
  return new Promise((resolve,reject) => {
    if(!speechSupported()){
      reject(new Error('Este navegador não oferece síntese de voz local.'));
      return;
    }

    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'pt-BR';
    utterance.rate = 1;
    utterance.pitch = 1;

    const voices = window.speechSynthesis.getVoices();
    const voice = voices.find(v => /^pt-BR$/i.test(v.lang))
      || voices.find(v => /^pt/i.test(v.lang));
    if(voice) utterance.voice = voice;

    utterance.onstart = () => resolve('local');
    utterance.onerror = event => reject(new Error(event.error || 'Falha ao reproduzir a voz local.'));
    window.speechSynthesis.speak(utterance);

    setTimeout(() => resolve('local'), 500);
  });
}

async function remoteSpeak(text){
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 60000);
  try{
    const response = await fetch('/api/ai/speech',{
      method:'POST',
      headers:{'Content-Type':'application/json'},
      body:JSON.stringify({text}),
      signal:controller.signal
    });

    if(!response.ok){
      const raw = await response.text();
      let payload = null;
      try{ payload = raw ? JSON.parse(raw) : null; }catch{ payload = raw; }
      const error = new Error(payload?.message || `TTS em nuvem falhou (HTTP ${response.status}).`);
      error.payload = payload;
      error.status = response.status;
      throw error;
    }

    const blob = await response.blob();
    if(!blob.size) throw new Error('O TTS em nuvem retornou um arquivo vazio.');

    const objectUrl = URL.createObjectURL(blob);
    const audio = new Audio(objectUrl);
    state.currentAudio = audio;

    audio.addEventListener('ended',() => {
      URL.revokeObjectURL(objectUrl);
      if(state.currentAudio === audio) state.currentAudio = null;
    },{once:true});
    audio.addEventListener('error',() => {
      URL.revokeObjectURL(objectUrl);
    },{once:true});

    await audio.play();
    return 'spring-ai';
  }finally{
    clearTimeout(timer);
  }
}

async function speakText(text, feedbackId){
  const safeText = String(text || '').trim();
  if(!safeText) throw new Error('Ainda não existe uma resposta para reproduzir.');

  stopSpeech();

  if(state.remoteTts){
    try{
      const mode = await remoteSpeak(safeText);
      if(feedbackId){
        showMessage(feedbackId,'success','Resposta em voz iniciada','Áudio MP3 gerado pelo TextToSpeechModel do Spring AI.','');
      }
      return mode;
    }catch(error){
      console.warn('TTS Spring AI indisponível; tentando fallback local.', error);
      state.remoteTts = false;
    }
  }

  try{
    const mode = await localSpeak(safeText);
    if(feedbackId){
      showMessage(feedbackId,'success','Resposta em voz iniciada','Usando a síntese de voz local do navegador como fallback.','');
    }
    return mode;
  }catch(error){
    if(feedbackId){
      showMessage(
        feedbackId,
        'warning',
        'A resposta em texto está pronta',
        'Não foi possível reproduzir voz neste navegador. O conteúdo continua disponível na tela.',
        error.message
      );
    }
    return 'text-only';
  }
}

async function speakLast(kind){
  const text = state.lastSpeech[kind];
  const feedbackId = kind === 'voice' ? 'voiceFeedback' : 'textFeedback';
  if(!text){
    showMessage(feedbackId,'warning','Nada para reproduzir','Execute primeiro um comando para gerar uma resposta.','');
    return;
  }
  await speakText(text, feedbackId);
}

el('audio').addEventListener('change',() => {
  resetProgress();
  clearFeedback('voiceFeedback');
  const file = el('audio').files[0];
  if(file){
    try{
      validateAudioFile(file);
      setProgress(1,'done');
      showMessage('voiceFeedback','success','Áudio selecionado',`${file.name} • ${(file.size/1024/1024).toFixed(2)} MB`,'');
    }catch(error){
      showMessage('voiceFeedback','error','Arquivo inválido',error.message,'');
    }
  }
});

Promise.allSettled([
  loadCategories(),
  loadTransactions(),
  loadSystemStatus()
]);
