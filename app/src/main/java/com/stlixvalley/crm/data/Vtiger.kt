package com.stlixvalley.crm.data

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Minimal Vtiger 7 web-service client.
 *
 * Auth flow (per Vtiger docs):
 *   getchallenge(username) -> token
 *   login(username, accessKey = md5(token + userAccessKey)) -> sessionName
 * Then operations carry sessionName: query / retrieve / create / update.
 *
 * The base URL is the CRM root (e.g. https://crm.stlixvalley.com); the client
 * appends /webservice.php. Blocking calls — run them off the main thread.
 */
class Vtiger(private val baseUrl: String) {

    private val ws = baseUrl.trimEnd('/') + "/webservice.php"
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    var session: String? = null
        private set
    var userId: String? = null
        private set

    class VtigerException(message: String) : Exception(message)

    // ---- low-level ----------------------------------------------------------
    private fun get(params: Map<String, String>): JSONObject {
        val url = ws + "?" + params.entries.joinToString("&") {
            it.key + "=" + java.net.URLEncoder.encode(it.value, "UTF-8")
        }
        return exec(Request.Builder().url(url).get().build())
    }

    private fun post(params: Map<String, String>): JSONObject {
        val body = FormBody.Builder().apply { params.forEach { add(it.key, it.value) } }.build()
        return exec(Request.Builder().url(ws).post(body).build())
    }

    private fun exec(req: Request): JSONObject {
        http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            val json = runCatching { JSONObject(raw) }
                .getOrElse { throw VtigerException("bad response: ${raw.take(180)}") }
            if (!json.optBoolean("success")) {
                val msg = json.optJSONObject("error")?.optString("message") ?: raw.take(180)
                throw VtigerException(msg)
            }
            return json
        }
    }

    private fun md5(s: String): String {
        val d = MessageDigest.getInstance("MD5").digest(s.toByteArray())
        return BigInteger(1, d).toString(16).padStart(32, '0')
    }

    // ---- auth ---------------------------------------------------------------
    /** Log in with a username and that user's web-service Access Key. */
    fun login(username: String, accessKey: String) {
        val challenge = get(mapOf("operation" to "getchallenge", "username" to username))
            .getJSONObject("result").getString("token")
        val result = post(
            mapOf(
                "operation" to "login",
                "username" to username,
                "accessKey" to md5(challenge + accessKey),
            )
        ).getJSONObject("result")
        session = result.getString("sessionName")
        userId = result.optString("userId")
    }

    private fun requireSession(): String =
        session ?: throw VtigerException("not logged in")

    // ---- data ---------------------------------------------------------------
    /** Run a Vtiger SQL-like query (must end with ';'). Returns the rows. */
    fun query(sql: String): JSONArray =
        get(mapOf("operation" to "query", "sessionName" to requireSession(), "query" to sql))
            .getJSONArray("result")

    /** First [limit] records of a module, newest first when the module supports it. */
    fun list(module: String, fields: String = "*", limit: Int = 50): JSONArray =
        query("SELECT $fields FROM $module LIMIT $limit;")

    /** Retrieve one record by its webservice id (e.g. 12x345). */
    fun retrieve(id: String): JSONObject =
        get(mapOf("operation" to "retrieve", "sessionName" to requireSession(), "id" to id))
            .getJSONObject("result")

    /** Create a record; [element] is the field map. Returns the created record. */
    fun create(module: String, element: JSONObject): JSONObject =
        post(
            mapOf(
                "operation" to "create",
                "sessionName" to requireSession(),
                "elementType" to module,
                "element" to element.toString(),
            )
        ).getJSONObject("result")

    /** Field metadata for a module (labels, mandatory, types). */
    fun describe(module: String): JSONObject =
        get(mapOf("operation" to "describe", "sessionName" to requireSession(), "elementType" to module))
            .getJSONObject("result")
}
