package pt.isel.ls.http

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.routing.singlePageApp
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.postgresql.ds.PGSimpleDataSource
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("pt.isel.ls.http.HTTPServer")

fun Request.log() =
    logger.info(
        "incoming request: method=$method, uri=$uri, " +
            "content-type=${header("content-type")}, accept=${header("accept")}",
    )

fun getDate(request: Request) =
    Response(OK)
        .header("content-type", "text/plain")
        .body(Clock.System.now().toString())

@Serializable
data class Student(val name: String, val number: Int)

val students =
    mutableListOf(
        Student("Filipe", 10),
        Student("Luis", 20),
        Student("Daniel", 30),
    )

inline fun <reified T> jsonResponse(
    status: Status,
    body: T,
) = Response(status)
    .header("content-type", "application/json")
    .body(Json.encodeToString(body))

fun getStudents(request: Request): Response {
    request.log()
    val limit = request.query("limit")?.toInt() ?: 4
    return jsonResponse(OK, students.take(limit))
}

fun getStudent(request: Request): Response {
    request.log()
    val stdNumber = request.path("number")?.toInt()
    return jsonResponse(OK, students.find { it.number == stdNumber })
}

fun postStudent(request: Request): Response {
    request.log()
    val std = Json.decodeFromString<Student>(request.bodyString())
    students.add(std)
    return jsonResponse(CREATED, std)
}

fun getStudentsFromPostgres(request: Request): Response {
    request.log()
    val dataSource = PGSimpleDataSource()
    val jdbcDatabaseURL = System.getenv("JDBC_DATABASE_URL") ?: "jdbc:postgresql://localhost/postgres?user=postgres&password=postgres"
    dataSource.setURL(jdbcDatabaseURL)

    val pStudents =
        dataSource.connection.use {
            val stm = it.prepareStatement("select name,number from students")
            val rs = stm.executeQuery()
            buildList {
                while (rs.next())
                    add(Student(rs.getString("name"), rs.getInt("number")))
            }
        }
    return jsonResponse(OK, pStudents)
}

fun main() {
    val studentRoutes =
        routes(
            "students" bind GET to ::getStudents,
            "students/{number}" bind GET to ::getStudent,
            "students" bind POST to ::postStudent,
        )

    val app =
        routes(
            studentRoutes,
            "date" bind GET to ::getDate,
            "postgres/students" bind GET to ::getStudentsFromPostgres,
            singlePageApp(ResourceLoader.Directory("static-content")),
        )

    val jettyServer = app.asServer(Jetty(9000)).start()
    logger.info("server started listening")

    readln()
    jettyServer.stop()

    logger.info("leaving Main")
}
