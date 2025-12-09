package com.mapprjct.database.daoimpl

import com.mapprjct.database.dao.ProjectDAO
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.dto.Project
import com.mapprjct.dto.User
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class ProjectDAOImpl(val database: Database) : ProjectDAO {
    override suspend fun getAllUserProjects(user: User): List<Project> {
        return try {
            transaction(database) {
                (ProjectUsersTable innerJoin ProjectTable)
                    .select(ProjectTable.columns)
                    .where {
                        ProjectUsersTable.userPhone eq user.phone
                    }
                    .map { result ->
                        Project(
                            projectID = result[ProjectTable.id].toString(),
                            name = result[ProjectTable.name]
                        )
                    }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            emptyList()
        }
    }

    override suspend fun createProject(
        creatorPhone: String,
        project: Project
    ): Project? {
        return try {
            val newProjectUUID = UUID.randomUUID()
            transaction(database) {
                ProjectTable.insert {
                    it[ProjectTable.name] = project.name
                    it[ProjectTable.id] = newProjectUUID
                }
                ProjectUsersTable.insert {
                    it[ProjectUsersTable.projectId] = newProjectUUID
                    it[ProjectUsersTable.userPhone] = creatorPhone
                    it[ProjectUsersTable.role] = 1
                }
            }
            return Project(
                projectID = newProjectUUID.toString(),
                name = project.name,
            )
        }catch (e: Exception){
            e.printStackTrace()
            null
        }
    }


}