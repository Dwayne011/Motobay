import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, test } from 'node:test';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import assert from 'node:assert/strict';
import { deleteDoc, deleteField, doc, getDoc, serverTimestamp, setDoc, updateDoc } from 'firebase/firestore';
import { ref, uploadBytes, getBytes, deleteObject } from 'firebase/storage';

const ADMIN_EMAIL = 'dwayne.edwards89@googlemail.com';
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'motobays-rules-test',
    firestore: { rules: readFileSync('../firestore.rules', 'utf8'), host: '127.0.0.1', port: 8080 },
    storage: { rules: readFileSync('../storage.rules', 'utf8'), host: '127.0.0.1', port: 9199 },
  });
});
after(() => env.cleanup());
beforeEach(async () => {
  await env.clearFirestore();
  await env.clearStorage();
  await env.withSecurityRulesDisabled(async (ctx) => {
    for (const [uid, name] of Object.entries(NAMES)) {
      await setDoc(doc(ctx.firestore(), `displayNames/${name.toLowerCase()}`), { uid, name, createdAt: new Date() });
    }
  });
});

const google = (uid, email, extra = {}) =>
  env.authenticatedContext(uid, { email, email_verified: true, firebase: { sign_in_provider: 'google.com' }, ...extra });
const rider = (uid = 'rider1') => google(uid, `${uid}@example.com`);
const admin = () => google('admin1', ADMIN_EMAIL);
const gmailAdmin = () => google('admin2', 'dwayne.edwards89@gmail.com');
const anon = () => env.unauthenticatedContext();

// Each test account's claimed display name; seeded into displayNames before every test.
const NAMES = { rider1: 'Sam', rider2: 'Alex', admin1: 'Dwayne', admin2: 'Dwayne G', someoneElse: 'Other' };
const nameOf = (uid) => NAMES[uid] ?? 'Anonymous rider';

const comment = (uid = 'rider1', over = {}) => ({
  bayId: 'node/1', bayName: 'Bay', text: 'Usually has space', author: nameOf(uid), authorUid: uid,
  createdAt: serverTimestamp(), status: 'Pending', ...over,
});
const photo = (uid = 'rider1', id = 'p1', over = {}) => ({
  bayId: 'node/1', bayName: 'Bay', caption: '', author: nameOf(uid), authorUid: uid,
  url: `https://firebasestorage.googleapis.com/v0/b/x/o/photos%2F${uid}%2F${id}.jpg?alt=media&token=t`,
  storagePath: `photos/${uid}/${id}.jpg`, createdAt: serverTimestamp(), status: 'Pending', ...over,
});
const bay = (uid = 'rider1', over = {}) => ({
  lat: 51.5, lon: -0.1, name: 'New bay', fee: 'Free', notes: '', author: nameOf(uid), authorUid: uid,
  createdAt: serverTimestamp(), status: 'Pending', ...over,
});

/** Writes a document bypassing the rules. */
const seed = (path, data) =>
  env.withSecurityRulesDisabled((ctx) => setDoc(doc(ctx.firestore(), path), { ...data, createdAt: new Date() }));

describe('submitting', () => {
  for (const [col, make] of [['comments', comment], ['photos', photo], ['bays', bay]]) {
    const id = col === 'photos' ? 'p1' : 'x1';
    test(`${col}: a signed-in rider can submit a pending item`, async () => {
      await assertSucceeds(setDoc(doc(rider().firestore(), `${col}/${id}`), make('rider1')));
    });
    test(`${col}: signed-out users cannot submit`, async () => {
      await assertFails(setDoc(doc(anon().firestore(), `${col}/${id}`), make('rider1')));
    });
    test(`${col}: a rider cannot submit as approved`, async () => {
      await assertFails(setDoc(doc(rider().firestore(), `${col}/${id}`), make('rider1', { status: 'Approved' }) ));
    });
    test(`${col}: a rider cannot submit in someone else's name`, async () => {
      await assertFails(setDoc(doc(rider().firestore(), `${col}/${id}`), make('someoneElse')));
    });
    test(`${col}: createdAt must be the server time`, async () => {
      await assertFails(setDoc(doc(rider().firestore(), `${col}/${id}`), make('rider1', { createdAt: new Date(2020, 1, 1) })));
    });
    test(`${col}: unknown fields are rejected`, async () => {
      await assertFails(setDoc(doc(rider().firestore(), `${col}/${id}`), { ...make('rider1'), isAdmin: true }));
    });
  }

  test('the author must be the rider\'s own claimed name, or anonymous', async () => {
    const db = rider('rider1').firestore();
    await assertSucceeds(setDoc(doc(db, 'comments/a'), comment('rider1', { author: 'Sam' })));
    await assertSucceeds(setDoc(doc(db, 'comments/b'), comment('rider1', { author: 'Anonymous rider' })));
    await assertFails(setDoc(doc(db, 'comments/c'), comment('rider1', { author: 'Alex' })));
    await assertFails(setDoc(doc(db, 'comments/d'), comment('rider1', { author: 'Nobody' })));
  });

  test('comments: empty and over-long text are rejected', async () => {
    const db = rider().firestore();
    await assertFails(setDoc(doc(db, 'comments/a'), comment('rider1', { text: '' })));
    await assertFails(setDoc(doc(db, 'comments/b'), comment('rider1', { text: 'x'.repeat(501) })));
    await assertSucceeds(setDoc(doc(db, 'comments/c'), comment('rider1', { text: 'x'.repeat(500) })));
  });

  test('photos: URL must be a Firebase Storage URL and path must be in the rider\'s own folder', async () => {
    const db = rider().firestore();
    await assertFails(setDoc(doc(db, 'photos/p1'), photo('rider1', 'p1', { url: 'https://evil.example/x.jpg' })));
    await assertFails(setDoc(doc(db, 'photos/p1'), photo('rider1', 'p1', { storagePath: 'photos/other/p1.jpg' })));
    await assertFails(setDoc(doc(db, 'photos/p2'), photo('rider1', 'p1')));
    await assertSucceeds(setDoc(doc(db, 'photos/p1'), photo('rider1', 'p1')));
  });

  test('bays: coordinates, fee and capacity are validated', async () => {
    const db = rider().firestore();
    await assertFails(setDoc(doc(db, 'bays/a'), bay('rider1', { lat: 95 })));
    await assertFails(setDoc(doc(db, 'bays/b'), bay('rider1', { lon: -181 })));
    await assertFails(setDoc(doc(db, 'bays/c'), bay('rider1', { fee: 'Cheap' })));
    await assertFails(setDoc(doc(db, 'bays/d'), bay('rider1', { capacity: -1 })));
    await assertFails(setDoc(doc(db, 'bays/e'), bay('rider1', { covered: 'yes' })));
    await assertSucceeds(setDoc(doc(db, 'bays/f'), bay('rider1', { capacity: 6, covered: true, editOf: 'node/1' })));
  });

  test('even the admin cannot add a bay straight to approved; it goes through the queue', async () => {
    await assertFails(setDoc(doc(admin().firestore(), 'bays/a'), bay('admin1', { status: 'Approved' })));
    await assertSucceeds(setDoc(doc(admin().firestore(), 'bays/a'), bay('admin1')));
  });

  test('the admin can also sign in with the gmail.com form of the address', async () => {
    await seed('comments/g1', comment('rider1'));
    await assertSucceeds(updateDoc(doc(gmailAdmin().firestore(), 'comments/g1'), { status: 'Approved' }));
  });
});

describe('reading', () => {
  beforeEach(async () => {
    await seed('comments/approved', comment('rider1', { status: 'Approved' }));
    await seed('comments/pending', comment('rider1'));
    await seed('comments/rejected', comment('rider1', { status: 'Rejected' }));
  });

  test('anyone, even signed out, can read approved items', async () => {
    await assertSucceeds(getDoc(doc(anon().firestore(), 'comments/approved')));
  });
  test('signed-out users cannot read pending or rejected items', async () => {
    await assertFails(getDoc(doc(anon().firestore(), 'comments/pending')));
    await assertFails(getDoc(doc(anon().firestore(), 'comments/rejected')));
  });
  test('the author can read their own pending and rejected items', async () => {
    await assertSucceeds(getDoc(doc(rider('rider1').firestore(), 'comments/pending')));
    await assertSucceeds(getDoc(doc(rider('rider1').firestore(), 'comments/rejected')));
  });
  test('other riders cannot read someone else\'s pending or rejected items', async () => {
    await assertFails(getDoc(doc(rider('rider2').firestore(), 'comments/pending')));
    await assertFails(getDoc(doc(rider('rider2').firestore(), 'comments/rejected')));
  });
  test('the admin can read everything', async () => {
    for (const id of ['approved', 'pending', 'rejected']) {
      await assertSucceeds(getDoc(doc(admin().firestore(), `comments/${id}`)));
    }
  });
});

describe('moderating', () => {
  beforeEach(() => seed('comments/c1', comment('rider1')));

  test('the admin can approve, reject and undo', async () => {
    const d = doc(admin().firestore(), 'comments/c1');
    await assertSucceeds(updateDoc(d, { status: 'Approved' }));
    await assertSucceeds(updateDoc(d, { status: 'Rejected' }));
    await assertSucceeds(updateDoc(d, { status: 'Pending' }));
  });
  test('the admin can only change the status', async () => {
    const d = doc(admin().firestore(), 'comments/c1');
    await assertFails(updateDoc(d, { status: 'Approved', text: 'edited' }));
    await assertFails(updateDoc(d, { status: 'Maybe' }));
  });
  test('the admin can give a reason when rejecting, and it clears when re-queued', async () => {
    const d = doc(admin().firestore(), 'comments/c1');
    await assertSucceeds(updateDoc(d, { status: 'Rejected', reason: 'Not a motorcycle bay' }));
    await assertFails(updateDoc(d, { status: 'Rejected', reason: 'x'.repeat(301) }));
    await assertFails(updateDoc(d, { status: 'Approved', reason: 'reasons only go with rejections' }));
    await assertSucceeds(updateDoc(d, { status: 'Pending', reason: deleteField() }));
  });
  test('the author can read the reason but not write one', async () => {
    await updateDoc(doc(admin().firestore(), 'comments/c1'), { status: 'Rejected', reason: 'Blurry' });
    const snap = await getDoc(doc(rider('rider1').firestore(), 'comments/c1'));
    assert.equal(snap.data().reason, 'Blurry');
    await assertFails(updateDoc(doc(rider('rider1').firestore(), 'comments/c1'), { reason: 'fine actually' }));
  });
  test('the author cannot approve their own item', async () => {
    await assertFails(updateDoc(doc(rider('rider1').firestore(), 'comments/c1'), { status: 'Approved' }));
  });
  test('other riders and signed-out users cannot moderate', async () => {
    await assertFails(updateDoc(doc(rider('rider2').firestore(), 'comments/c1'), { status: 'Approved' }));
    await assertFails(updateDoc(doc(anon().firestore(), 'comments/c1'), { status: 'Approved' }));
  });
  test('only the admin can delete', async () => {
    await assertFails(deleteDoc(doc(rider('rider1').firestore(), 'comments/c1')));
    await assertSucceeds(deleteDoc(doc(admin().firestore(), 'comments/c1')));
  });
  test('the admin email only counts when verified and from Google sign-in', async () => {
    const d = (ctx) => doc(ctx.firestore(), 'comments/c1');
    await assertFails(updateDoc(d(google('x', ADMIN_EMAIL, { email_verified: false })), { status: 'Approved' }));
    await assertFails(updateDoc(d(google('x', ADMIN_EMAIL, { firebase: { sign_in_provider: 'apple.com' } })), { status: 'Approved' }));
    await assertFails(updateDoc(d(google('x', 'dwayne.edwards89@example.com')), { status: 'Approved' }));
  });
  test('unlisted collections are closed', async () => {
    await assertFails(setDoc(doc(admin().firestore(), 'other/x'), { a: 1 }));
    await assertFails(getDoc(doc(admin().firestore(), 'other/x')));
  });
});

describe('display names', () => {
  const claim = (ctx, name, uid, over = {}) =>
    setDoc(doc(ctx.firestore(), `displayNames/${name.toLowerCase()}`), { uid, name, createdAt: serverTimestamp(), ...over });

  test('a signed-in rider can claim a free name', async () => {
    await assertSucceeds(claim(rider('rider1'), 'Night Rider', 'rider1'));
  });
  test('a name someone else holds cannot be taken over', async () => {
    await assertFails(claim(rider('rider1'), 'Alex', 'rider1'));
  });
  test('a rider can re-save their own name and release it', async () => {
    await assertSucceeds(claim(rider('rider1'), 'Sam', 'rider1'));
    await assertSucceeds(deleteDoc(doc(rider('rider1').firestore(), 'displayNames/sam')));
    await assertFails(deleteDoc(doc(rider('rider1').firestore(), 'displayNames/alex')));
  });
  test('the key must be the lower-case name, and the name must be valid', async () => {
    const ctx = rider('rider1');
    await assertFails(setDoc(doc(ctx.firestore(), 'displayNames/wrongkey'), { uid: 'rider1', name: 'Night Rider', createdAt: serverTimestamp() }));
    await assertFails(claim(ctx, 'A', 'rider1'));
    await assertFails(claim(ctx, 'x'.repeat(41), 'rider1'));
    await assertFails(claim(ctx, 'Bad<name>', 'rider1'));
    await assertFails(claim(ctx, 'Anonymous rider', 'rider1'));
    await assertFails(claim(ctx, 'Night Rider', 'rider2'));
  });
  test('signed-out users can neither read nor claim names', async () => {
    await assertFails(claim(anon(), 'Ghost', 'rider1'));
    await assertFails(getDoc(doc(anon().firestore(), 'displayNames/sam')));
    await assertSucceeds(getDoc(doc(rider('rider2').firestore(), 'displayNames/sam')));
  });
});

describe('storage', () => {
  const jpeg = () => new Uint8Array([0xff, 0xd8, 0xff, 0xd9]);
  const meta = { contentType: 'image/jpeg' };
  const at = (ctx, path) => ref(ctx.storage(), path);

  test('a rider can upload a jpeg into their own folder', async () => {
    await assertSucceeds(uploadBytes(at(rider('rider1'), 'photos/rider1/p1.jpg'), jpeg(), meta));
  });
  test('a rider cannot upload into someone else\'s folder', async () => {
    await assertFails(uploadBytes(at(rider('rider1'), 'photos/rider2/p1.jpg'), jpeg(), meta));
  });
  test('signed-out users cannot upload', async () => {
    await assertFails(uploadBytes(at(anon(), 'photos/rider1/p1.jpg'), jpeg(), meta));
  });
  test('only jpegs under 5 MB are accepted', async () => {
    await assertFails(uploadBytes(at(rider('rider1'), 'photos/rider1/p1.png'), jpeg(), { contentType: 'image/png' }));
    await assertFails(uploadBytes(at(rider('rider1'), 'photos/rider1/big.jpg'), new Uint8Array(5 * 1024 * 1024 + 1), meta));
  });
  test('uploads outside photos/ are refused', async () => {
    await assertFails(uploadBytes(at(rider('rider1'), 'other/rider1/p1.jpg'), jpeg(), meta));
  });
  test('only the owner and the admin can read directly; riders cannot overwrite', async () => {
    await uploadBytes(at(rider('rider1'), 'photos/rider1/p1.jpg'), jpeg(), meta);
    await assertSucceeds(getBytes(at(rider('rider1'), 'photos/rider1/p1.jpg')));
    await assertSucceeds(getBytes(at(admin(), 'photos/rider1/p1.jpg')));
    await assertFails(getBytes(at(rider('rider2'), 'photos/rider1/p1.jpg')));
    await assertFails(getBytes(at(anon(), 'photos/rider1/p1.jpg')));
  });
  test('only the admin can delete photos', async () => {
    await uploadBytes(at(rider('rider1'), 'photos/rider1/p1.jpg'), jpeg(), meta);
    await assertFails(deleteObject(at(rider('rider1'), 'photos/rider1/p1.jpg')));
    await assertSucceeds(deleteObject(at(admin(), 'photos/rider1/p1.jpg')));
  });
});
