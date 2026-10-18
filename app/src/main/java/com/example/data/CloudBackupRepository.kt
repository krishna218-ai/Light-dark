package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

@IgnoreExtraProperties
data class CloudConfigBackup(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val configJson: String = "{}",
    val accentPreset: String = "CYBER_CYAN",
    val moduleCount: Long = 0L,
    val addonCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

class CloudBackupRepository(private val db: FirebaseFirestore) {

    // Convenience constructor: resolves the named database ID from string resources
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserBackups(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<List<CloudConfigBackup>> =
        callbackFlow {
            val targetUid = userId.ifBlank { "unauthenticated" }
            val path = "users/$targetUid/configBackups"
            val registration = db.collection("users")
                .document(targetUid)
                .collection("configBackups")
                .whereEqualTo("userId", targetUid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(error, OperationType.LIST, path)
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val items = snapshot.toObjects(
                            CloudConfigBackup::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                        trySend(items)
                    }
                }
            awaitClose { registration.remove() }
        }

    suspend fun getUserBackups(): Result<List<CloudConfigBackup>> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/configBackups"
        return try {
            val snapshot = db.collection("users")
                .document(uid)
                .collection("configBackups")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val items = snapshot.toObjects(
                CloudConfigBackup::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            Result.success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getBackupById(ownerUserId: String, backupId: String): Result<CloudConfigBackup> {
        val docRef = db.collection("users")
            .document(ownerUserId)
            .collection("configBackups")
            .document(backupId)
        return try {
            val snapshot = docRef.get().await()
            val backup = snapshot.toObject(
                CloudConfigBackup::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw NoSuchElementException("Cloud backup $backupId not found")
            Result.success(backup)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createBackup(
        title: String,
        description: String,
        configJson: String,
        accentPreset: String,
        moduleCount: Int,
        addonCount: Int
    ): Result<String> {
        val uid = requireUserId()
        val backupId = "backup_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val docRef = db.collection("users")
            .document(uid)
            .collection("configBackups")
            .document(backupId)

        val safeTitle = title.trim().ifEmpty { "Flarial Cloud Snapshot" }.take(120)
        val safeDesc = description.trim().take(500)
        val safeJson = configJson.ifBlank { "{}" }.take(190000)
        val safeAccent = accentPreset.trim().ifEmpty { "CYBER_CYAN" }.take(60)

        val payload = mapOf(
            "userId" to uid,
            "title" to safeTitle,
            "description" to safeDesc,
            "configJson" to safeJson,
            "accentPreset" to safeAccent,
            "moduleCount" to moduleCount.coerceIn(0, 100),
            "addonCount" to addonCount.coerceIn(0, 200),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(backupId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateBackup(
        backupId: String,
        title: String,
        description: String,
        configJson: String,
        accentPreset: String,
        moduleCount: Int,
        addonCount: Int
    ): Result<Unit> {
        val uid = requireUserId()
        require(backupId.isNotBlank()) { "Backup ID must not be blank for update" }
        val docRef = db.collection("users")
            .document(uid)
            .collection("configBackups")
            .document(backupId)

        val updates = mapOf(
            "title" to title.trim().ifEmpty { "Flarial Cloud Snapshot" }.take(120),
            "description" to description.trim().take(500),
            "configJson" to configJson.ifBlank { "{}" }.take(190000),
            "accentPreset" to accentPreset.trim().ifEmpty { "CYBER_CYAN" }.take(60),
            "moduleCount" to moduleCount.coerceIn(0, 100),
            "addonCount" to addonCount.coerceIn(0, 200),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteBackup(backupId: String): Result<Unit> {
        val uid = requireUserId()
        require(backupId.isNotBlank()) { "Backup ID must not be blank for delete" }
        val docRef = db.collection("users")
            .document(uid)
            .collection("configBackups")
            .document(backupId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
