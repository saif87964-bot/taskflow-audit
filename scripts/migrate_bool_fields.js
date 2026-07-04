/**
 * One-time migration: earlier app builds serialized Kotlin "isXxx" booleans
 * as "xxx" (active / admin). Merge those rogue fields back into the proper
 * names and delete them.
 */
const { initializeApp, cert } = require('firebase-admin/app');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');

initializeApp({ credential: cert(require('./service-account.json')) });
const db = getFirestore();

async function migrate(collection, wrongName, rightName) {
  const snap = await db.collection(collection).get();
  let fixed = 0;
  for (const doc of snap.docs) {
    const data = doc.data();
    if (data[wrongName] === undefined) continue;
    const value = data[rightName] !== undefined ? data[rightName] : data[wrongName];
    await doc.ref.update({ [rightName]: value, [wrongName]: FieldValue.delete() });
    console.log(`  ✓ ${collection}/${doc.id}: ${rightName}=${value} ("${wrongName}" removed)`);
    fixed++;
  }
  console.log(`${collection}: ${fixed} doc(s) migrated`);
}

(async () => {
  await migrate('timeSessions', 'active', 'isActive');
  await migrate('engagements', 'active', 'isActive');
  await migrate('staff', 'admin', 'isAdmin');
  console.log('Done.');
  process.exit(0);
})().catch(e => { console.error(e.message); process.exit(1); });
