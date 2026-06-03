import mongoose from 'mongoose';

const expenseSchema = new mongoose.Schema(
  {
    title: {
      type: String,
      required: true,
      trim: true
    },
    vendor: {
      type: String,
      required: true,
      trim: true
    },
    amount: {
      type: Number,
      required: true,
      min: 0
    },
    category: {
      type: String,
      required: true,
      enum: ['Food', 'Utility', 'Subscriptions', 'Others'],
      default: 'Others'
    },
    date: {
      type: String, // format YYYY-MM-DD for consistency
      required: true
    },
    originalDocumentName: {
      type: String,
      default: ''
    }
  },
  {
    timestamps: true
  }
);

export const Expense = mongoose.model('Expense', expenseSchema);
