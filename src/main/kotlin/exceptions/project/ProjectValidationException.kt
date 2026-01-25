package com.mapprjct.exceptions.project

import com.mapprjct.exceptions.BaseAppException

sealed class ProjectValidationException : BaseAppException() {
    class UserAlreadyProjectMember(projectID : String) : ProjectValidationException() {
        override val shortMessage: String = "User already project member"
        override val detailedMessage: String = "User already take part in project with id $projectID member"
    }
    class EmptyProjectName : ProjectValidationException() {
        override val shortMessage: String = "Project name invalid"
        override val detailedMessage: String = "Project name cannot be empty"
    }
}