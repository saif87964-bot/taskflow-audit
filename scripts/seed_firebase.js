/**
 * Firebase Admin SDK seed script.
 * Run once to provision staff accounts and initial Firestore data.
 *
 * Usage:
 *   npm install firebase-admin
 *   node scripts/seed_firebase.js
 *
 * Prerequisites:
 *   1. Download your Firebase Admin service account JSON from:
 *      Firebase Console → Project Settings → Service Accounts → Generate new private key
 *   2. Set GOOGLE_APPLICATION_CREDENTIALS env var to its path:
 *      set GOOGLE_APPLICATION_CREDENTIALS=C:\path\to\serviceAccountKey.json
 *   3. Set your Firebase project ID below
 */

const admin = require('firebase-admin');

const PROJECT_ID = 'YOUR_FIREBASE_PROJECT_ID'; // ← replace this

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  projectId: PROJECT_ID
});

const auth = admin.auth();
const db = admin.firestore();

const DEFAULT_PIN = '1234'; // Staff will be prompted to change this on first login

const staff = [
  { shortId: 'im', fullName: 'Imtiaz M',  role: 'Senior Auditor',  initials: 'IM', colorHex: '#1565C0', isAdmin: false },
  { shortId: 'kh', fullName: 'Khalid H',  role: 'Audit Manager',   initials: 'KH', colorHex: '#00695C', isAdmin: true  },
  { shortId: 'av', fullName: 'Anita V',   role: 'Auditor',         initials: 'AV', colorHex: '#6A1B9A', isAdmin: false },
  { shortId: 'an', fullName: 'Amina N',   role: 'Tax Consultant',  initials: 'AN', colorHex: '#E65100', isAdmin: false },
  { shortId: 'jp', fullName: 'James P',   role: 'Partner',         initials: 'JP', colorHex: '#37474F', isAdmin: true  },
];

const engagements = [
  { id: 'e1', code: 'ABC-2026', name: 'Al Baneen Connect',       clientName: 'Al Baneen Connect',       type: 'AUDIT',    colorHex: '#1565C0', budgetHours: 20 },
  { id: 'e2', code: 'TRA-SFU',  name: 'TRA – Statutory Follow-up', clientName: 'TRA',                  type: 'TAX',      colorHex: '#E65100', budgetHours: 30 },
  { id: 'e3', code: 'MZL-2026', name: 'Moyo Zanzibar Limited',  clientName: 'Moyo Zanzibar Limited',   type: 'AUDIT',    colorHex: '#00695C', budgetHours: 15 },
  { id: 'e4', code: 'UTR-2026', name: 'Union Trust Resorts',    clientName: 'Union Trust Resorts Ltd', type: 'AUDIT',    colorHex: '#6A1B9A', budgetHours: 25 },
  { id: 'e5', code: 'ACL-2026', name: 'Aqua Cool Limited',      clientName: 'Aqua Cool Limited',       type: 'ADVISORY', colorHex: '#00838F', budgetHours: 10 },
  { id: 'e6', code: 'NJH-2026', name: 'New Jambiani Hotel',     clientName: 'New Jambiani Hotel',      type: 'AUDIT',    colorHex: '#F57F17', budgetHours: 20 },
  { id: 'e7', code: 'CPH-2026', name: 'CPHK',                   clientName: 'CPHK',                    type: 'TAX',      colorHex: '#880E4F', budgetHours: 15 },
  { id: 'e8', code: 'INT-ADM',  name: 'Admin / Internal',       clientName: 'Internal',                type: 'ADMIN',    colorHex: '#546E7A', budgetHours: 999 },
];

async function seed() {
  console.log('🌱 Seeding Firebase...\n');

  // Create Auth users + Firestore staff documents
  for (const s of staff) {
    const email = `${s.shortId}@taskflow.audit`;
    try {
      // Create Firebase Auth user
      let user;
      try {
        user = await auth.getUserByEmail(email);
        console.log(`  ↩  ${s.fullName} already exists (${user.uid})`);
      } catch {
        user = await auth.createUser({ email, password: DEFAULT_PIN, displayName: s.fullName });
        console.log(`  ✓  Created auth user: ${s.fullName} (${user.uid})`);
      }

      // Write Firestore staff document (uid as document ID)
      await db.collection('staff').doc(user.uid).set({
        uid: user.uid,
        shortId: s.shortId,
        fullName: s.fullName,
        role: s.role,
        initials: s.initials,
        colorHex: s.colorHex,
        isAdmin: s.isAdmin,
        email
      });
      console.log(`  ✓  Staff doc written for ${s.fullName}`);
    } catch (err) {
      console.error(`  ✗  ${s.fullName}: ${err.message}`);
    }
  }

  console.log('\n📁 Writing engagements...');
  for (const e of engagements) {
    await db.collection('engagements').doc(e.id).set({ ...e, isActive: true });
    console.log(`  ✓  ${e.name}`);
  }

  console.log('\n✅ Seed complete. All staff PIN is "1234" — instruct them to change it on first login.\n');
  process.exit(0);
}

seed().catch(err => {
  console.error('Seed failed:', err);
  process.exit(1);
});
