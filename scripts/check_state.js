const { initializeApp, cert } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');

initializeApp({ credential: cert(require('./service-account.json')) });
const db = getFirestore();

(async () => {
  const staff = await db.collection('staff').get();
  console.log('--- STAFF ---');
  staff.forEach(d => {
    const s = d.data();
    console.log(`${s.shortId}  ${s.fullName}  isAdmin=${s.isAdmin}  pendingPinReset=${s.pendingPinReset ?? false}`);
  });
  const sessions = await db.collection('timeSessions').get();
  console.log(`--- TIME SESSIONS: ${sessions.size} total ---`);
  sessions.forEach(d => {
    const s = d.data();
    console.log(`staff=${s.staffId.slice(0,8)} eng=${s.engagementId} active=${s.isActive} start=${s.startTime ? s.startTime.toDate().toISOString() : 'null'}`);
  });
  process.exit(0);
})().catch(e => { console.error(e.message); process.exit(1); });
