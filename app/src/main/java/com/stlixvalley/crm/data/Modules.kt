package com.stlixvalley.crm.data

/**
 * The CRM modules the MVP exposes. Each maps a Vtiger module to a display title
 * and which fields render as each record's title / subtitle in the list.
 */
data class CrmModule(
    val key: String,
    val module: String,
    val title: String,
    val emoji: String,
    val titleFields: List<String>,
    val subtitleFields: List<String>,
)

object Modules {
    val CONTACTS = CrmModule(
        "contacts", "Contacts", "العملاء", "👤",
        titleFields = listOf("firstname", "lastname"),
        subtitleFields = listOf("email", "phone", "mobile"),
    )
    val OPPORTUNITIES = CrmModule(
        "opportunities", "Potentials", "الفرص", "💼",
        titleFields = listOf("potentialname"),
        subtitleFields = listOf("amount", "sales_stage"),
    )
    val TASKS = CrmModule(
        "tasks", "Calendar", "المهام", "✅",
        titleFields = listOf("subject"),
        subtitleFields = listOf("date_start", "taskstatus", "status"),
    )

    val all = listOf(CONTACTS, OPPORTUNITIES, TASKS)
    fun byKey(k: String): CrmModule = all.first { it.key == k }
}
