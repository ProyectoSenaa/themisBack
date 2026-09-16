import { gql } from "@apollo/client"

// Query para obtener todos los follow-ups (casos)
export const GET_ALL_FOLLOW_UPS = gql`
  query GetAllFollowUps($page: Int, $size: Int) {
    allFollowUps(page: $page, size: $size) {
      data {
        id
        creationDate
        caseDescription
        evidenceFiles
        isActive
        studentId
        teacherId
        coordinatorId
        studySheetId
        followUpTypeId
        followUpStatusId
        followUpFlowStatusId
        // Eliminados followUpType, followUpStatus y followUpFlowStatus
      }
      currentPage
      totalItems
      totalPages
      code
      message
    }
  }
`

// Query para obtener un follow-up por ID
export const GET_FOLLOW_UP_BY_ID = gql`
  query GetFollowUpById($id: ID!) {
    followUpById(id: $id) {
      id
      creationDate
      caseDescription
      evidenceFiles
      isActive
      studentId
      teacherId
      coordinatorId
      studySheetId
      followUpTypeId
      followUpStatusId
      followUpFlowStatusId
    }
  }
`

export const GET_FOLLOW_UPS_BY_STUDENT = gql`
  # studentId is optional now because callers may query without passing it
  query GetFollowUpsByStudent($studentId: ID, $page: Int, $size: Int) {
    followUpsByStudent(studentId: $studentId, page: $page, size: $size) {
      data {
        id
        creationDate
        caseDescription
        evidenceFiles
        isActive
        studentId
        teacherId
        coordinatorId
        studySheetId
        followUpTypeId
        followUpStatusId
        followUpFlowStatusId
      }
      currentPage
      totalItems
      totalPages
      code
      message
    }
  }
`
