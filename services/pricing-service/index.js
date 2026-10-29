const express = require('express');
const app = express();
app.get('/health', (req, res) =>
  res.json({ status: 'ok', service: process.env.SERVICE_NAME }));
// TODO: replace this placeholder with your real service code
app.listen(process.env.PORT || 3000, () =>
  console.log(process.env.SERVICE_NAME + ' listening'));
