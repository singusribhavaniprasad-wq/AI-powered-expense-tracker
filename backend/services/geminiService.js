import { GoogleGenAI } from '@google/genai';

/**
 * Extracts invoice/bill details using the modern @google/genai SDK on Gemini 2.5 Flash
 * @param {Buffer} base64Buffer Raw file buffer
 * @param {string} mimeType File MIME type
 * @returns {Promise<string>} Response text representing raw serialized extracted JSON
 */
export const processReceiptWithGemini = async (base64Buffer, mimeType) => {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    throw new Error('GEMINI_API_KEY environment variable is missing from server configuration.');
  }

  const ai = new GoogleGenAI({ apiKey });

  const promptText = `
    You are an expert AI receipt OCR scanner.
    Analyze this invoice, receipt, or bill and extract:
    1. Total amount (Number: extract decimal digits only, ignore currency symbols).
    2. Invoice/Bill name (String: brief descriptor of what was purchased).
    3. Vendor/Merchant name (String: store name, company name).
    4. Date (String: Format as YYYY-MM-DD. If year is missing, assume current year 2026).
    5. Category (String: Classify strictly as one of: "Food", "Utility", "Subscriptions", "Others"). Use rules:
       - "Food": restaurants, grocery, Starbucks, UberEats, McDonald's.
       - "Utility": power, electricity, water, internet, Comcast, AT&T.
       - "Subscriptions": Spotify, Netflix, AWS, OpenAI, GitHub, SaaS.
       - "Others": any other retail, department store, clothes, unclassified.

    Return strictly a valid JSON object matching this schema exactly, do not output any surrounding text or markdown formatting:
    {
      "totalAmount": 0.0,
      "billName": "",
      "vendorName": "",
      "date": "YYYY-MM-DD",
      "category": ""
    }
  `;

  try {
    const response = await ai.models.generateContent({
      model: 'gemini-2.5-flash',
      contents: [
        {
          role: 'user',
          parts: [
            { text: promptText },
            {
              inlineData: {
                data: base64Buffer.toString('base64'),
                mimeType: mimeType
              }
            }
          ]
        }
      ],
      generationConfig: {
        responseMimeType: 'application/json',
        temperature: 0.1
      }
    });

    if (!response || !response.text) {
      throw new Error('Received an empty response from Gemini 2.5 API');
    }

    return response.text;
  } catch (error) {
    console.error('Gemini OCR API execution error:', error.message);
    throw new Error(`Gemini AI Extraction Failed: ${error.message}`);
  }
};

/**
 * Generates spending recommendations and insights based on logs data
 * @param {Array} expenseData List of raw expense logs
 * @returns {Promise<string>} Textual insights
 */
export const generateInsightsWithGemini = async (expenseData) => {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    throw new Error('GEMINI_API_KEY is not configured on the backend.');
  }

  const ai = new GoogleGenAI({ apiKey });

  const logsText = expenseData.map(e => 
    `- Merchant: ${e.vendor}, Description: ${e.title}, Amount: $${e.amount}, Category: ${e.category}, Date: ${e.date}`
  ).join('\n');

  const promptText = `
    You are a senior financial advisor and assistant.
    Review the following list of raw category logs and generate bulletproof AI-powered spending insights:
    - Detect overspending trends or high spending clusters.
    - Identify categories that occupy too much of the budget.
    - Suggest key budgeting improvements and clear actionable ideas.
    - Highlight recurring SaaS or media subscriptions if detected.

    Keep the tone extremely professional, clean, analytical, and action-oriented.
    DO NOT include any emojis anywhere.
    Format the response as beautiful structured text with bullet points.

    Expense Log Data:
    ${logsText}
  `;

  try {
    const response = await ai.models.generateContent({
      model: 'gemini-2.5-flash',
      contents: [
        {
          role: 'user',
          parts: [{ text: promptText }]
        }
      ],
      generationConfig: {
        temperature: 0.3
      }
    });

    return response.text || 'Unable to analyze spent profiles at this time.';
  } catch (error) {
    console.error('Gemini Insights Service Error:', error.message);
    throw new Error(`Gemini Insights Failed: ${error.message}`);
  }
};
