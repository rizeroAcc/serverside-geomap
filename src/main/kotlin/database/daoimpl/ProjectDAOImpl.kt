package com.mapprjct.database.daoimpl

import com.mapprjct.database.dao.ProjectDAO
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.dto.ProjectDTO
import com.mapprjct.dto.User
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class ProjectDAOImpl(val database: Database) : ProjectDAO {
    override suspend fun getAllUserProjects(user: User): List<ProjectDTO> {
        return try {
            transaction(database) {
                (ProjectUsersTable innerJoin ProjectTable)
                    .select(ProjectTable.columns)
                    .where {
                        ProjectUsersTable.userPhone eq user.phone
                    }
                    .map { result ->
                        ProjectDTO(
                            projectId = result[ProjectTable.id].toString(),
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
        projectDTO: ProjectDTO
    ): ProjectDTO? {
        return try {
            val newProjectUUID = UUID.randomUUID()
            transaction(database) {
                ProjectTable.insert {
                    it[ProjectTable.name] = projectDTO.name
                    it[ProjectTable.id] = newProjectUUID
                }
                ProjectUsersTable.insert {
                    it[ProjectUsersTable.projectId] = newProjectUUID
                    it[ProjectUsersTable.userPhone] = creatorPhone
                    it[ProjectUsersTable.role] = 1
                }
            }
            return ProjectDTO(
                projectId = newProjectUUID.toString(),
                name = projectDTO.name,
            )
        }catch (e: Exception){
            e.printStackTrace()
            null
        }
    }


}