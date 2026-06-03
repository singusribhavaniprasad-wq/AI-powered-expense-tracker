import React, { useState, useEffect } from 'react';
import { 
  PieChart, 
  Pie, 
  Cell, 
  ResponsiveContainer, 
  Tooltip, 
  Legend 
} from 'recharts';
import { 
  Plus, 
  Trash2, 
  Edit2, 
  Sparkles, 
  FileText, 
  PlusCircle, 
  Activity, 
  DollarSign, 
  Calendar, 
  Folder, 
  User, 
  Loader2,
  CheckCircle,
  XCircle,
  AlertTriangle
} from 'lucide-react';

const API_BASE = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
  ? 'http://localhost:8080/api/expenses'
  : '/api/expenses';

const CATEGORY_META = {
  Food: { color: '#FF7043', label: 'Food & Groceries' },
  Utility: { color: '#26A69A', label: 'Utility & Services' },
  Subscriptions: { color: '#5C6BC0', label: 'SaaS Subscriptions' },
  Others: { color: '#78909C', label: 'Others' }
};

export default function App() {
  const [expenses, setExpenses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [inspectedInsights, setInspectedInsights] = useState('');
  const [insightsLoading, setInsightsLoading] = useState(false);
  const [ocrLoading, setOcrLoading] = useState(false);
  const [errorText, setErrorText] = useState('');
  const [scannedResult, setScannedResult] = useState(null);

  // Modal & Edit fields
  const [showManualForm, setShowManualForm] = useState(false);
  const [showPresetsScanner, setShowPresetsScanner] = useState(false);
  const [editId, setEditId] = useState(null);

  // Form states
  const [formData, setFormData] = useState({
    title: '',
    vendor: '',
    amount: '',
    category: 'Food',
    date: new Date().toISOString().split('T')[0]
  });

  useEffect(() => {
    loadAllExpenses();
  }, []);

  const loadAllExpenses = async () => {
    setLoading(true);
    try {
      const res = await fetch(API_BASE);
      if (res.ok) {
        const data = await res.json();
        setExpenses(data);
      } else {
        setErrorText('Server communication failed when retrieving expense histories');
      }
    } catch (e) {
      setErrorText('Error connecting to Node backend. Verify server is running on port 8080');
    } finally {
      setLoading(false);
    }
  };

  const handleManualSave = async (e) => {
    e.preventDefault();
    const amt = parseFloat(formData.amount);
    if (!formData.title || !formData.vendor || isNaN(amt) || amt <= 0) {
      alert('Fill in all fields and double-check amount value.');
      return;
    }

    try {
      const url = editId ? `${API_BASE}/${editId}` : API_BASE;
      const method = editId ? 'PUT' : 'POST';

      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...formData, amount: amt })
      });

      if (res.ok) {
        loadAllExpenses();
        setShowManualForm(false);
        resetForm();
      } else {
        alert('Could not save expense entry to server database.');
      }
    } catch (error) {
      alert('Error saving data to database.');
    }
  };

  const resetForm = () => {
    setFormData({
      title: '',
      vendor: '',
      amount: '',
      category: 'Food',
      date: new Date().toISOString().split('T')[0]
    });
    setEditId(null);
  };

  const handleEditClick = (expense) => {
    setFormData({
      title: expense.title,
      vendor: expense.vendor,
      amount: expense.amount.toString(),
      category: expense.category,
      date: expense.date
    });
    setEditId(expense._id);
    setShowManualForm(true);
  };

  const handleDeleteClick = async (id) => {
    if (!window.confirm('Confirm delete this expense log?')) return;
    try {
      const res = await fetch(`${API_BASE}/${id}`, { method: 'DELETE' });
      if (res.ok) {
        loadAllExpenses();
      } else {
        alert('Purge request failed on server');
      }
    } catch (e) {
      alert('Unable to connect to service layer to delete database entry');
    }
  };

  const handleAISpendingInsights = async () => {
    setInsightsLoading(true);
    setInspectedInsights('');
    try {
      const res = await fetch(`${API_BASE}/insights`);
      if (res.ok) {
        const data = await res.json();
        setInspectedInsights(data.insights);
      } else {
        alert('Server returned error compiling spending insights.');
      }
    } catch (e) {
      alert('Network fault while executing insights services.');
    } finally {
      setInsightsLoading(false);
    }
  };

  // Preset Receipt Simulator for quick OCR testing
  const playPresetScanOCR = async (presetName, fileType = 'image/jpeg') => {
    setOcrLoading(true);
    setErrorText('');
    setScannedResult(null);

    // Hardcoded realistic thermal receipts represented syntactically
    const presetBodies = {
      WholeFoods: {
        totalAmount: 74.50,
        billName: "Weekly Organic Food and Produce Pack",
        vendorName: "Whole Foods Premium Market",
        date: "2026-05-28",
        category: "Food"
      },
      AWS: {
        totalAmount: 145.20,
        billName: "EC2 Cloud Instances and RDS Storage Billing",
        vendorName: "Amazon Web Services",
        date: "2026-06-01",
        category: "Subscriptions"
      },
      Comcast: {
        totalAmount: 89.99,
        billName: "Gigabit Highspeed Fiber Rental",
        vendorName: "Comcast Xfinity Premium",
        date: "2026-05-15",
        category: "Utility"
      }
    };

    // Simulate an actual fetch parsing payload from simulated Multipart / base64
    // We mock the service response structure directly so they get realistic parsing experience in static web clients
    setTimeout(async () => {
      const result = presetBodies[presetName];
      if (result) {
        setScannedResult(result);
      } else {
        setErrorText('Failed running simulated invoice parser OCR');
      }
      setOcrLoading(false);
    }, 1500);
  };

  const handleSaveScannedResult = async () => {
    if (!scannedResult) return;
    try {
      const res = await fetch(API_BASE, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          title: scannedResult.billName,
          vendor: scannedResult.vendorName,
          amount: parseFloat(scannedResult.totalAmount),
          category: scannedResult.category,
          date: scannedResult.date
        })
      });

      if (res.ok) {
        loadAllExpenses();
        setShowPresetsScanner(false);
        setScannedResult(null);
      } else {
        alert('Could not write OCR extracted logs to Database');
      }
    } catch (e) {
      alert('Network exception saving OCR results');
    }
  };

  // Compile calculations to show in dashboard
  const totalSpend = expenses.reduce((acc, curr) => acc + curr.amount, 0);

  const getCategorySum = (cat) => {
    return expenses
      .filter(e => e.category.toLowerCase() === cat.toLowerCase())
      .reduce((sum, curr) => sum + curr.amount, 0);
  };

  const chartData = [
    { name: 'Food', value: getCategorySum('Food'), color: CATEGORY_META.Food.color },
    { name: 'Utility', value: getCategorySum('Utility'), color: CATEGORY_META.Utility.color },
    { name: 'Subscriptions', value: getCategorySum('Subscriptions'), color: CATEGORY_META.Subscriptions.color },
    { name: 'Others', value: getCategorySum('Others'), color: CATEGORY_META.Others.color }
  ].filter(item => item.value > 0);

  return (
    <div style={{ minHeight: '100vh', backgroundColor: '#F8FAFC', paddingBottom: '80px' }}>
      {/* Header Bar */}
      <header style={{
        backgroundColor: '#0F172A',
        color: '#FFFFFF',
        padding: '16px 24px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <Activity size={24} style={{ color: '#38BDF8' }} />
          <h1 style={{ fontSize: '20px', fontWeight: 'bold', margin: 0, letterSpacing: '0.5px' }}>
            Portfolio AI Expense Tracker
          </h1>
        </div>
        <div style={{ display: 'flex', gap: '12px' }}>
          <button 
            onClick={() => setShowPresetsScanner(true)}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              backgroundColor: '#1E293B',
              border: '1px solid #334155',
              borderRadius: '6px',
              padding: '8px 16px',
              color: '#38BDF8',
              fontWeight: '6px',
              cursor: 'pointer',
              transition: 'background-color 0.2s'
            }}
          >
            <Sparkles size={16} />
            Scan Invoice
          </button>
          <button 
            onClick={() => { resetForm(); setShowManualForm(true); }}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              backgroundColor: '#38BDF8',
              border: 'none',
              borderRadius: '6px',
              padding: '8px 16px',
              color: '#0F172A',
              fontWeight: 'bold',
              cursor: 'pointer'
            }}
          >
            <Plus size={16} />
            Record Entry
          </button>
        </div>
      </header>

      {/* Main Container */}
      <main style={{ maxWidth: '1200px', margin: '40px auto 0 auto', padding: '0 24px', display: 'grid', gridTemplateColumns: '7fr 5fr', gap: '30px' }}>
        
        {/* Left Column: Metrics & Ledgers */}
        <section style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          
          {/* Summary Cards */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <div style={{
              backgroundColor: '#38BDF8',
              borderRadius: '12px',
              padding: '24px',
              color: '#0F172A'
            }}>
              <span style={{ fontSize: '12px', fontWeight: 'bold', textTransform: 'uppercase', opacity: 0.8 }}>Total Outflows Sum</span>
              <h2 style={{ fontSize: '36px', fontWeight: '900', margin: '4px 0 0 0' }}>
                ${totalSpend.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </h2>
            </div>
            <div style={{
              backgroundColor: '#FFFFFF',
              borderRadius: '12px',
              padding: '24px',
              boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
              border: '1px solid #E2E8F0',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center'
            }}>
              <span style={{ fontSize: '12px', fontWeight: 'bold', color: '#64748B', textTransform: 'uppercase' }}>Recorded Receipts</span>
              <h2 style={{ fontSize: '36px', fontWeight: '900', color: '#0F172A', margin: '4px 0 0 0' }}>
                {expenses.length} Entries
              </h2>
            </div>
          </div>

          {/* Database Log Table */}
          <div style={{
            backgroundColor: '#FFFFFF',
            borderRadius: '12px',
            border: '1px solid #E2E8F0',
            padding: '24px',
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
          }}>
            <h3 style={{ fontSize: '18px', fontWeight: 'bold', margin: '0 0 16px 0', color: '#0F172A' }}>Transaction History Ledger</h3>
            
            {loading ? (
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '40px 0', gap: '8px' }}>
                <Loader2 size={20} className="animate-spin" style={{ color: '#38BDF8' }} />
                <span>Synchronizing database entries...</span>
              </div>
            ) : expenses.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '40px 0', color: '#64748B' }}>
                <FileText size={40} style={{ margin: '0 auto 12px auto', opacity: 0.4 }} />
                <p style={{ margin: 0, fontWeight: '500' }}>No recorded statements inside MongoDB database.</p>
                <p style={{ margin: '4px 0 0 0', fontSize: '12px' }}>Use 'Record Entry' or OCR scanner above to save.</p>
              </div>
            ) : (
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                <thead>
                  <tr style={{ borderBottom: '2px solid #F1F5F9', color: '#64748B', fontSize: '11px', fontWeight: 'bold', textTransform: 'uppercase' }}>
                    <th style={{ padding: '8px 12px' }}>Category</th>
                    <th style={{ padding: '8px 12px' }}>Description</th>
                    <th style={{ padding: '8px 12px' }}>Merchant</th>
                    <th style={{ padding: '8px 12px' }}>Date</th>
                    <th style={{ padding: '8px 12px', textAlign: 'right' }}>Amount</th>
                    <th style={{ padding: '8px 12px', textAlign: 'center' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {expenses.map((expense) => {
                    const meta = CATEGORY_META[expense.category] || CATEGORY_META.Others;
                    return (
                      <tr key={expense._id} style={{ borderBottom: '1px solid #F1F5F9', fontSize: '14px' }}>
                        <td style={{ padding: '12px' }}>
                          <span style={{
                            backgroundColor: `${meta.color}15`,
                            color: meta.color,
                            fontWeight: 'bold',
                            fontSize: '11px',
                            padding: '4px 8px',
                            borderRadius: '20px'
                          }}>
                            {expense.category}
                          </span>
                        </td>
                        <td style={{ padding: '12px', fontWeight: 'bold', color: '#1E293B' }}>{expense.title}</td>
                        <td style={{ padding: '12px', color: '#475569' }}>{expense.vendor}</td>
                        <td style={{ padding: '12px', color: '#64748B', whiteSpace: 'nowrap' }}>{expense.date}</td>
                        <td style={{ padding: '12px', textAlign: 'right', fontWeight: 'bold', color: meta.color }}>
                          ${expense.amount.toFixed(2)}
                        </td>
                        <td style={{ padding: '12px', textAlign: 'center' }}>
                          <div style={{ display: 'flex', gap: '8px', justifyContent: 'center' }}>
                            <button 
                              onClick={() => handleEditClick(expense)}
                              style={{ background: 'none', border: 'none', color: '#64748B', cursor: 'pointer', padding: '4px' }}
                            >
                              <Edit2 size={14} />
                            </button>
                            <button 
                              onClick={() => handleDeleteClick(expense._id)}
                              style={{ background: 'none', border: 'none', color: '#EF4444', cursor: 'pointer', padding: '4px' }}
                            >
                              <Trash2 size={14} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        </section>

        {/* Right Column: Insights & Charts */}
        <section style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          
          {/* Charts Display */}
          <div style={{
            backgroundColor: '#FFFFFF',
            borderRadius: '12px',
            border: '1px solid #E2E8F0',
            padding: '24px',
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
          }}>
            <h3 style={{ fontSize: '18px', fontWeight: 'bold', margin: '0 0 16px 0', color: '#0F172A' }}>Donut Allocation Chart</h3>
            
            {chartData.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '40px 0', color: '#64748B' }}>
                <p>Add recorded logs to draw distribution models.</p>
              </div>
            ) : (
              <div style={{ width: '100%', height: '240px' }}>
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={chartData}
                      innerRadius={60}
                      outerRadius={80}
                      paddingAngle={5}
                      dataKey="value"
                    >
                      {chartData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip formatter={(value) => `$${value.toFixed(2)}`} />
                    <Legend verticalAlign="bottom" height={36} />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            )}
          </div>

          {/* AI Advisor Panel */}
          <div style={{
            background: 'linear-gradient(135deg, #1E293B 0%, #0F172A 100%)',
            color: '#FFFFFF',
            borderRadius: '12px',
            padding: '24px',
            boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Sparkles size={18} style={{ color: '#38BDF8' }} />
                <h4 style={{ fontSize: '16px', fontWeight: 'bold', margin: 0 }}>AI Spending Insights</h4>
              </div>
              <button 
                onClick={handleAISpendingInsights}
                disabled={insightsLoading}
                style={{
                  backgroundColor: '#38BDF8',
                  color: '#0F172A',
                  border: 'none',
                  borderRadius: '4px',
                  padding: '6px 12px',
                  fontSize: '12px',
                  fontWeight: 'bold',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px'
                }}
              >
                {insightsLoading ? (
                  <Loader2 size={12} className="animate-spin" />
                ) : (
                  'Generate'
                )}
              </button>
            </div>

            {insightsLoading ? (
              <p style={{ fontSize: '13px', color: '#94A3B8', margin: 0 }}>Gemini 2.5 Flash is analyzing logs...</p>
            ) : inspectedInsights ? (
              <div style={{ fontSize: '13px', color: '#CDD5E0', lineHeight: '1.6', whiteSpace: 'pre-wrap' }}>
                {inspectedInsights}
              </div>
            ) : (
              <p style={{ fontSize: '13px', color: '#94A3B8', margin: 0 }}>
                Click 'Generate' to trigger Gemini model analysis to pinpoint overspending, detect regular SaaS charges, and outline active budgets.
              </p>
            )}
          </div>
        </section>
      </main>

      {/* MODAL: Manual Entry Form */}
      {showManualForm && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          width: '100%',
          height: '100%',
          backgroundColor: 'rgba(15, 23, 42, 0.6)',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          zIndex: 1000
        }}>
          <div style={{
            backgroundColor: '#FFFFFF',
            borderRadius: '12px',
            padding: '24px',
            width: '450px',
            boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1)'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
              <h4 style={{ fontSize: '18px', fontWeight: 'bold', margin: 0, color: '#0F172A' }}>
                {editId ? 'Edit Statement Details' : 'Record Manual Entry'}
              </h4>
              <button onClick={() => setShowManualForm(false)} style={{ background: 'none', border: 'none', fontSize: '18px', cursor: 'pointer', color: '#64748B' }}>×</button>
            </div>

            <form onSubmit={handleManualSave} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '12px', fontWeight: 'bold', display: 'block', color: '#475569', marginBottom: '4px' }}>Item Title</label>
                <input 
                  type="text"
                  placeholder="e.g. Server hosting"
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                  style={{ width: '100%', padding: '8px', border: '1px solid #CBD5E1', borderRadius: '4px', boxSizing: 'border-box' }}
                />
              </div>

              <div>
                <label style={{ fontSize: '12px', fontWeight: 'bold', display: 'block', color: '#475569', marginBottom: '4px' }}>Merchant / Vendor</label>
                <input 
                  type="text"
                  placeholder="e.g. AWS Europe"
                  value={formData.vendor}
                  onChange={(e) => setFormData({ ...formData, vendor: e.target.value })}
                  style={{ width: '100%', padding: '8px', border: '1px solid #CBD5E1', borderRadius: '4px', boxSizing: 'border-box' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.2fr', gap: '10px' }}>
                <div>
                  <label style={{ fontSize: '12px', fontWeight: 'bold', display: 'block', color: '#475569', marginBottom: '4px' }}>Amount ($)</label>
                  <input 
                    type="number"
                    step="0.01"
                    placeholder="25.50"
                    value={formData.amount}
                    onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
                    style={{ width: '100%', padding: '8px', border: '1px solid #CBD5E1', borderRadius: '4px', boxSizing: 'border-box' }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: '12px', fontWeight: 'bold', display: 'block', color: '#475569', marginBottom: '4px' }}>Category</label>
                  <select 
                    value={formData.category}
                    onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                    style={{ width: '100%', padding: '8px', border: '1px solid #CBD5E1', borderRadius: '4px', boxSizing: 'border-box', backgroundColor: '#FFF' }}
                  >
                    <option value="Food">Food</option>
                    <option value="Utility">Utility</option>
                    <option value="Subscriptions">Subscriptions</option>
                    <option value="Others">Others</option>
                  </select>
                </div>
              </div>

              <div>
                <label style={{ fontSize: '12px', fontWeight: 'bold', display: 'block', color: '#475569', marginBottom: '4px' }}>Date</label>
                <input 
                  type="date"
                  value={formData.date}
                  onChange={(e) => setFormData({ ...formData, date: e.target.value })}
                  style={{ width: '100%', padding: '8px', border: '1px solid #CBD5E1', borderRadius: '4px', boxSizing: 'border-box' }}
                />
              </div>

              <div style={{ display: 'flex', gap: '10px', marginTop: '12px' }}>
                <button 
                  type="button" 
                  onClick={() => setShowManualForm(false)}
                  style={{ flex: 1, padding: '10px', backgroundColor: '#F1F5F9', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold' }}
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  style={{ flex: 1, padding: '10px', backgroundColor: '#38BDF8', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold', color: '#0F172A' }}
                >
                  Confirm Save
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: Presets Invoice Scanner */}
      {showPresetsScanner && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          width: '100%',
          height: '100%',
          backgroundColor: 'rgba(15, 23, 42, 0.6)',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          zIndex: 1000
        }}>
          <div style={{
            backgroundColor: '#FFFFFF',
            borderRadius: '12px',
            padding: '24px',
            width: '500px',
            boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1)'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
              <h4 style={{ fontSize: '18px', fontWeight: 'bold', margin: 0, color: '#0F172A' }}>
                AI Digital Invoice OCR Scanner
              </h4>
              <button onClick={() => setShowPresetsScanner(false)} style={{ background: 'none', border: 'none', fontSize: '18px', cursor: 'pointer', color: '#64748B' }}>×</button>
            </div>

            <p style={{ margin: '0 0 16px 0', fontSize: '13px', color: '#64748B', lineHeight: '1.5' }}>
              Select one of the digital statements below to simulate uploading receipts to the Gemini 2.5 Flash API for multi-modal parsing and categorization:
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '20px' }}>
              <button 
                onClick={() => playPresetScanOCR('WholeFoods')}
                disabled={ocrLoading}
                style={{
                  padding: '12px',
                  backgroundColor: '#F8FAFC',
                  border: '1px solid #E2E8F0',
                  borderRadius: '6px',
                  textAlign: 'left',
                  cursor: 'pointer',
                  fontWeight: '600'
                }}
              >
                Simulate Whole Foods Groceries Receipt ($74.50)
              </button>
              <button 
                onClick={() => playPresetScanOCR('AWS')}
                disabled={ocrLoading}
                style={{
                  padding: '12px',
                  backgroundColor: '#F8FAFC',
                  border: '1px solid #E2E8F0',
                  borderRadius: '6px',
                  textAlign: 'left',
                  cursor: 'pointer',
                  fontWeight: '600'
                }}
              >
                Simulate Amazon Web Services Bill ($145.20)
              </button>
              <button 
                onClick={() => playPresetScanOCR('Comcast')}
                disabled={ocrLoading}
                style={{
                  padding: '12px',
                  backgroundColor: '#F8FAFC',
                  border: '1px solid #E2E8F0',
                  borderRadius: '6px',
                  textAlign: 'left',
                  cursor: 'pointer',
                  fontWeight: '600'
                }}
              >
                Simulate Comcast Xfinity Broadband Statement ($89.99)
              </button>
            </div>

            {ocrLoading && (
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', padding: '16px', backgroundColor: '#F1F5F9', borderRadius: '6px' }}>
                <Loader2 size={16} className="animate-spin" style={{ color: '#38BDF8' }} />
                <span style={{ fontSize: '12px', fontWeight: 'bold' }}>Gemini Extraction In Progress...</span>
              </div>
            )}

            {scannedResult && (
              <div style={{ padding: '16px', backgroundColor: '#ECFDF5', border: '1px solid #A7F3D0', borderRadius: '6px', marginBottom: '16px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '10px', color: '#047857' }}>
                  <CheckCircle size={16} />
                  <span style={{ fontSize: '13px', fontWeight: 'bold' }}>Gemini OCR Parsing Successful!</span>
                </div>
                <div style={{ fontSize: '12px', display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <div><strong>Merchant:</strong> {scannedResult.vendorName}</div>
                  <div><strong>Descriptor:</strong> {scannedResult.billName}</div>
                  <div><strong>Total Amount:</strong> ${scannedResult.totalAmount.toFixed(2)}</div>
                  <div><strong>Date:</strong> {scannedResult.date}</div>
                  <div><strong>Category:</strong> {scannedResult.category}</div>
                </div>
                <button 
                  onClick={handleSaveScannedResult}
                  style={{
                    marginTop: '12px',
                    width: '100%',
                    padding: '8px',
                    backgroundColor: '#10B981',
                    border: 'none',
                    borderRadius: '4px',
                    color: '#FFF',
                    fontWeight: 'bold',
                    cursor: 'pointer'
                  }}
                >
                  Save Parsed Expense into MDB
                </button>
              </div>
            )}

            <button 
              onClick={() => setShowPresetsScanner(false)}
              style={{
                width: '100%',
                padding: '10px',
                backgroundColor: '#F1F5F9',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer',
                fontWeight: 'bold'
              }}
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
