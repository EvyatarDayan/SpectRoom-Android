#!/usr/bin/env node
/**
 * Creates 10 ready-to-use test accounts:
 *   tomato1@yopmail.com … tomato10@yopmail.com
 * Password for all: Password1
 *
 * Profiles are complete (discover-ready) and tagged mfaEnrolled=true so the app
 * skips email/SMS MFA enrollment (see MfaAuthRepository.isMfaEnrolledOrBypassed).
 *
 * Usage (from this repo root):
 *   node scripts/seed_tomato_users.js
 *
 * Optional Admin upgrade (sets emailVerified in Firebase Auth when credentials exist):
 *   GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json node scripts/seed_tomato_users.js
 */

const https = require('https');
const fs = require('fs');
const path = require('path');

const PROJECT_ID = 'spectroom-553a0';
const PASSWORD = 'Password1';
const COUNT = 10;
const TEST_ACCOUNT_KEY = 'tomato1-tomato10';

function readApiKey() {
  const googleServicesPath = path.resolve(__dirname, '../app/google-services.json');
  const json = JSON.parse(fs.readFileSync(googleServicesPath, 'utf8'));
  const key = json.client?.[0]?.api_key?.[0]?.current_key;
  if (!key) throw new Error(`API key not found in ${googleServicesPath}`);
  return key;
}

const API_KEY = readApiKey();

const FIRST_NAMES = [
  'Ella', 'Maja', 'Saga', 'Viktor', 'Noah', 'Alma', 'Leo', 'Freja', 'Oscar', 'Nora',
];

const LAST_NAMES = [
  'Lindberg', 'Bergström', 'Karlsson', 'Eriksson', 'Johansson', 'Nilsson', 'Holm',
  'Lundgren', 'Sandberg', 'Forsberg',
];

const INTERESTS = [
  'Photography', 'Music', 'Art', 'Reading', 'Cooking', 'Travel', 'Sports', 'Gaming',
  'Movies', 'Nature', 'Technology', 'Fitness', 'Dancing', 'Writing', 'Gardening',
  'Animals', 'Science', 'History', 'Languages', 'Volunteering',
];

const GENDERS = ['Man', 'Woman', 'Non-binary'];
const LOOKING_FOR = [
  'Romantic relationship',
  'In-Person Friendship',
  'Online Friendship',
  'Hobby Buddy',
];
const LIFESTYLES = ['independent', 'familySupport', 'assistedSetting'];
const BODY_TYPES = ['slim', 'average', 'athletic', 'fullBodied'];
const FIRST_DATES = [
  'relaxingParkWalk',
  'cozyCafeMeet',
  'museumGalleryStroll',
  'cinemaMovieNight',
  'friendlyVideoChat',
];
const SENSORY = [
  'lowNoiseEnvironments',
  'dimSoftLighting',
  'activeWalkingDates',
  'shortLowPressureMeetups',
  'visuallyCalmSpaces',
];

const CITIES = [
  { city: 'Stockholm', latitude: 59.3293, longitude: 18.0686 },
  { city: 'Göteborg', latitude: 57.7089, longitude: 11.9746 },
  { city: 'Malmö', latitude: 55.6050, longitude: 13.0038 },
  { city: 'Uppsala', latitude: 59.8586, longitude: 17.6389 },
  { city: 'Lund', latitude: 55.7047, longitude: 13.1910 },
  { city: 'Umeå', latitude: 63.8258, longitude: 20.2630 },
  { city: 'Örebro', latitude: 59.2753, longitude: 15.2134 },
  { city: 'Linköping', latitude: 58.4108, longitude: 15.6214 },
  { city: 'Helsingborg', latitude: 56.0465, longitude: 12.6945 },
  { city: 'Jönköping', latitude: 57.7826, longitude: 14.1618 },
];

const BIOS = [
  'Coffee lover who enjoys quiet museum afternoons and spontaneous city walks. Looking for genuine connection and good conversation.',
  'Weekend hiker, weekday reader. I appreciate honesty, humor, and people who are kind without trying too hard.',
  'Into live music, board games, and trying new recipes. Hoping to meet someone curious about the world and comfortable being themselves.',
  'Nature resets me — forests, lakes, and long chats afterward. Open to friendship or something more with the right person.',
  'Film nerd with a soft spot for cozy cafés. I value clear communication and low-pressure ways to get to know each other.',
  'Runner, podcast addict, and amateur photographer. Always happy to explore a new neighborhood or find a great lunch spot.',
  'Creative soul who loves art galleries and calm evenings in. Seeking someone patient, warm, and up for both adventure and downtime.',
  'Dog person, salsa beginner, and travel planner. Let us swap stories over coffee and see where it goes.',
  'Tech by day, pottery class by night. I enjoy thoughtful people who do not take themselves too seriously.',
  'Bookshop wanderer and thrift-store treasure hunter. Looking for kindness, laughter, and shared little adventures.',
];

function request(method, url, body, headers = {}) {
  const data = body == null ? null : JSON.stringify(body);
  return new Promise((resolve, reject) => {
    const u = new URL(url);
    const req = https.request(
      {
        hostname: u.hostname,
        path: u.pathname + u.search,
        method,
        headers: {
          ...(data
            ? { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(data) }
            : {}),
          ...headers,
        },
      },
      (res) => {
        let chunks = '';
        res.on('data', (c) => (chunks += c));
        res.on('end', () => {
          let parsed = null;
          try {
            parsed = chunks ? JSON.parse(chunks) : null;
          } catch {
            parsed = { raw: chunks };
          }
          resolve({ status: res.statusCode, body: parsed, raw: chunks });
        });
      },
    );
    req.on('error', reject);
    if (data) req.write(data);
    req.end();
  });
}

function pick(arr, rng) {
  return arr[Math.floor(rng() * arr.length)];
}

function pickN(arr, n, rng) {
  const copy = [...arr];
  const out = [];
  while (out.length < n && copy.length) {
    const idx = Math.floor(rng() * copy.length);
    out.push(copy.splice(idx, 1)[0]);
  }
  return out;
}

function mulberry32(seed) {
  return function () {
    let t = (seed += 0x6d2b79f5);
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function firestoreValue(value) {
  if (value === null || value === undefined) return { nullValue: null };
  if (typeof value === 'string') return { stringValue: value };
  if (typeof value === 'boolean') return { booleanValue: value };
  if (typeof value === 'number') {
    if (Number.isInteger(value)) return { integerValue: String(value) };
    return { doubleValue: value };
  }
  if (Array.isArray(value)) {
    return { arrayValue: { values: value.map(firestoreValue) } };
  }
  if (typeof value === 'object') {
    const fields = {};
    for (const [k, v] of Object.entries(value)) {
      fields[k] = firestoreValue(v);
    }
    return { mapValue: { fields } };
  }
  throw new Error(`Unsupported value type: ${typeof value}`);
}

function toFirestoreFields(obj) {
  const fields = {};
  for (const [k, v] of Object.entries(obj)) {
    fields[k] = firestoreValue(v);
  }
  return fields;
}

function buildProfile(index) {
  const rng = mulberry32(9000 + index * 131);
  const gender = pick(GENDERS, rng);
  const interestedIn =
    gender === 'Man'
      ? pick([['Woman'], ['Woman', 'Non-binary'], ['Anyone']], rng)
      : gender === 'Woman'
        ? pick([['Man'], ['Man', 'Non-binary'], ['Anyone']], rng)
        : pick([['Anyone'], ['Man', 'Woman'], ['Non-binary']], rng);

  const age = 20 + Math.floor(rng() * 11); // 20–30
  const birthYear = new Date().getFullYear() - age;
  const birthMonth = 1 + Math.floor(rng() * 12);
  const birthDay = 1 + Math.floor(rng() * 28);
  const dateOfBirth = Date.UTC(birthYear, birthMonth - 1, birthDay) / 1000;
  const now = Date.now() / 1000;

  const city = pick(CITIES, rng);
  const first = FIRST_NAMES[index - 1];
  const last = LAST_NAMES[index - 1];
  const name = `${first} ${last}`;

  const preferredAgeMin = Math.max(18, age - 3 - Math.floor(rng() * 2));
  const preferredAgeMax = Math.min(40, age + 4 + Math.floor(rng() * 4));

  const photoSeed = `tomato-user-${index}`;
  const photos = [1, 2, 3].map(
    (n) => `https://picsum.photos/seed/${photoSeed}-${n}/800/1000`,
  );
  const profilePhotoURL = `https://picsum.photos/seed/${photoSeed}-avatar/600/600`;

  const energyRoll = rng();
  const energyLevel = energyRoll < 0.33 ? 0 : energyRoll < 0.66 ? 2 : 4;

  return {
    id: `tomato-${index}-${Date.now()}`,
    name,
    email: `tomato${index}@yopmail.com`,
    age,
    dateOfBirth,
    bio: BIOS[index - 1],
    interests: pickN(INTERESTS, 3 + Math.floor(rng() * 3), rng),
    gender,
    interestedIn,
    lookingFor: pickN(LOOKING_FOR, 1 + Math.floor(rng() * 2), rng),
    preferredAgeMin,
    preferredAgeMax,
    lifestyle: pick(LIFESTYLES, rng),
    shareLifestyle: rng() > 0.25,
    bodyType: pick(BODY_TYPES, rng),
    shareBodyType: rng() > 0.3,
    idealFirstDate: pickN(FIRST_DATES, 1 + Math.floor(rng() * 2), rng),
    shareIdealFirstDate: true,
    sensoryPreferences: pickN(SENSORY, 2 + Math.floor(rng() * 2), rng),
    shareSensoryPreferences: true,
    energyLevel,
    location: {
      displayString: `${city.city}, Sweden`,
      city: city.city,
      state: '',
      country: 'Sweden',
      latitude: city.latitude + (rng() - 0.5) * 0.03,
      longitude: city.longitude + (rng() - 0.5) * 0.03,
    },
    profilePhotoURL,
    photos,
    profileStatus: 'complete',
    mfaEnrolled: true,
    phoneVerifiedAt: now,
    createdAt: now,
    lastSeen: now,
    isOnline: false,
    appIsActive: false,
    unreadBadgeCount: 0,
    isTestUser: true,
    testAccountKey: TEST_ACCOUNT_KEY,
    testAccountIndex: index,
  };
}

async function ensureAuthUser(email, password, displayName) {
  const signUp = await request(
    'POST',
    `https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${API_KEY}`,
    { email, password, returnSecureToken: true },
  );

  if (signUp.status === 200 && signUp.body.localId) {
    return {
      uid: signUp.body.localId,
      idToken: signUp.body.idToken,
      created: true,
    };
  }

  const errMsg = signUp.body?.error?.message || '';
  if (errMsg.includes('EMAIL_EXISTS')) {
    const signIn = await request(
      'POST',
      `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${API_KEY}`,
      { email, password, returnSecureToken: true },
    );
    if (signIn.status !== 200 || !signIn.body.localId) {
      throw new Error(`Sign-in failed for ${email}: ${signIn.raw}`);
    }
    return {
      uid: signIn.body.localId,
      idToken: signIn.body.idToken,
      created: false,
    };
  }

  throw new Error(`Sign-up failed for ${email}: ${signUp.raw}`);
}

async function writeProfile(uid, idToken, profile) {
  const docName = `projects/${PROJECT_ID}/databases/(default)/documents/users/${uid}`;
  const res = await request(
    'POST',
    `https://firestore.googleapis.com/v1/projects/${PROJECT_ID}/databases/(default)/documents:commit`,
    {
      writes: [
        {
          update: {
            name: docName,
            fields: toFirestoreFields(profile),
          },
        },
      ],
    },
    { Authorization: `Bearer ${idToken}` },
  );

  if (res.status !== 200) {
    throw new Error(`Firestore write failed for ${uid}: ${res.raw}`);
  }
}

async function markEmailVerifiedWithAdmin(uid) {
  let admin;
  try {
    admin = require('firebase-admin');
  } catch {
    return false;
  }

  if (!admin.apps.length) {
    try {
      admin.initializeApp({ projectId: PROJECT_ID });
    } catch {
      return false;
    }
  }

  try {
    await admin.auth().updateUser(uid, { emailVerified: true, displayName: undefined });
    return true;
  } catch {
    return false;
  }
}

async function main() {
  console.log(`Seeding ${COUNT} tomato test users in ${PROJECT_ID}`);
  console.log(`Password: ${PASSWORD}`);
  console.log(`testAccountKey="${TEST_ACCOUNT_KEY}"\n`);

  const summary = [];

  for (let i = 1; i <= COUNT; i++) {
    const email = `tomato${i}@yopmail.com`;
    const profile = buildProfile(i);
    process.stdout.write(`[${i}/${COUNT}] ${email} (${profile.name}, ${profile.age}) ... `);

    try {
      const auth = await ensureAuthUser(email, PASSWORD, profile.name);
      await writeProfile(auth.uid, auth.idToken, profile);
      const verified = await markEmailVerifiedWithAdmin(auth.uid);
      summary.push({
        index: i,
        email,
        password: PASSWORD,
        uid: auth.uid,
        name: profile.name,
        age: profile.age,
        city: profile.location.city,
        created: auth.created,
        emailVerifiedInAuth: verified,
      });
      const tag = auth.created ? 'created' : 'updated';
      console.log(`${tag} ✅${verified ? ' (email verified)' : ''}`);
    } catch (error) {
      console.log('FAILED ❌');
      console.error(`  ${error.message}`);
      summary.push({ index: i, email, error: error.message });
    }

    await new Promise((r) => setTimeout(r, 150));
  }

  const outPath = path.join(__dirname, 'seed_tomato_users.result.json');
  fs.writeFileSync(outPath, JSON.stringify(summary, null, 2));
  const ok = summary.filter((s) => !s.error).length;
  console.log(`\nDone: ${ok}/${COUNT} succeeded.`);
  console.log(`Credentials saved to: ${outPath}`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
