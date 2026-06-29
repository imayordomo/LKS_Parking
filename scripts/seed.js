const admin = require('firebase-admin');
const path = require('path');
const crypto = require('crypto');

// ---------------------------------------------------------------------------
// CONFIGURATION
// ---------------------------------------------------------------------------
const SERVICE_ACCOUNT_PATH = path.join(__dirname, 'serviceAccountKey.json');
const PASSWORD = 'Test1234'; // password for all seeded auth users
const NUM_USERS = 60;
const NUM_SPOTS = 50;
const DATE_START = '2026-06-29';
const DATE_END = '2026-07-30';
const RESERVATIONS_PER_DAY_MIN = 100;
const RESERVATIONS_PER_DAY_MAX = 150;
const ALERT_MINUTES_BEFORE = 30;

// Vehicle type distribution
const VEHICLE_DISTRIBUTION = [
  { type: 'CAR', weight: 65 },
  { type: 'ELECTRIC', weight: 15 },
  { type: 'MOTORCYCLE', weight: 15 },
  { type: 'DISABLED', weight: 5 },
];

// Available durations in minutes
const DURATIONS = [30, 60, 120, 240];

// ---------------------------------------------------------------------------
// NAMES (Spanish)
// ---------------------------------------------------------------------------
const FIRST_NAMES = [
  'Alejandro', 'María', 'Javier', 'Carmen', 'Antonio', 'Isabel', 'Manuel',
  'Laura', 'José', 'Ana', 'David', 'Marta', 'Francisco', 'Elena', 'Carlos',
  'Sara', 'Juan', 'Lucía', 'Jorge', 'Paula', 'Pedro', 'Raquel', 'Ángel',
  'Rosa', 'Miguel', 'Nuria', 'Rafael', 'Cristina', 'Luis', 'Patricia',
  'Pablo', 'Sofía', 'Sergio', 'Andrea', 'Alberto', 'Eva', 'Diego',
  'Claudia', 'Fernando', 'Teresa', 'Rubén', 'Alicia', 'Víctor', 'Ruth',
  'Ramón', 'Silvia', 'Ignacio', 'Lourdes', 'Hugo', 'Beatriz', 'Óscar',
  'Mercedes', 'Alba', 'Gabriel', 'Victoria', 'Adrián', 'Rocío', 'Iván',
  'Pilar', 'Marcos',
];

const LAST_NAMES = [
  'García', 'Rodríguez', 'Martínez', 'López', 'Sánchez', 'Pérez', 'González',
  'Fernández', 'Moreno', 'Jiménez', 'Ruiz', 'Díaz', 'Álvarez', 'Romero',
  'Navarro', 'Torres', 'Domínguez', 'Vázquez', 'Ramos', 'Gil', 'Ramírez',
  'Serrano', 'Blanco', 'Castro', 'Suárez', 'Ortega', 'Rubio', 'Molina',
  'Delgado', 'Ortiz', 'Marín', 'Santos', 'Cruz', 'Iglesias', 'Herrera',
  'Medina', 'Campos', 'Núñez', 'Flores', 'Peña', 'Santiago', 'Carrasco',
  'Cabrera', 'Reyes', 'Cano', 'Calvo', 'Lara', 'Hidalgo', 'Gallego', 'Cortés',
];

// ---------------------------------------------------------------------------
// HELPERS
// ---------------------------------------------------------------------------
function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function pickRandom(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function shuffleArray(arr) {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = randomInt(0, i);
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

function uuid() {
  return crypto.randomUUID();
}

// Generate a Spanish license plate: 4 digits + 3 consonants
const CONSONANTS = 'BCDFGHJKLMNPRSTVWXYZ'.split('');
function randomLicensePlate() {
  const num = String(randomInt(0, 9999)).padStart(4, '0');
  const letters = Array.from({ length: 3 }, () => pickRandom(CONSONANTS)).join('');
  return `${num}${letters}`;
}

function randomVehicleType() {
  const totalWeight = VEHICLE_DISTRIBUTION.reduce((s, v) => s + v.weight, 0);
  let r = Math.random() * totalWeight;
  for (const entry of VEHICLE_DISTRIBUTION) {
    r -= entry.weight;
    if (r <= 0) return entry.type;
  }
  return 'CAR';
}

// Generate a realistic email: firstname.lastnameNN@lksnext.com
function generateEmail(firstName, lastName, index) {
  const prefix = `${firstName.toLowerCase()}.${lastName.toLowerCase()}${index}`;
  return `${prefix}@lksnext.com`;
}

// Convert HH:mm to minutes from midnight
function timeToMinutes(t) {
  const [h, m] = t.split(':').map(Number);
  return h * 60 + m;
}

// Convert minutes from midnight to HH:mm
function minutesToTime(m) {
  const h = Math.floor(m / 60);
  const min = m % 60;
  return `${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}`;
}

// Parse date string to Date object
function parseDate(str) {
  const [y, m, d] = str.split('-').map(Number);
  return new Date(y, m - 1, d);
}

function dateToTimestamp(dateStr, minuteOfDay) {
  const d = parseDate(dateStr);
  return new Date(d.getTime() + minuteOfDay * 60 * 1000);
}

// Add days to a date string
function addDays(dateStr, days) {
  const d = parseDate(dateStr);
  d.setDate(d.getDate() + days);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

// Calculate the number of days between two date strings (inclusive)
function daysBetween(start, end) {
  const s = parseDate(start);
  const e = parseDate(end);
  return Math.floor((e - s) / (1000 * 60 * 60 * 24)) + 1;
}

// ---------------------------------------------------------------------------
// OVERLAP CHECK
// ---------------------------------------------------------------------------
// For a given spot-day, maintain a list of [startMinute, endMinute] occupied
function doesOverlap(occupied, start, end) {
  for (const [os, oe] of occupied) {
    if (start < oe && end > os) return true;
  }
  return false;
}

// ---------------------------------------------------------------------------
// GENERATE USERS
// ---------------------------------------------------------------------------
function generateUsers(count) {
  const users = [];
  const usedEmails = new Set();

  for (let i = 0; i < count; i++) {
    const firstName = pickRandom(FIRST_NAMES);
    const lastName = pickRandom(LAST_NAMES);
    const email = generateEmail(firstName, lastName, i + 1);
    if (usedEmails.has(email)) continue;
    usedEmails.add(email);

    users.push({
      id: uuid(),
      email,
      name: `${firstName} ${lastName}`,
      profileImage: null,
      fcmToken: '',
    });
  }

  return users;
}

// ---------------------------------------------------------------------------
// GENERATE VEHICLES
// ---------------------------------------------------------------------------
function generateVehicles(users) {
  const vehicles = [];

  for (const user of users) {
    const numVehicles = randomInt(1, 3);
    const userPlates = new Set();

    for (let v = 0; v < numVehicles; v++) {
      let plate;
      do {
        plate = randomLicensePlate();
      } while (userPlates.has(plate));
      userPlates.add(plate);

      vehicles.push({
        id: uuid(),
        userId: user.id,
        type: randomVehicleType(),
        licensePlate: plate,
      });
    }
  }

  return vehicles;
}

// ---------------------------------------------------------------------------
// GENERATE RESERVATIONS
// ---------------------------------------------------------------------------
function generateReservations(users, vehicles) {
  const reservations = [];
  const numDays = daysBetween(DATE_START, DATE_END);
  const vehicleByUser = new Map();
  for (const v of vehicles) {
    if (!vehicleByUser.has(v.userId)) vehicleByUser.set(v.userId, []);
    vehicleByUser.get(v.userId).push(v);
  }

  const userMap = new Map(users.map((u) => [u.id, u]));

  for (let dayOffset = 0; dayOffset < numDays; dayOffset++) {
    const date = addDays(DATE_START, dayOffset);
    const targetCount = randomInt(RESERVATIONS_PER_DAY_MIN, RESERVATIONS_PER_DAY_MAX);

    // occupied[spotNumber] = [[start, end], ...]
    const occupied = {};
    for (let s = 1; s <= NUM_SPOTS; s++) occupied[s] = [];

    const dayReservations = [];
    let attempts = 0;

    while (dayReservations.length < targetCount && attempts < targetCount * 50) {
      attempts++;

      // Pick random user and vehicle
      const user = pickRandom(users);
      const userVehicles = vehicleByUser.get(user.id) || [];
      if (userVehicles.length === 0) continue;
      const vehicle = pickRandom(userVehicles);

      // Pick random spot
      const spotNumber = randomInt(1, NUM_SPOTS);

      // Pick random duration
      const duration = pickRandom(DURATIONS);

      // Pick random start time (07:00 to 22:00 - duration)
      const minStart = 7 * 60; // 07:00
      const maxStart = 22 * 60 - duration; // 22:00 minus duration
      if (maxStart < minStart) continue;

      const startMinute = randomInt(minStart, maxStart);
      const endMinute = startMinute + duration;

      // Check overlap for this spot on this day
      if (doesOverlap(occupied[spotNumber], startMinute, endMinute)) continue;

      // Mark as occupied
      occupied[spotNumber].push([startMinute, endMinute]);

      const startTime = minutesToTime(startMinute);
      const endTime = minutesToTime(endMinute);

      // Calculate alert timestamps (30 minutes before)
      const fechaAlertaInicio = dateToTimestamp(date, startMinute - ALERT_MINUTES_BEFORE);
      const fechaAlertaFin = dateToTimestamp(date, endMinute - ALERT_MINUTES_BEFORE);

      dayReservations.push({
        id: uuid(),
        spotNumber,
        date,
        startTime,
        endTime,
        userId: user.id,
        vehicleId: vehicle.id,
        userName: user.name,
        licensePlate: vehicle.licensePlate,
        alertaInicioEnviada: false,
        alertaFinEnviada: false,
        fechaAlertaInicio: admin.firestore.Timestamp.fromDate(fechaAlertaInicio),
        fechaAlertaFin: admin.firestore.Timestamp.fromDate(fechaAlertaFin),
      });
    }

    reservations.push(...dayReservations);
    console.log(`  Día ${date}: ${dayReservations.length} reservas generadas`);
  }

  return reservations;
}

// ---------------------------------------------------------------------------
// FIRESTORE WRITES (batched)
// ---------------------------------------------------------------------------
async function writeBatchToFirestore(collection, documents, batchSize = 500) {
  const db = admin.firestore();
  let count = 0;

  for (let i = 0; i < documents.length; i += batchSize) {
    const batch = db.batch();
    const chunk = documents.slice(i, i + batchSize);

    for (const doc of chunk) {
      const ref = db.collection(collection).doc(doc.id);
      batch.set(ref, doc);
    }

    await batch.commit();
    count += chunk.length;
    console.log(`    ${count}/${documents.length} escritos en '${collection}'`);
  }
}

// ---------------------------------------------------------------------------
// CREATE FIREBASE AUTH USERS
// ---------------------------------------------------------------------------
async function createAuthUsers(users) {
  const created = [];

  for (const user of users) {
    try {
      const authUser = await admin.auth().createUser({
        uid: user.id,
        email: user.email,
        emailVerified: true,
        password: PASSWORD,
        displayName: user.name,
      });
      created.push(authUser.uid);
      console.log(`  Auth: ${user.email} (${user.name})`);
    } catch (err) {
      if (err.code === 'auth/uid-already-exists' || err.code === 'auth/email-already-exists') {
        console.log(`  Auth (ya existe): ${user.email}`);
        created.push(user.id);
      } else {
        console.error(`  Error creating auth user ${user.email}: ${err.message}`);
      }
    }
  }

  return created;
}

// ---------------------------------------------------------------------------
// MAIN
// ---------------------------------------------------------------------------
async function deleteAllDocuments(db, collectionName) {
  const snapshot = await db.collection(collectionName).get();
  if (snapshot.empty) return 0;

  let deleted = 0;
  const batches = [];
  let batch = db.batch();
  let count = 0;

  snapshot.forEach((doc) => {
    batch.delete(doc.ref);
    count++;
    if (count === 500) {
      batches.push(batch.commit());
      batch = db.batch();
      count = 0;
    }
  });

  if (count > 0) batches.push(batch.commit());
  await Promise.all(batches);
  return snapshot.size;
}

async function main() {
  const args = process.argv.slice(2);
  const shouldClear = args.includes('--clear');

  console.log('========================================');
  console.log('  Firestore Seed - LKS Parking');
  console.log('========================================\n');

  // Check for service account key
  const fs = require('fs');
  if (!fs.existsSync(SERVICE_ACCOUNT_PATH)) {
    console.error('ERROR: No se encuentra serviceAccountKey.json');
    console.error(`Coloca el archivo en: ${SERVICE_ACCOUNT_PATH}`);
    console.error('\nPara obtenerlo:');
    console.error('  1. Ve a https://console.firebase.google.com');
    console.error('  2. Proyecto > Ajustes > Cuentas de servicio');
    console.error('  3. "Generar nueva clave privada"');
    console.error('  4. Guarda el archivo como serviceAccountKey.json en la carpeta scripts/');
    process.exit(1);
  }

  // Initialize Firebase Admin
  const serviceAccount = require(SERVICE_ACCOUNT_PATH);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });
  const db = admin.firestore();

  if (shouldClear) {
    console.log('Limpiando datos existentes...');
    const deletedUsers = await deleteAllDocuments(db, 'usuarios');
    const deletedVehicles = await deleteAllDocuments(db, 'vehiculos');
    const deletedReservas = await deleteAllDocuments(db, 'reservas');
    console.log(`   Usuarios: ${deletedUsers} eliminados`);
    console.log(`   Vehículos: ${deletedVehicles} eliminados`);
    console.log(`   Reservas: ${deletedReservas} eliminadas\n`);
  } else {
    const existingUsers = await db.collection('usuarios').limit(1).get();
    const existingVehicles = await db.collection('vehiculos').limit(1).get();
    const existingReservas = await db.collection('reservas').limit(1).get();

    if (!existingUsers.empty || !existingVehicles.empty || !existingReservas.empty) {
      console.log('Ya existen datos en Firestore. Abortando.');
      console.log('Ejecuta con --clear para borrar y repoblar:\n');
      console.log('  node seed.js --clear\n');
      process.exit(0);
    }
  }

  console.log('1. Generando usuarios...');
  const users = generateUsers(NUM_USERS);
  console.log(`   ${users.length} usuarios generados\n`);

  console.log('2. Generando vehículos...');
  const vehicles = generateVehicles(users);
  console.log(`   ${vehicles.length} vehículos generados\n`);

  console.log('3. Generando reservas...');
  const reservations = generateReservations(users, vehicles);
  console.log(`   ${reservations.length} reservas generadas en total\n`);

  console.log('4. Escribiendo usuarios en Firestore...');
  await writeBatchToFirestore('usuarios', users);

  console.log('\n5. Escribiendo vehículos en Firestore...');
  await writeBatchToFirestore('vehiculos', vehicles);

  console.log('\n6. Escribiendo reservas en Firestore...');
  await writeBatchToFirestore('reservas', reservations);

  console.log('\n7. Creando usuarios en Firebase Auth...');
  const authUids = await createAuthUsers(users);
  console.log(`   ${authUids.length} usuarios creados en Auth\n`);

  console.log('\n========================================');
  console.log('  Seed completado con exito!');
  console.log('========================================');
  console.log(`  Usuarios:    ${users.length}`);
  console.log(`  Vehiculos:   ${vehicles.length}`);
  console.log(`  Reservas:    ${reservations.length}`);
  console.log(`  Contrasena:  ${PASSWORD}`);
  console.log('========================================');
}

main().catch((err) => {
  console.error('Error:', err);
  process.exit(1);
});
