# Vercel Production Deployment Configuration File

This directory contains key information, environment parameters, and configurations for deploying the AI Expense Tracker to Vercel as a full-stack monorepo application.

## Vercel Application Structure

The project is pre-configured to build both:
1. **Frontend**: Static React.js built from `/frontend` and routed globally.
2. **Backend**: Express.js serverless API routes running on Node.js runtime, bound under `/api/*` routes.

## Deployment Steps

1. **Install Vercel CLI** (Optionally for local testing):
   ```bash
   npm install -g vercel
   ```

2. **Login and Link Project**:
   ```bash
   vercel login
   ```

3. **Configure Environment Parameters** inside Vercel Dashboard -> Project Settings -> Environment Variables:
   - `MONGODB_URI`: Your production MongoDB Atlas Connection String
   - `GEMINI_API_KEY`: Your Google AI Studio Gemini API Key
   - `PORT`: 8080 (or any customized backup port)

4. **Deploy Application**:
   ```bash
   vercel --prod
   ```

## Production Routing & Vercel.json

The central `vercel.json` file in the root workspace automates this multi-build routing.
It tells Vercel to:
- Feed `/api/*` requests directly to backend Express routes.
- Build the static React distribution and serve files seamlessly.
