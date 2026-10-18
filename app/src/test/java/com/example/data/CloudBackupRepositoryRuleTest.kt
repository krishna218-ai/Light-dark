package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudBackupRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createBackup_validPayload_createsDocumentAndReturnsId() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = CloudBackupRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createBackup(
                title = "Hive Ranked Config",
                description = "Low latency 120 FPS preset",
                configJson = "{\"version\":2,\"activeProfileId\":\"profile_pvp\"}",
                accentPreset = "CYBER_CYAN",
                moduleCount = 6,
                addonCount = 3
            )
        }
        assertTrue("Expected createBackup to succeed", createResult.isSuccess)
        val backupId = createResult.getOrThrow()
        assertTrue(backupId.isNotBlank())
    }

    @Test
    fun getUserBackups_authenticatedOwner_returnsMatchingBackups() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = CloudBackupRepository(firestore)
        val backupId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createBackup(
                title = "SMP Utility Config",
                description = "Coordinates + Fullbright",
                configJson = "{\"version\":2,\"activeProfileId\":\"profile_smp\"}",
                accentPreset = "EMERALD_MATRIX",
                moduleCount = 5,
                addonCount = 2
            ).getOrThrow()
        }

        val backupsResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getUserBackups() }
        assertTrue(backupsResult.isSuccess)
        assertTrue(backupsResult.getOrThrow().any { it.id == backupId })
    }

    @Test
    fun observeUserBackups_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = CloudBackupRepository(firestore)
        val backupId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createBackup(
                title = "Realtime Snapshot Test",
                description = "Testing realtime flow",
                configJson = "{\"version\":2}",
                accentPreset = "ELECTRIC_VIOLET",
                moduleCount = 4,
                addonCount = 1
            ).getOrThrow()
        }

        val emittedBackups = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserBackups(aliceUid).first { list -> list.any { it.id == backupId } }
        }
        assertTrue(emittedBackups.any { it.id == backupId })
    }

    @Test
    fun getBackupById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CloudBackupRepository(firestore)
        val backupId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createBackup(
                title = "Alice Private Config",
                description = "Private preset",
                configJson = "{\"version\":2}",
                accentPreset = "CYBER_CYAN",
                moduleCount = 7,
                addonCount = 4
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = CloudBackupRepository(firestore)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getBackupById(aliceUid, backupId)
        }
        assertTrue("Cross-user access must fail", result.isFailure)
        val exception = result.exceptionOrNull() as? FirebaseFirestoreException
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception?.code)
    }

    @Test
    fun observeUserBackups_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = CloudBackupRepository(firestore)

        var caughtException: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeUserBackups("alice_unauth_test").first()
            }
        } catch (e: Throwable) {
            caughtException = e
        }
        assertTrue("Expected FirebaseFirestoreException", caughtException is FirebaseFirestoreException)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            (caughtException as FirebaseFirestoreException).code
        )
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
