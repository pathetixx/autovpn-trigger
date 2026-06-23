package pw.x4.autovpn.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pw.x4.autovpn.data.local.TriggerAppEntity
import pw.x4.autovpn.data.local.TriggerDao
import pw.x4.autovpn.domain.repository.TriggerRepository

class TriggerRepositoryImpl(private val dao: TriggerDao) : TriggerRepository {

    override val triggerPackages: Flow<Set<String>> =
        dao.observePackages().map { it.toSet() }

    override suspend fun isTriggerApp(packageName: String): Boolean =
        dao.exists(packageName)

    override suspend fun setTrigger(packageName: String, enabled: Boolean) {
        if (enabled) dao.insert(TriggerAppEntity(packageName)) else dao.delete(packageName)
    }
}
