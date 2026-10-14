package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.domain.exam.ExamQuestion
import com.example.domain.exam.ExamScorer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseIsolationRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var pdfDao: PdfDao
    private lateinit var attemptDao: AttemptDao
    private lateinit var userDao: UserDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pdfDao = db.pdfDao()
        attemptDao = db.attemptDao()
        userDao = db.userDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun userIsolation_userCannotSeeOtherUsersPrivatePdfs() = runBlocking {
        val userA = "user-a-uuid"
        val userB = "user-b-uuid"

        val pdfA = PdfEntity(
            id = "pdf-a",
            title = "User A Private Notes",
            ownerUserId = userA,
            sourceType = "USER",
            isVisible = false,
            filePath = "/path/a.pdf",
            pageCount = 10,
            extractionStatus = "EXTRACTED"
        )

        val pdfB = PdfEntity(
            id = "pdf-b",
            title = "User B Private Notes",
            ownerUserId = userB,
            sourceType = "USER",
            isVisible = false,
            filePath = "/path/b.pdf",
            pageCount = 5,
            extractionStatus = "EXTRACTED"
        )

        pdfDao.insertPdf(pdfA)
        pdfDao.insertPdf(pdfB)

        // Query for User A
        val userAPdfs = pdfDao.getPrivatePdfsForUserDirect(userA)
        assertEquals(1, userAPdfs.size)
        assertEquals("User A Private Notes", userAPdfs[0].title)

        // User A querying for User B's PDF directly should return null
        val forbiddenAccess = pdfDao.getPdfForUserDirect("pdf-b", userA)
        assertNull(forbiddenAccess)
    }

    @Test
    fun adminPdfVisibility_hiddenAdminPdfsAreNotVisibleToNormalUsers() = runBlocking {
        val normalUser = "student-123"

        val publishedAdminPdf = PdfEntity(
            id = "admin-published",
            title = "Official Electrician Module 1",
            ownerUserId = null,
            sourceType = "ADMIN",
            isVisible = true,
            filePath = "/path/admin1.pdf",
            pageCount = 20,
            extractionStatus = "EXTRACTED"
        )

        val hiddenAdminPdf = PdfEntity(
            id = "admin-draft-hidden",
            title = "Draft Exam (Hidden)",
            ownerUserId = null,
            sourceType = "ADMIN",
            isVisible = false,
            filePath = "/path/admin2.pdf",
            pageCount = 15,
            extractionStatus = "EXTRACTED"
        )

        pdfDao.insertPdf(publishedAdminPdf)
        pdfDao.insertPdf(hiddenAdminPdf)

        // Visible admin PDFs query should only return published
        val visibleAdmin = pdfDao.getVisibleAdminPdfsDirect()
        assertEquals(1, visibleAdmin.size)
        assertEquals("Official Electrician Module 1", visibleAdmin[0].title)

        // Normal user requesting hidden PDF directly should be denied
        val denied = pdfDao.getPdfForUserDirect("admin-draft-hidden", normalUser)
        assertNull(denied)

        // Normal user requesting published admin PDF is allowed
        val allowed = pdfDao.getPdfForUserDirect("admin-published", normalUser)
        assertNotNull(allowed)
        assertEquals("Official Electrician Module 1", allowed?.title)
    }

    @Test
    fun attemptHistory_isolatedByUser() = runBlocking {
        val userA = "student-A"
        val userB = "student-B"

        val attemptA = AttemptEntity(
            id = "attempt-1",
            userId = userA,
            pdfId = "pdf-1",
            pdfTitleSnapshot = "Safety Test",
            questionCount = 10,
            score = 80,
            correctCount = 8,
            wrongCount = 2,
            unansweredCount = 0,
            status = "SUBMITTED"
        )

        val attemptB = AttemptEntity(
            id = "attempt-2",
            userId = userB,
            pdfId = "pdf-2",
            pdfTitleSnapshot = "Circuits Test",
            questionCount = 10,
            score = 90,
            correctCount = 9,
            wrongCount = 1,
            unansweredCount = 0,
            status = "SUBMITTED"
        )

        attemptDao.insertAttempt(attemptA)
        attemptDao.insertAttempt(attemptB)

        // Querying attempt for User A
        val foundA = attemptDao.getAttemptByIdDirect("attempt-1", userA)
        assertNotNull(foundA)

        // User B cannot access User A's attempt
        val forbiddenAttempt = attemptDao.getAttemptByIdDirect("attempt-1", userB)
        assertNull(forbiddenAttempt)
    }
}
