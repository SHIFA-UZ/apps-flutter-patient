package shifa.uz.data.debug

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import shifa.uz.data.api.ApiConfig.BASE_URL
import shifa.uz.data.api.ApiConfig.Endpoints
import shifa.uz.data.api.apiJson
import shifa.uz.data.api.createHttpEngine
import shifa.uz.data.model.BookingRequest
import shifa.uz.data.model.CheckInRequest
import shifa.uz.data.model.ProfileUpdate
import shifa.uz.data.model.SendMessageRequest
import shifa.uz.utils.getCurrentInstant
import shifa.uz.utils.isDebugBuild
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Debug menu → Demo data: the shared client talks to [DemoBackend.shared]. Kept across launches; debug builds only. */
object DemoMode {
    private const val KEY = "debug_demo_mode"
    private val settings by lazy { Settings() }
    private val _enabled by lazy { MutableStateFlow(isDebugBuild && settings.getBoolean(KEY, false)) }
    val enabled: StateFlow<Boolean> get() = _enabled

    fun set(on: Boolean) {
        settings[KEY] = on && isDebugBuild
        _enabled.value = on && isDebugBuild
    }
}

/**
 * A fake backend for the debug menu, so booking, check-ins, chat and pushes can be tried without touching
 * production. It answers the main features from in-memory data, seeded around now: a video visit you can join,
 * a pending one, an unpaid one, check-ins due now, unread notifications and chats. Booking, cancel, check-in,
 * mark read, sending and uploads change that data; it's reseeded on each launch and by [reset].
 * A GET it doesn't fake goes to the real backend (reads are safe), and so does `auth/` (logging in still works).
 * Any other write is refused, so demo mode never changes production.
 * ponytail: one fixed data set; add scenarios when a feature needs a state this one doesn't have.
 */
class DemoBackend(
    private val clock: () -> Instant = ::getCurrentInstant,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
    passthrough: () -> HttpClient = { HttpClient(createHttpEngine()) },
) {
    private val real by lazy(passthrough)
    private val mutex = Mutex()
    private var data = Data()

    fun engine(): HttpClientEngine =
        MockEngine { request ->
            val body = if (request.method == HttpMethod.Get) "" else request.body.toByteArray().decodeToString()
            val reply = mutex.withLock { route(request, body) }
            if (reply != null) {
                respond(reply.body, reply.status, headersOf(HttpHeaders.ContentType, reply.type.toString()))
            } else {
                val forwarded =
                    real.request(request.url) {
                        method = request.method
                        request.headers.forEach { name, values ->
                            if (name != HttpHeaders.ContentType && name != HttpHeaders.ContentLength) {
                                values.forEach { headers.append(name, it) }
                            }
                        }
                        setBody(request.body)
                    }
                val type = forwarded.headers[HttpHeaders.ContentType] ?: ContentType.Application.Json.toString()
                respond(forwarded.readBytes(), forwarded.status, headersOf(HttpHeaders.ContentType, type))
            }
        }

    suspend fun reset() = mutex.withLock { data = Data() }

    private class Reply(
        val status: HttpStatusCode,
        val body: String,
        val type: ContentType = ContentType.Application.Json,
    )

    private fun ok(json: JsonElement) = Reply(HttpStatusCode.OK, apiJson.encodeToString(JsonElement.serializer(), json))

    private fun fail(
        status: HttpStatusCode,
        message: String,
    ) = Reply(
        status,
        apiJson.encodeToString(JsonElement.serializer(), obj("message" to message, "status" to status.value)),
    )

    private fun route(
        request: HttpRequestData,
        body: String,
    ): Reply? {
        val get = request.method == HttpMethod.Get
        // Other hosts: files such as the demo PDF.
        if (request.url.host != Url(BASE_URL).host) return if (get) null else refused()
        val path = request.url.encodedPath.removePrefix(Url(BASE_URL).encodedPath)
        if (path.startsWith("auth/")) return null
        val p = path.split('/')
        val post = request.method == HttpMethod.Post
        data.deliverReplies()
        return when {
            get && path == Endpoints.PROFILE -> ok(data.profile)
            request.method == HttpMethod.Patch && path == Endpoints.PROFILE -> data.updateProfile(body)
            post && path == Endpoints.PATIENT_PHOTO -> ok(obj("photoUrl" to DEMO_PHOTO))
            request.method == HttpMethod.Put && path == Endpoints.FCM_TOKEN -> ok(JsonObject(emptyMap()))
            get && path == Endpoints.APPOINTMENTS -> ok(JsonArray(data.appointments))
            post && path == Endpoints.APPOINTMENTS -> data.book(body)
            request.method == HttpMethod.Delete &&
                path.startsWith(
                    "${Endpoints.APPOINTMENTS}/",
                )
            -> data.cancel(p.last())
            get && path.startsWith("patients/me/schedule/doctors/") ->
                ok(data.slots(p[4], request.url.parameters["day"], request.url.parameters["locationId"]))
            get && path == Endpoints.DOCTORS -> ok(data.search(request.url.parameters["search"]))
            get && p.size == 4 && path.startsWith(Endpoints.DOCTORS) && p[3] == "locations" ->
                data.locations[p[2]]?.let { ok(JsonArray(it)) }
            get && p.size == 3 && path.startsWith(Endpoints.DOCTORS) -> data.doctors[p[2]]?.let(::ok)
            get && path == Endpoints.MY_TASKS -> ok(obj("data" to JsonArray(data.tasks())))
            get && p.size == 4 && path.startsWith(Endpoints.MY_TASKS) && p[3] == "check-ins" ->
                ok(JsonArray(data.checkIns[p[2]].orEmpty()))
            post && p.size == 4 &&
                path.startsWith(
                    Endpoints.MY_TASKS,
                ) && p[3] == "check-in" -> data.submitCheckIn(p[2], body)
            get && path == Endpoints.NOTIFICATIONS -> ok(JsonArray(data.notifications))
            get && path == Endpoints.UNREAD_NOTIFICATIONS_COUNT -> ok(obj("count" to data.unread()))
            request.method == HttpMethod.Put && path == Endpoints.NOTIFICATIONS_READ_ALL -> data.readAll()
            request.method == HttpMethod.Put && p.size == 3 && p[0] == "notifications" && p[2] == "read" ->
                data.read(p[1])
            get && path == Endpoints.DOCUMENTS -> ok(JsonArray(data.documents))
            post && path == Endpoints.DOCUMENTS -> data.upload(body)
            request.method == HttpMethod.Delete && path.startsWith("${Endpoints.DOCUMENTS}/") ->
                data.deleteDocument(p.last())
            get && path == Endpoints.CONVERSATIONS -> ok(JsonArray(data.chats.map { it.contact() }))
            get && p.size == 5 && path.startsWith(Endpoints.CONVERSATIONS) -> data.chat(p[4])
            post && p.size == 6 && path.startsWith(Endpoints.CONVERSATIONS) && p[5] == "read" -> data.readChat(p[4])
            post && path == Endpoints.SEND_MESSAGE -> data.send(body)
            get && path == Endpoints.SEARCH_USERS -> ok(data.users(request.url.parameters["q"]))
            post && path == Endpoints.UPLOAD_ATTACHMENT -> ok(obj("url" to DEMO_PHOTO))
            post && path == Endpoints.COPILOT_STREAM ->
                Reply(
                    HttpStatusCode.OK,
                    DEMO_AI_REPLY,
                    ContentType.Text.EventStream,
                )
            get -> null
            else -> refused()
        }
    }

    private fun refused() = fail(HttpStatusCode.NotImplemented, "Not available in demo mode")

    /** The demo data, seeded around now. Ids start at 9000 so they can't be mistaken for real ones. */
    private inner class Data {
        private val now = clock()
        private val today = now.toLocalDateTime(zone).date
        private var nextId = 9900L

        private fun at(
            days: Int,
            hour: Int,
            minute: Int = 0,
        ) = today.plus(days, DateTimeUnit.DAY).atTime(hour, minute).toInstant(zone)

        private fun id() = nextId++

        var profile =
            obj(
                "id" to 9000,
                "fullName" to DEMO_PATIENT,
                "phone" to "+998900000000",
                "email" to "demo@shifa.uz",
                "birthDate" to "1990-01-01",
                "gender" to "MALE",
                "features" to listOf("PATIENT_SHIFA_AI"),
            )

        val doctors =
            mapOf(
                "9001" to
                    doctor(
                        9001,
                        "Aziza",
                        "Karimova",
                        "Dentist",
                        online = true,
                        services =
                            listOf(
                                service(9201, "Free video consultation (demo)", free = true),
                                service(9202, "Video check-up (demo)", 15_000_000, location = null),
                                service(9203, "Filling, clinic only (demo)", 9_000_000, location = 9301),
                            ),
                    ),
                "9002" to
                    doctor(
                        9002,
                        "Malika",
                        "Rahimova",
                        "Cardiologist",
                        online = false,
                        services = listOf(service(9204, "Consultation (demo)", 20_000_000, location = 9302)),
                    ),
            )

        val locations =
            mapOf(
                "9001" to listOf(location(9301, DEMO_CLINIC, "Amir Temur 1")),
                "9002" to listOf(location(9302, "Shifa Demo Heart Centre", "Navoi 15")),
            )

        private fun doctor(
            id: Int,
            first: String,
            last: String,
            profession: String,
            online: Boolean,
            services: List<JsonObject>,
        ) = obj(
            "id" to id,
            "firstName" to first,
            "lastName" to last,
            "fullName" to "$first $last",
            "profession" to profession,
            "clinic" to DEMO_CLINIC,
            "averageRating" to 4.9,
            "reviewCount" to 120,
            "supportsOnline" to online,
            "supportsInPerson" to true,
            "nextAvailableStartAt" to at(1, 9).toString(),
            "biography" to "A demo doctor. Bookings, chats and check-ins with her only change demo data.",
            "phone" to "+998901112233",
            "email" to "doctor@shifa.uz",
            "locationCity" to "Tashkent",
            "locationStreetAddress" to "Amir Temur 1",
            "latitude" to 41.31,
            "longitude" to 69.28,
            "certificates" to emptyList<String>(),
            "serviceItems" to services,
        )

        private fun service(
            id: Int,
            title: String,
            price: Long? = null,
            location: Int? = null,
            free: Boolean = false,
        ) = obj(
            "id" to id,
            "title" to title,
            "isFreeConsultation" to free,
            "prices" to
                listOfNotNull(
                    price?.let { obj("amountMinor" to it, "currency" to "UZS", "locationId" to location) },
                ),
        )

        private fun location(
            id: Int,
            label: String,
            address: String,
        ) = obj(
            "id" to id,
            "label" to label,
            "clinic" to label,
            "address" to address,
            "locationCity" to "Tashkent",
            "latitude" to 41.31,
            "longitude" to 69.28,
            "isPrimary" to true,
        )

        val appointments =
            mutableListOf(
                // Its join window is open now, so Home shows "Now" and Join.
                visit(9101, "9001", now - 5.minutes, video = true),
                visit(9102, "9002", at(1, 10), status = "REQUESTED"),
                visit(9103, "9001", at(3, 15), video = true, payment = "PENDING"),
                visit(9104, "9002", at(5, 9, 30)),
                visit(9105, "9001", at(-10, 11), status = "COMPLETED"),
                visit(9106, "9002", at(-20, 14), status = "CANCELLED"),
            )

        private fun visit(
            id: Long,
            doctorId: String,
            start: Instant,
            video: Boolean = false,
            status: String = "CONFIRMED",
            payment: String = "NOT_REQUIRED",
            reason: String? = "Check-up",
        ): JsonObject {
            val doctor = doctors.getValue(doctorId)
            return obj(
                "id" to id,
                "doctorId" to doctorId,
                "doctorName" to doctor.str("fullName"),
                "doctorProfession" to doctor.str("profession"),
                "doctorClinic" to DEMO_CLINIC,
                "startAt" to start.toString(),
                "endAt" to (start + 30.minutes).toString(),
                "location" to if (video) "Video Consultation" else DEMO_CLINIC,
                "isVideo" to video,
                "reason" to reason,
                "status" to status,
                "paymentStatus" to payment,
                "paymentAmountMinor" to 15_000_000.takeIf { payment == "PENDING" },
                "paymentCurrency" to "UZS".takeIf { payment == "PENDING" },
            )
        }

        fun search(query: String?): JsonObject {
            val found =
                doctors.values.filter {
                    query.isNullOrBlank() ||
                        it.str(
                            "fullName",
                        ).contains(query.trim(), true)
                }
            return obj("doctors" to found, "totalCount" to found.size)
        }

        // 09:00 to 16:30 each day, every 30 min, minus what's booked or over.
        fun slots(
            doctorId: String,
            day: String?,
            locationId: String?,
        ): JsonArray {
            val date = day?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return JsonArray(emptyList())
            if (doctorId !in doctors) return JsonArray(emptyList())
            val taken =
                appointments.filter {
                    it.str("doctorId") == doctorId && it.str("status") != "CANCELLED"
                }.map { it.str("startAt") }
            return JsonArray(
                (0 until 16)
                    .map { date.atTime(LocalTime(9 + it / 2, it % 2 * 30)).toInstant(zone) }
                    .filter { it > now && it.toString() !in taken }
                    .map {
                        obj(
                            "startAt" to it.toString(),
                            "endAt" to (it + 30.minutes).toString(),
                            "slotMinutes" to 30,
                            "locationId" to locationId?.toIntOrNull(),
                        )
                    },
            )
        }

        // B3 like production: a video visit needs a service with an online price (or a free one).
        fun book(body: String): Reply {
            val request = apiJson.decodeFromString(BookingRequest.serializer(), body)
            val doctor =
                doctors[request.doctorId.toString()] ?: return fail(
                    HttpStatusCode.NotFound,
                    "Doctor not found",
                )
            val service =
                doctor.list("serviceItems").map { it.jsonObject }.firstOrNull { it.long("id") == request.serviceId }
            if (request.isVideo && service == null) {
                return fail(
                    HttpStatusCode.BadRequest,
                    "serviceId is required for video consultation",
                )
            }
            val prices = service?.list("prices").orEmpty().map { it.jsonObject }
            val free = service?.get("isFreeConsultation")?.jsonPrimitive?.booleanOrNull == true
            if (request.isVideo && !free && prices.none { it["locationId"] == null || it["locationId"] is JsonNull }) {
                return fail(HttpStatusCode.BadRequest, "Selected service has no price configured")
            }
            val taken =
                appointments.any {
                    it.str(
                        "doctorId",
                    ) == request.doctorId.toString() && it.str(
                        "startAt",
                    ) == request.startAt && it.str("status") != "CANCELLED"
                }
            if (taken) return fail(HttpStatusCode.Conflict, "This slot is no longer available")
            val start = Instant.parse(request.startAt)
            val booked =
                visit(
                    id(),
                    request.doctorId.toString(),
                    start,
                    video = request.isVideo,
                    status = "REQUESTED",
                    payment = if (request.isVideo && !free) "PENDING" else "NOT_REQUIRED",
                    reason = request.reason,
                )
            appointments += booked
            return ok(booked)
        }

        // B5 deletes, as production does.
        fun cancel(id: String): Reply =
            if (appointments.removeAll {
                    it.str("id") == id
                }
            ) {
                ok(JsonObject(emptyMap()))
            } else {
                fail(HttpStatusCode.NotFound, "Appointment not found")
            }

        val checkIns =
            mutableMapOf(
                "9401" to dueNow(9501),
                "9402" to dueNow(9511),
                "9403" to dueNow(9521),
                "9404" to mutableListOf(checkIn(9531, now - 1.days, "COMPLETED")),
            )

        // One done yesterday, one due now (due counts from 10 min before), two later.
        private fun dueNow(first: Long) =
            mutableListOf(
                checkIn(first, now - 1.days, "COMPLETED"),
                checkIn(first + 1, now - 5.minutes),
                checkIn(first + 2, now + 4.hours),
                checkIn(first + 3, now + 1.days),
            )

        private fun checkIn(
            id: Long,
            at: Instant,
            status: String = "PENDING",
        ): JsonObject {
            val local = at.toLocalDateTime(zone)
            return obj(
                "id" to id,
                "scheduledDate" to local.date.toString(),
                "scheduledTime" to local.time.toString().take(5) + ":00",
                "status" to status,
            )
        }

        private val taskList =
            listOf(
                task(
                    9401,
                    "Blood pressure (demo)",
                    "VITAL",
                    "NUMERIC",
                    "Morning and evening",
                    label = "Systolic, mmHg",
                ),
                task(9402, "Vitamin D (demo)", "MEDICATION", "BOOLEAN", "One tablet after breakfast", label = "Taken?"),
                task(
                    9403,
                    "Walk 30 minutes (demo)",
                    "EXERCISE",
                    "TEXT",
                    "Easy pace",
                    label = "Distance",
                    notes = "How did it go?",
                ),
                task(9404, "Diet diary (demo)", "OTHER", "TEXT", "Two weeks", status = "COMPLETED"),
            )

        private fun task(
            id: Long,
            name: String,
            category: String,
            input: String,
            description: String,
            label: String? = null,
            notes: String? = null,
            status: String = "ACTIVE",
        ) = obj(
            "id" to id,
            "taskName" to name,
            "description" to description,
            "category" to category,
            "status" to status,
            "startDate" to today.plus(-3, DateTimeUnit.DAY).toString(),
            "inputType" to input,
            "inputLabel" to label,
            "notesRequired" to (notes != null),
            "notesLabel" to notes,
        )

        fun tasks() =
            taskList.map { task ->
                val all = checkIns[task.str("id")].orEmpty()
                val done = all.count { it.str("status") == "COMPLETED" }
                JsonObject(
                    task + (
                        "progress" to
                            obj(
                                "totalCheckIns" to all.size,
                                "completedCheckIns" to done,
                                "pendingCheckIns" to all.size - done,
                            )
                    ),
                )
            }

        fun submitCheckIn(
            taskId: String,
            body: String,
        ): Reply {
            val request = apiJson.decodeFromString(CheckInRequest.serializer(), body)
            val list = checkIns[taskId] ?: return fail(HttpStatusCode.NotFound, "Task not found")
            val i = list.indexOfFirst { it.long("id") == request.checkInId }
            if (i < 0) return fail(HttpStatusCode.NotFound, "Check-in not found")
            list[i] = JsonObject(list[i] + ("status" to JsonPrimitive("COMPLETED")))
            return ok(JsonObject(emptyMap()))
        }

        val notifications =
            mutableListOf(
                notification(
                    9601,
                    "APPOINTMENT_CONFIRMED",
                    "Appointment confirmed",
                    "Your video visit with Aziza Karimova starts now.",
                    now - 10.minutes,
                    "appointmentId" to "9101",
                ),
                notification(
                    9602,
                    "TASK_REMINDER",
                    "Time for your check-in",
                    "Blood pressure (demo) is due.",
                    now - 5.minutes,
                    "taskId" to "9401",
                ),
                notification(
                    9603,
                    "CONSULTATION_PAYMENT_DUE_24H",
                    "Payment due",
                    "Pay for your video check-up to keep the slot.",
                    now - 1.hours,
                    "appointmentId" to "9103",
                ),
                notification(
                    9604,
                    "NEW_MESSAGE",
                    "New message",
                    "Aziza Karimova: How are you feeling?",
                    now - 15.minutes,
                    "chatId" to "9801",
                ),
                notification(
                    9605,
                    "NEW_DOCUMENT",
                    "New document",
                    "Blood test results (demo)",
                    now - 1.days,
                    "documentId" to "9701",
                    read = true,
                ),
            )

        private fun notification(
            id: Int,
            type: String,
            title: String,
            message: String,
            at: Instant,
            link: Pair<String, String>,
            read: Boolean = false,
        ) = obj(
            "id" to id,
            "type" to type,
            "title" to title,
            "message" to message,
            "createdAt" to at.toString(),
            "readAt" to at.toString().takeIf { read },
            "doctorName" to "Aziza Karimova",
            link,
        )

        fun unread() = notifications.count { it["readAt"] == null || it["readAt"] is JsonNull }

        fun read(id: String): Reply {
            val i = notifications.indexOfFirst { it.str("id") == id }
            if (i < 0) return fail(HttpStatusCode.NotFound, "Notification not found")
            notifications[i] = JsonObject(notifications[i] + ("readAt" to JsonPrimitive(clock().toString())))
            return ok(JsonObject(emptyMap()))
        }

        fun readAll(): Reply {
            notifications.indices.forEach {
                notifications[it] = JsonObject(notifications[it] + ("readAt" to JsonPrimitive(clock().toString())))
            }
            return ok(JsonObject(emptyMap()))
        }

        val documents =
            mutableListOf(
                document(9701, "Blood test results (demo)", "BLOOD_TEST", "Doctor", now - 1.days),
                document(9702, "My X-ray (demo)", "XRAY", DEMO_PATIENT, now - 7.days),
            )

        private fun document(
            id: Long,
            title: String,
            category: String?,
            creator: String,
            at: Instant,
        ) = obj(
            "id" to id,
            "title" to title,
            "category" to category,
            "creatorLabel" to creator,
            "doctorName" to "Aziza Karimova".takeIf { creator == "Doctor" },
            "createdAt" to at.toString(),
            "date" to at.toString(),
            "fileUrl" to DEMO_PDF,
            "type" to "application/pdf",
        )

        // ponytail: the title and category are read from the multipart text, not parsed; the file is always DEMO_PDF.
        fun upload(body: String): Reply {
            fun field(name: String) =
                Regex(
                    "name=\"$name\"\\r\\n(?:[^\\r\\n]+\\r\\n)*\\r\\n([^\\r\\n]*)",
                ).find(body)?.groupValues?.get(1)?.ifBlank {
                    null
                }
            val added =
                document(id(), field("title") ?: "Uploaded document (demo)", field("category"), DEMO_PATIENT, clock())
            documents.add(0, added)
            return ok(added)
        }

        fun deleteDocument(id: String): Reply =
            if (documents.removeAll {
                    it.str("id") == id
                }
            ) {
                ok(JsonObject(emptyMap()))
            } else {
                fail(HttpStatusCode.NotFound, "Document not found")
            }

        val chats =
            mutableListOf(
                Chat(
                    "9801",
                    "9001",
                    "Aziza Karimova",
                    mutableListOf(
                        message("system", "Chat started", now - 2.days),
                        message(null, "Hello! How are you feeling after the visit?", now - 1.days),
                        message(null, "Much better, thank you.", now - 1.days + 10.minutes, mine = true),
                        message(null, null, now - 30.minutes, file = "Recommendations.pdf"),
                        message(null, "How are you feeling?", now - 15.minutes),
                    ),
                    unread = 2,
                ),
                Chat(
                    "9802",
                    "9002",
                    "Malika Rahimova",
                    mutableListOf(message(null, "See you on Monday", now - 3.days, mine = true)),
                ),
            )

        inner class Chat(
            val id: String,
            val doctorId: String,
            val name: String,
            val messages: MutableList<JsonObject>,
            var unread: Int = 0,
            var replyAt: Instant? = null,
        ) {
            fun contact() =
                obj(
                    "id" to id,
                    "participantId" to doctorId,
                    "participantName" to name,
                    "lastMessage" to (messages.last().str("text").ifEmpty { "Attachment" }),
                    "lastMessageAt" to messages.last().str("createdAt"),
                    "unreadCount" to unread,
                )
        }

        private fun message(
            type: String?,
            text: String?,
            at: Instant,
            mine: Boolean = false,
            file: String? = null,
        ) = obj(
            "id" to id(),
            "type" to type,
            "content" to type?.let { obj("text" to text) },
            "text" to text.takeIf { type == null },
            "attachmentUrl" to file?.let { DEMO_PDF },
            "attachmentName" to file,
            "createdAt" to at.toString(),
            "isMine" to mine,
            "isRead" to mine,
        )

        fun chat(id: String): Reply {
            val chat =
                chats.firstOrNull { it.id == id } ?: return fail(
                    HttpStatusCode.NotFound,
                    "Conversation not found",
                )
            return ok(obj("conversation" to chat.contact(), "messages" to chat.messages))
        }

        fun readChat(id: String): Reply {
            chats.firstOrNull { it.id == id }?.unread = 0
            return ok(JsonObject(emptyMap()))
        }

        // The doctor answers each message 4 s later, so the 5 s poll has something new.
        fun send(body: String): Reply {
            val request = apiJson.decodeFromString(SendMessageRequest.serializer(), body)
            val chat =
                chats.firstOrNull { it.id == request.conversationId?.toString() }
                    ?: request.recipientDoctorId?.toString()?.let { doctorId ->
                        chats.firstOrNull { it.doctorId == doctorId }
                            ?: doctors[doctorId]?.let {
                                Chat(
                                    id().toString(),
                                    doctorId,
                                    it.str("fullName"),
                                    mutableListOf(),
                                ).also(chats::add)
                            }
                    }
                    ?: return fail(HttpStatusCode.NotFound, "Conversation not found")
            val sent =
                obj(
                    "id" to id(),
                    "conversationId" to chat.id,
                    "type" to request.type,
                    "text" to request.text,
                    "attachmentUrl" to request.attachmentUrl,
                    "attachmentName" to request.attachmentName,
                    "createdAt" to clock().toString(),
                    "isMine" to true,
                )
            chat.messages += sent
            chat.replyAt = clock() + 4.seconds
            return ok(sent)
        }

        fun deliverReplies() {
            chats.filter { it.replyAt?.let { at -> clock() >= at } == true }.forEach {
                it.messages += message(null, "Thanks, noted. (demo reply)", it.replyAt!!)
                it.unread++
                it.replyAt = null
            }
        }

        fun users(query: String?) =
            JsonArray(
                doctors.values
                    .filter { query.isNullOrBlank() || it.str("fullName").contains(query.trim(), true) }
                    .map { obj("id" to it.str("id"), "name" to it.str("fullName")) },
            )

        fun updateProfile(body: String): Reply {
            val update = apiJson.decodeFromString(ProfileUpdate.serializer(), body)
            val name = listOfNotNull(update.firstName, update.lastName).joinToString(" ").ifBlank { null }
            val changes =
                listOf(
                    "fullName" to name,
                    "phone" to update.phone,
                    "birthDate" to update.birthDate,
                    "gender" to update.gender,
                    "address" to update.address,
                    "language" to update.language,
                    "photoUrl" to update.photoUrl,
                    "locationStreetAddress" to update.locationStreetAddress,
                ).filter { it.second != null }
            profile = JsonObject(profile + changes.associate { it.first to JsonPrimitive(it.second) })
            return ok(profile)
        }
    }

    companion object {
        /** The app's instance, behind the shared client while [DemoMode] is on. */
        val shared by lazy { DemoBackend() }
        const val DEMO_PATIENT = "Demo Patient"
        private const val DEMO_CLINIC = "Shifa Demo Clinic"
        private const val DEMO_PDF = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"
        private const val DEMO_PHOTO = "https://picsum.photos/id/64/400/400"

        // X1's SSE: tokens as `data:` lines, each event ended by a blank line (09 §8).
        private const val DEMO_AI_REPLY = "data: This is a demo reply.\n\ndata:  Demo mode doesn't ask the real Shifa AI.\n\n"
    }
}

private fun obj(vararg pairs: Pair<String, Any?>) = JsonObject(pairs.associate { it.first to json(it.second) })

private fun json(value: Any?): JsonElement =
    when (value) {
        null -> JsonNull
        is JsonElement -> value
        is String -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is List<*> -> JsonArray(value.map(::json))
        else -> JsonPrimitive(value.toString())
    }

private fun JsonObject.str(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()

private fun JsonObject.long(key: String) = (get(key) as? JsonPrimitive)?.longOrNull

private fun JsonObject.list(key: String) = (get(key) as? JsonArray)?.jsonArray.orEmpty()
