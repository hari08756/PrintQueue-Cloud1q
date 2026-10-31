
import React, { useEffect, useState, useCallback } from 'react';
import { createRoot } from 'react-dom/client';
import './style.css';

// Use Vite proxy instead of direct backend URL.
const API = '/api';

const readableError = (error) =>
  error instanceof TypeError
    ? 'Cannot connect to PrintQueue API. Check backend and Vite proxy.'
    : error.message;

function App() {
  const [username, setUsername] = useState('owner');
  const [stocks,setStocks]=useState([]);
  const [stockDraft,setStockDraft]=useState({name:'A4 Paper',sheets:'500',lowStockThreshold:'100',costPerSheet:'0.50'});
  const [expenses,setExpenses]=useState(null);
  const [expenseDraft,setExpenseDraft]=useState({category:'INK_TONER',description:'',amount:''});
  const [inventoryBusy,setInventoryBusy]=useState(false);

  const [password, setPassword] = useState('');
  const [credentials, setCredentials] = useState('');
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [brand,setBrand]=useState({shopName:'My Print Shop',logoDataUrl:null});
  const [shopNameInput,setShopNameInput]=useState('My Print Shop');
  const [shopPhone,setShopPhone]=useState('');
  const [shopAddress,setShopAddress]=useState('');
  const [receiptMessage,setReceiptMessage]=useState('');
  const [logoFile,setLogoFile]=useState(null);
  const [removeLogo,setRemoveLogo]=useState(false);
  const [brandingBusy,setBrandingBusy]=useState(false);
  const [rates,setRates]=useState({bwRate:'2.00',colorRate:'10.00'});
  const [limits,setLimits]=useState({maxFiles:10,maxFileMb:15});
  const [limitsBusy,setLimitsBusy]=useState(false);
  const [savingRates,setSavingRates]=useState(false);
  const [query,setQuery]=useState('');
  const [statusFilter,setStatusFilter]=useState('ALL');
  const [paymentFilter,setPaymentFilter]=useState('ALL');
  const [historyDate,setHistoryDate]=useState('');
  const [reportDate,setReportDate]=useState(()=>new Date(Date.now()+330*60000).toISOString().slice(0,10));
  const [dailyReport,setDailyReport]=useState(null);
  const [reportBusy,setReportBusy]=useState(false);
  const [paymentBusy,setPaymentBusy]=useState(null);
  const [selected,setSelected]=useState([]);
  const [bulkBusy,setBulkBusy]=useState(false);
  const [bulkTarget,setBulkTarget]=useState('PRINTING');
  const [statusBusy,setStatusBusy]=useState(null);
  const [notice,setNotice]=useState('');
  const istDate = iso => {const parts=new Intl.DateTimeFormat('en-GB',{timeZone:'Asia/Kolkata',year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(new Date(iso));const get=t=>parts.find(p=>p.type===t)?.value;return `${get('year')}-${get('month')}-${get('day')}`;};
  const visibleOrders = orders.filter(o => (statusFilter === 'ALL' || o.status === statusFilter) && (paymentFilter === 'ALL' || (o.paymentStatus || 'UNPAID') === paymentFilter) && (!historyDate || istDate(o.createdAt) === historyDate) && [o.token,o.customerName,...(o.files||[]).map(f=>f.name)].some(v=>String(v||'').toLowerCase().includes(query.toLowerCase().trim())));
  const fmt = v => v == null ? 'Legacy' : '₹'+Number(v).toFixed(2);
  function exportQueueCsv() {
    const cell = v => '"'+String(v??'').replace(/"/g,'""')+'"';
    const lines=[['Token','Customer','Status','Files','Total','Payment Method','Payment Status','Created At'],
      ...visibleOrders.map(o=>[o.token,o.customerName,o.status,(o.files||[]).map(f=>f.name+' ('+f.pages+' pages × '+f.copies+')').join('; '),o.totalAmount,o.paymentMethod,o.paymentStatus,o.createdAt])];
    const csv='\uFEFF'+lines.map(row=>row.map(cell).join(',')).join('\r\n');
    const url=URL.createObjectURL(new Blob([csv],{type:'text/csv;charset=utf-8'}));
    const link=document.createElement('a'); link.href=url; link.download='printqueue-orders.csv';link.click();
    setTimeout(()=>URL.revokeObjectURL(url),1000);
  }


  const api = useCallback(async (path, options = {}) => {
    const response = await fetch(API + path, {
      ...options,
      headers: {
        Authorization: 'Basic ' + credentials,
        ...options.headers
      }
    });

    if (!response.ok) {
      let detail = '';
      try {
        const body = await response.json();
        detail = body.detail || body.message || body.error || '';
      } catch (_) { /* Not every error response contains JSON. */ }
      if (response.status === 404 && path.includes('/files/')) {
        throw new Error('File is not present in the active upload directory (HTTP 404). Upload a fresh document through the Customer Portal, or restore files from an older project folder.');
      }
      if (response.status === 401 || response.status === 403) {
        throw new Error('Access denied (HTTP ' + response.status + '). Please sign out, sign back in as owner and retry.' + (detail ? ' ' + detail : ''));
      }
      throw new Error('Request failed (HTTP ' + response.status + ')' + (detail ? ': ' + detail : ''));
    }

    return response;
  }, [credentials]);

  const load = useCallback(async () => {
    if (!credentials) return;

    try {
      const response = await api('/owner/orders');
      const data = await response.json();
      setOrders(data);
      setError('');
    } catch (err) {
      setError(readableError(err));
    }
  }, [credentials, api]);

  const loadRates=useCallback(async()=>{
    if(!credentials)return;
    try {const r=await api('/owner/pricing');setRates(await r.json());}
    catch(e){setError(readableError(e));}
  },[credentials,api]);
  async function saveRates(e){
    e.preventDefault();setSavingRates(true);
    try {const r=await api('/owner/pricing',{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify(rates)});
      setRates(await r.json());setError('');setNotice('Printing rates saved. New orders use the updated prices.');
    } catch(e){setError(readableError(e));} finally{setSavingRates(false);}
  }
  useEffect(() => {
    if (!credentials) return;

    load();loadRates();api('/owner/shop-branding').then(r=>r.json()).then(b=>{setBrand(b);setShopNameInput(b.shopName);setShopPhone(b.phone||'');setShopAddress(b.address||'');setReceiptMessage(b.receiptMessage||'');}).catch(e=>setError(readableError(e)));api('/owner/upload-settings').then(r=>r.json()).then(setLimits).catch(e=>setError(readableError(e)));

    const timer = setInterval(load, 6000);

    return () => clearInterval(timer);
  }, [credentials, load,loadRates]);

  const loadOperations=useCallback(async()=>{
    if(!credentials)return;
    try{
      const [a,b]=await Promise.all([api('/owner/operations/stocks'),api('/owner/operations/summary?date='+encodeURIComponent(reportDate))]);
      setStocks(await a.json());setExpenses(await b.json());
    }catch(e){setError(readableError(e));}
  },[api,credentials,reportDate]);
  useEffect(()=>{if(credentials)loadOperations();},[credentials,loadOperations]);
  async function createStock(e){
    e.preventDefault();setInventoryBusy(true);setError('');
    try{await api('/owner/operations/stocks',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({name:stockDraft.name,sheets:Number(stockDraft.sheets),lowStockThreshold:Number(stockDraft.lowStockThreshold),costPerSheet:Number(stockDraft.costPerSheet)})});await loadOperations();setNotice('Paper stock added.');}
    catch(e){setError(readableError(e));}finally{setInventoryBusy(false);}
  }
  async function adjustStock(stock,delta){
    if(!Number.isInteger(delta)||delta===0){setError('Enter a nonzero whole number of sheets.');return;}
    if(!window.confirm(`${delta>0?'Add':'Use'} ${Math.abs(delta)} sheets for ${stock.name}?`))return;
    setInventoryBusy(true);setError('');
    try{await api(`/owner/operations/stocks/${stock.id}/adjust`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({delta})});await loadOperations();setNotice('Stock updated.');}
    catch(e){setError(readableError(e));}finally{setInventoryBusy(false);}
  }
  async function updateStock(stock){
    const name=window.prompt('Paper type name',stock.name);if(name===null)return;
    const threshold=window.prompt('Low-stock warning threshold (sheets)',String(stock.lowStockThreshold));if(threshold===null)return;
    const cost=window.prompt('Estimated cost per sheet (₹)',String(stock.costPerSheet));if(cost===null)return;
    setInventoryBusy(true);setError('');
    try{await api(`/owner/operations/stocks/${stock.id}`,{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify({name,lowStockThreshold:Number(threshold),costPerSheet:Number(cost)})});await loadOperations();setNotice('Paper settings saved.');}
    catch(e){setError(readableError(e));}finally{setInventoryBusy(false);}
  }
  async function addExpense(e){
    e.preventDefault();setInventoryBusy(true);setError('');
    try{await api('/owner/operations/expenses',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({...expenseDraft,amount:Number(expenseDraft.amount),date:reportDate})});setExpenseDraft({...expenseDraft,description:'',amount:''});await loadOperations();setNotice('Expense recorded.');}
    catch(e){setError(readableError(e));}finally{setInventoryBusy(false);}
  }
  async function deleteExpense(expense){
    if(!window.confirm(`Delete ${expense.description} (${fmt(expense.amount)})?`))return;
    try{await api('/owner/operations/expenses/'+expense.id,{method:'DELETE'});await loadOperations();setNotice('Expense deleted.');}
    catch(e){setError(readableError(e));}
  }
  async function login(event) {
    event.preventDefault();
    setLoading(true);
    setError('');

    const token = btoa(
      unescape(encodeURIComponent(username + ':' + password))
    );

    try {
      const response = await fetch(API + '/owner/orders', {
        headers: {
          Authorization: 'Basic ' + token
        }
      });

      if (!response.ok) {
        throw new Error(
          'Owner login failed (HTTP ' + response.status + ')'
        );
      }

      const data = await response.json();

      setOrders(data);
      setCredentials(token);
      setPassword('');
      setError('');
    } catch (err) {
      setError(readableError(err));
    } finally {
      setLoading(false);
    }
  }

  function logout() {
    setCredentials('');
    setPassword('');
    setOrders([]);
    setSelected([]);
    setError('');
  }

  async function setPaymentStatus(order, newStatus){
    if(!window.confirm(`Mark token ${order.token} as ${newStatus}? Only do this after verifying payment.`))return;
    setPaymentBusy(order.id);setError('');
    try {
      await api(`/owner/orders/${order.id}/payment`,{method:'PATCH',headers:{'Content-Type':'application/json'},body:JSON.stringify({paymentStatus:newStatus})});
      await load();setNotice(`Token ${order.token} marked ${newStatus}.`);
      setDailyReport(null);
    }catch(e){setError(readableError(e));}finally{setPaymentBusy(null);}
  }
  async function fetchDailyReport(){
    setReportBusy(true);setError('');
    try{const r=await api('/owner/reports/daily?date='+encodeURIComponent(reportDate));setDailyReport(await r.json());}
    catch(e){setError(readableError(e));}finally{setReportBusy(false);}
  }
  function printDailyReport(){
    if(!dailyReport)return;
    const popup=window.open('','_blank');
    if(!popup){setError('Allow pop-ups to print daily reports.');return;}
    // Build DOM via textContent to prevent customer-supplied data from becoming HTML.
    const doc=popup.document;
    doc.title=`Daily report ${dailyReport.date}`;
    const style=doc.createElement('style');style.textContent='body{font:14px Arial;padding:24px;color:#111}h1{margin-bottom:4px}table{border-collapse:collapse;width:100%;margin-top:18px}td,th{border:1px solid #bbb;padding:7px;text-align:left}th{background:#eee}.totals{display:flex;gap:20px;flex-wrap:wrap;margin:14px 0}@media print{button{display:none}}';doc.head.appendChild(style);
    const heading=doc.createElement('h1');heading.textContent=brand.shopName+' — Daily Sales Report';doc.body.appendChild(heading);
    const date=doc.createElement('p');date.textContent=`Date: ${dailyReport.date} (${dailyReport.timezone})`;doc.body.appendChild(date);
    const totals=doc.createElement('div');totals.className='totals';
    [['Orders',dailyReport.totalOrders],['Paid (non-cancelled)',dailyReport.paidOrders],['Unpaid (non-cancelled)',dailyReport.unpaidOrders],['Cancelled',dailyReport.cancelledOrders],['Collected',fmt(dailyReport.collectedAmount)],['Outstanding',fmt(dailyReport.outstandingAmount)]].forEach(([k,v])=>{const p=doc.createElement('p');p.textContent=`${k}: ${v}`;totals.appendChild(p)});doc.body.appendChild(totals);
    const table=doc.createElement('table');const header=doc.createElement('tr');['Token','Customer','Order status','Payment','Amount'].forEach(v=>{const th=doc.createElement('th');th.textContent=v;header.appendChild(th)});table.appendChild(header);
    dailyReport.orders.forEach(o=>{const tr=doc.createElement('tr');[o.token,o.customerName,o.status,o.paymentStatus,fmt(o.totalAmount)].forEach(v=>{const td=doc.createElement('td');td.textContent=String(v??'');tr.appendChild(td)});table.appendChild(tr)});doc.body.appendChild(table);
    const button=doc.createElement('button');button.textContent='Print report';button.onclick=()=>popup.print();doc.body.insertBefore(button,table);
    popup.focus();popup.print();
  }

  async function updateStatus(id, value) {
    setStatusBusy(id); setNotice('');
    try {
      await api('/owner/orders/' + id + '/status', {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ status: value })
      });

      await load();
      setNotice('Order status updated. Customers will see this on refresh.');
    } catch (err) {
      setError(readableError(err));
      await load();
    } finally { setStatusBusy(null); }
  }

  async function applyBulk() {
    if(!selected.length || !window.confirm(`Change ${selected.length} selected order(s) to ${bulkTarget}?`)) return;
    setBulkBusy(true);setError('');setNotice('');
    let updated=0;
    try {
      for(const id of selected){
        await api('/owner/orders/'+id+'/status',{method:'PATCH',headers:{'Content-Type':'application/json'},body:JSON.stringify({status:bulkTarget})});
        updated++;
      }
      setSelected([]);setNotice(`${updated} order(s) updated to ${bulkTarget}.`);
    } catch(err){setError(`${updated} updated before error: ${readableError(err)}`);}
    finally {await load();setBulkBusy(false);}
  }
  const [printBusy,setPrintBusy] = useState(null);
  async function printFile(orderId, file) {
    // Browsers block windows opened after async fetches. Open during the click.
    const popup = window.open('', '_blank');
    if (!popup) {
      setError('Pop-up blocked. In Chrome allow pop-ups for 127.0.0.1:5173, then try Print again.');
      return;
    }
    popup.document.title = 'PrintQueue: loading document';
    popup.document.body.textContent = 'Preparing print preview...';
    setPrintBusy(String(orderId) + '-' + file.id);
    let objectUrl;
    try {
      const response = await api('/owner/orders/' + orderId + '/files/' + file.id + '/print-preview');
      const blob = await response.blob();
      const mime = (blob.type || '').split(';')[0].toLowerCase();
      if (!['application/pdf', 'image/png', 'image/jpeg'].includes(mime)) {
        throw new Error('Unsupported print format: ' + (mime || 'unknown'));
      }
      objectUrl = URL.createObjectURL(blob);
      if (popup.closed) {
        URL.revokeObjectURL(objectUrl);
        setError('The print window was closed. Please try again.');
        return;
      }

      if (mime === 'application/pdf') {
        // Chrome's embedded PDF viewer does not reliably expose window.print().
        // Open the PDF directly so its native Print toolbar and Ctrl+P work.
        popup.location.replace(objectUrl);
        popup.focus();
        setNotice('PDF opened in Chrome. Click the printer icon in the PDF toolbar or press Ctrl+P to open the print dialog.');
        // Keep the blob URL alive while the native PDF viewer needs it.
        // Revoke only when this dashboard tab is unloaded.
        window.addEventListener('beforeunload', () => URL.revokeObjectURL(objectUrl), {once:true});
      } else {
        // For images, printing can be initiated reliably after the image loads.
        const escapedName = file.name || 'Document';
        popup.document.title = 'PrintQueue: ' + escapedName;
        popup.document.body.replaceChildren();
        const style = popup.document.createElement('style');
        style.textContent = 'body{margin:0;font-family:Arial,sans-serif}header{padding:12px;background:#eef2ff;display:flex;gap:16px;align-items:center}button{padding:10px 18px;cursor:pointer}img{display:block;max-width:100%;max-height:calc(100vh - 80px);margin:auto;object-fit:contain}@media print{header{display:none}img{max-height:none;width:auto;max-width:100%;page-break-inside:avoid}}';
        popup.document.head.appendChild(style);
        const header = popup.document.createElement('header');
        const label = popup.document.createElement('span');
        label.textContent = escapedName + ' · ' + file.mode + ' × ' + file.copies;
        const btn = popup.document.createElement('button');
        btn.textContent = 'Print (Ctrl+P)';
        btn.onclick = () => { popup.focus(); popup.print(); };
        header.append(label, btn);
        const img = popup.document.createElement('img');
        img.alt = escapedName;
        img.onload = () => { popup.focus(); popup.print(); };
        img.onerror = () => { popup.document.body.append('Could not load image for printing.'); };
        popup.document.body.append(header, img);
        img.src = objectUrl;
        popup.addEventListener('beforeunload', () => URL.revokeObjectURL(objectUrl), {once:true});
        setNotice('Image print window opened. If the dialog is not visible, click Print in that window.');
      }
    } catch (err) {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
      if (!popup.closed) popup.close();
      setError(readableError(err));
    } finally {
      setPrintBusy(null);
    }
  }
  async function saveLimits(e){
    e.preventDefault();setLimitsBusy(true);setError('');
    try{const r=await api('/owner/upload-settings',{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify({maxFiles:Number(limits.maxFiles),maxFileMb:Number(limits.maxFileMb)})});setLimits(await r.json());setNotice('Customer upload limits updated.');}
    catch(e){setError(readableError(e));}finally{setLimitsBusy(false);}
  }
  async function deleteOrderSet(order){
    if(!window.confirm(`Hide ALL ${order.files.length} documents in token ${order.token} from OWNER view? Customer copies remain available.`))return;
    try{await api(`/owner/orders/${order.id}/documents`,{method:'DELETE'});await load();setNotice('Entire document set hidden from owner view; customer files preserved.');}
    catch(e){setError(readableError(e));}
  }
  async function resetTokens(){
    if(!window.confirm('Start a NEW token series at 0001? Existing orders and tokens stay unchanged.'))return;
    try{const r=await api('/owner/tokens/reset',{method:'POST'});const data=await r.json();setNotice('Token series reset. Next token: '+data.nextToken);}
    catch(e){setError(readableError(e));}
  }
  async function hideOwnerFile(orderId, file) {
    if (!window.confirm(`Remove "${file.name}" from OWNER view only? Customer will still see the document. This does NOT erase it from disk.`)) return;
    setError(''); setNotice('');
    try {
      await api(`/owner/orders/${orderId}/files/${file.id}`, { method: 'DELETE' });
      await load();
      setNotice('Removed from owner view only. Customer can still see this document.');
    } catch(e) { setError(readableError(e)); }
  }
  async function downloadFile(orderId, file) {
    try {
      const response = await api(
        '/owner/orders/' + orderId + '/files/' + file.id
      );

      const blob = await response.blob();
      const url = URL.createObjectURL(blob);

      const link = document.createElement('a');
      link.href = url;
      link.download = file.name || 'document';
      document.body.appendChild(link);
      link.click();
      link.remove();

      setTimeout(() => URL.revokeObjectURL(url), 60000);
      setError('');
      setNotice('Download started: ' + (file.name || 'document'));
    } catch (err) {
      setError(readableError(err));
    }
  }

  async function saveBranding(e){
    e.preventDefault();setBrandingBusy(true);setError('');
    try{const form=new FormData();form.append('shopName',shopNameInput.trim());form.append('phone',shopPhone.trim());form.append('address',shopAddress.trim());form.append('receiptMessage',receiptMessage.trim());if(logoFile)form.append('logo',logoFile);form.append('removeLogo',String(removeLogo));
      const r=await api('/owner/shop-branding',{method:'PUT',body:form});const b=await r.json();setBrand(b);setShopNameInput(b.shopName);setShopPhone(b.phone||'');setShopAddress(b.address||'');setReceiptMessage(b.receiptMessage||'');setLogoFile(null);setRemoveLogo(false);setNotice('Shop branding saved. Customer portal and token receipts now show your shop.');
    }catch(e){setError(readableError(e));}finally{setBrandingBusy(false);}
  }
  return (
    <main>
      <header>
        <div>
          <small>PRINTQUEUE · PRIVATE OWNER VIEW</small>
          <h1>{brand.shopName} — Owner Dashboard</h1>
          <p>Only the authenticated owner can access queue documents</p>
        </div>

        {brand.logoDataUrl && <img src={brand.logoDataUrl} alt="Shop logo" style={{width:75,height:75,objectFit:"contain",marginLeft:"auto"}}/>}
        {credentials && (
          <button onClick={logout}>Sign out</button>
        )}
      </header>

      {error && <div role="alert" className="error">{error}</div>}
      {notice && <div role="status" className="success-banner">{notice}</div>}

      {!credentials ? (
        <section className="card">
          <h2>Owner sign in</h2>

          <form onSubmit={login}>
            <input
              placeholder="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />

            <input
              type="password"
              placeholder="Owner password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />

            <button type="submit" disabled={loading}>
              {loading ? 'Signing in...' : 'Sign in'}
            </button>
          </form>

          <p>
            Owner credentials are configured by the backend
            environment variables, not shared with customers.
          </p>
        </section>
      ) : (
        <>
          <section className="card"><h2>Shop branding</h2>
            <p>The shop name, logo and contact details appear on the customer portal and token receipts.</p>
            <form onSubmit={saveBranding} style={{display:'flex',gap:12,alignItems:'end',flexWrap:'wrap'}}>
              <label>Shop name<br/><input maxLength={100} required value={shopNameInput} onChange={e=>setShopNameInput(e.target.value)}/></label>
              <label>Shop phone<br/><input type="tel" maxLength={25} pattern="[0-9+() .-]*" value={shopPhone} onChange={e=>setShopPhone(e.target.value)} placeholder="Optional contact number"/></label>
              <label>Shop address<br/><input maxLength={220} value={shopAddress} onChange={e=>setShopAddress(e.target.value)} placeholder="Optional shop address"/></label>
              <label>Message on receipts<br/><input maxLength={250} value={receiptMessage} onChange={e=>setReceiptMessage(e.target.value)} placeholder="Thank you for visiting!"/></label>
              <label>Shop logo (PNG/JPG, up to 300 KB)<br/><input type="file" accept="image/png,image/jpeg" onChange={e=>setLogoFile(e.target.files?.[0]||null)}/></label>
              <label><input type="checkbox" checked={removeLogo} onChange={e=>setRemoveLogo(e.target.checked)}/> Remove logo</label>
              <button disabled={brandingBusy}>{brandingBusy?'Saving...':'Save shop branding'}</button>
            </form>
            {brand.logoDataUrl && <img src={brand.logoDataUrl} alt="Current shop logo" style={{width:100,height:100,objectFit:'contain',marginTop:12}}/>}
          </section>
          <section className="card">
            <h2>Printing prices (₹ per page)</h2>
            <form onSubmit={saveRates} style={{display:'flex',gap:12,flexWrap:'wrap',alignItems:'end'}}>
              <label>Black &amp; White<br/><input type="number" min="0" max="10000" step="0.01" required value={rates.bwRate} onChange={e=>setRates({...rates,bwRate:e.target.value})}/></label>
              <label>Color<br/><input type="number" min="0" max="10000" step="0.01" required value={rates.colorRate} onChange={e=>setRates({...rates,colorRate:e.target.value})}/></label>
              <button disabled={savingRates}>{savingRates?'Saving...':'Save rates'}</button>
            </form>
            <p>Saved rates apply to new orders. Previous order totals remain unchanged.</p>
          </section>
          <section className="card"><h2>Owner upload controls</h2>
            <form onSubmit={saveLimits} style={{display:'flex',gap:12,alignItems:'end',flexWrap:'wrap'}}>
              <label>Max documents per order<br/><input type="number" min="1" max="30" required value={limits.maxFiles} onChange={e=>setLimits({...limits,maxFiles:e.target.value})}/></label>
              <label>Max size per file (MB)<br/><input type="number" min="1" max="50" required value={limits.maxFileMb} onChange={e=>setLimits({...limits,maxFileMb:e.target.value})}/></label>
              <button disabled={limitsBusy}>{limitsBusy?'Saving...':'Save upload limits'}</button>
            </form>
            <p>Applies immediately to customer browser uploads and server validation. Max 30 documents and 50 MB per file.</p>
            <h3>Token sequence</h3><button type="button" onClick={resetTokens}>Reset next token to 0001</button>
            <p>Uses a new R-series to avoid duplicate tokens; existing orders are not removed.</p>
          </section>
          <a className="portal-link" href="/customer.html">Open Customer Test Portal →</a>
          <section className="stats">
            <article>
              <small>Total orders</small>
              <strong>{orders.length}</strong>
            </article>

            <article>
              <small>Waiting</small>
              <strong>
                {orders.filter(o => o.status === 'PENDING').length}
              </strong>
            </article>

            <article>
              <small>Ready</small>
              <strong>
                {orders.filter(o => o.status === 'READY').length}
              </strong>
            </article>
          </section>

          <section className="card">
            <h2>Inventory &amp; expenses</h2>
            <p>Track paper sheets manually, record ink/toner and other expenses, and estimate daily profit. These figures are for local shop planning.</p>
            {stocks.some(s=>s.sheets<=s.lowStockThreshold) && <div role="alert" className="error">Low stock: {stocks.filter(s=>s.sheets<=s.lowStockThreshold).map(s=>s.name+' ('+s.sheets+' sheets)').join(', ')}</div>}
            <h3>Paper inventory</h3>
            <form onSubmit={createStock} style={{display:'flex',gap:10,flexWrap:'wrap',alignItems:'end'}}>
              <label>Paper type<br/><input required maxLength={80} value={stockDraft.name} onChange={e=>setStockDraft({...stockDraft,name:e.target.value})}/></label>
              <label>Initial sheets<br/><input required type="number" min="0" max="10000000" value={stockDraft.sheets} onChange={e=>setStockDraft({...stockDraft,sheets:e.target.value})}/></label>
              <label>Low stock alert at<br/><input required type="number" min="0" value={stockDraft.lowStockThreshold} onChange={e=>setStockDraft({...stockDraft,lowStockThreshold:e.target.value})}/></label>
              <label>Cost / sheet (₹)<br/><input required type="number" min="0" step="0.01" value={stockDraft.costPerSheet} onChange={e=>setStockDraft({...stockDraft,costPerSheet:e.target.value})}/></label>
              <button disabled={inventoryBusy}>Add paper stock</button>
            </form>
            <div className="tablewrap"><table><thead><tr><th>Paper</th><th>Sheets</th><th>Threshold</th><th>Cost/sheet</th><th>Actions</th></tr></thead><tbody>
              {stocks.map(s=><tr key={s.id}><td>{s.name}{s.sheets<=s.lowStockThreshold?' ⚠ Low stock':''}</td><td>{s.sheets}</td><td>{s.lowStockThreshold}</td><td>{fmt(s.costPerSheet)}</td><td><button type="button" disabled={inventoryBusy} onClick={()=>{const n=window.prompt('Sheets to add', '100');if(n!==null)adjustStock(s,Number(n))}}>Restock</button> <button type="button" disabled={inventoryBusy} onClick={()=>{const n=window.prompt('Sheets used', '10');if(n!==null)adjustStock(s,-Number(n))}}>Use sheets</button> <button type="button" disabled={inventoryBusy} onClick={()=>updateStock(s)}>Edit</button></td></tr>)}
            </tbody></table></div>
            <p>Paper usage is recorded only when you click <strong>Use sheets</strong>. Printing and status changes do not automatically alter stock.</p>
            <h3>Record expense for {reportDate}</h3>
            <form onSubmit={addExpense} style={{display:'flex',gap:10,flexWrap:'wrap',alignItems:'end'}}>
              <label>Category<br/><select value={expenseDraft.category} onChange={e=>setExpenseDraft({...expenseDraft,category:e.target.value})}>{['INK_TONER','PAPER_PURCHASE','ELECTRICITY','MAINTENANCE','OTHER'].map(c=><option key={c} value={c}>{c.replace('_',' / ')}</option>)}</select></label>
              <label>Description<br/><input maxLength={180} required value={expenseDraft.description} onChange={e=>setExpenseDraft({...expenseDraft,description:e.target.value})}/></label>
              <label>Amount (₹)<br/><input type="number" min="0.01" step="0.01" required value={expenseDraft.amount} onChange={e=>setExpenseDraft({...expenseDraft,amount:e.target.value})}/></label>
              <button disabled={inventoryBusy||!reportDate}>Save expense</button>
            </form>
            <div style={{display:'flex',gap:10,flexWrap:'wrap',alignItems:'end',marginTop:14}}><label>Profit report date (IST)<br/><input type="date" value={reportDate} onChange={e=>{setReportDate(e.target.value);setDailyReport(null)}}/></label><button type="button" onClick={loadOperations}>Refresh inventory &amp; profit</button></div>
            {expenses&&<>
              <div className="stats" style={{marginTop:14}}><article><small>Paid revenue</small><strong>{fmt(expenses.collectedRevenue)}</strong></article><article><small>Recorded expenses</small><strong>{fmt(expenses.expenses)}</strong></article><article><small>Est. paper cost</small><strong>{fmt(expenses.estimatedPaperCost)}</strong></article><article><small>Est. profit</small><strong>{fmt(expenses.estimatedProfit)}</strong></article></div>
              <p>Estimated sheets for paid orders: {expenses.estimatedPaperSheets} · {expenses.paperCostConfigured?'Uses cost/sheet from the first paper stock entry.':'Add a paper stock entry to calculate estimated paper cost.'} Expenses are charged on the date entered; recorded purchases and estimated paper costs can overlap.</p>
              <div className="tablewrap"><table><thead><tr><th>Date</th><th>Category</th><th>Description</th><th>Amount</th><th></th></tr></thead><tbody>{expenses.expenseEntries.map(e=><tr key={e.id}><td>{e.expenseDate}</td><td>{e.category}</td><td>{e.description}</td><td>{fmt(e.amount)}</td><td><button type="button" className="secondary" onClick={()=>deleteExpense(e)}>Delete</button></td></tr>)}</tbody></table></div>
            </>}
          </section>
          <section className="card">
            <h2>Daily sales and payments</h2>
            <p>Amounts reflect payment statuses set by the owner, not verified online transactions. Cancelled orders are excluded from revenue totals. Reports use India time (Asia/Kolkata).</p>
            <div style={{display:'flex',gap:12,flexWrap:'wrap',alignItems:'end'}}>
              <label>Sales date<br/><input type="date" value={reportDate} onChange={e=>{setReportDate(e.target.value);setDailyReport(null)}}/></label>
              <button type="button" disabled={reportBusy||!reportDate} onClick={fetchDailyReport}>{reportBusy?'Loading...':'Generate daily report'}</button>
              <button type="button" disabled={!dailyReport} onClick={printDailyReport}>Print daily report</button>
            </div>
            {dailyReport && <>
              <div className="stats" style={{marginTop:16}}>
                <article><small>Orders</small><strong>{dailyReport.totalOrders}</strong></article>
                <article><small>Paid orders</small><strong>{dailyReport.paidOrders}</strong></article>
                <article><small>Collected</small><strong>{fmt(dailyReport.collectedAmount)}</strong></article>
                <article><small>Outstanding</small><strong>{fmt(dailyReport.outstandingAmount)}</strong></article>
              </div>
              <p>Cancelled: {dailyReport.cancelledOrders} · Unpaid: {dailyReport.unpaidOrders} · Booked (non-cancelled): {fmt(dailyReport.bookedAmount)}</p>
              <div className="tablewrap"><table><thead><tr><th>Token</th><th>Customer</th><th>Status</th><th>Payment</th><th>Total</th></tr></thead><tbody>{dailyReport.orders.map(o=><tr key={o.id}><td>{o.token}</td><td>{o.customerName}</td><td>{o.status}</td><td>{o.paymentStatus}</td><td>{fmt(o.totalAmount)}</td></tr>)}</tbody></table></div>
            </>}
          </section>
          <section className="card">
            <div className="queue-head"><div><h2>Print queue</h2><p>Search customer, token or document; filter by status.</p></div><button type="button" onClick={exportQueueCsv}>Export visible CSV</button> <button onClick={load}>Refresh queue</button></div>
            <div className="queue-filters"><label>Search orders<input aria-label="Search orders" type="search" placeholder="Token, customer or filename" value={query} onChange={e=>setQuery(e.target.value)}/></label><label>Status<select aria-label="Filter order status" value={statusFilter} onChange={e=>setStatusFilter(e.target.value)}>{['ALL','PENDING','PRINTING','READY','COLLECTED','CANCELLED'].map(s=><option key={s} value={s}>{s==='ALL'?'All statuses':s}</option>)}</select></label><label>Payment<select aria-label="Filter payment" value={paymentFilter} onChange={e=>setPaymentFilter(e.target.value)}><option value="ALL">All payments</option><option value="PAID">Paid</option><option value="UNPAID">Unpaid</option></select></label><label>Order date (IST)<input aria-label="Filter order date" type="date" value={historyDate} onChange={e=>setHistoryDate(e.target.value)}/></label><button type="button" onClick={()=>{setHistoryDate('');setPaymentFilter('ALL');setStatusFilter('ALL');setQuery('')}}>Clear filters</button><span className="queue-count">Showing {visibleOrders.length} / {orders.length}</span></div>

            <div className="bulk-tools"><span>{selected.length} selected</span><label>Set status <select value={bulkTarget} onChange={e=>setBulkTarget(e.target.value)}>{['PENDING','PRINTING','READY','COLLECTED','CANCELLED'].map(s=><option key={s}>{s}</option>)}</select></label><button type="button" disabled={bulkBusy||selected.length===0} onClick={applyBulk}>{bulkBusy?'Updating...':'Apply to selected'}</button><button type="button" disabled={!selected.length||bulkBusy} onClick={()=>setSelected([])}>Clear</button></div>
            <div className="tablewrap">
              <table>
                <thead>
                  <tr>
                    <th><input aria-label="Select visible orders" type="checkbox" checked={visibleOrders.length>0 && visibleOrders.every(o=>selected.includes(o.id))} onChange={e=>setSelected(prev=>e.target.checked?[...new Set([...prev,...visibleOrders.map(o=>o.id)])]:prev.filter(id=>!visibleOrders.some(o=>o.id===id)))} /></th><th>Token</th>
                    <th>Customer</th>
                    <th>Documents</th>
                    <th>Total</th><th>Payment</th>
                    <th>Status</th>
                  </tr>
                </thead>

                <tbody>
                  {visibleOrders.map(order => (
                    <tr key={order.id}>
                      <td><input aria-label={'Select '+order.token} type="checkbox" checked={selected.includes(order.id)} onChange={e=>setSelected(prev=>e.target.checked?[...prev,order.id]:prev.filter(id=>id!==order.id))}/></td><td>
                        <b>{order.token}</b>
                      </td>

                      <td>{order.customerName}</td>

                      <td>
                        <div><button type="button" className="secondary" onClick={()=>deleteOrderSet(order)}>Remove entire document set (owner only)</button></div>
                        {(order.files || []).map(file => (
                          <div
                            key={file.id}
                            className="fileline"
                          >
                            <span>
                              {file.name}{file.available === false ? " · FILE MISSING (re-upload)" : ""} · {file.mode} · {file.pages || '?'} page(s) × {file.copies} · {file.lineAmount == null ? 'Legacy pricing' : '₹'+Number(file.lineAmount).toFixed(2)}
                            </span>

                            <div className="file-actions">
                              <button type="button" className="print-button" disabled={file.available === false || printBusy === String(order.id)+'-'+file.id} onClick={() => printFile(order.id,file)}>
                                {printBusy === String(order.id)+'-'+file.id ? 'Opening...' : 'Print'}
                              </button>
                            <button
                              disabled={file.available === false} onClick={() =>
                                downloadFile(order.id, file)
                              }
                            >
                              Download
                            </button>
                            <button type="button" className="secondary" onClick={() => hideOwnerFile(order.id,file)} title="Hide only in owner dashboard; customer retains document">Delete (owner only)</button>
                            </div>
                          </div>
                        ))}
                      </td>

                      <td>{order.totalAmount == null ? 'Legacy' : '₹'+Number(order.totalAmount).toFixed(2)}</td>
                      <td>
                        {order.paymentMethod}

                        <small className="block">{order.paymentStatus}</small>
                        <select aria-label={'Payment status for '+order.token} value={order.paymentStatus==='PAID'?'PAID':'UNPAID'} disabled={paymentBusy===order.id} onChange={e=>setPaymentStatus(order,e.target.value)}>
                          <option value="UNPAID">UNPAID</option><option value="PAID">PAID</option>
                        </select>
                      </td>

                      <td>
                        <select
                          value={order.status}
                          disabled={statusBusy === order.id}
                          onChange={(e) =>
                            updateStatus(order.id, e.target.value)
                          }
                        >
                          {[
                            'PENDING',
                            'PRINTING',
                            'READY',
                            'COLLECTED',
                            'CANCELLED'
                          ].map(status => (
                            <option key={status} value={status}>
                              {status}
                            </option>
                          ))}
                        </select>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {visibleOrders.length === 0 && (
              <p>{orders.length ? 'No orders match your search or filters.' : 'No orders yet. Submit a test document from the Customer Portal.'}</p>
            )}
          </section>
        </>
      )}

      <footer>
        LOCAL TESTING ONLY. Use HTTPS, payment verification
        and production-grade security before launch.
        Existing Phase 3 anonymous orders remain owner-only.
      </footer>
    </main>
  );
}

createRoot(document.getElementById('root')).render(<App />);
