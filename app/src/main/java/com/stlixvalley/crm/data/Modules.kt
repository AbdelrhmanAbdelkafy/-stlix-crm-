package com.stlixvalley.crm.data

/**
 * The CRM modules the app exposes. Each maps a Vtiger module to a display title
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
    val EVENTS = CrmModule(
        "events", "Events", "الأحداث", "📅",
        titleFields = listOf("subject"),
        subtitleFields = listOf("date_start", "eventstatus"),
    )
    val TICKETS = CrmModule(
        "tickets", "HelpDesk", "التيكتات", "🎫",
        titleFields = listOf("ticket_title"),
        subtitleFields = listOf("ticketstatus", "ticketpriorities"),
    )

    /** Every module DetailActivity / ListActivity can resolve by key. */
    val lookup = listOf(CONTACTS, OPPORTUNITIES, TASKS, EVENTS, TICKETS)

    /** Cards shown on Home that open a plain list. The combined activities view
     *  (tasks + events + tickets) is added by HomeActivity as its own card. */
    val homeCards = listOf(CONTACTS, OPPORTUNITIES)

    fun byKey(k: String): CrmModule = lookup.first { it.key == k }
}
