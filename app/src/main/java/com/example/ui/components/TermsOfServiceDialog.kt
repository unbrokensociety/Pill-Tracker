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
            decorFitsSystemWindows = true
        )
    ) {
        Scaffold(
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
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
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
                                    "uk" -> "Офіційні правила використання сервісу PillTracker (Україна, ЄС, США)"
                                    "ru" -> "Официальные правила использования сервиса PillTracker (Украина, ЕС, США)"
                                    else -> "Official PillTracker Service Agreement (Ukraine, EU, USA)"
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

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun UkrainianTermsContent() {
    TermsCardSection(
        number = "1",
        title = "Предмет Угоди та Сфера Застосування",
        body = "Ця Угода регулює використання мобільного додатка PillTracker на території України, країн Європейського Союзу (ЄС) та США. Використовуючи додаток (як зареєстрований користувач чи в гостьовому режимі), ви беззастережно приймаєте ці умови."
    )
    TermsCardSection(
        number = "2",
        title = "Медичне застереження та Відповідальність",
        body = "PillTracker є електронним нагадуванням та інструментом ведення щоденника прийому ліків. Сервіс не є медичним пристроєм, не надає лікарських приписів чи медичних діагнозів. Ви та ваш лікуючий лікар несуть повну відповідальність за дозування та схему лікування."
    )
    TermsCardSection(
        number = "3",
        title = "Гостьовий режим та Хмарна синхронізація",
        body = "При використанні гостьового режиму ваші дані зберігаються суворо локально на вашому пристрої. При створенні облікового запису та увімкненні синхронізації дані захищаються шифруванням AES-256 та передаються через безпечне з'єднання SSL/TLS."
    )
    TermsCardSection(
        number = "4",
        title = "Припинення використання та Видалення даних",
        body = "Ви можете в будь-який момент видалити обліковий запис. Запит активації видалення діє 30 днів (відповідно до ст. 15 Закону України № 2297-VI та GDPR Art. 17). Протягом цього часу процедуру можна скасувати."
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
        body = "Настоящее Соглашение регулирует использование приложения PillTracker на территории Украины, стран ЕС и США. Используя приложение (включая гостевой режим), вы принимаете данные условия."
    )
    TermsCardSection(
        number = "2",
        title = "Медицинская оговорка и Ответственность",
        body = "PillTracker является цифровым органайзером и напоминанием о приёме лекарств. Сервис не является медицинским устройством и не ставит диагнозы. За правильность графика и дозировок отвечает пользователь и его лечащий врач."
    )
    TermsCardSection(
        number = "3",
        title = "Гостевой режим и Облачная синхронизация",
        body = "В гостевом режиме данные хранятся локально на устройстве. При регистрации и включении синхронизации данные зашифрованы стандартом AES-256 и передаются по защищённому каналу SSL/TLS."
    )
    TermsCardSection(
        number = "4",
        title = "Прекращение использования и Удаление аккаунта",
        body = "Вы можете запросить удаление аккаунта в любой момент. Запрос действует 30 дней (согласно ст. 15 Закона Украины № 2297-VI и GDPR Art. 17) и может быть отменён в течение этого периода."
    )
    TermsCardSection(
        number = "5",
        title = "Изменение Условий",
        body = "Мы оставляет за собой право обновлять данные Условия. Новая версия вступает в силу с момента публикации в приложении."
    )
}

@Composable
private fun EnglishTermsContent() {
    TermsCardSection(
        number = "1",
        title = "Scope of Agreement & Jurisdiction",
        body = "This Agreement governs the use of PillTracker across Ukraine, the European Union, and the United States. By using the application (registered or guest mode), you agree to these legal terms."
    )
    TermsCardSection(
        number = "2",
        title = "Medical Disclaimer & Limitation of Liability",
        body = "PillTracker is a digital reminder and health log tool. It is not a certified medical software, does not prescribe medication, and cannot substitute for professional physician judgment or emergency response."
    )
    TermsCardSection(
        number = "3",
        title = "Guest Mode & Cloud Synchronization",
        body = "In Guest Mode, records remain stored locally on device. Upon account creation and sync enablement, health records are encrypted using hardware AES-256-GCM and transferred securely over TLS."
    )
    TermsCardSection(
        number = "4",
        title = "Account Termination & Right to be Forgotten",
        body = "You may request complete account erasure anytime. A 30-day grace period is granted pursuant to Ukraine Law No. 2297-VI and EU GDPR Art. 17, during which deletion can be revoked in app."
    )
    TermsCardSection(
        number = "5",
        title = "Amendments to Terms",
        body = "We reserve the right to revise these Terms of Service. Revisions become binding upon publication within the application."
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
