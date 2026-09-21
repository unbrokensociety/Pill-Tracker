package com.aistudio.meditracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/*
 * Условия использования v2 (v2.4.0).
 *
 *  • Тот же фикс «кнопка уезжает под экран»: платформенный диалог,
 *    скролл в середине, кнопка приклеена к низу карточки, высота ≤ 90%
 *    экрана — кнопка видна всегда, на любом устройстве.
 *  • Текст умнее и честнее: условия теперь описывают и встроенный
 *    апдейтер (обновления из GitHub, разовое разрешение установки),
 *    и то, что сервис бесплатный и персональный.
 */

@Composable
fun TermsOfServiceDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lang = context.resources.configuration.locales[0].language

    val maxDialogHeight = (LocalConfiguration.current.screenHeightDp * 0.9f).dp

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxDialogHeight),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                /* ── Шапка ── */
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassCircleIcon(
                        size = 40.dp,
                        tintColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Gavel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = when (lang) {
                            "uk" -> "Умови використання"
                            "ru" -> "Условия использования"
                            else -> "Terms of Service"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = when (lang) {
                                "uk" -> "Закрити"
                                "ru" -> "Закрыть"
                                else -> "Close"
                            },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                /* ── Середина: скролл ── */
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = when (lang) {
                                "uk" -> "Pill Tracker — безкоштовний особистий органайзер для ліків. Використовуючи застосунок, ви погоджуєтесь з цими умовами. Оновлення: вересень 2026."
                                "ru" -> "Pill Tracker — бесплатный личный органайзер для лекарств. Используя приложение, вы соглашаетесь с этими условиями. Обновление: сентябрь 2026."
                                else -> "Pill Tracker is a free personal medication organizer. By using the app you agree to these terms. Updated: September 2026."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (lang) {
                        "uk" -> UkrainianTermsContent()
                        "ru" -> RussianTermsContent()
                        else -> EnglishTermsContent()
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                /* ── Низ: кнопка всегда на экране ── */
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = when (lang) {
                                "uk" -> "Зрозуміло та приймаю"
                                "ru" -> "Понятно и принимаю"
                                else -> "I understand & accept"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UkrainianTermsContent() {
    TermsCardSection(
        number = "1",
        title = "Предмет угоди",
        body = "Ці умови регулюють використання мобільного застосунку Pill Tracker — безкоштовного особистого органайзера для нагадувань про прийом ліків. Застосунок призначений для особистого некомерційного використання."
    )
    TermsCardSection(
        number = "2",
        title = "Медичне застереження",
        body = "Pill Tracker не є медичним виробом, не ставить діагнозів, не призначає й не скасовує лікування. Нагадування не замінюють призначення лікаря: дозування та графік прийому узгоджуйте з фахівцем. Відповідальність за фактичний прийом препаратів залишається на вас."
    )
    TermsCardSection(
        number = "3",
        title = "Дані та приватність",
        body = "Всі дані зберігаються локально на пристрої. Деталі — в Політиці конфіденційності (Налаштування → Політика конфіденційності). Використовуючи застосунок, ви погоджуєтесь і з нею."
    )
    TermsCardSection(
        number = "4",
        title = "Оновлення застосунку",
        body = "Оновлення розповсюджуються через GitHub Releases. Перевірка й завантаження виконуються на ваш запит або автоматично не частіше ніж раз на 3 години; жодні ваші дані при цьому не надсилаються. Встановлення потребує одноразового дозволу Android «встановлювати з цього джерела» — це вимога системи, а не наша примха."
    )
    TermsCardSection(
        number = "5",
        title = "Обмеження відповідальності",
        body = "Застосунок надається «як є», без гарантій безперебійної роботи. Ми не відповідаємо за пропущений прийом, наслідки прийому чи рішення, ухвалені на основі даних застосунку. Функції залежать від системи: виробники пристроїв можуть обмежувати точні будильники чи сповіщення."
    )
    TermsCardSection(
        number = "6",
        title = "Припинення використання",
        body = "Ви можете будь-коли припинити користування: видаліть застосунок — разом з ним зникнуть усі збережені дані, і жодних дій від нас не потрібно."
    )
    TermsCardSection(
        number = "7",
        title = "Зміни умов",
        body = "Оновлена редакція набирає чинності з моменту публікації в застосунку. Суттєві зміни описуватимуться в примітках до оновлення."
    )
}

@Composable
private fun RussianTermsContent() {
    TermsCardSection(
        number = "1",
        title = "Предмет соглашения",
        body = "Настоящие условия регулируют использование мобильного приложения Pill Tracker — бесплатного личного органайзера для напоминаний о приёме лекарств. Приложение предназначено для личного некоммерческого использования."
    )
    TermsCardSection(
        number = "2",
        title = "Медицинская оговорка",
        body = "Pill Tracker не является медицинским изделием, не ставит диагнозов и не назначает лечение. Напоминания не заменяют назначений врача: дозировку и график приёма согласовывайте со специалистом. Ответственность за фактический приём препаратов остаётся на вас."
    )
    TermsCardSection(
        number = "3",
        title = "Данные и приватность",
        body = "Все данные хранятся локально на устройстве. Подробности — в Политике конфиденциальности (Настройки → Политика конфиденциальности). Используя приложение, вы соглашаетесь и с ней."
    )
    TermsCardSection(
        number = "4",
        title = "Обновления приложения",
        body = "Обновления распространяются через GitHub Releases. Проверка и загрузка выполняются по вашему запросу или автоматически не чаще раза в 3 часа; ваши данные при этом не отправляются. Установка требует разового разрешения Android «устанавливать из этого источника» — это требование системы, а не наша прихоть."
    )
    TermsCardSection(
        number = "5",
        title = "Ограничение ответственности",
        body = "Приложение предоставляется «как есть», без гарантий бесперебойной работы. Мы не отвечаем за пропущенный приём, последствия приёма или решения, принятые на основе данных приложения. Функции зависят от системы: производители устройств могут ограничивать точные будильники и уведомления."
    )
    TermsCardSection(
        number = "6",
        title = "Прекращение использования",
        body = "Вы можете в любой момент прекратить использование: удалите приложение — вместе с ним исчезнут все сохранённые данные, никаких действий с нашей стороны не требуется."
    )
    TermsCardSection(
        number = "7",
        title = "Изменение условий",
        body = "Обновлённая редакция вступает в силу с момента публикации в приложении. Существенные изменения будут описываться в примечаниях к обновлению."
    )
}

@Composable
private fun EnglishTermsContent() {
    TermsCardSection(
        number = "1",
        title = "Scope of the agreement",
        body = "These terms govern the use of the Pill Tracker mobile app — a free personal medication reminder organizer. The app is intended for personal, non-commercial use."
    )
    TermsCardSection(
        number = "2",
        title = "Medical disclaimer",
        body = "Pill Tracker is not a medical device: it does not diagnose, prescribe or cancel treatment. Reminders never replace a physician’s orders — confirm dosage and schedule with a professional. Responsibility for actually taking your medication remains with you."
    )
    TermsCardSection(
        number = "3",
        title = "Data and privacy",
        body = "All data is stored locally on the device. Details are in the Privacy Policy (Settings → Privacy Policy). By using the app you agree to it as well."
    )
    TermsCardSection(
        number = "4",
        title = "App updates",
        body = "Updates are distributed via GitHub Releases. Checks and downloads happen on your request or automatically at most once every 3 hours; none of your data is ever uploaded. Installation requires the one-time Android permission “install from this source” — that is a system requirement, not our choice."
    )
    TermsCardSection(
        number = "5",
        title = "Limitation of liability",
        body = "The app is provided “as is”, without warranties of uninterrupted operation. We are not liable for missed intakes, consequences of taking medication, or decisions based on app data. Features depend on the system: device vendors may restrict exact alarms and notifications."
    )
    TermsCardSection(
        number = "6",
        title = "Ending your use",
        body = "You may stop using the app at any moment: uninstall it and all stored data disappears with it — no action on our side is needed."
    )
    TermsCardSection(
        number = "7",
        title = "Changes to the terms",
        body = "An updated revision takes effect once published inside the app. Significant changes will be described in the update notes."
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
    Spacer(modifier = Modifier.height(12.dp))
}
