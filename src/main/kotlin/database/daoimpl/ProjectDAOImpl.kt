package com.mapprjct.database.daoimpl

import com.mapprjct.database.dao.ProjectDAO
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.dto.Project
import com.mapprjct.dto.Role
import com.mapprjct.dto.asRole
import com.mapprjct.truncatePhone
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class ProjectDAOImpl(val database: Database) : ProjectDAO {
    override suspend fun getAllUserProjects(userPhone: String): List<Pair<Project, Role>> {
        return try {
            val truncatePhone = userPhone.truncatePhone()
            transaction(database) {
                (ProjectUsersTable innerJoin ProjectTable)
                    .select(
                        ProjectTable.id,
                        ProjectTable.name,
                        ProjectTable.membersCount,
                        ProjectUsersTable.role)
                    .where {
                        ProjectUsersTable.userPhone eq truncatePhone
                    }
                    .map { result ->
                        Project(
                            projectID = result[ProjectTable.id].toString(),
                            name = result[ProjectTable.name],
                            membersCount = result[ProjectTable.membersCount].toInt()
                        ) to result[ProjectUsersTable.role].asRole()
                    }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            emptyList()
        }
    }

    override suspend fun createProject(
        creatorPhone: String,
        projectName: String
    ): Project? {
        return try {
            val truncatedPhone = creatorPhone.truncatePhone()
            val newProjectUUID = UUID.randomUUID()
            transaction(database) {
                ProjectTable.insert {
                    it[ProjectTable.name] = projectName
                    it[ProjectTable.id] = newProjectUUID
                    it[ProjectTable.membersCount] = 1
                }
                ProjectUsersTable.insert {
                    it[ProjectUsersTable.projectId] = newProjectUUID
                    it[ProjectUsersTable.userPhone] = truncatedPhone
                    it[ProjectUsersTable.role] = 1
                }
            }
            return Project(
                projectID = newProjectUUID.toString(),
                name = projectName,
                membersCount = 1,
            )
        }catch (e: Exception){
            e.printStackTrace()
            null
        }
    }


}