package com.example.studentapplication.di

import android.content.Context
import androidx.room.Room
import com.example.studentapplication.data.local.AttendanceDao
import com.example.studentapplication.data.local.StudentDao
import com.example.studentapplication.data.local.StudentDatabase
import com.example.studentapplication.data.repository.AttendanceRepository
import com.example.studentapplication.data.repository.FirestoreRepository
import com.example.studentapplication.data.repository.StudentRepository
import com.example.studentapplication.utils.AuthPreferences
import com.example.studentapplication.utils.NotificationHelper
import com.example.studentapplication.worker.WorkManagerScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module     //this class provide dependency
@InstallIn(SingletonComponent::class)   //keep this dependency available throughout the entire application
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFirestoreRepository(): FirestoreRepository = FirestoreRepository()

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Tells Hilt how to create the Database
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): StudentDatabase {
        return StudentDatabase.getDatabase(context)
    }

    // Tells Hilt how to create the Dao
    @Provides
    @Singleton
    fun provideStudentDao(
        database: StudentDatabase
    ): StudentDao {
        return database.studentDao()
    }

    //  Tells Hilt how to create the Repository
    @Provides
    @Singleton
    fun provideStudentRepository(
        studentDao: StudentDao,
        firestoreRepository: FirestoreRepository,
        appScope: CoroutineScope
    ): StudentRepository {
        return StudentRepository(studentDao, firestoreRepository, appScope)
    }

    @Provides
    @Singleton
    fun provideAttendanceRepository(
        attendanceDao: AttendanceDao,
        firestoreRepository: FirestoreRepository,
        appScope: CoroutineScope
    ): AttendanceRepository =
        AttendanceRepository(attendanceDao, firestoreRepository, appScope)

    // ADD THESE in DatabaseModule.kt

    @Provides
    @Singleton
    fun provideNotificationHelper(
        @ApplicationContext context: Context
    ): NotificationHelper {
        return NotificationHelper(context)
    }

    @Provides
    @Singleton
    fun provideWorkManagerScheduler(
        @ApplicationContext context: Context
    ): WorkManagerScheduler {
        return WorkManagerScheduler(context)
    }

    @Provides
    @Singleton
    fun provideAuthPreferences(
        @ApplicationContext context: Context
    ): AuthPreferences {
        return AuthPreferences(context)
    }

    //  NEW: provides AttendanceDao to Hilt injection graph
    @Provides
    @Singleton
    fun provideAttendanceDao(database: StudentDatabase): AttendanceDao =
        database.attendanceDao()

}