package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Gavel
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
fun TermsOfServiceDialog(
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
                        title = {
                            Text(
                                text = when (currentLang) {
                                    "uk" -> "Умови Використання"
                                    "ru" -> "Условия Использования"
                                    else -> "Terms of Service"
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
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(16.dp)
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
                                        imageVector = Icons.Filled.Gavel,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "LEGAL SERVICE AGREEMENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when (currentLang) {
                                        "uk" -> "Офіційні правила використання сервісу PillTracker"
                                        "ru" -> "Официальные правила использования сервиса PillTracker"
                                        else -> "Official PillTracker Service Terms"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    when (currentLang) {
                        "uk" -> UkrainianTermsContent()
                        "ru" -> RussianTermsContent()
                        else -> EnglishTermsContent()
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun UkrainianTermsContent() {
    TermsCardSection(
        number = "1",
        title = "Предмет Угоди та Сфера Застосування",
        body = "Ця Угода регулює використання мобільного додатка PillTracker. Використовуючи додаток, ви беззастережно приймаєте ці умови. Додаток призначений для локального використання без необхідності реєстрації."
    )
    TermsCardSection(
        number = "2",
        title = "Медичне застереження та Відповідальність",
        body = "PillTracker є електронним нагадуванням та інструментом ведення щоденника прийому ліків. Сервіс не є медичним пристроєм, не надає лікарських приписів чи медичних діагнозів."
    )
    TermsCardSection(
        number = "3",
        title = "Локальний режим та збереження даних",
        body = "Ваші дані зберігаються суворо локально на вашому пристрої. Сервіс не вимагає передачі даних у хмару чи мережу Інтернет."
    )
    TermsCardSection(
        number = "4",
        title = "Видалення даних",
        body = "Ви можете в будь-який момент видалити всі дані, очистивши кеш додатка або видаливши його з пристрою."
    )
    TermsCardSection(
        number = "5",
        title = "Зміни до Умов",
        body = "Адміністрація залишає за собою право оновлювати ці Умови. Оновлена версія набуває чинності з моменту її публікації в додатку."
    )
}

@Composable
private fun RussianTermsContent() {
    TermsCardSection(
        number = "1",
        title = "Предмет Соглашения и Область Действия",
        body = "Настоящее Соглашение регулирует использование приложения PillTracker. Используя приложение, вы принимаете данные условия. Приложение предназначено для локального использования без регистрации."
    )
    TermsCardSection(
        number = "2",
        title = "Медицинская оговорка и Ответственность",
        body = "PillTracker является цифровым органайзером и напоминанием о приёме лекарств. Сервис не является медицинским устройством и не ставит диагнозы."
    )
    TermsCardSection(
        number = "3",
        title = "Локальный режим хранения данных",
        body = "Все ваши данные хранятся исключительно локально на вашем устройстве."
    )
    TermsCardSection(
        number = "4",
        title = "Удаление данных",
        body = "Вы можете в любой момент удалить данные, очистив память приложения или удалив его."
    )
    TermsCardSection(
        number = "5",
        title = "Изменение Условий",
        body = "Мы оставляем за собой право обновлять данные Условия. Новая версия вступает в силу с момента публикации."
    )
}

@Composable
private fun EnglishTermsContent() {
    TermsCardSection(
        number = "1",
        title = "Scope of Agreement",
        body = "This Agreement governs the use of PillTracker. By using the application, you agree to these legal terms. The application operates locally without registration."
    )
    TermsCardSection(
        number = "2",
        title = "Medical Disclaimer",
        body = "PillTracker is a digital reminder and health log tool. It is not a medical device, does not prescribe medication, and cannot substitute for professional physician judgment."
    )
    TermsCardSection(
        number = "3",
        title = "Local Data Storage",
        body = "All medication schedules and records stay stored locally on your device."
    )
    TermsCardSection(
        number = "4",
        title = "Data Erasure",
        body = "You may erase all application data at any time by clearing application storage or uninstalling the app."
    )
    TermsCardSection(
        number = "5",
        title = "Amendments to Terms",
        body = "We reserve the right to revise these Terms of Service upon publishing inside the application."
    )
}

@Composable
private fun TermsCardSection(
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
