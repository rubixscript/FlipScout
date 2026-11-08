import React from 'react';
import { Card } from '../ui';

const Stat = ({ t, label, value, color }) => (
  <Card t={t} style={{ padding: '10px 12px' }}>
    <div style={{ fontSize: 11, color: t.muted, fontWeight: 700, letterSpacing: 0.4 }}>{label.toUpperCase()}</div>
    <div style={{ fontSize: 19, fontWeight: 800, letterSpacing: -0.4, color: color || t.text, marginTop: 2 }}>{value}</div>
  </Card>
);


export default Stat;
