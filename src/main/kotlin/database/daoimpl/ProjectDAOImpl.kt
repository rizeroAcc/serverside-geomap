package com.mapprjct.database.daoimpl

import com.mapprjct.database.dao.ProjectDAO
import com.mapprjct.database.tables.InviteCodeTable
import com.mapprjct.database.tables.ProjectTable
import com.mapprjct.database.tables.ProjectUsersTable
import com.mapprjct.dto.Project
import com.mapprjct.dto.ProjectWithRole
import com.mapprjct.dto.Role
import com.mapprjct.dto.asRole
import com.mapprjct.model.Invitation
import com.mapprjct.truncatePhone
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class ProjectDAOImpl(val database: Database) : ProjectDAO {
    override suspend fun getAllUserProjects(userPhone: String): List<ProjectWithRole> {
        val truncatePhone = userPhone.truncatePhone()
        return transaction(database) {
            (ProjectUsersTable innerJoin ProjectTable)
                .select(
                    ProjectTable.id ,
                    ProjectTable.name ,
                    ProjectTable.membersCount ,
                    ProjectUsersTable.role
                )
                .where {
                    ProjectUsersTable.userPhone eq truncatePhone
                }
                .map { result ->
                    ProjectWithRole(
                        project = Project(
                            projectID = result[ProjectTable.id].toString() ,
                            name = result[ProjectTable.name] ,
                            membersCount = result[ProjectTable.membersCount].toInt()
                        ) ,
                        role = result[ProjectUsersTable.role].toInt()
                    )
                }
        }
    }

    override suspend fun insertProject(
        creatorPhone: String,
        projectName: String
    ): Project {
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
            projectID = newProjectUUID.toString() ,
            name = projectName ,
            membersCount = 1 ,
        )
    }

    override suspend fun getProjectById(projectId: UUID): Project? {
        return transaction(database) {
            ProjectTable.selectAll().where{
                ProjectTable.id eq projectId
            }.singleOrNull()?.let {
                Project(
                    projectID = it[ProjectTable.id].toString(),
                    name = it[ProjectTable.name],
                    membersCount = it[ProjectTable.membersCount].toInt()
                )
            }
        }
    }

    override suspend fun addMemberToProject(
        userPhone: String,
        project: Project,
        role: Role
    ) {
        val projectUUID = UUID.fromString(project.projectID)
        transaction(database) {
            ProjectUsersTable.insert {
                it[ProjectUsersTable.userPhone] = userPhone
                it[ProjectUsersTable.projectId] = projectUUID
                it[ProjectUsersTable.role] = role.toShort()
            }
            ProjectTable.update(
                where = { ProjectTable.id eq projectUUID },
                body = {
                    it[ProjectTable.membersCount] = (project.membersCount + 1).toShort()
                }
            )
        }
    }


}