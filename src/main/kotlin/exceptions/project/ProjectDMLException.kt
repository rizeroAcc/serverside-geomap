package com.mapprjct.exceptions.project

import com.mapprjct.exceptions.BaseAppException

sealed class ProjectDMLException : BaseAppException() {
    class ProjectNotFoundException(projectID : String) : ProjectDMLException(){
        override val shortMessage: String = "Project not found"
        override val detailedMessage: String = "Project with ID $projectID not found"
    }
}