import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import './style.css';
import './customer.css';

const API = '/api';
const fmt = value => '₹' + Number(value ?? 0).toFixed(2);
function errorText(e) { return e instanceof TypeError ? 'Cannot connect to backend. Make sure START_BACKEND.cmd is running.' : e.message; }
async function request(path, options = {}, token = '') {
  const response = await fetch(API + path, {
    ...options,
    headers: { ...(token ? { Authorization: 'Basic ' + token } : {}), ...options.headers }
  });
  if (!response.ok) {
    const msg = response.status === 401 ? 'Wrong username/password, or this is an owner account (use a customer account).' :
      response.status === 403 ? 'Access denied. Please sign in with a customer account.' :
      response.status === 409 ? 'That username already exists.' :
      `Request failed (HTTP ${response.status}). ${await response.text().catch(() => '')}`;
    throw new Error(msg);
  }
  return response;
}
const previewable = f => f && (/^image\//.test(f.type) || f.type === 'application/pdf' || /\.(pdf|jpe?g|png)$/i.test(f.name));
const newRow = () => ({ id: Math.random().toString(36).slice(2), file: null, mode: 'BW', copies: 1, pages: 1, detecting: false, pdf: false });
function App() {
  const [brand,setBrand]=useState({shopName:'My Print Shop',logoDataUrl:null});
  const [screen, setScreen] = useState('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [auth, setAuth] = useState('');
  const [rates, setRates] = useState(null);
  const [limits,setLimits]=useState({maxFiles:10,maxFileMb:15});
  const [orders, setOrders] = useState([]);
  const [rows, setRows] = useState([newRow()]);
  const [paymentMethod, setPaymentMethod] = useState('PAY_AT_SHOP');
  const [lastOrder, setLastOrder] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [preview,setPreview] = useState(null);
  const [historyFilter,setHistoryFilter]=useState('ALL');
  const [historyQuery,setHistoryQuery]=useState('');
  const [lastUpdated,setLastUpdated]=useState('');
  const steps=['PENDING','PRINTING','READY','COLLECTED'];
  const visibleOrders=orders.filter(o=>(historyFilter==='ALL'||o.status===historyFilter)&&[o.token,...(o.files||[]).map(f=>f.name)].some(v=>String(v||'').toLowerCase().includes(historyQuery.toLowerCase().trim())));
  function printReceipt(order){
    const w=window.open('','_blank','width=650,height=760');if(!w){setError('Allow pop-ups to print a receipt.');return;}
    const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    const safeLogo=brand.logoDataUrl && /^data:image\/(png|jpeg);base64,[A-Za-z0-9+/=]+$/.test(brand.logoDataUrl)?brand.logoDataUrl:null;
    const logo=safeLogo?'<img src="'+safeLogo+'" alt="Shop logo" style="width:88px;height:88px;object-fit:contain;float:right"/>':'';
    w.document.write('<!doctype html><html><head><title>'+esc(brand.shopName)+' — Token</title><style>body{font:16px system-ui;margin:40px}table{border-collapse:collapse;width:100%}td,th{padding:9px;border-bottom:1px solid #ddd;text-align:left}@media print{body{margin:12mm}}</style></head><body>'+logo+'<h1>'+esc(brand.shopName)+'</h1>'+(brand.address?'<p>'+esc(brand.address)+'</p>':'')+(brand.phone?'<p>Phone: '+esc(brand.phone)+'</p>':'')+'<p>Order receipt · Token: <strong>'+esc(order.token)+'</strong></p><p>Status: '+esc(order.status)+'</p><table><tr><th>Document</th><th>Settings</th><th>Amount</th></tr>'+(order.files||[]).map(f=>'<tr><td>'+esc(f.name)+'</td><td>'+esc(f.mode)+' · '+esc(f.pages)+' pages × '+esc(f.copies)+'</td><td>'+esc(fmt(f.lineAmount))+'</td></tr>').join('')+'</table><h2>Total: '+esc(fmt(order.totalAmount))+'</h2><p>Payment method: '+esc(order.paymentMethod)+'</p>'+(brand.receiptMessage?'<p>'+esc(brand.receiptMessage)+'</p>':'')+'<p>Not proof of payment.</p></body></html>');
    w.document.close();w.focus();setTimeout(()=>w.print(),500);
  }

  useEffect(()=>{request('/customer/shop-branding',{},auth).then(r=>r.json()).then(setBrand).catch(()=>{});},[auth]);
  async function refresh(token = auth) {
    const [r, o, l] = await Promise.all([
      request('/customer/pricing', {}, token),
      request('/customer/orders', {}, token),
      request('/customer/upload-settings', {}, token)
    ]);
    setRates(await r.json());setLimits(await l.json());
    setOrders(await o.json());setLastUpdated(new Date().toLocaleTimeString());
  }
  useEffect(() => { if (auth) {
    const timer = setInterval(() => refresh(auth).catch(e => setError(errorText(e))), 10000);
    return () => clearInterval(timer);
  } }, [auth]);
  async function signIn(e) {
    e.preventDefault(); setError(''); setNotice(''); setBusy(true);
    const token = btoa(unescape(encodeURIComponent(username.trim().toLowerCase() + ':' + password)));
    try { await refresh(token); setAuth(token); setPassword(''); }
    catch(e) { setError(errorText(e)); }
    finally { setBusy(false); }
  }
  async function register(e) {
    e.preventDefault(); setError(''); setNotice(''); setBusy(true);
    try {
      await request('/auth/register', {
        method:'POST', headers:{'Content-Type':'application/json'},
        body:JSON.stringify({ username:username.trim(), password, displayName:displayName.trim() })
      });
      setNotice('Customer account created! You can now sign in.'); setScreen('login');
    } catch(e) { setError(errorText(e)); }
    finally { setBusy(false); }
  }
  async function deleteCustomerFile(order, file) {
    if (!window.confirm(`Permanently delete "${file.name}"? It will disappear from BOTH customer and owner views, and the stored document will be erased. This cannot be undone.`)) return;
    setBusy(true); setError(''); setNotice('');
    try {
      await request(`/customer/orders/${order.id}/files/${file.id}`, { method:'DELETE' }, auth);
      await refresh(auth);
      setLastOrder(null);
      setNotice('Document deleted from both portals and local storage.');
    } catch(e) { setError(errorText(e)); }
    finally { setBusy(false); }
  }
  async function deleteCustomerSet(order){
    if(!window.confirm(`Permanently delete ALL ${order.files.length} files under ${order.token}? They will disappear for BOTH owner and customer. This cannot be undone.`))return;
    setBusy(true);setError('');setNotice('');
    try{await request(`/customer/orders/${order.id}/documents`,{method:'DELETE'},auth);await refresh(auth);setLastOrder(null);setNotice('Entire document set permanently deleted from both portals.');}
    catch(e){setError(errorText(e));}finally{setBusy(false);}
  }
  function showPreview(file) {
    if (!previewable(file)) { setError('Preview is available only for PDF, JPG and PNG files. Word documents can be uploaded but not previewed.'); return; }
    const url=URL.createObjectURL(file);
    setPreview({url,name:file.name,isPdf:/\.pdf$/i.test(file.name)||file.type==='application/pdf'});
  }
  function closePreview() { if(preview) URL.revokeObjectURL(preview.url); setPreview(null); }
  function editRow(id, patch) { setRows(prev => prev.map(r => r.id === id ? { ...r, ...patch } : r)); }
  function addFiles(event) {
    const selected = Array.from(event.target.files || []);
    // Let users choose the same documents again after removing them.
    event.target.value = '';
    if (!selected.length) return;
    setError('');
    const current = rows.filter(r => r.file);
    const available = Math.max(0, Number(limits.maxFiles) - current.length);
    const valid = selected.filter(file => /\.(pdf|doc|docx|jpg|jpeg|png)$/i.test(file.name) && file.size > 0 && file.size <= Number(limits.maxFileMb) * 1024 * 1024);
    const chosen = valid.slice(0, available);
    if (valid.length !== selected.length || chosen.length !== selected.length) {
      setError(`Added ${chosen.length} of ${selected.length} selected files. Only PDF, DOC, DOCX, JPG and PNG up to ${limits.maxFileMb} MB each are accepted, with a maximum of ${limits.maxFiles} documents.`);
    }
    if (!chosen.length) return;
    const newRows = chosen.map(file => ({ ...newRow(), file, pdf: /\.pdf$/i.test(file.name), detecting: /\.pdf$/i.test(file.name), pages: 1 }));
    setRows(prev => [...prev.filter(r => r.file), ...newRows]);
    // PDF count is checked on the backend separately for each chosen file.
    for (const row of newRows) {
      if (!row.pdf) continue;
      inspectPDF(row.id, row.file);
    }
  }
  async function inspectPDF(id, file) {
    try {
      const form = new FormData(); form.append('file', file);
      const response = await request('/customer/inspect', { method: 'POST', body: form }, auth);
      const data = await response.json();
      setRows(prev => prev.map(row => row.id === id && row.file === file ? { ...row, pages: data.pages, detecting: false } : row));
    } catch (e) {
      setRows(prev => prev.map(row => row.id === id && row.file === file ? { ...row, pages: 0, detecting: false } : row));
      setError(`Could not inspect ${file.name}: ${errorText(e)}`);
    }
  }
  function chooseFile(id, file) {
    const pdf = !!file && /\.pdf$/i.test(file.name);
    editRow(id, { file, pdf, detecting: pdf, pages: 1 });
    if (pdf) inspectPDF(id, file);
  }
  const total = rows.reduce((sum,r) => sum + (r.file && rates ?
    Number(r.mode === 'BW' ? rates.bwRate : rates.colorRate) * Number(r.pages || 0) * Number(r.copies || 0) : 0),0);
  async function submit(e) {
    e.preventDefault(); setError(''); setNotice('');
    if (!rows.length || rows.some(r => r.detecting || !r.file || r.file.size > Number(limits.maxFileMb)*1024*1024 || !/\.(pdf|doc|docx|jpg|jpeg|png)$/i.test(r.file.name) || !Number.isInteger(r.pages) || r.pages < 1 || r.pages > 10000 || !Number.isInteger(r.copies) || r.copies < 1 || r.copies > 100)) {
      setError(`Select PDF, DOC, DOCX, JPG or PNG files (max ${limits.maxFileMb} MB each), and enter whole-number pages (1–10000) and copies (1–100).`); return;
    }
    const form = new FormData();
    form.append('paymentMethod', paymentMethod);
    for (const row of rows) {
      form.append('files', row.file); form.append('modes',row.mode);
      form.append('copies',String(row.copies)); form.append('pages',String(row.pages));
    }
    setBusy(true);
    try {
      const response = await request('/customer/orders', { method:'POST', body:form }, auth);
      const order = await response.json();
      closePreview(); setLastOrder(order); setRows([newRow()]);
      await refresh(); setNotice('Order submitted successfully!');
    } catch(e) { setError(errorText(e)); }
    finally { setBusy(false); }
  }
  return <main className="customer"><header><div><small>PRINTQUEUE · CUSTOMER TEST PORTAL</small><h1>{brand.shopName}</h1>{brand.address && <p>{brand.address}</p>}{brand.phone && <p>Contact: {brand.phone}</p>}<p>Print documents</p><p>Local browser test • PDF, DOC, DOCX, JPG, PNG</p></div>{brand.logoDataUrl && <img src={brand.logoDataUrl} alt="Shop logo" style={{width:75,height:75,objectFit:"contain",marginLeft:"auto"}}/>}<div className="nav"><a href="/">Owner dashboard</a>{auth && <button type="button" onClick={() => {setAuth('');setOrders([]);setLastOrder(null);setRates(null);setNotice('');setError('');}}>Sign out</button>}</div></header>
    {preview && <div className="preview-overlay" role="dialog" aria-modal="true" aria-label="Local document preview"><div className="preview-panel"><div className="preview-header"><strong>{preview.name}</strong><button type="button" onClick={closePreview}>Close preview</button></div>{preview.isPdf?<iframe title={preview.name} src={preview.url}/>:<img src={preview.url} alt={preview.name}/>}<p>Preview stays in your browser; it does not upload the file.</p></div></div>}
    {error && <div role="alert" className="error">{error}</div>}
    {notice && <div role="status" className="success">{notice}</div>}
    {!auth ? <section className="card auth-card"><div className="tabs"><button className={screen === 'login' ? '' : 'secondary'} onClick={()=>{setScreen('login');setError('');}}>Customer sign in</button><button className={screen === 'register' ? '' : 'secondary'} onClick={()=>{setScreen('register');setError('');}}>Register customer</button></div><form onSubmit={screen === 'login' ? signIn : register} className="auth-form">{screen === 'register' && <label>Display name<input value={displayName} onChange={e=>setDisplayName(e.target.value)} placeholder="Customer name" required /></label>}<label>Username<input value={username} onChange={e=>setUsername(e.target.value)} placeholder="e.g. customer1" required minLength={3}/></label><label>Password<input type="password" value={password} onChange={e=>setPassword(e.target.value)} required minLength={8} placeholder="Minimum 8 characters"/></label><button disabled={busy}>{busy ? 'Please wait…' : screen === 'login' ? 'Sign in' : 'Create account'}</button></form><p>Use a new customer account. Owner credentials only work on the owner dashboard.</p></section>
    : <><section className="card"><h2>Upload print documents</h2><p>Choose up to {limits.maxFiles} files, maximum {limits.maxFileMb} MB each. PDF pages are detected automatically. Enter page counts manually for Word and images. Payment is at shop only for local testing.</p>{rates && <p className="rates">B&W: {fmt(rates.bwRate)} / page · Color: {fmt(rates.colorRate)} / page</p>}<form className="upload-form" onSubmit={submit}><div className="multi-upload"><label htmlFor="multi-files">Select multiple documents at once</label><input id="multi-files" type="file" multiple accept=".pdf,.doc,.docx,.jpg,.jpeg,.png" onChange={addFiles} disabled={busy || rows.filter(r=>r.file).length>=limits.maxFiles}/><small>{rows.filter(r=>r.file).length}/{limits.maxFiles} documents selected · {limits.maxFileMb} MB maximum each · You can add files in batches.</small></div>{rows.map((row,i) => <div className="file-card" key={row.id}><div className="file-card-head"><strong>File {i+1}</strong>{rows.length>1 && <button type="button" className="secondary" onClick={()=>setRows(prev=>prev.filter(x=>x.id!==row.id))}>Remove</button>}</div><label>Document<input type="file" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png" required={!row.file} onChange={e=>chooseFile(row.id,e.target.files?.[0] ?? null)}/></label>{row.file && <div className="file-hint"><span>{row.file.name} · {(row.file.size/1024/1024).toFixed(2)} MB</span>{previewable(row.file) && <button type="button" className="secondary" onClick={()=>showPreview(row.file)}>Preview file</button>}</div>}<div className="file-fields"><label>Print type<select value={row.mode} onChange={e=>editRow(row.id,{mode:e.target.value})}><option value="BW">Black & white</option><option value="COLOR">Color</option></select></label><label>{row.pdf ? "PDF pages (auto-detected)" : "Pages (manual)"}<input type="number" min="1" max="10000" value={row.pages} readOnly={row.pdf} onChange={e=>editRow(row.id,{pages:Number(e.target.value)})} required/>{row.detecting && <small>Checking PDF pages…</small>}</label><label>Copies<input type="number" min="1" max="100" value={row.copies} onChange={e=>editRow(row.id,{copies:Number(e.target.value)})} required/></label></div></div>)}<div className="actions">{rows.length<limits.maxFiles && <button type="button" className="secondary" onClick={()=>setRows(prev=>[...prev,newRow()])}>+ Add another file</button>}<label>Payment method<select value={paymentMethod} onChange={e=>setPaymentMethod(e.target.value)}><option value="PAY_AT_SHOP">Pay at shop (test)</option></select></label></div><div className="checkout"><span>Estimated total (PDF pages verified on server)</span><strong>{fmt(total)}</strong><button type="submit" disabled={busy || !rates || rows.some(r=>r.detecting || (r.pdf && r.pages<1))}>{busy?'Uploading…':'Submit order & get token'}</button></div></form></section>{lastOrder && <section className="card token-result"><h2>Order submitted</h2><p>Your token</p><strong>{lastOrder.token}</strong><p>Amount: {fmt(lastOrder.totalAmount)} · Status: {lastOrder.status}</p><button type="button" onClick={()=>printReceipt(lastOrder)}>Print token receipt</button></section>}<section className="card"><div className="history-head"><h2>My orders</h2><button type="button" onClick={()=>refresh().then(()=>setError('')).catch(e=>setError(errorText(e)))}>Refresh</button></div><p>Orders refresh automatically every 10 seconds. {lastUpdated && 'Last checked: '+lastUpdated}</p><div className="history-filters"><input aria-label="Search my orders" type="search" placeholder="Search token or filename" value={historyQuery} onChange={e=>setHistoryQuery(e.target.value)}/><select aria-label="Filter my orders" value={historyFilter} onChange={e=>setHistoryFilter(e.target.value)}>{['ALL','PENDING','PRINTING','READY','COLLECTED','CANCELLED'].map(s=><option key={s}>{s}</option>)}</select></div>{visibleOrders.length===0 ? <p>{orders.length ? 'No matching orders.' : 'No orders yet.'}</p> : <div className="tablewrap"><table><thead><tr><th>Token</th><th>Documents</th><th>Amount</th><th>Status</th><th>Receipt</th></tr></thead><tbody>{visibleOrders.map(o=><tr key={o.id}><td><b>{o.token}</b></td><td><button type="button" className="secondary" disabled={busy} onClick={()=>deleteCustomerSet(o)}>Delete entire document set</button>{(o.files||[]).map(f=><div key={f.id} className="fileline"><span>{f.name} · {f.mode} · {f.pages} pages × {f.copies}</span><button type="button" className="secondary" disabled={busy} onClick={()=>deleteCustomerFile(o,f)}>Delete permanently</button></div>)}</td><td>{fmt(o.totalAmount)}</td><td><span className={'status-label status-'+o.status.toLowerCase()}>{o.status}</span>{o.status!=='CANCELLED'&&<div className="progress-track"><span style={{width:(((steps.indexOf(o.status)+1)/steps.length)*100)+'%'}}/></div>}</td><td><button type="button" className="secondary" onClick={()=>printReceipt(o)}>Print</button></td></tr>)}</tbody></table></div>}</section></>}
    <footer>LOCAL TEST ONLY. Customer files are private; the owner downloads them from the protected owner dashboard. Online payment status is not proof of payment.</footer>
  </main>;
}
createRoot(document.getElementById('root')).render(<App/>);
