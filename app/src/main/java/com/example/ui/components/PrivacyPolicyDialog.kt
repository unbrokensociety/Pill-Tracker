package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentLang = context.resources.configuration.locales[0].language

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentLang) {
                                "uk" -> "Політика Конфіденційності"
                                "ru" -> "Политика Конфиденциальности"
                                else -> "Privacy Policy & Data Rights"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                Surface(
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Check, null)
                                Text(
                                    text = when (currentLang) {
                                        "uk" -> "Я погоджуюсь та приймаю"
                                        "ru" -> "Я соглашаюсь и принимаю"
                                        else -> "I Understand & Agree"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Banner
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GLOBAL DATA PROTECTION STANDARDS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (currentLang) {
                                    "uk" -> "Захист здоров'я за законами України (№ 2297-VI), ЄС (EU GDPR) та США (HIPAA / CCPA)"
                                    "ru" -> "Защита здоровья по законам Украины (№ 2297-VI), ЕС (EU GDPR) и США (HIPAA / CCPA)"
                                    else -> "Health data protection under Ukraine Law № 2297-VI, EU GDPR & US HIPAA / CCPA"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                when (currentLang) {
                    "uk" -> UkrainianPolicyContent()
                    "ru" -> RussianPolicyContent()
                    else -> EnglishPolicyContent()
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun UkrainianPolicyContent() {
    PolicyCardSection(
        number = "1",
        title = "Правова база та глобальна відповідність",
        body = "Політика розроблена відповідно до Закону України «Про захист персональних даних» № 2297-VI, Регламенту ЄС EU GDPR (2016/679) та стандартів конфіденційності США (HIPAA Privacy Standards / CCPA). Обробка даних проводиться лише з вашої добровільної згоди."
    )
    PolicyCardSection(
        number = "2",
        title = "Шифрування даних (AES-256) та захист медичних записів",
        body = "Усі медичні дані, назви препаратів, графіки прийому та примітки зберігаються в зашифрованому вигляді за допомогою стандарту AES-256-GCM як на пристрої, так і під час синхронізації з хмарою Cloud Firestore."
    )
    PolicyCardSection(
        number = "3",
        title = "Право на видалення даних та 30-денний період (GDPR Art. 17 & Закон № 2297-VI)",
        body = "Ви маєте повне право на видалення облікового запису та даних. Запит створює 30-денний відкладений період, протягом якого видалення можна скасувати в додатку. Після 30 днів усі дані знищуються остаточно без можливості відновлення."
    )
    PolicyCardSection(
        number = "4",
        title = "Конфіденційність та відсутність продажу даних",
        body = "Ваші персональні та медичні дані є суворо приватними. Ми ніколи не продаємо, не передаємо та не використовуємо вашу інформацію для рекламного таргетингу третьою стороною."
    )
    PolicyCardSection(
        number = "5",
        title = "Медичне застереження (Medical Disclaimer)",
        body = "PillTracker є інструментом самоконтролю та нагадування про прийом ліків. Сервіс не встановлює діагнозів і не замінює консультації професійного лікаря чи екстрену медичну допомогу."
    )
}

@Composable
private fun RussianPolicyContent() {
    PolicyCardSection(
        number = "1",
        title = "Правовая база и глобальное соответствие",
        body = "Политика разработана в соответствии с Законом Украины «О защите персональных данных» № 2297-VI, Регламентом ЕС EU GDPR (2016/679) и стандартами США (HIPAA / CCPA). Обработка данных проводится исключительно с вашего согласия."
    )
    PolicyCardSection(
        number = "2",
        title = "Шифрование данных (AES-256) и защита медикаментов",
        body = "Все медицинские данные, названия препаратов и графики зашифрованы стандартом AES-256-GCM на вашем устройстве и при передаче в облако Cloud Firestore."
    )
    PolicyCardSection(
        number = "3",
        title = "Право на удаление и 30-дневный период (GDPR Art. 17 & Закон № 2297-VI)",
        body = "Вы имеете право на полное удаление аккаунта и данных. Запрос активирует 30-дневный льготный период, в течение которого удаление можно отменить. По истечении 30 дней данные уничтожаются безвозвратно."
    )
    PolicyCardSection(
        number = "4",
        title = "Конфиденциальность и запрет передачи третьим лицам",
        body = "Ваши личные и медицинские данные конфиденциальны. Мы никогда не продаём и не передаём ваши данные сторонним организациям или рекламодателям."
    )
    PolicyCardSection(
        number = "5",
        title = "Медицинская оговорка (Medical Disclaimer)",
        body = "PillTracker является органайзером приёма лекарств и не заменяет профессиональную медицинскую консультацию, диагноз или скорую помощь."
    )
}

@Composable
private fun EnglishPolicyContent() {
    PolicyCardSection(
        number = "1",
        title = "Legal Basis & Global Compliance",
        body = "Established pursuant to Law of Ukraine 'On Protection of Personal Data' No. 2297-VI, European Union GDPR (Regulation 2016/679), and US Health & State Privacy Standards (HIPAA Privacy Principles / CCPA). Data is processed solely based on consent."
    )
    PolicyCardSection(
        number = "2",
        title = "Data Scope & End-to-End Encryption (AES-256)",
        body = "All personal health records, prescription schedules, and dosage notes are encrypted using AES-256-GCM ciphers both locally on-device and during encrypted cloud sync."
    )
    PolicyCardSection(
        number = "3",
        title = "Right to Erasure & 30-Day Grace Period (GDPR Art. 17)",
        body = "You hold full rights to request complete deletion of your account and data. Requests trigger a 30-day grace period during which deletion can be instantly canceled in app. After 30 days, records are irreversibly purged."
    )
    PolicyCardSection(
        number = "4",
        title = "Strict Confidentiality & Zero Data Sale",
        body = "Your personal and health information is strictly confidential. We never sell, monetize, or transfer your personal data to third parties or advertising networks."
    )
    PolicyCardSection(
        number = "5",
        title = "Medical Disclaimer",
        body = "PillTracker is an automated reminder organizer. It is not a clinical diagnostic system and does not replace licensed medical practitioners or emergency healthcare dispatch."
    )
}

@Composable
private fun PolicyCardSection(
    number: String,
    title: String,
    body: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

