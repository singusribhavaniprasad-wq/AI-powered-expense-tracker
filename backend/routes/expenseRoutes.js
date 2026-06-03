import express from 'express';
import multer from 'multer';
import { Expense } from '../models/Expense.js';
import { processReceiptWithGemini, generateInsightsWithGemini } from '../services/geminiService.js';

const router = express.Router();

// Multer memory storage initialization for file stream buffering
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 10 * 1024 * 1024 } // limit 10MB
});

// 1. Invoice Upload and OCR Parsing Endpoint
router.post('/upload', upload.single('document'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'No file document uploaded.' });
    }

    const fileBuffer = req.file.buffer;
    const mimeType = req.file.mimetype;
    const docName = req.file.originalname;

    // Call Gemini 2.5 Flash service
    const rawResultJsonText = await processReceiptWithGemini(fileBuffer, mimeType);
    
    // Parse response
    let cleanJson = rawResultJsonText.trim();
    if (cleanJson.startsWith('```json')) {
      cleanJson = cleanJson.substring(7);
    }
    if (cleanJson.endsWith('```')) {
      cleanJson = cleanJson.substring(0, cleanJson.length - 3);
    }
    
    const parsedData = JSON.parse(cleanJson.trim());
    
    res.status(200).json({
      success: true,
      data: {
        totalAmount: parsedData.totalAmount || 0,
        billName: parsedData.billName || 'Invoice Descriptor',
        vendorName: parsedData.vendorName || 'Independent Merchant',
        date: parsedData.date || new Date().toISOString().split('T')[0],
        category: parsedData.category || 'Others',
        originalDocumentName: docName
      }
    });

  } catch (err) {
    console.error('Error processing upload and extraction OCR:', err);
    res.status(500).json({ error: err.message || 'Error occurred while running AI extraction OCR' });
  }
});

// 2. Generate AI-powered Financial Insights
router.get('/insights', async (req, res) => {
  try {
    const expenses = await Expense.find().sort({ date: -1 });
    if (expenses.length === 0) {
      return res.status(200).json({ 
        insights: 'Create and record your first set of transaction expenses to draw spending tips and advisory insights.' 
      });
    }

    const insights = await generateInsightsWithGemini(expenses);
    res.status(200).json({ insights });
  } catch (err) {
    console.error('Error in insights path:', err);
    res.status(500).json({ error: err.message || 'Error occurred while drafting insights' });
  }
});

// 3. READ: List all Expenses
router.get('/', async (req, res) => {
  try {
    const expenses = await Expense.find().sort({ date: -1 });
    res.status(200).json(expenses);
  } catch (err) {
    res.status(500).json({ error: 'Failed to seek recorded logs' });
  }
});

// 4. READ: View specified Expense by ID
router.get('/:id', async (req, res) => {
  try {
    const expense = await Expense.findById(req.params.id);
    if (!expense) return res.status(404).json({ error: 'Record not found' });
    res.status(200).json(expense);
  } catch (err) {
    res.status(500).json({ error: 'Failed to retrieve item' });
  }
});

// 5. CREATE: Manually Record an Expense
router.post('/', async (req, res) => {
  try {
    const { title, vendor, amount, category, date, originalDocumentName } = req.body;
    
    if (!title || !vendor || amount === undefined || !date) {
      return res.status(400).json({ error: 'Mandatory fields: title, vendor, amount, date' });
    }

    const newExpense = new Expense({
      title,
      vendor,
      amount,
      category,
      date,
      originalDocumentName
    });

    const saved = await newExpense.save();
    res.status(201).json(saved);
  } catch (err) {
    res.status(500).json({ error: 'Failed to record statement manual entries' });
  }
});

// 6. UPDATE: Correct elements of an Expense
router.put('/:id', async (req, res) => {
  try {
    const { title, vendor, amount, category, date } = req.body;
    
    const updated = await Expense.findByIdAndUpdate(
      req.params.id,
      { title, vendor, amount, category, date },
      { new: true, runValidators: true }
    );

    if (!updated) {
      return res.status(404).json({ error: 'Record could not be located to update' });
    }

    res.status(200).json(updated);
  } catch (err) {
    res.status(500).json({ error: 'Failed to commit updates' });
  }
});

// 7. DELETE: Eliminate an Expense log
router.delete('/:id', async (req, res) => {
  try {
    const deleted = await Expense.findByIdAndDelete(req.params.id);
    if (!deleted) {
      return res.status(404).json({ error: 'Record could not be located to delete' });
    }
    
    res.status(200).json({ success: true, message: 'Record purged successfully' });
  } catch (err) {
    res.status(500).json({ error: 'Failed to eliminate logging entries' });
  }
});

export default router;
