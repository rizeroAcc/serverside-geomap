package com.mapprjct

import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.database.tables.UserTable
import com.mapprjct.model.datatype.Password
import com.mapprjct.model.datatype.RussiaPhoneNumber
import com.mapprjct.model.datatype.StringUUID
import com.mapprjct.model.datatype.Username
import com.mapprjct.model.dto.ProjectMembershipDTO
import com.mapprjct.model.dto.UserDTO
import com.mapprjct.utils.toStringUUID
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.ktor.ext.getKoin
import java.util.UUID

fun getTestResourceAsChannel(path : String) : ByteReadChannel {
    return ClassLoader.getSystemResourceAsStream(path)!!.toByteReadChannel()
}

inline fun <reified T : Any> ApplicationTestBuilder.getBean() : T {
    return this.application.getKoin().get()
}

fun buildMultipartFromFile(
    path : String,
    filename : String? = null,
    type : String? = null,
) : MultiPartFormDataContent {
    val avatarData = getTestResourceAsChannel(path)
    val fileName = filename ?: path.substringAfterLast('/')
    val fileType = type ?: path.substringAfterLast('.')
    return MultiPartFormDataContent(
        parts = formData {
            this.append(
                key = "file",
                value = ChannelProvider{ avatarData },
                headers = Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=$fileName")
                    append(HttpHeaders.ContentType, "image/$fileType")
                }
            )
        }
    )
}

suspend fun registerUser(phone : RussiaPhoneNumber, database: Database?) : UserDTO {
    suspendTransaction(database) {
        UserTable.insert {
            it[UserTable.phone] = phone.normalizeAsRussiaPhone()
            it[UserTable.username] = "some username"
            it[UserTable.passwordHash] = "some test password"
        }
    }
    return UserDTO(phone, Username("some username"), null)
}

suspend fun registerProject(userPhone : RussiaPhoneNumber, projectName : String = "test project", database : Database?) : StringUUID {
    val id = UUID.randomUUID()
    suspendTransaction(database) {
        ProjectTable.insert{
            it[ProjectTable.name] = projectName
            it[ProjectTable.id] = id
            it[ProjectTable.membersCount] = 1
        }
        ProjectUsersTable.insert {
            it[ProjectUsersTable.projectId] = id
            it[ProjectUsersTable.userPhone] = userPhone.normalizeAsRussiaPhone()
            it[ProjectUsersTable.role] = 1
        }
    }
    return id.toStringUUID()
}

suspend fun withRegisteredProject(userPhone : RussiaPhoneNumber, projectName : String = "test project", database : Database? = null, block: suspend (projectID: StringUUID) -> Unit){
    val projectID = registerProject(userPhone, projectName, database)
    block(projectID)
}

suspend fun withRegisteredUser(phone : RussiaPhoneNumber = RussiaPhoneNumber("89036559989"),database : Database? = null, block: suspend (userPhone : RussiaPhoneNumber)->Unit ){
    registerUser(phone,database ?: TransactionManager.primaryDatabase)
    block(phone)
}