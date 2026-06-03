import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import { connectDB } from './config/db.js';
import expenseRoutes from './routes/expenseRoutes.js';

dotenv.config();

const app = express();

// Middlewares
app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

// Health Check Endpoint
app.get('/api/health', (req, res) => {
  res.status(200).json({ status: 'OK', message: 'Expense Tracker API is fully operational' });
});

// Routes
app.use('/api/expenses', expenseRoutes);

// Global Error Handler
app.use((err, req, res, next) => {
  console.error('Unhandled Error:', err);
  res.status(err.status || 500).json({
    error: {
      message: err.message || 'Internal Server Error',
      status: err.status || 500
    }
  });
});

// Configure Port
const PORT = process.env.PORT || 8080;

// Initialize Database & Start Server
const startServer = async () => {
  try {
    await connectDB();
    app.listen(PORT, () => {
      console.log(`Server running on port ${PORT}`);
    });
  } catch (error) {
    console.error('Fatal database initialization failed:', error);
    process.exit(1);
  }
};

if (!process.env.VERCEL) {
  startServer();
} else {
  // Connect to DB for Vercel Serverless environment
  connectDB().catch(err => console.error('MongoDB async connection error for Vercel:', err));
}

export default app;
