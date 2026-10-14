package com.example.data.local

import android.content.Context
import com.example.data.preferences.UserPreferences
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import java.io.File
import java.util.UUID

object AppInitializer {

    suspend fun initialize(context: Context, database: AppDatabase, preferences: UserPreferences) {
        // 1. Initialize Users if empty
        val existingUsers = database.userDao().getAllUsersDirect()
        var studentId: String
        var adminId: String

        if (existingUsers.isEmpty()) {
            studentId = UUID.randomUUID().toString()
            adminId = UUID.randomUUID().toString()

            val student = UserEntity(
                id = studentId,
                displayName = "Rahul Kumar (ITI Student)",
                role = "USER"
            )
            val admin = UserEntity(
                id = adminId,
                displayName = "Er. Sharma (ITI Instructor / Admin)",
                role = "ADMIN"
            )
            database.userDao().insertUser(student)
            database.userDao().insertUser(admin)
            preferences.setActiveUserId(student.id)
        } else {
            studentId = existingUsers.firstOrNull { it.role == "USER" }?.id ?: existingUsers.first().id
            adminId = existingUsers.firstOrNull { it.role == "ADMIN" }?.id ?: existingUsers.first().id
            val currentActive = preferences.activeUserIdFlow.firstOrNull()
            if (currentActive == null) {
                preferences.setActiveUserId(studentId)
            }
        }

        // 2. Initialize Pre-loaded Admin ITI PDF if Admin library is empty
        val existingAdminPdfs = database.pdfDao().getVisibleAdminPdfsDirect()
        if (existingAdminPdfs.isEmpty()) {
            val samplePdfId = "iti-electrician-mod1-sample"
            val sampleDir = File(context.filesDir, "pdfs/admin")
            if (!sampleDir.exists()) sampleDir.mkdirs()
            val sampleFile = File(sampleDir, "$samplePdfId.pdf")
            if (!sampleFile.exists()) {
                sampleFile.writeText("%PDF-1.4 ITI Electrician Module 1 Reference Document")
            }

            val sampleContent = """
[Page 1]
ITI Electrician & Wireman Trade - Semester 1
Module: Electrical Safety Practices and Basic Hand Tools

Safety is the first fundamental practice in any electrical workshop. Electric shock occurs when a human body becomes part of the electrical circuit.
The severity of an electric shock depends on three factors:
1. Magnitude of current (Current of 10mA to 20mA produces painful shock; 50mA to 100mA can cause ventricular fibrillation and death).
2. Path through the body.
3. Duration of contact.

Fire extinguishers used in electrical fires:
- Class A: Wood, paper, textiles (Water type).
- Class B: Flammable liquids like petrol, diesel (Foam / Dry powder).
- Class C: Flammable gases like LPG, acetylene (Dry powder).
- Class D: Burning metals and electrical equipment fires (Halon, Carbon Dioxide CO2, and CTC - Carbon Tetrachloride extinguishers).
NEVER use water on live electrical equipment fire.

[Page 2]
Ohm's Law and Fundamental Electrical Quantities

Ohm's Law states that at constant temperature, the current (I) flowing through a metallic conductor is directly proportional to the potential difference (V) across its ends.
Formula: V = I * R, where:
- V = Voltage in Volts (V)
- I = Current in Amperes (A)
- R = Resistance in Ohms (Ω)

Specific Resistance (Resistivity, ρ):
Resistance R = ρ * (L / A), where L is the conductor length and A is the cross-sectional area.
Units of resistivity: Ohm-metre (Ω·m).
Copper and Aluminum are the most widely used conductors in electrical wiring due to high conductivity and mechanical ductility.

[Page 3]
Electrical Measuring Instruments & Earthing

1. PMMC (Permanent Magnet Moving Coil) Instruments:
- Used ONLY for Direct Current (DC) measurements.
- Linear scale.
- Deflecting torque is directly proportional to current (T_d ∝ I).

2. MI (Moving Iron) Instruments:
- Used for BOTH AC and DC measurements.
- Non-linear (cramped at starting) square-law scale (T_d ∝ I²).

Earthing:
The primary purpose of equipment earthing is to protect human life from electric shock caused by insulation failure.
The permissible earth resistance for domestic installations should not exceed 1 Ohm to 5 Ohms, and for large substations, less than 0.5 Ohm.
Pipe earthing and Plate earthing are standard systems approved by the Bureau of Indian Standards (BIS 3043).
            """.trimIndent()

            val adminPdf = PdfEntity(
                id = samplePdfId,
                title = "ITI Electrician Module 1: Safety & Fundamentals",
                ownerUserId = null,
                sourceType = "ADMIN",
                isVisible = true,
                filePath = sampleFile.absolutePath,
                pageCount = 3,
                extractionStatus = "EXTRACTED",
                subject = "Trade Theory",
                trade = "Electrician / Wireman",
                topic = "Safety, Ohm's Law & Instruments",
                description = "Standard syllabus reference for first-year ITI Electrician candidates covering safety codes, fire classification, Ohm's law, and earthing.",
                extractedText = sampleContent
            )
            database.pdfDao().insertPdf(adminPdf)

            // Seed 10 validated questions in Hindi for this preloaded module
            val questions = listOf(
                QuestionEntity(
                    id = UUID.randomUUID().toString(),
                    pdfId = samplePdfId,
                    ownerUserId = null,
                    questionText = "विद्युत उपकरणों (Live Electrical Equipment) में लगी आग को बुझाने के लिए किस प्रकार के अग्निशामक यंत्र की सिफारिश की जाती है?",
                    optionsJson = JSONArray(listOf("जल प्रकार (Water Jet)", "कार्बन डाइऑक्साइड (CO2) / हैलॉन CTC प्रकार", "फोम (झाग) प्रकार", "गीला रासायनिक प्रकार")).toString(),
                    correctIndex = 1,
                    explanation = "विद्युत उपकरणों पर लगी आग (वर्ग D) बुझाने के लिए CO2 या CTC शुष्क पाउडर का उपयोग किया जाता है। लाइव तारों पर जल का प्रयोग कभी न करें।",
                    pageNumber = 1,
                    topic = "विद्युत सुरक्षा (Electrical Safety)"
                ),
                QuestionEntity(
                    id = UUID.randomUUID().toString(),
                    pdfId = samplePdfId,
                    ownerUserId = null,
                    questionText = "मानव शरीर से कितनी विद्युत धारा प्रवाहित होने पर हृदय में वेंट्रिकुलर फाइब्रिलेशन (घातक झटका) हो सकता है?",
                    optionsJson = JSONArray(listOf("1mA से 5mA", "5mA से 9mA", "50mA से 100mA", "500 एम्पीयर से ऊपर")).toString(),
                    correctIndex = 2,
                    explanation = "मानव शरीर में 50mA से 100mA की धारा हृदय के सामान्य संकुचन को बाधित कर घातक फाइब्रिलेशन उत्पन्न कर सकती है।",
                    pageNumber = 1,
                    topic = "विद्युत सुरक्षा (Electrical Safety)"
                ),
                QuestionEntity(
                    id = UUID.randomUUID().toString(),
                    pdfId = samplePdfId,
                    ownerUserId = null,
                    questionText = "ओम के नियमानुसार, स्थिर तापमान पर वोल्टेज (V), धारा (I) और प्रतिरोध (R) के बीच क्या संबंध है?",
                    optionsJson = JSONArray(listOf("V = I * R", "V = I / R", "V = R / I", "V = I² * R")).toString(),
                    correctIndex = 0,
                    explanation = "ओम का नियम बताता है कि V = I * R, अर्थात वोल्टेज सीधे धारा और प्रतिरोध के गुणनफल के बराबर होता है।",
                    pageNumber = 2,
                    topic = "ओम का नियम (Ohm's Law)"
                ),
                QuestionEntity(
                    id = UUID.randomUUID().toString(),
                    pdfId = samplePdfId,
                    ownerUserId = null,
                    questionText = "किसी चालक के विशिष्ट प्रतिरोध (Resistivity, ρ) की SI इकाई क्या है?",
                    optionsJson = JSONArray(listOf("ओम (Ω)", "ओम-मीटर (Ω·m)", "ओम प्रति मीटर", "म्हो (Mho)")).toString(),
                    correctIndex = 1,
                    explanation = "विशिष्ट प्रतिरोध की SI इकाई ओम-मीटर (Ω·m) होती है।",
                    pageNumber = 2,
                    topic = "चालक और प्रतिरोधकता (Conductors)"
                ),
                QuestionEntity(
                    id = UUID.randomUUID().toString(),
                    pdfId = samplePdfId,
                    ownerUserId = null,
                    questionText = "इनमें से किस मापक यंत्र का उपयोग केवल दिष्ट धारा (DC) मापने के लिए किया जाता है?",
                    optionsJson = JSONArray(listOf("मूविंग आयरन आकर्षण प्रकार (MI Attraction)", "स्थायी चुंबक चल कुण्डली (PMMC) यंत्र", "मूविंग आयरन प्रतिकर्षण प्रकार", "डायनेमोमीटर प्रकार")).toString(),
                    correctIndex = 1,
                    explanation = "PMMC यंत्र में स्थायी चुंबकीय क्षेत्र होता है और यह केवल DC धारा पर कार्य करता है। AC पर इसका औसत विक्षेपण शून्य होता है।",
                    pageNumber = 3,
                    topic = "मापक यंत्र (Measuring Instruments)"
                ),
                QuestionEntity(
                    id = UUID.randomUUID().toString(),
                    pdfId = samplePdfId,
                    ownerUserId = null,
                    questionText = "मूविंग आयरन (MI) मीटर का पैमाना शुरुआत में सघन (Non-linear/Cramped) क्यों होता है?",
                    optionsJson = JSONArray(listOf("विक्षेपण बलआघूर्ण धारा के समानुपाती होता है", "विक्षेपण बलआघूर्ण धारा के वर्ग के समानुपाती होता है (T_d ∝ I²)", "मूविंग कुण्डली बहुत भारी होती है", "स्प्रिंग नियंत्रण अनुपस्थित होता है")).toString(),
                    correctIndex = 1,
                    explanation = "मूविंग आयरन मीटर में विक्षेपक टॉर्क धारा के वर्ग (I²) के समानुपाती होता है, जिससे इसका प्रारंभिक पैमाना सघन होता है।",
                    pageNumber = 3,
                    topic = "मापक यंत्र (Measuring Instruments)"
                ),
                QuestionEntity(
                    id = UUID.randomUUID().toString(),
                    pdfId = samplePdfId,
                    ownerUserId = null,
                    questionText = "विद्युत संस्थापनों में उपकरणों की अर्थिंग (Earthing) करने का मुख्य उद्देश्य क्या है?",
                    optionsJson = JSONArray(listOf("मोटर का पावर फैक्टर बढ़ाना", "इन्सुलेशन टूटने की स्थिति में मानव जीवन को बिजली के झटके से बचाना", "बिजली का बिल कम करना", "सप्लाई वोल्टेज स्थिर रखना")).toString(),
                    correctIndex = 1,
                    explanation = "अर्थिंग लीकेज धारा को सुरक्षित भूमिगत मार्ग प्रदान करती है जिससे उपकरण के धातु आवरण को छूने पर बिजली का झटका नहीं लगता।",
                    pageNumber = 3,
                    topic = "अर्थिंग (Earthing)"
                ),
                QuestionEntity(
                    id = UUID.randomUUID().toString(),
                    pdfId = samplePdfId,
                    ownerUserId = null,
                    questionText = "यदि किसी चालक तार की लंबाई दोगुनी कर दी जाए और उसका अनुप्रस्थ काट क्षेत्रफल समान रहे, तो प्रतिरोध पर क्या प्रभाव पड़ेगा?",
                    optionsJson = JSONArray(listOf("प्रतिरोध आधा हो जाएगा", "प्रतिरोध अपरिवर्तित रहेगा", "प्रतिरोध दोगुना हो जाएगा", "प्रतिरोध चार गुना हो जाएगा")).toString(),
                    correctIndex = 2,
                    explanation = "चालक का प्रतिरोध उसकी लंबाई के समानुपाती होता है (R ∝ L)। लंबाई दोगुनी करने पर प्रतिरोध भी दोगुना हो जाएगा।",
                    pageNumber = 2,
                    topic = "ओम का नियम (Ohm's Law)"
                ),
                QuestionEntity(
                    id = UUID.randomUUID().toString(),
                    pdfId = samplePdfId,
                    ownerUserId = null,
                    questionText = "अर्थिंग के नियम और अभ्यास संहिता (Code of Practice) के लिए कौन सा भारतीय मानक (IS) कोड लागू होता है?",
                    optionsJson = JSONArray(listOf("IS 3043", "IS 1200", "IS 456", "IS 800")).toString(),
                    correctIndex = 0,
                    explanation = "Bureau of Indian Standards के अनुसार IS 3043 अर्थिंग की मानक अभ्यास संहिता है।",
                    pageNumber = 3,
                    topic = "अर्थिंग मानक (Earthing Standards)"
                ),
                QuestionEntity(
                    id = UUID.randomUUID().toString(),
                    pdfId = samplePdfId,
                    ownerUserId = null,
                    questionText = "घरेलू विद्युत वायरिंग में उच्च चालकता और लचीलेपन के कारण सबसे अधिक किस पदार्थ का उपयोग किया जाता है?",
                    optionsJson = JSONArray(listOf("टंगस्टन (Tungsten)", "तांबा (Copper)", "नाइक्रोम (Nichrome)", "जर्मेनियम (Germanium)")).toString(),
                    correctIndex = 1,
                    explanation = "विद्युत वायरिंग में इलेक्ट्रोलाइटिक कॉपर (तांबा) का सर्वाधिक प्रयोग उच्च चालकता और कम प्रतिरोध के लिए किया जाता।",
                    pageNumber = 2,
                    topic = "चालक (Conductors)"
                )
            )

            database.questionDao().insertQuestions(questions)
        }
    }
}
