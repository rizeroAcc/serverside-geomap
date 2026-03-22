package com.mapprjct.kotest.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.mapprjct.builders.createTestUser
import com.mapprjct.com.mapprjct.utils.BypassTransactionProvider
import com.mapprjct.database.repository.UserRepository
import com.mapprjct.database.storage.AvatarStorage
import com.mapprjct.exceptions.domain.user.*
import com.mapprjct.exceptions.storage.UpdateAvatarFileError
import com.mapprjct.model.datatype.Password
import com.mapprjct.model.datatype.Username
import com.mapprjct.model.dto.UserCredentialsDTO
import com.mapprjct.model.dto.UserDTO
import com.mapprjct.service.UserService
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.utils.io.*
import io.mockk.*
import kotlinx.io.IOException
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.postgresql.util.PSQLState
import java.io.File
import java.sql.SQLException

class UserServiceTest : FunSpec(){
    val userRepo = mockk<UserRepository>()
    val avatarStorage = mockk<AvatarStorage>()
    val userService = UserService(
        userRepository = userRepo,
        avatarStorage = avatarStorage,
        transactionProvider = BypassTransactionProvider(),
    )
    val password = Password("password")
    val userDTO = createTestUser {  }

    init {

        beforeEach {
            clearMocks(userRepo, answers = true, recordedCalls = true, verificationMarks = true)
            clearMocks(avatarStorage, answers = true, recordedCalls = true, verificationMarks = true)
        }

        context("create user") {
            val credentials = UserCredentialsDTO(userDTO.phone, password)
            test("should create user with valid info"){
                coEvery { userRepo.insert(any(), any()) } returns userDTO

                userService.createUser(userCredentialsDTO = credentials, username = userDTO.username) shouldBeRight userDTO

                coVerify(exactly = 1) {
                    userRepo.insert(
                        userDTO = eq(userDTO),
                        password = eq(password)
                    )
                }
            }
            test("should return UserAlreadyExists error on Unique violation"){
                val uniqueViolationException = ExposedSQLException(SQLException("reason", PSQLState.UNIQUE_VIOLATION.state),emptyList(),mockk())
                coEvery { userRepo.insert(any(), any()) } throws uniqueViolationException

                userService.createUser(userCredentialsDTO = credentials, username = userDTO.username) shouldBeLeft CreateUserError.UserAlreadyExists(userDTO.phone.value)
                coVerify(exactly = 1) {
                    userRepo.insert(
                        userDTO = withArg { insertedUser ->
                            insertedUser.phone shouldBe userDTO.phone
                            insertedUser.username shouldBe userDTO.username
                            insertedUser.avatarFilename shouldBe null
                        },
                        password = eq(password)
                    )
                }
            }
            test("should return Database error on ExposedSQLException"){
                val exposedException = ExposedSQLException(null,emptyList(),mockk())
                coEvery { userRepo.insert(any(), any()) } throws exposedException

                userService.createUser(userCredentialsDTO = credentials, username = userDTO.username) shouldBeLeft CreateUserError.Database(exposedException)
                coVerify(exactly = 1) {
                    userRepo.insert(
                        userDTO = eq(userDTO),
                        password = eq(password)
                    )
                }
            }
            test("should return Unexpected error on Throwable"){
                coEvery { userRepo.insert(any(), any()) } throws Throwable()
                userService.createUser(userCredentialsDTO = credentials, username = userDTO.username) shouldBeLeft CreateUserError.Unexpected(Throwable())
                coVerify(exactly = 1) {
                    userRepo.insert(
                        userDTO = eq(userDTO),
                        password = eq(password)
                    )
                }
            }
        }
        context("credentials validation"){

            val validCredentials = UserCredentialsDTO(userDTO.phone, password)
            val invalidPassword = Password(password.value.plus("foo"))
            val invalidCredentials = UserCredentialsDTO(userDTO.phone, invalidPassword)

            test("should return true if credentials are valid"){
                coEvery { userRepo.getUserCredentials(validCredentials.phone) } returns validCredentials
                userService.validateCredentials(validCredentials) shouldBeRight  true
                coVerify(exactly = 1) {
                    userRepo.getUserCredentials(
                        phone = eq(userDTO.phone)
                    )
                }
            }
            test("should return false if credentials are invalid"){
                coEvery { userRepo.getUserCredentials(any()) } returns validCredentials
                userService.validateCredentials(invalidCredentials) shouldBeRight false
                coVerify(exactly = 1) { userRepo.getUserCredentials(phone = eq(validCredentials.phone)) }
            }
            test("should return Database error on ExposedSQLException"){
                val databaseException = ExposedSQLException(null,emptyList(),mockk())
                coEvery { userRepo.getUserCredentials(any()) } throws databaseException
                userService.validateCredentials(invalidCredentials) shouldBeLeft ValidateCredentialError.Database(databaseException)
                coVerify(exactly = 1) { userRepo.getUserCredentials(phone = eq(validCredentials.phone)) }
            }
            test("should return Unexpected error on any throwable"){
                val exception = Throwable()
                coEvery { userRepo.getUserCredentials(any()) } throws exception
                userService.validateCredentials(invalidCredentials) shouldBeLeft ValidateCredentialError.Unexpected(exception)
                coVerify(exactly = 1) { userRepo.getUserCredentials(phone = eq(invalidCredentials.phone)) }
            }
        }
        context("get user"){
            test("should return existing user"){
                coEvery { userRepo.findUser(any()) } returns userDTO
                userService.getUser(userDTO.phone) shouldBeRight userDTO
                coVerify(exactly = 1) {
                    userRepo.findUser(phone = eq(userDTO.phone))
                }
            }
            test("should return UserNotFoundException user doesn't exists"){
                coEvery { userRepo.findUser(any()) } returns null
                userService.getUser(userDTO.phone) shouldBeLeft FindUserException.UserNotFound(userDTO.phone.value)
                coVerify(exactly = 1) {
                    userRepo.findUser(phone = eq(userDTO.phone))
                }
            }
            test("should return Database error on ExposedSQLException"){
                val databaseException = ExposedSQLException(null,emptyList(),mockk())
                coEvery { userRepo.findUser(any()) } throws databaseException
                userService.getUser(userDTO.phone) shouldBeLeft FindUserException.Database(databaseException)
                coVerify(exactly = 1) { userRepo.findUser(phone = eq(userDTO.phone)) }
            }
            test("should return Unexpected error on any throwable"){
                val exception = Throwable()
                coEvery { userRepo.findUser(any()) } throws exception
                userService.getUser(userDTO.phone) shouldBeLeft FindUserException.Unexpected(exception)
                coVerify(exactly = 1) { userRepo.findUser(phone = eq(userDTO.phone)) }
            }
        }
        context("update"){
            context("user info"){
                test("should update user"){
                    val updatedUser = userDTO.copy(username = Username("new_test"), avatarFilename = "path")
                    coEvery { userRepo.updateUser(any()) } returns updatedUser
                    userService.updateUser(updatedUser) shouldBeRight updatedUser
                    coVerify(exactly = 1) { userRepo.updateUser(eq(updatedUser)) }
                }
                test("should return UserNotFound if update returns null"){
                    coEvery { userRepo.updateUser(any()) } returns null
                    userService.updateUser(userDTO) shouldBeLeft UpdateUserError.UserNotFound(userDTO.phone.value)
                    coVerify(exactly = 1) { userRepo.updateUser(eq(userDTO)) }
                }
                test("should return Database error on ExposedSQLException"){
                    val databaseException = ExposedSQLException(null,emptyList(),mockk())
                    coEvery { userRepo.updateUser(any()) } throws databaseException
                    userService.updateUser(userDTO) shouldBeLeft UpdateUserError.Database(databaseException)
                    coVerify(exactly = 1) { userRepo.updateUser(eq(userDTO)) }
                }
                test("should return Unexpected error on any throwable"){
                    val exception = Throwable()
                    coEvery { userRepo.updateUser(any()) } throws exception
                    userService.updateUser(userDTO) shouldBeLeft UpdateUserError.Unexpected(exception)
                    coVerify(exactly = 1) { userRepo.updateUser(eq(userDTO)) }
                }
            }
            context("password"){
                val validCredentials = UserCredentialsDTO(userDTO.phone, password)
                val newCredentials = validCredentials.copy(password = Password("new_password"))
                test("should update user password"){
                    coEvery { userRepo.getUserCredentials(any()) } returns validCredentials
                    coEvery { userRepo.updateUserPassword(any(),any()) } returns newCredentials.password
                    userService.updateUserPassword(
                        oldCredentials = validCredentials,
                        newUserPassword = newCredentials.password
                    ) shouldBeRight newCredentials
                    coVerify(exactly = 1) {
                        userRepo.updateUserPassword(
                            userPhone = eq(validCredentials.phone),
                            password = eq(newCredentials.password)
                        )
                    }
                }
                test("should return InvalidPassword if old password incorrect"){
                    val invalidCredentials = validCredentials.copy(password = Password("invalid_password"))
                    coEvery { userRepo.getUserCredentials(any()) } returns validCredentials
                    userService.updateUserPassword(
                        oldCredentials = invalidCredentials,
                        newUserPassword = newCredentials.password
                    ) shouldBeLeft UpdateUserPasswordError.IncorrectPassword()
                    coVerify(exactly = 0) {
                        userRepo.updateUserPassword(any(),any())
                    }
                }
                test("should return UserNotFoundException if user credentials not found"){
                    coEvery { userRepo.getUserCredentials(any()) } returns null
                    userService.updateUserPassword(
                        oldCredentials = validCredentials,
                        newUserPassword = newCredentials.password
                    ) shouldBeLeft UpdateUserPasswordError.UserNotFound(validCredentials.phone.value)
                    coVerify(exactly = 0) {
                        userRepo.updateUserPassword(any(),any())
                    }
                }
                test("should return Database error on ExposedSQLException"){
                    val databaseException = ExposedSQLException(null,emptyList(),mockk())
                    coEvery { userRepo.getUserCredentials(any()) } returns validCredentials
                    coEvery { userRepo.updateUserPassword(any(),any()) } throws databaseException
                    userService.updateUserPassword(
                        oldCredentials = validCredentials,
                        newUserPassword = newCredentials.password
                    ) shouldBeLeft UpdateUserPasswordError.Database(databaseException)
                }
                test("should return Unexpected error on any throwable"){
                    val exception = Throwable()
                    coEvery { userRepo.getUserCredentials(any()) } throws exception
                    userService.updateUserPassword(
                        oldCredentials = validCredentials,
                        newUserPassword = newCredentials.password
                    ) shouldBeLeft UpdateUserPasswordError.Unexpected(exception)
                    coVerify(exactly = 0) { userRepo.updateUserPassword(any(),any()) }
                }
            }
        }
        context("update avatar"){
            test("should update user avatar"){
                val avatarBytes = ClassLoader.getSystemResourceAsStream("avatar/AppLogo.png")!!.readAllBytes()
                val testAvatarData = ByteReadChannel(avatarBytes)
                val expectedUserDTO = userDTO.copy(avatarFilename = "new_avatar.png")
                val byteProviderSlot = slot<suspend ()-> ByteReadChannel>()
                coEvery { avatarStorage.saveOrReplaceUserAvatar(any(),any(),capture(byteProviderSlot)) } returns "new_avatar.png".right()
                coEvery { userRepo.findUser(any()) } returns userDTO
                coEvery { userRepo.getUserCredentials(any()) } returns UserCredentialsDTO(userDTO.phone,password)
                coEvery { userRepo.updateUser(any()) } answers { firstArg<UserDTO>() }

                userService.updateUserAvatar(
                    userDTO = userDTO,
                    fileName = "AppLogo.png",
                    fileDataChannelProvider = { testAvatarData },
                ) shouldBeRight expectedUserDTO

                coVerify(exactly = 1) {
                    avatarStorage.saveOrReplaceUserAvatar(
                        userDTO = eq(userDTO),
                        fileExtension = eq(".png"),
                        avatarByteProvider = eq(byteProviderSlot.captured)
                    )
                }
                coVerify(exactly = 1) {
                    userRepo.updateUser(
                        userDTO = eq(userDTO.copy(avatarFilename = "new_avatar.png"))
                    )
                }
                byteProviderSlot.captured().toByteArray() shouldBe avatarBytes
            }
            test("should return InvalidAvatarFormat if file is not image"){
                userService.updateUserAvatar(
                    userDTO = userDTO,
                    fileName = "AppLogo.html",
                    fileDataChannelProvider = { ByteReadChannel(ByteArray(5)) }
                ) shouldBeLeft UpdateAvatarError.InvalidAvatarFormat(userService.getAllowedAvatarFormats())
            }
            test("should return UserNotFound if user not found"){
                coEvery { userRepo.findUser(any()) } returns null
                userService.updateUserAvatar(
                    userDTO = userDTO,
                    fileName = "AppLogo.png",
                    fileDataChannelProvider = { ByteReadChannel(ByteArray(5)) }
                ) shouldBeLeft UpdateAvatarError.UserNotFound(userDTO.phone.value)
            }
            test("should return FilesystemUnavailable on filesystem error"){
                coEvery { userRepo.findUser(any()) } returns userDTO
                coEvery { avatarStorage.saveOrReplaceUserAvatar(any(),any(),any()) } returns Either.Left(UpdateAvatarFileError.FilesystemError)
                userService.updateUserAvatar(
                    userDTO = userDTO,
                    fileName = "AppLogo.png",
                    fileDataChannelProvider = { ByteReadChannel(ByteArray(5)) }
                ) shouldBeLeft UpdateAvatarError.FilesystemUnavailable
            }
            test("should return ConnectionTerminated on connection terminated"){
                coEvery { userRepo.findUser(any()) } returns userDTO
                coEvery { avatarStorage.saveOrReplaceUserAvatar(any(),any(),any()) } returns Either.Left(UpdateAvatarFileError.ConnectionTerminated)
                userService.updateUserAvatar(
                    userDTO = userDTO,
                    fileName = "AppLogo.png",
                    fileDataChannelProvider = { ByteReadChannel(ByteArray(5)) }
                ) shouldBeLeft UpdateAvatarError.ConnectionTerminated
            }
            test("should return Database error on ExposedSqlException"){
                val databaseError = ExposedSQLException(null,emptyList(),mockk())
                coEvery { userRepo.findUser(any()) } returns userDTO
                coEvery { avatarStorage.saveOrReplaceUserAvatar(any(),any(),any()) } returns Either.Right("new_avatar.png")
                coEvery { userRepo.updateUser(any()) } throws databaseError
                userService.updateUserAvatar(
                    userDTO = userDTO,
                    fileName = "AppLogo.png",
                    fileDataChannelProvider = { ByteReadChannel(ByteArray(5)) }
                ) shouldBeLeft UpdateAvatarError.Database(databaseError)
            }
            test("should return Unexpected on storage unknown exception"){
                val exception = Throwable()
                coEvery { userRepo.findUser(any()) } returns userDTO
                coEvery {
                    avatarStorage.saveOrReplaceUserAvatar(any(),any(),any())
                } returns Either.Left(UpdateAvatarFileError.Unexpected(exception))
                userService.updateUserAvatar(
                    userDTO = userDTO,
                    fileName = "AppLogo.png",
                    fileDataChannelProvider = { ByteReadChannel(ByteArray(5)) }
                ) shouldBeLeft UpdateAvatarError.Unexpected(exception)
            }
            test("should return Unexpected on unknown exception"){
                val exception = Throwable()
                coEvery { userRepo.findUser(any()) } returns userDTO
                coEvery { avatarStorage.saveOrReplaceUserAvatar(any(),any(),any()) } returns Either.Right("new_avatar.png")
                coEvery { userRepo.updateUser(any()) } throws exception
                userService.updateUserAvatar(
                    userDTO = userDTO,
                    fileName = "AppLogo.png",
                    fileDataChannelProvider = { ByteReadChannel(ByteArray(5)) }
                ) shouldBeLeft UpdateAvatarError.Unexpected(exception)
            }
        }
        context("get avatar"){
            val dtoWithoutAvatar = userDTO.copy()
            val dtoWithAvatar = userDTO.copy(avatarFilename = "AppLogo.png")
            test("should get user avatar"){
                val avatarFile = File("avatar/AppLogo.png")
                coEvery { userRepo.findUser(any()) } returns dtoWithAvatar
                coEvery { avatarStorage.getUserAvatar(any()) } returns avatarFile.right()
                userService.getUserAvatar(dtoWithAvatar.phone) shouldBeRight avatarFile
                coVerify(exactly = 1) { avatarStorage.getUserAvatar(any()) }
            }
            test("should return UserNotFound if user not found"){
                coEvery { userRepo.findUser(any()) } returns null
                userService.getUserAvatar(userDTO.phone) shouldBeLeft FindUserAvatarError.UserNotFound(userDTO.phone.value)
                coVerify(exactly = 0) { avatarStorage.getUserAvatar(any()) }
            }
            test("should return AvatarNotFound if user hasn't avatar"){
                coEvery { userRepo.findUser(any()) } returns dtoWithoutAvatar
                userService.getUserAvatar(userDTO.phone) shouldBeLeft FindUserAvatarError.UserAvatarNotFound
                coVerify(exactly = 0) { avatarStorage.getUserAvatar(any()) }
            }
            test("should return Database error on ExposedSqlException"){
                val databaseError = ExposedSQLException(null,emptyList(),mockk())
                coEvery { userRepo.findUser(any()) } throws databaseError
                userService.getUserAvatar(userDTO.phone) shouldBeLeft FindUserAvatarError.Database(databaseError)
                coVerify(exactly = 0) { avatarStorage.getUserAvatar(any()) }
            }
            test("should return Unexpected on unexpected exception"){
                val exception = Throwable()
                coEvery { userRepo.findUser(any()) } throws exception
                userService.getUserAvatar(userDTO.phone) shouldBeLeft FindUserAvatarError.Unexpected(exception)
                coVerify(exactly = 0) { avatarStorage.getUserAvatar(any()) }
            }
        }
        context("delete avatar"){
            val dtoWithoutAvatar = userDTO.copy()
            val dtoWithAvatar = userDTO.copy(avatarFilename = "AppLogo.png")
            test("should delete avatar"){
                coEvery { userRepo.findUser(any()) } returns dtoWithAvatar
                coEvery { avatarStorage.deleteAvatar(any()) } returns Unit.right()
                coEvery { userRepo.updateUser(any()) } answers { firstArg<UserDTO>() }
                userService.deleteUserAvatar(dtoWithAvatar.phone) shouldBeRight dtoWithoutAvatar
                coVerify(exactly = 1) { avatarStorage.deleteAvatar(any()) }
            }
            test("should return UserNotFound if user not found"){
                coEvery { userRepo.findUser(any()) } returns null
                userService.deleteUserAvatar(userDTO.phone) shouldBeLeft DeleteUserAvatarError.UserNotFound
                coVerify(exactly = 0) { avatarStorage.deleteAvatar(any()) }
            }
            test("should return UserAvatarNotFound if user hasn't avatar"){
                coEvery { userRepo.findUser(any()) } returns dtoWithoutAvatar
                userService.deleteUserAvatar(dtoWithoutAvatar.phone) shouldBeLeft DeleteUserAvatarError.UserAvatarNotFound
                coVerify(exactly = 0) { avatarStorage.deleteAvatar(any()) }
            }
            test("should return FilesystemUnavailable error on FilesystemError"){
                coEvery { userRepo.findUser(any()) } returns dtoWithAvatar
                coEvery { avatarStorage.deleteAvatar(any()) } returns IOException().left()
                userService.deleteUserAvatar(dtoWithAvatar.phone) shouldBeLeft DeleteUserAvatarError.FileSystemUnavailable
            }
            test("should return Database error on ExposedSqlException"){
                val databaseError = ExposedSQLException(null,emptyList(),mockk())
                coEvery { userRepo.findUser(any()) } returns dtoWithAvatar
                coEvery { userRepo.updateUser(any()) } throws databaseError
                coEvery { avatarStorage.deleteAvatar(any()) } returns Unit.right()
                userService.deleteUserAvatar(dtoWithAvatar.phone) shouldBeLeft DeleteUserAvatarError.Database(databaseError)
            }
            test("should return Unexpected on unexpected exception"){
                val exception = Throwable()
                coEvery { userRepo.findUser(any()) } throws exception
                userService.deleteUserAvatar(userDTO.phone) shouldBeLeft DeleteUserAvatarError.Unexpected(exception)
            }
        }
    }
}