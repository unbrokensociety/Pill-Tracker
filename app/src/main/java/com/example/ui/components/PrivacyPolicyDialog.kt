package com.example.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        modifier = Modifier.statusBarsPadding(),
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
                        tonalElevation = 8.dp,
                        shadowElevation = 12.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Check, null)
                                    Text(
                                        text = when (currentLang) {
                                            "uk" -> "Зрозуміло та Приймаю"
                                            "ru" -> "Понятно и Принимаю"
                                            else -> "I Understand & Accept"
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
                        .padding(horizontal = 20.dp, vertical = 16.dp),
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
                                    text = "LOCAL DATA PROTECTION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when (currentLang) {
                                        "uk" -> "Локальне збереження даних. Захист здоров'я за законами України (№ 2297-VI) та ЄС (EU GDPR)"
                                        "ru" -> "Локальное хранение данных. Защита здоровья по законам Украины (№ 2297-VI) и ЕС (EU GDPR)"
                                        else -> "Local offline data storage under Ukraine Law № 2297-VI & EU GDPR"
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

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun UkrainianPolicyContent() {
    PolicyCardSection(
        number = "1",
        title = "Локальне збереження даних",
        body = "Додаток PillTracker працює в повністю автономному (офлайн) режимі. Усі ваші персональні дані, назви ліків, розклад прийому та примітки зберігаються виключно на вашому пристрої у локальній базі даних. Реєстрація чи створення облікового запису не потрібні."
    )
    PolicyCardSection(
        number = "2",
        title = "Правова база та стандарти безпеки",
        body = "Обробка персональних даних відповідає Закону України «Про захист персональних даних» № 2297-VI та загальному регламенту про захист даних ЄС (EU GDPR). Ваші дані не передаються на сторонні сервери."
    )
    PolicyCardSection(
        number = "3",
        title = "Повний контроль та видалення даних",
        body = "Ви володієте повним контролем над своїми даними. Видалення додатка з пристрою або очищення його даних повністю й безповоротно видаляє всі ваші розклади та історії прийому."
    )
    PolicyCardSection(
        number = "4",
        title = "Конфіденційність",
        body = "Ми не збираємо, не відстежуємо і не продаємо ваші приватні медичні дані третім особам або рекламним мережам."
    )
    PolicyCardSection(
        number = "5",
        title = "Медичне застереження",
        body = "PillTracker є зручним особистим органайзером та нагадуванням. Додаток не ставить медичних діагнозів і не замінює консультацію кваліфікованого лікаря."
    )
}

@Composable
private fun RussianPolicyContent() {
    PolicyCardSection(
        number = "1",
        title = "Локальное хранение данных",
        body = "Приложение PillTracker работает полностью автономно (офлайн). Все ваши данные, названия лекарств и расписание хранятся исключительно на вашем устройстве в локальной базе данных. Регистрация не требуется."
    )
    PolicyCardSection(
        number = "2",
        title = "Правовая база и стандарты безопасности",
        body = "Обработка данных соответствует Закону Украины «О защите персональных данных» № 2297-VI и регламенту ЕС (EU GDPR). Ваши данные не передаются на сторонние серверы."
    )
    PolicyCardSection(
        number = "3",
        title = "Полный контроль и удаление данных",
        body = "Вы полностью контролируете свои данные. Удаление приложения или очистка его данных полностью удаляет всю историю и графики."
    )
    PolicyCardSection(
        number = "4",
        title = "Конфиденциальность",
        body = "Мы не собираем, не отслеживаем и не передаём ваши личные медицинские данные третьим лицам."
    )
    PolicyCardSection(
        number = "5",
        title = "Медицинская оговорка",
        body = "PillTracker является удобным органайзером и не заменяет консультацию профессионального врача."
    )
}

@Composable
private fun EnglishPolicyContent() {
    PolicyCardSection(
        number = "1",
        title = "Local Offline Data Storage",
        body = "PillTracker operates entirely offline. All health records, prescription schedules, and medication logs stay stored strictly on your device. No user registration or cloud account is required."
    )
    PolicyCardSection(
        number = "2",
        title = "Legal Compliance",
        body = "Data processing adheres to Law of Ukraine No. 2297-VI and EU GDPR principles. Your records are never transmitted to external cloud servers."
    )
    PolicyCardSection(
        number = "3",
        title = "Complete User Control",
        body = "You maintain 100% control over your stored records. Uninstalling the app or clearing data erases all logs instantly from the device."
    )
    PolicyCardSection(
        number = "4",
        title = "Strict Privacy",
        body = "We do not track, collect, or share your personal health data with third parties or advertising services."
    )
    PolicyCardSection(
        number = "5",
        title = "Medical Disclaimer",
        body = "PillTracker is a personal scheduling assistant and cannot substitute for professional physician judgment or medical diagnosis."
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


